package dev.chiraitori.anis.vpn

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DohClient(
    private val socketProtector: ((Int) -> Boolean)? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val dnsMediaType = "application/dns-message".toMediaType()

    suspend fun resolve(dohUrl: String, queryBytes: ByteArray): ByteArray? {
        return try {
            val request = Request.Builder()
                .url(dohUrl)
                .post(queryBytes.toRequestBody(dnsMediaType))
                .header("Accept", "application/dns-message")
                .header("User-Agent", "AnisDNS/2.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.w("DohClient", "DoH query failed with HTTP code ${response.code}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.w("DohClient", "DoH query network error: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("DohClient", "DoH resolution error", e)
            null
        }
    }
}
