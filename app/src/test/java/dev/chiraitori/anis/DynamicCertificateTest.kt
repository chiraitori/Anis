package dev.chiraitori.anis

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class DynamicCertificateTest {

    @Test
    fun testGenerateDynamicLeafCertificate() {
        val keyGen = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048, SecureRandom())
        }
        val caKeyPair = keyGen.generateKeyPair()
        val caIssuer = X500Name("CN=Anis Root CA, O=Anis Guard")

        // Create CA cert
        val caCertBuilder = JcaX509v3CertificateBuilder(
            caIssuer,
            BigInteger.ONE,
            Date(System.currentTimeMillis() - 10000),
            Date(System.currentTimeMillis() + 10000000),
            caIssuer,
            caKeyPair.public
        ).apply {
            addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
        }
        val caSigner = JcaContentSignerBuilder("SHA256withRSA").build(caKeyPair.private)
        val caCert = JcaX509CertificateConverter().getCertificate(caCertBuilder.build(caSigner))

        // Generate Leaf Cert for example.com
        val leafKeyPair = keyGen.generateKeyPair()
        val targetHost = "example.com"
        val leafSubject = X500Name("CN=$targetHost, O=Anis Intercepted Session")

        val leafCertBuilder = JcaX509v3CertificateBuilder(
            caIssuer,
            BigInteger.valueOf(2),
            Date(System.currentTimeMillis() - 10000),
            Date(System.currentTimeMillis() + 10000000),
            leafSubject,
            leafKeyPair.public
        ).apply {
            addExtension(Extension.basicConstraints, false, BasicConstraints(false))
            addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment))
            addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_serverAuth)))
            val san = GeneralNames(arrayOf(
                GeneralName(GeneralName.dNSName, targetHost),
                GeneralName(GeneralName.dNSName, "*.$targetHost")
            ))
            addExtension(Extension.subjectAlternativeName, false, san)
        }

        val leafCert = JcaX509CertificateConverter().getCertificate(leafCertBuilder.build(caSigner))

        assertNotNull(leafCert)
        assertEquals(leafCert.basicConstraints, -1) // -1 signifies end-entity (isCA = false)
        assertTrue(leafCert.issuerX500Principal.name.contains("CN=Anis Root CA"))
        assertTrue(leafCert.subjectX500Principal.name.contains("CN=example.com"))

        // Verify Leaf Signature with CA Public Key
        leafCert.verify(caKeyPair.public)

        // Verify KeyStore & SSLContext initialization
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("key", leafKeyPair.private, "password".toCharArray(), arrayOf(leafCert, caCert))
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "password".toCharArray())
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, null, SecureRandom())
        }
        assertNotNull(sslContext)
        assertNotNull(sslContext.socketFactory)
    }
}
