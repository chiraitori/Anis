package dev.chiraitori.anis

import dev.chiraitori.anis.vpn.DnsPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class DnsPacketTest {

    @Test
    fun testParseAndBuildBlockResponse() {
        // Construct standard DNS query for "ads.google.com" (TYPE A)
        val stream = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12)
        header.putShort(0x1234.toShort()) // Transaction ID
        header.putShort(0x0100.toShort()) // Standard query, RD=1
        header.putShort(1.toShort()) // QDCOUNT = 1
        header.putShort(0.toShort()) // ANCOUNT = 0
        header.putShort(0.toShort()) // NSCOUNT = 0
        header.putShort(0.toShort()) // ARCOUNT = 0
        stream.write(header.array())

        // QNAME: ads.google.com
        for (label in listOf("ads", "google", "com")) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            stream.write(bytes.size)
            stream.write(bytes)
        }
        stream.write(0)

        // QTYPE = 1 (A), QCLASS = 1 (IN)
        val qFooter = ByteBuffer.allocate(4)
        qFooter.putShort(1.toShort())
        qFooter.putShort(1.toShort())
        stream.write(qFooter.array())

        val rawQuery = stream.toByteArray()
        val parsed = DnsPacket.parse(rawQuery)

        assertNotNull(parsed)
        assertEquals(0x1234.toShort(), parsed!!.transactionId)
        assertEquals("ads.google.com", parsed.qname)
        assertEquals(DnsPacket.TYPE_A, parsed.qtype)

        // Build Block Response
        val blockResponseBytes = DnsPacket.buildBlockResponse(parsed)
        val responseParsed = DnsPacket.parse(blockResponseBytes)

        assertNotNull(responseParsed)
        assertEquals(0x1234.toShort(), responseParsed!!.transactionId)
        assertTrue(responseParsed.isResponse)
        assertEquals("ads.google.com", responseParsed.qname)
    }

    @Test
    fun testParseAndBuildNxDomainResponse() {
        val stream = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12)
        header.putShort(0x5678.toShort())
        header.putShort(0x0100.toShort())
        header.putShort(1.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        header.putShort(0.toShort())
        stream.write(header.array())

        for (label in listOf("tracker", "example", "org")) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            stream.write(bytes.size)
            stream.write(bytes)
        }
        stream.write(0)

        val qFooter = ByteBuffer.allocate(4)
        qFooter.putShort(28.toShort()) // AAAA
        qFooter.putShort(1.toShort())
        stream.write(qFooter.array())

        val parsed = DnsPacket.parse(stream.toByteArray())
        assertNotNull(parsed)
        assertEquals("tracker.example.org", parsed!!.qname)
        assertEquals(DnsPacket.TYPE_AAAA, parsed.qtype)

        val nxResponse = DnsPacket.buildNxDomainResponse(parsed)
        val nxParsed = DnsPacket.parse(nxResponse)
        assertNotNull(nxParsed)
        assertTrue(nxParsed!!.isResponse)
        assertEquals(3, nxParsed.rcode) // NXDOMAIN
    }
}
