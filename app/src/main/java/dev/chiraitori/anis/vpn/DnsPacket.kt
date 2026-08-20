package dev.chiraitori.anis.vpn

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class DnsPacket(
    val transactionId: Short,
    val isResponse: Boolean,
    val opcode: Int,
    val rcode: Int,
    val qname: String,
    val qtype: Int,
    val qclass: Int,
    val rawBytes: ByteArray
) {
    companion object {
        const val TYPE_A = 1
        const val TYPE_NS = 2
        const val TYPE_CNAME = 5
        const val TYPE_SOA = 6
        const val TYPE_PTR = 12
        const val TYPE_MX = 15
        const val TYPE_TXT = 16
        const val TYPE_AAAA = 28
        const val TYPE_HTTPS = 65
        const val TYPE_ANY = 255

        fun getTypeName(type: Int): String {
            return when (type) {
                TYPE_A -> "A"
                TYPE_AAAA -> "AAAA"
                TYPE_CNAME -> "CNAME"
                TYPE_HTTPS -> "HTTPS"
                TYPE_TXT -> "TXT"
                TYPE_MX -> "MX"
                TYPE_NS -> "NS"
                TYPE_SOA -> "SOA"
                TYPE_PTR -> "PTR"
                else -> "TYPE($type)"
            }
        }

        fun parse(payload: ByteArray): DnsPacket? {
            if (payload.size < 12) return null

            try {
                val buffer = ByteBuffer.wrap(payload)
                val transactionId = buffer.short
                val flags = buffer.short.toInt() and 0xFFFF

                val isResponse = (flags and 0x8000) != 0
                val opcode = (flags ushr 11) and 0x0F
                val rcode = flags and 0x0F

                val qdCount = buffer.short.toInt() and 0xFFFF
                val anCount = buffer.short.toInt() and 0xFFFF
                val nsCount = buffer.short.toInt() and 0xFFFF
                val arCount = buffer.short.toInt() and 0xFFFF

                if (qdCount < 1) return null

                // Parse QNAME
                val nameBuilder = StringBuilder()
                var length = buffer.get().toInt() and 0xFF

                while (length > 0) {
                    if ((length and 0xC0) == 0xC0) {
                        val offset = ((length and 0x3F) shl 8) or (buffer.get().toInt() and 0xFF)
                        break
                    }
                    if (buffer.remaining() < length) return null

                    val label = ByteArray(length)
                    buffer.get(label)
                    if (nameBuilder.isNotEmpty()) nameBuilder.append(".")
                    nameBuilder.append(String(label, Charsets.US_ASCII))

                    if (!buffer.hasRemaining()) break
                    length = buffer.get().toInt() and 0xFF
                }

                if (buffer.remaining() < 4) return null
                val qtype = buffer.short.toInt() and 0xFFFF
                val qclass = buffer.short.toInt() and 0xFFFF

                return DnsPacket(
                    transactionId = transactionId,
                    isResponse = isResponse,
                    opcode = opcode,
                    rcode = rcode,
                    qname = nameBuilder.toString().lowercase(),
                    qtype = qtype,
                    qclass = qclass,
                    rawBytes = payload
                )
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * Builds a synthetic DNS Response blocking the domain (returns 0.0.0.0 for A or :: for AAAA)
         */
        fun buildBlockResponse(query: DnsPacket): ByteArray {
            val stream = ByteArrayOutputStream()

            // Header (12 bytes)
            val header = ByteBuffer.allocate(12)
            header.putShort(query.transactionId)
            header.putShort(0x8580.toShort())
            header.putShort(1.toShort()) // QDCOUNT = 1
            header.putShort(1.toShort()) // ANCOUNT = 1
            header.putShort(0.toShort()) // NSCOUNT = 0
            header.putShort(0.toShort()) // ARCOUNT = 0
            stream.write(header.array())

            // Question Section (encode domain labels)
            val labels = query.qname.split(".")
            for (label in labels) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                stream.write(bytes.size)
                stream.write(bytes)
            }
            stream.write(0) // End of domain

            val qFooter = ByteBuffer.allocate(4)
            qFooter.putShort(query.qtype.toShort())
            qFooter.putShort(query.qclass.toShort())
            stream.write(qFooter.array())

            // Answer RR
            val answer = ByteBuffer.allocate(if (query.qtype == TYPE_AAAA) 28 else 16)
            answer.putShort(0xC00C.toShort()) // Pointer to QNAME at offset 12
            answer.putShort(query.qtype.toShort()) // TYPE
            answer.putShort(1.toShort()) // CLASS IN
            answer.putInt(300) // TTL 300s

            if (query.qtype == TYPE_AAAA) {
                answer.putShort(16.toShort()) // RDLENGTH 16 bytes
                answer.put(ByteArray(16)) // ::
            } else {
                answer.putShort(4.toShort()) // RDLENGTH 4 bytes
                answer.put(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0
            }

            stream.write(answer.array())

            return stream.toByteArray()
        }

        /**
         * Builds an NXDOMAIN response
         */
        fun buildNxDomainResponse(query: DnsPacket): ByteArray {
            val stream = ByteArrayOutputStream()

            // Header (12 bytes)
            val header = ByteBuffer.allocate(12)
            header.putShort(query.transactionId)
            header.putShort(0x8483.toShort())
            header.putShort(1.toShort()) // QDCOUNT = 1
            header.putShort(0.toShort()) // ANCOUNT = 0
            header.putShort(0.toShort()) // NSCOUNT = 0
            header.putShort(0.toShort()) // ARCOUNT = 0
            stream.write(header.array())

            // Echo Question
            val labels = query.qname.split(".")
            for (label in labels) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                stream.write(bytes.size)
                stream.write(bytes)
            }
            stream.write(0)

            val qFooter = ByteBuffer.allocate(4)
            qFooter.putShort(query.qtype.toShort())
            qFooter.putShort(query.qclass.toShort())
            stream.write(qFooter.array())

            return stream.toByteArray()
        }

        /**
         * Builds a REFUSED DNS error response (RCODE = 5)
         */
        fun buildRefusedResponse(query: DnsPacket): ByteArray {
            val stream = ByteArrayOutputStream()

            // Header (12 bytes)
            val header = ByteBuffer.allocate(12)
            header.putShort(query.transactionId)
            header.putShort(0x8485.toShort()) // QR=1, AA=1, RA=1, RCODE=5 (Refused)
            header.putShort(1.toShort()) // QDCOUNT = 1
            header.putShort(0.toShort()) // ANCOUNT = 0
            header.putShort(0.toShort()) // NSCOUNT = 0
            header.putShort(0.toShort()) // ARCOUNT = 0
            stream.write(header.array())

            // Echo Question
            val labels = query.qname.split(".")
            for (label in labels) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                stream.write(bytes.size)
                stream.write(bytes)
            }
            stream.write(0)

            val qFooter = ByteBuffer.allocate(4)
            qFooter.putShort(query.qtype.toShort())
            qFooter.putShort(query.qclass.toShort())
            stream.write(qFooter.array())

            return stream.toByteArray()
        }

        /**
         * Dispatches synthetic block response based on user DnsResponseType setting.
         */
        fun buildConfiguredBlockResponse(
            query: DnsPacket,
            responseType: dev.chiraitori.anis.data.model.DnsResponseType
        ): ByteArray {
            return when (responseType) {
                dev.chiraitori.anis.data.model.DnsResponseType.ZERO_IP -> buildBlockResponse(query)
                dev.chiraitori.anis.data.model.DnsResponseType.NXDOMAIN -> buildNxDomainResponse(query)
                dev.chiraitori.anis.data.model.DnsResponseType.REFUSED -> buildRefusedResponse(query)
            }
        }

        /**
         * Builds a synthetic DNS response rewriting the query to a target IPv4 address
         */
        fun buildIpResponse(query: DnsPacket, ipAddress: String): ByteArray {
            val stream = ByteArrayOutputStream()

            // Header (12 bytes)
            val header = ByteBuffer.allocate(12)
            header.putShort(query.transactionId)
            header.putShort(0x8580.toShort()) // Standard response, No error
            header.putShort(1.toShort()) // QDCOUNT = 1
            header.putShort(1.toShort()) // ANCOUNT = 1
            header.putShort(0.toShort()) // NSCOUNT = 0
            header.putShort(0.toShort()) // ARCOUNT = 0
            stream.write(header.array())

            // Question Section
            val labels = query.qname.split(".")
            for (label in labels) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                stream.write(bytes.size)
                stream.write(bytes)
            }
            stream.write(0)

            val qFooter = ByteBuffer.allocate(4)
            qFooter.putShort(1.toShort()) // TYPE A
            qFooter.putShort(query.qclass.toShort())
            stream.write(qFooter.array())

            // Answer RR
            val answer = ByteBuffer.allocate(16)
            answer.putShort(0xC00C.toShort()) // Pointer to QNAME
            answer.putShort(1.toShort()) // TYPE A
            answer.putShort(1.toShort()) // CLASS IN
            answer.putInt(300) // TTL
            answer.putShort(4.toShort()) // RDLENGTH

            val ipParts = ipAddress.split(".").map { it.toInt() and 0xFF }
            if (ipParts.size == 4) {
                answer.put(byteArrayOf(ipParts[0].toByte(), ipParts[1].toByte(), ipParts[2].toByte(), ipParts[3].toByte()))
            } else {
                answer.put(byteArrayOf(0, 0, 0, 0))
            }

            stream.write(answer.array())

            return stream.toByteArray()
        }
    }
}
