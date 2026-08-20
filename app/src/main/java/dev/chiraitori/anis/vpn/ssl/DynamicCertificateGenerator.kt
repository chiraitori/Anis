package dev.chiraitori.anis.vpn.ssl

import android.util.Log
import android.util.LruCache
import dev.chiraitori.anis.vpn.CertificateAuthorityManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

class DynamicCertificateGenerator(private val caManager: CertificateAuthorityManager) {

    private val sslContextCache = LruCache<String, SSLContext>(128)
    private val keyGen = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048, SecureRandom())
    }

    /**
     * Gets or generates an SSLContext configured with a dynamically forged X.509 certificate
     * for the given target domain, signed by the Anis Root CA.
     */
    @Synchronized
    fun getOrCreateSslContext(hostname: String): SSLContext {
        val cleanHost = hostname.lowercase().trim()
        sslContextCache.get(cleanHost)?.let { return it }

        val sslContext = generateSslContextForHost(cleanHost)
        sslContextCache.put(cleanHost, sslContext)
        return sslContext
    }

    private fun generateSslContextForHost(hostname: String): SSLContext {
        val rootCert = caManager.getRootCaCertificate()
        val rootKey = caManager.getRootCaPrivateKey()

        // 1. Generate leaf keypair
        val leafKeyPair: KeyPair = keyGen.generateKeyPair()

        // 2. Certificate validity: 1 year (from yesterday to next year)
        val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val notAfter = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000L)
        val serialNumber = BigInteger(64, SecureRandom())

        val issuerName = X500Name.getInstance(rootCert.subjectX500Principal.encoded)
        val subjectName = X500Name("CN=$hostname, O=Anis Intercepted Session, OU=Security Engine")

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBefore,
            notAfter,
            subjectName,
            leafKeyPair.public
        )

        // End-entity constraint: isCA = false
        certBuilder.addExtension(
            Extension.basicConstraints,
            false,
            BasicConstraints(false)
        )

        // Key usage for TLS server
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        )

        // Extended key usage: serverAuth + clientAuth
        certBuilder.addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth))
        )

        // Subject Alternative Name (SAN)
        val sanType = if (isIpAddress(hostname)) GeneralName.iPAddress else GeneralName.dNSName
        val generalNames = GeneralNames(arrayOf(
            GeneralName(sanType, hostname),
            // Also include wildcard if subdomain
            if (sanType == GeneralName.dNSName && !hostname.startsWith("*.") && hostname.contains(".")) {
                GeneralName(GeneralName.dNSName, "*.$hostname")
            } else null
        ).filterNotNull().toTypedArray())

        certBuilder.addExtension(Extension.subjectAlternativeName, false, generalNames)

        // 3. Sign using Root CA Private Key
        val signatureAlgorithm = when (rootKey.algorithm.uppercase()) {
            "EC", "ECDSA" -> "SHA256withECDSA"
            else -> "SHA256withRSA"
        }
        val signer = JcaContentSignerBuilder(signatureAlgorithm).build(rootKey)
        val holder = certBuilder.build(signer)
        val leafCert: X509Certificate = JcaX509CertificateConverter().getCertificate(holder)

        // 4. Create KeyStore with [leafCert, rootCert]
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(
                "key",
                leafKeyPair.private,
                "password".toCharArray(),
                arrayOf(leafCert, rootCert)
            )
        }

        // 5. Initialize KeyManagerFactory and SSLContext
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "password".toCharArray())
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, null, SecureRandom())
        }

        return sslContext
    }

    private fun isIpAddress(host: String): Boolean {
        return host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) || host.contains(":")
    }

    companion object {
        private const val TAG = "DynamicCertGen"
    }
}
