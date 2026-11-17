package com.yiqiu.shirohaquiz.ai

import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal object AiEndpointPolicy {
    fun endpoint(value: String): HttpUrl {
        val url = value.trim().toHttpUrlOrNull()
            ?: throw IllegalStateException("请填写完整的 http:// 或 https:// API 地址。")
        check(url.username.isEmpty() && url.password.isEmpty()) { "API 地址不能包含用户名或密码。" }
        check(url.query == null && url.fragment == null) { "API 地址不能包含查询参数或片段。" }
        if (!url.isHttps && (':' in url.host || url.host.all { it.isDigit() || it == '.' })) {
            check(isPrivateAddress(InetAddress.getByName(url.host))) { "HTTP AI 接口仅支持回环或内网地址；公网接口请使用 HTTPS。" }
        }
        val path = url.encodedPath.trimEnd('/')
        // Cloud providers such as DeepSeek expose /chat/completions at the root.
        val base = if (path.isEmpty() && !url.isHttps) "/v1" else path
        val target = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        return url.newBuilder().encodedPath(target).build()
    }

    fun isPrivateAddress(address: InetAddress): Boolean {
        val bytes = address.address.map { it.toInt() and 255 }
        if (bytes.size == 4) return bytes[0] == 127 || bytes[0] == 10 ||
            (bytes[0] == 172 && bytes[1] in 16..31) || (bytes[0] == 192 && bytes[1] == 168)
        return bytes.size == 16 && (address.isLoopbackAddress ||
            (bytes[0] and 254) == 252 || (bytes[0] == 254 && (bytes[1] and 192) == 128))
    }

    fun privateDns(delegate: Dns = Dns.SYSTEM): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = delegate.lookup(hostname)
            if (addresses.isEmpty() || addresses.any { !isPrivateAddress(it) }) {
                throw UnknownHostException("HTTP AI 接口仅支持回环或内网地址；公网接口请使用 HTTPS。")
            }
            return addresses
        }
    }

    fun requiresApiKey(value: String): Boolean = endpoint(value).isHttps

    fun isConfigured(base: String, key: String, model: String): Boolean =
        model.isNotBlank() && runCatching { !requiresApiKey(base) || key.isNotBlank() }.getOrDefault(false)

    fun client(url: HttpUrl, timeoutMillis: Long, dns: Dns = Dns.SYSTEM): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(minOf(timeoutMillis, 30_000), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .apply {
                if (!url.isHttps) {
                    // A proxy could bypass our DNS check and resolve the host elsewhere.
                    proxy(Proxy.NO_PROXY)
                    dns(privateDns(dns))
                    addNetworkInterceptor { chain ->
                        val address = chain.connection()?.route()?.socketAddress?.address
                        if (address == null || !isPrivateAddress(address)) {
                            throw java.io.IOException("HTTP AI 接口禁止连接公网地址。")
                        }
                        chain.proceed(chain.request())
                    }
                }
            }.build()

    fun request(url: HttpUrl, key: String): Request.Builder = Request.Builder().url(url)
        .header("Accept", "application/json")
        .apply { if (key.isNotBlank()) header("Authorization", "Bearer ${key.trim()}") }
}
