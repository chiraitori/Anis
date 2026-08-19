package dev.chiraitori.anis

import dev.chiraitori.anis.vpn.DnsPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class DnsEngineTest {

    @Test
    fun testBuildIpResponse() {
        val stream = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12)
        header.putShort(0x9ABC.toShort())
        header.putShort(0x0100.toShort())
        header.putShort(1.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        stream.write(header.array())

        for (label in listOf("google", "com")) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            stream.write(bytes.size)
            stream.write(bytes)
        }
        stream.write(0)

        val qFooter = ByteBuffer.allocate(4)
        qFooter.putShort(1.toShort()) // TYPE A
        qFooter.putShort(1.toShort()) // CLASS IN
        stream.write(qFooter.array())

        val parsed = DnsPacket.parse(stream.toByteArray())
        assertNotNull(parsed)
        assertEquals("google.com", parsed!!.qname)

        // Rewrite to SafeSearch IP (216.239.38.120)
        val ipResponse = DnsPacket.buildIpResponse(parsed, "216.239.38.120")
        val responseParsed = DnsPacket.parse(ipResponse)

        assertNotNull(responseParsed)
        assertTrue(responseParsed!!.isResponse)
        assertEquals(0, responseParsed.rcode) // NOERROR
        assertEquals(0x9ABC.toShort(), responseParsed.transactionId)
    }
}
