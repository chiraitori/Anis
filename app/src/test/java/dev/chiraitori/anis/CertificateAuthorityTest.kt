package dev.chiraitori.anis

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

class CertificateAuthorityTest {

    @Test
    fun testGenerateRootCaCertificate() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        val keyPair: KeyPair = keyGen.generateKeyPair()

        val notBefore = Date(System.currentTimeMillis() - 1000L * 60)
        val notAfter = Date(System.currentTimeMillis() + 10L * 365 * 24 * 60 * 60 * 1000L)
        val serialNumber = BigInteger(64, SecureRandom())
        val issuerName = X500Name("CN=Anis HTTPS & AdBlock CA, O=Anis Guard, OU=Security Engine, C=US")

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBefore,
            notAfter,
            issuerName,
            keyPair.public
        )

        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(true)
        )

        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(holder)

        assertNotNull(cert)
        assertTrue(cert.subjectX500Principal.name.contains("CN=Anis HTTPS & AdBlock CA"))
        assertTrue(cert.basicConstraints != -1) // is CA
        cert.verify(keyPair.public) // Verify self-signed signature
    }
}
