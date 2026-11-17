package com.yiqiu.shirohaquiz.ai

import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okio.buffer
import okio.source
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShirohaAiClientLocalTest {
    @Test fun realChatCompletionWithoutKeyUsesLocalEndpoint() {
        val executor = Executors.newSingleThreadExecutor()
        try {
            ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
                server.soTimeout = 5000
                val received = executor.submit<Pair<List<String>, String>> {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().source().buffer()
                        val headers = mutableListOf<String>()
                        while (true) {
                            val line = reader.readUtf8LineStrict()
                            if (line.isEmpty()) break
                            headers += line
                        }
                        val length = headers.first { it.startsWith("Content-Length:", true) }.substringAfter(':').trim().toInt()
                        val body = reader.readUtf8(length.toLong())
                        val response = """{"choices":[{"message":{"content":"connected"}}]}""".toByteArray()
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(response)
                            flush()
                        }
                        headers to body
                    }
                }
                val result = ShirohaAiClient.testConnection("http://127.0.0.1:${server.localPort}", "", "fake-local")
                assertTrue(result.contains("connected"))
                val (headers, body) = received.get(5, TimeUnit.SECONDS)
                assertEquals("POST /v1/chat/completions HTTP/1.1", headers.first())
                assertFalse(headers.any { it.startsWith("Authorization:", true) })
                assertEquals("fake-local", JSONObject(body).getString("model"))
            }
        } finally { executor.shutdownNow() }
    }
}
