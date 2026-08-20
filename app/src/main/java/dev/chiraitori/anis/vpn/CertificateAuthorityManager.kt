package dev.chiraitori.anis.vpn

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import tunnel.Tunnel

class CertificateAuthorityManager(private val context: Context) {

    private val caDir = File(context.filesDir, "ca").apply {
        if (!exists()) mkdirs()
    }

    // These filenames intentionally match the Go tunnel's persistent CA store.
    private val certFile = File(caDir, "ca.crt")
    private val keyFile = File(caDir, "ca.key")

    /**
     * Retrieves or auto-generates the local Root CA certificate PEM string.
     */
    fun getOrCreateCaCertificatePem(): String {
        if (certFile.exists() && certFile.length() > 0) {
            return certFile.readText()
        }

        return generateAndSaveRootCa()
    }

    /**
     * Retrieves the Root CA X509Certificate instance.
     */
    fun getRootCaCertificate(): X509Certificate {
        val pem = getOrCreateCaCertificatePem()
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(
            ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8))
        ) as X509Certificate
    }

    /** Retrieves the Go tunnel's ECDSA CA key for the legacy root proxy. */
    fun getRootCaPrivateKey(): java.security.PrivateKey {
        if (!keyFile.exists() || keyFile.length() == 0L) {
            generateAndSaveRootCa()
        }
        val parsed = PEMParser(StringReader(keyFile.readText())).use { it.readObject() }
        val converter = JcaPEMKeyConverter()
        return when (parsed) {
            is PEMKeyPair -> converter.getKeyPair(parsed).private
            is PrivateKeyInfo -> converter.getPrivateKey(parsed)
            else -> error("Unsupported Anis CA private-key format")
        }
    }

    /** Generates the same persistent ECDSA CA used by the Go MITM engine. */
    private fun generateAndSaveRootCa(): String {
        return try {
            Tunnel.newCertManager(caDir.absolutePath).getCACertPEM().also {
                Log.i("CAManager", "Generated the shared Go/Kotlin Root CA")
            }
        } catch (error: Exception) {
            Log.e("CAManager", "Failed to generate Root CA", error)
            throw error
        }
    }

    /**
     * Checks whether the Anis Root CA is installed in the Android User Certificate Trust Store.
     */
    fun isCaInstalledInTrustStore(): Boolean {
        return try {
            val pem = getOrCreateCaCertificatePem()
            val certFactory = CertificateFactory.getInstance("X.509")
            val ourCert = certFactory.generateCertificate(
                ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8))
            ) as X509Certificate
            val ourSubject = ourCert.subjectX500Principal

            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null)

            for (alias in keyStore.aliases()) {
                if (!alias.startsWith("user:")) continue
                val cert = keyStore.getCertificate(alias) as? X509Certificate ?: continue
                if (cert.subjectX500Principal == ourSubject) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w("CAManager", "Error querying AndroidCAStore: ${e.message}")
            false
        }
    }

    /**
     * Exports the CA Certificate to the device's Downloads directory.
     */
    fun exportCaToDownloads(): Result<String> {
        return try {
            val pem = getOrCreateCaCertificatePem()
            val fileName = "Anis-RootCA.crt"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                // Delete old file if present
                resolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(fileName)
                )

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/x-x509-ca-cert")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Could not create MediaStore entry in Downloads"))

                resolver.openOutputStream(uri)?.use { out ->
                    out.write(pem.toByteArray(Charsets.UTF_8))
                } ?: return Result.failure(Exception("Failed to open output stream"))

                Result.success("Saved '$fileName' to Downloads")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(pem)
                Result.success("Saved '$fileName' to Downloads")
            }
        } catch (e: Exception) {
            Log.e("CAManager", "Failed to export CA cert", e)
            Result.failure(e)
        }
    }

    /**
     * Intent to open Android Security & Privacy Settings for Certificate Installation.
     */
    fun createInstallCertIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent("android.settings.SECURITY_SETTINGS")
        } else {
            Intent("android.credentials.INSTALL").apply {
                type = "application/x-x509-ca-cert"
            }
        }
    }

    companion object {
        @Volatile
        private var instance: CertificateAuthorityManager? = null

        fun getInstance(context: Context): CertificateAuthorityManager {
            return instance ?: synchronized(this) {
                instance ?: CertificateAuthorityManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
