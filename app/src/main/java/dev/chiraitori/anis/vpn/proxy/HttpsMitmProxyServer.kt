package dev.chiraitori.anis.vpn.proxy

import android.util.Log
import dev.chiraitori.anis.data.QueryLogRepository
import dev.chiraitori.anis.data.model.QueryStatus
import dev.chiraitori.anis.vpn.filter.HttpsFilterEngine
import dev.chiraitori.anis.vpn.ssl.DynamicCertificateGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class HttpsMitmProxyServer(
    private val dynamicCertGen: DynamicCertificateGenerator,
    private val filterEngine: HttpsFilterEngine,
    private val queryLogRepository: QueryLogRepository,
    private val socketProtector: ((Socket) -> Boolean)? = null
) {

    private val proxyScope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    val isRunning: Boolean
        get() = serverSocket != null && !serverSocket!!.isClosed

    /**
     * Starts the MITM Proxy Server listening on the given local port (default 8443).
     */
    fun start(port: Int = DEFAULT_PROXY_PORT): Boolean {
        if (isRunning) return true

        return try {
            val server = ServerSocket(port, 100, InetAddress.getByName("127.0.0.1"))
            serverSocket = server

            serverJob = proxyScope.launch {
                Log.i(TAG, "HTTPS MITM Proxy listening on 127.0.0.1:$port")
                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        launch {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            Log.w(TAG, "Error accepting proxy connection: ${e.message}")
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTPS MITM Proxy on port $port", e)
            false
        }
    }

    /**
     * Stops the MITM Proxy Server.
     */
    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            Log.i(TAG, "HTTPS MITM Proxy stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping HTTPS MITM Proxy", e)
        }
    }

    private fun handleClientConnection(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 15000
            val clientIn = BufferedInputStream(clientSocket.getInputStream())
            val clientOut = BufferedOutputStream(clientSocket.getOutputStream())

            // Read the first line of the HTTP request
            val firstLine = readAsciiLine(clientIn) ?: run {
                clientSocket.close()
                return
            }

            val parts = firstLine.split(" ")
            if (parts.size < 3) {
                clientSocket.close()
                return
            }

            val method = parts[0].uppercase()
            val uri = parts[1]
            val httpVersion = parts[2]

            if (method == "CONNECT") {
                handleHttpConnect(clientSocket, clientIn, clientOut, uri)
            } else {
                // Direct transparent HTTP request
                val host = extractHostFromUriOrHeaders(uri, clientIn) ?: "unknown"
                handleDirectHttp(clientSocket, clientIn, clientOut, method, uri, httpVersion, host)
            }

        } catch (e: Exception) {
            // Socket closed or connection reset
        } finally {
            try { clientSocket.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Handles HTTP CONNECT method for HTTPS tunneling & MITM decryption.
     */
    private fun handleHttpConnect(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        targetHostPort: String
    ) {
        val host = targetHostPort.substringBefore(":")
        val port = targetHostPort.substringAfter(":", "443").toIntOrNull() ?: 443

        // Drain remainder of the CONNECT request headers
        while (true) {
            val line = readAsciiLine(clientIn) ?: break
            if (line.isEmpty()) break
        }

        // 1. Check if the target host should bypass MITM (Certificate Pinning / Sensitive apps)
        if (filterEngine.shouldBypassHost(host)) {
            // Direct blind TCP tunnel
            pipeDirectBlindTunnel(clientSocket, clientIn, clientOut, host, port)
            return
        }

        // 2. Perform MITM Decryption
        try {
            // Send 200 Connection Established to client
            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
            clientOut.flush()

            // Upgrade client connection to TLS using forged cert
            val sslContext = dynamicCertGen.getOrCreateSslContext(host)
            val sslClientSocket = sslContext.socketFactory.createSocket(
                clientSocket,
                host,
                port,
                false
            ) as SSLSocket
            sslClientSocket.useClientMode = false
            sslClientSocket.startHandshake()

            val sslClientIn = BufferedInputStream(sslClientSocket.getInputStream())
            val sslClientOut = BufferedOutputStream(sslClientSocket.getOutputStream())

            // Read the decrypted inner HTTP request
            val innerReqLine = readAsciiLine(sslClientIn) ?: return
            val innerParts = innerReqLine.split(" ")
            if (innerParts.size < 3) return

            val innerMethod = innerParts[0]
            val innerPath = innerParts[1]
            val innerHttpVer = innerParts[2]

            // 3. Evaluate against URL & Path Filter Engine
            if (filterEngine.shouldBlockUrl(host, innerPath)) {
                // Blocked! Return 204 No Content
                val blockResponse = "HTTP/1.1 204 No Content\r\nConnection: close\r\nContent-Length: 0\r\n\r\n"
                sslClientOut.write(blockResponse.toByteArray(Charsets.US_ASCII))
                sslClientOut.flush()

                queryLogRepository.logQuery(
                    domain = host,
                    queryType = "HTTPS-URL",
                    status = QueryStatus.BLOCKED_AD,
                    blockReason = "HTTPS Cosmetic/URL Filter: $innerPath"
                )
                return
            }

            // 4. Forward allowed request to real upstream server
            val upstreamSocket = Socket().also { sock ->
                socketProtector?.invoke(sock)
                sock.connect(java.net.InetSocketAddress(host, port), 8000)
            }

            val sslUpstreamSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(upstreamSocket, host, port, true) as SSLSocket
            sslUpstreamSocket.startHandshake()

            val sslUpstreamIn = BufferedInputStream(sslUpstreamSocket.getInputStream())
            val sslUpstreamOut = BufferedOutputStream(sslUpstreamSocket.getOutputStream())

            // Send modified/clean request to upstream
            val cleanPath = filterEngine.sanitizeQuery(innerPath.substringAfter("?", ""))?.let { cleanQuery ->
                "${innerPath.substringBefore("?")}?$cleanQuery"
            } ?: innerPath.substringBefore("?")

            sslUpstreamOut.write("$innerMethod $cleanPath $innerHttpVer\r\n".toByteArray(Charsets.US_ASCII))

            // Forward headers
            while (true) {
                val headerLine = readAsciiLine(sslClientIn) ?: break
                if (headerLine.isEmpty()) {
                    sslUpstreamOut.write("\r\n".toByteArray(Charsets.US_ASCII))
                    break
                }
                sslUpstreamOut.write("$headerLine\r\n".toByteArray(Charsets.US_ASCII))
            }
            sslUpstreamOut.flush()

            // Stream response back to client
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (sslUpstreamIn.read(buffer).also { bytesRead = it } != -1) {
                sslClientOut.write(buffer, 0, bytesRead)
                sslClientOut.flush()
            }

            try { sslUpstreamSocket.close() } catch (ignored: Exception) {}
            try { sslClientSocket.close() } catch (ignored: Exception) {}

        } catch (e: Exception) {
            // Handshake failed or client aborted
        }
    }

    /**
     * Direct blind TCP tunnel when MITM is bypassed for certificate pinned domains.
     */
    private fun pipeDirectBlindTunnel(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        host: String,
        port: Int
    ) {
        try {
            val upstream = Socket().also { sock ->
                socketProtector?.invoke(sock)
                sock.connect(java.net.InetSocketAddress(host, port), 8000)
            }

            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
            clientOut.flush()

            val upstreamIn = upstream.getInputStream()
            val upstreamOut = upstream.getOutputStream()

            // Bi-directional byte piping
            proxyScope.launch {
                pipeStreams(clientIn, upstreamOut)
                try { upstream.close() } catch (ignored: Exception) {}
            }
            pipeStreams(upstreamIn, clientOut)

        } catch (e: Exception) {
            // Tunnel error
        }
    }

    private fun handleDirectHttp(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        method: String,
        uri: String,
        httpVer: String,
        host: String
    ) {
        val path = if (uri.startsWith("http://")) URI(uri).path else uri
        if (filterEngine.shouldBlockUrl(host, path)) {
            val blockResponse = "HTTP/1.1 204 No Content\r\nConnection: close\r\nContent-Length: 0\r\n\r\n"
            clientOut.write(blockResponse.toByteArray(Charsets.US_ASCII))
            clientOut.flush()
            return
        }

        // Forward plain HTTP
        try {
            val upstream = Socket().also { sock ->
                socketProtector?.invoke(sock)
                sock.connect(java.net.InetSocketAddress(host, 80), 8000)
            }
            val upIn = upstream.getInputStream()
            val upOut = upstream.getOutputStream()

            upOut.write("$method $path $httpVer\r\n".toByteArray(Charsets.US_ASCII))
            proxyScope.launch { pipeStreams(clientIn, upOut) }
            pipeStreams(upIn, clientOut)
        } catch (e: Exception) {
            // Direct HTTP error
        }
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var bytes: Int
        try {
            while (input.read(buffer).also { bytes = it } != -1) {
                output.write(buffer, 0, bytes)
                output.flush()
            }
        } catch (ignored: Exception) {}
    }

    private fun readAsciiLine(input: InputStream): String? {
        val sb = StringBuilder()
        var c: Int
        while (input.read().also { c = it } != -1) {
            if (c == '\n'.code) break
            if (c != '\r'.code) {
                sb.append(c.toChar())
            }
        }
        return if (sb.isEmpty() && c == -1) null else sb.toString()
    }

    private fun extractHostFromUriOrHeaders(uri: String, input: InputStream): String? {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return URI(uri).host
        }
        return null
    }

    companion object {
        private const val TAG = "HttpsMitmProxy"
        const val DEFAULT_PROXY_PORT = 8443
    }
}
