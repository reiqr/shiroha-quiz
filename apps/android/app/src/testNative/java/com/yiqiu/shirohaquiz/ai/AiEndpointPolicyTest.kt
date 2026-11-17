package com.yiqiu.shirohaquiz.ai

import java.net.InetAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import okhttp3.Dns
import org.junit.Assert.*
import org.junit.Test

class AiEndpointPolicyTest {
    private fun fixedDns(vararg addresses: InetAddress): Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
    }
    @Test fun normalizesLocalAndPreservesCloudEndpoints() {
        assertEquals("http://127.0.0.1:11434/v1/chat/completions", AiEndpointPolicy.endpoint("http://127.0.0.1:11434/").toString())
        assertEquals("http://192.168.1.2/v1/chat/completions", AiEndpointPolicy.endpoint("http://192.168.1.2/v1/").toString())
        assertEquals("https://api.deepseek.com/chat/completions", AiEndpointPolicy.endpoint("https://api.deepseek.com").toString())
        assertEquals("https://api.openai.com/v1/chat/completions", AiEndpointPolicy.endpoint("https://api.openai.com/v1/chat/completions/").toString())
    }

    @Test fun acceptsOnlyPrivateOrLoopbackAddresses() {
        listOf("127.0.0.2", "10.0.0.1", "172.16.0.1", "172.31.255.254", "192.168.1.2", "::1", "fc00::1", "fd00::1", "fe80::1").forEach {
            assertTrue(it, AiEndpointPolicy.isPrivateAddress(InetAddress.getByName(it)))
        }
        listOf("0.0.0.0", "8.8.8.8", "172.15.0.1", "172.32.0.1", "169.254.1.1", "224.0.0.1", "::", "2001:4860::1", "ff02::1").forEach {
            assertFalse(it, AiEndpointPolicy.isPrivateAddress(InetAddress.getByName(it)))
        }
    }

    @Test fun rejectsPublicHttpAndUrlCredentials() {
        listOf("http://8.8.8.8", "http://0.0.0.0", "http://[::]", "ftp://localhost", "https://user:pass@example.com", "https://example.com/?key=secret").forEach {
            assertThrows(it, IllegalStateException::class.java) { AiEndpointPolicy.endpoint(it) }
        }
    }

    @Test fun validatesResolvedAddressesWithoutDroppingPublicAnswers() {
        val local = InetAddress.getByName("192.168.1.2")
        val public = InetAddress.getByName("8.8.8.8")
        assertEquals(listOf(local), AiEndpointPolicy.privateDns(fixedDns(local)).lookup("model.internal"))
        assertThrows(UnknownHostException::class.java) {
            AiEndpointPolicy.privateDns(fixedDns(local, public)).lookup("model.internal")
        }
        assertThrows(UnknownHostException::class.java) { AiEndpointPolicy.privateDns(fixedDns()).lookup("empty.internal") }
    }

    @Test fun localKeyIsOptionalButCloudKeyIsRequired() {
        assertTrue(AiEndpointPolicy.isConfigured("http://model.internal/v1", "", "local-model"))
        assertFalse(AiEndpointPolicy.isConfigured("https://api.example.com/v1", "", "model"))
        assertTrue(AiEndpointPolicy.isConfigured("https://api.example.com/v1", "key", "model"))
        val url = AiEndpointPolicy.endpoint("http://localhost:11434")
        assertNull(AiEndpointPolicy.request(url, " ").build().header("Authorization"))
        assertEquals("Bearer test-key", AiEndpointPolicy.request(url, "test-key").build().header("Authorization"))
    }

    @Test fun actualClientRejectsPublicDnsBeforeConnecting() {
        val url = AiEndpointPolicy.endpoint("http://model.internal/v1")
        val client = AiEndpointPolicy.client(url, 1000, fixedDns(InetAddress.getByName("8.8.8.8")))
        assertThrows(UnknownHostException::class.java) {
            client.newCall(AiEndpointPolicy.request(url, "test-key").build()).execute().close()
        }
    }

    @Test fun realLocalConnectionDoesNotFollowRedirect() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 5000
            val worker = Thread {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (!reader.readLine().isNullOrEmpty()) { /* consume request headers */ }
                    socket.getOutputStream().write("HTTP/1.1 302 Found\r\nLocation: http://8.8.8.8/\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                }
            }.apply { isDaemon = true; start() }
            val url = AiEndpointPolicy.endpoint("http://model.internal:${server.localPort}")
            val client = AiEndpointPolicy.client(url, 5000, fixedDns(InetAddress.getByName("127.0.0.1")))
            assertFalse(client.followRedirects)
            assertFalse(client.followSslRedirects)
            client.newCall(AiEndpointPolicy.request(url, "").build()).execute().use { assertEquals(302, it.code) }
            worker.join(5000)
            assertFalse(worker.isAlive)
        }
    }
}
