package dev.chiraitori.anis.vpn

import java.net.InetAddress
import java.nio.ByteBuffer

class IpPacket(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int, // 17 for UDP, 6 for TCP
    val sourceIp: InetAddress,
    val destinationIp: InetAddress,
    val payload: ByteArray
) {
    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        fun parse(buffer: ByteBuffer): IpPacket? {
            if (buffer.remaining() < 20) return null

            val startPos = buffer.position()
            val versionAndIhl = buffer.get().toInt() and 0xFF
            val version = versionAndIhl ushr 4
            if (version != 4) return null // Support IPv4

            val ihl = (versionAndIhl and 0x0F) * 4
            if (ihl < 20 || buffer.remaining() + 1 < ihl) return null

            val typeOfService = buffer.get()
            val totalLength = buffer.short.toInt() and 0xFFFF
            val identification = buffer.short
            val flagsAndFragment = buffer.short
            val ttl = buffer.get()
            val protocol = buffer.get().toInt() and 0xFF
            val checksum = buffer.short

            val srcBytes = ByteArray(4)
            buffer.get(srcBytes)
            val srcIp = InetAddress.getByAddress(srcBytes)

            val dstBytes = ByteArray(4)
            buffer.get(dstBytes)
            val dstIp = InetAddress.getByAddress(dstBytes)

            // Skip any IP options if IHL > 20
            if (ihl > 20) {
                val optionsLen = ihl - 20
                if (buffer.remaining() < optionsLen) return null
                buffer.position(buffer.position() + optionsLen)
            }

            val payloadLength = totalLength - ihl
            if (payloadLength < 0 || buffer.remaining() < payloadLength) {
                // If buffer is smaller than declared totalLength, read what remains
                val available = buffer.remaining()
                val payload = ByteArray(available)
                buffer.get(payload)
                return IpPacket(version, ihl, totalLength, protocol, srcIp, dstIp, payload)
            }

            val payload = ByteArray(payloadLength)
            buffer.get(payload)

            return IpPacket(version, ihl, totalLength, protocol, srcIp, dstIp, payload)
        }

        fun buildUdpIpPacket(
            sourceIp: InetAddress,
            destinationIp: InetAddress,
            sourcePort: Int,
            destinationPort: Int,
            udpPayload: ByteArray
        ): ByteArray {
            val udpLength = 8 + udpPayload.size
            val ipTotalLength = 20 + udpLength

            val buffer = ByteBuffer.allocate(ipTotalLength)

            // IPv4 Header (20 bytes)
            buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
            buffer.put(0x00.toByte()) // DSCP / ECN
            buffer.putShort(ipTotalLength.toShort()) // Total Length
            buffer.putShort((0..65535).random().toShort()) // Identification
            buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment), Offset 0
            buffer.put(64.toByte()) // TTL
            buffer.put(PROTOCOL_UDP.toByte()) // Protocol 17
            buffer.putShort(0.toShort()) // Checksum placeholder
            buffer.put(sourceIp.address)
            buffer.put(destinationIp.address)

            // Compute IP Header Checksum
            val ipChecksum = computeChecksum(buffer.array(), 0, 20)
            buffer.putShort(10, ipChecksum.toShort())

            // UDP Header (8 bytes)
            val udpStart = 20
            buffer.position(udpStart)
            buffer.putShort(sourcePort.toShort())
            buffer.putShort(destinationPort.toShort())
            buffer.putShort(udpLength.toShort())
            buffer.putShort(0.toShort()) // UDP Checksum (0 means disabled/optional in IPv4)

            // UDP Payload
            buffer.put(udpPayload)

            return buffer.array()
        }

        private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
            var sum = 0L
            var i = offset
            val end = offset + length

            while (i < end - 1) {
                val b1 = data[i].toInt() and 0xFF
                val b2 = data[i + 1].toInt() and 0xFF
                val word = (b1 shl 8) or b2
                sum += word
                i += 2
            }

            if (i < end) {
                val b1 = data[i].toInt() and 0xFF
                sum += (b1 shl 8)
            }

            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }

            return sum.inv().toInt() and 0xFFFF
        }
    }
}
