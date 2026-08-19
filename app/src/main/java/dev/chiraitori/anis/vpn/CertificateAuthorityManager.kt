package dev.chiraitori.anis.vpn

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

class CertificateAuthorityManager(private val context: Context) {

    private val caDir = File(context.filesDir, "ca").apply {
        if (!exists()) mkdirs()
    }

    private val certFile = File(caDir, "anis_root_ca.crt")
    private val keyFile = File(caDir, "anis_root_ca.key")

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
     * Auto-generates a self-signed X.509 v3 Root CA Certificate.
     */
    private fun generateAndSaveRootCa(): String {
        try {
            // 1. Generate RSA 2048 KeyPair
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048, SecureRandom())
            val keyPair: KeyPair = keyGen.generateKeyPair()

            // 2. Certificate validity: 10 years
            val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L) // Yesterday
            val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000L) // 10 years

            val serialNumber = BigInteger(64, SecureRandom())
            val issuerName = X500Name("CN=Anis HTTPS & AdBlock CA, O=Anis Guard, OU=Security Engine, C=US")

            val certBuilder = JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                notBefore,
                notAfter,
                issuerName, // Self-signed
                keyPair.public
            )

            // Critical CA constraint: isCA = true
            certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true)
            )

            // Key Usage: certSign + cRLSign
            certBuilder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign or KeyUsage.digitalSignature)
            )

            // 3. Sign certificate using SHA256withRSA
            val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
            val holder = certBuilder.build(signer)
            val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(holder)

            // 4. Encode to PEM format
            val certPem = buildString {
                append("-----BEGIN CERTIFICATE-----\n")
                append(android.util.Base64.encodeToString(cert.encoded, android.util.Base64.DEFAULT))
                append("-----END CERTIFICATE-----\n")
            }

            val keyPem = buildString {
                append("-----BEGIN PRIVATE KEY-----\n")
                append(android.util.Base64.encodeToString(keyPair.private.encoded, android.util.Base64.DEFAULT))
                append("-----END PRIVATE KEY-----\n")
            }

            certFile.writeText(certPem)
            keyFile.writeText(keyPem)

            Log.i("CAManager", "Successfully generated and saved Anis Root CA Certificate")
            return certPem
        } catch (e: Exception) {
            Log.e("CAManager", "Failed to generate Root CA", e)
            throw e
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
