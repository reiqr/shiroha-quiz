package com.yiqiu.shirohaquiz.sync.webdav

import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.Timeout
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class WebDavClientTest {
    @Test fun rejectsHttpAndUnsafePaths() {
        assertThrows(IllegalArgumentException::class.java) { client(FakeDav(), "http://dav.example.com/dav/") }
        assertThrows(IllegalArgumentException::class.java) { WebDavSettings("https://dav.example.com/", "user", "../data").validated() }
    }

    @Test fun connectionTestNeverCreatesDirectory() {
        val dav = FakeDav()
        client(dav).testConnection()
        assertTrue(dav.requests.all { it.method == "PROPFIND" && it.header("Depth") == "0" })
    }

    @Test fun uploadMoveAndDownloadRoundTrip() {
        val dav = FakeDav()
        withDirectory { dir ->
            val source = File(dir, "source.zip").apply { writeBytes(ByteArray(24000) { (it % 127).toByte() }) }
            val digest = WebDavClient.hashFile(source)
            val name = WebDavPaths.newBackupName(1_800_000_000_000, "0.9.8.5", digest.sha256)
            val result = client(dav).uploadAtomic(source, name)
            assertEquals(digest.sha256, result.sha256)
            assertTrue(dav.requests.filter { it.method == "PUT" }.all { it.url.encodedPath.endsWith(".uploading") })
            assertEquals("F", dav.requests.single { it.method == "MOVE" }.header("Overwrite"))
            val downloaded = File(dir, "download.zip")
            client(dav).download(WebDavBackupEntry(name, null, source.length(), sha256 = digest.sha256), downloaded)
            assertArrayEquals(source.readBytes(), downloaded.readBytes())
        }
    }

    @Test fun hashMismatchNeverKeepsDownloadedFile() {
        val dav = FakeDav()
        withDirectory { dir ->
            val name = WebDavPaths.newBackupName(1_800_000_000_000, "0.9.8.5", "a".repeat(64))
            dav.files["/dav/ShirohaQuiz/Backups/$name"] = "changed".toByteArray()
            val destination = File(dir, "corrupt.zip")
            assertThrows(WebDavIntegrityException::class.java) {
                client(dav).download(WebDavBackupEntry(name, null, 7, sha256 = "a".repeat(64)), destination)
            }
            assertFalse(destination.exists())
        }
    }

    @Test fun cancellationSendsNoRequest() {
        val dav = FakeDav()
        val token = WebDavCancellation().apply { cancel() }
        assertThrows(java.util.concurrent.CancellationException::class.java) { client(dav).list(token) }
        assertTrue(dav.requests.isEmpty())
    }

    @Test fun foreignRedirectNeverReceivesCredentials() {
        val requests = mutableListOf<Request>()
        val factory = Call.Factory { request -> FakeCall(request) {
            requests += it
            response(it, 302, ByteArray(0)).newBuilder().header("Location", "https://other.example.com/dav/").build()
        } }
        val client = WebDavClient("https://dav.example.com/dav/", "/ShirohaQuiz/Backups/", WebDavCredentials("user", "secret"), callFactory = factory)
        assertThrows(WebDavException::class.java) { client.testConnection() }
        assertEquals(1, requests.size)
        assertEquals("dav.example.com", requests.single().url.host)
    }

    @Test fun oversizedFileIsRejectedBeforeUpload() {
        val dav = FakeDav()
        withDirectory { dir ->
            val file = File(dir, "large.zip").apply { writeBytes(ByteArray(12)) }
            val client = WebDavClient("https://dav.example.com/dav/", "/ShirohaQuiz/Backups/", WebDavCredentials("user", "secret"), callFactory = dav, maxBackupBytes = 10)
            assertThrows(IllegalArgumentException::class.java) { client.uploadAtomic(file, "shiroha_backup_20260914_183500.zip") }
            assertTrue(dav.requests.isEmpty())
        }
    }

    @Test fun xmlDocumentTypeIsRejected() {
        val factory = Call.Factory { request -> FakeCall(request) {
            response(it, 207, "<!DOCTYPE d:multistatus [<!ENTITY hidden SYSTEM 'file:///private.txt'>]><d:multistatus xmlns:d='DAV:'>&hidden;</d:multistatus>".toByteArray())
        } }
        assertThrows(WebDavException::class.java) { client(factory).list() }
    }

    private fun client(factory: Call.Factory, url: String = "https://dav.example.com/dav/") =
        WebDavClient(url, "/ShirohaQuiz/Backups/", WebDavCredentials("user", "secret"), callFactory = factory,
            xmlParserFactory = { org.kxml2.io.KXmlParser() })

    private fun withDirectory(block: (File) -> Unit) {
        val dir = kotlin.io.path.createTempDirectory("webdav-test").toFile()
        try { block(dir) } finally { dir.deleteRecursively() }
    }

    internal class FakeDav : Call.Factory {
        val requests = mutableListOf<Request>()
        val files = linkedMapOf<String, ByteArray>()
        override fun newCall(request: Request): Call = FakeCall(request) { current ->
            requests += current
            val path = current.url.encodedPath
            when (current.method) {
                "PUT" -> {
                    if (files.containsKey(path)) response(current, 412, ByteArray(0))
                    else { files[path] = Buffer().also { current.body!!.writeTo(it) }.readByteArray(); response(current, 201, ByteArray(0)) }
                }
                "GET" -> files[path]?.let { response(current, 200, it) } ?: response(current, 404, ByteArray(0))
                "MOVE" -> {
                    val destination = HttpUrl.Builder().scheme("https").host("unused").build().resolve(current.header("Destination")!!)!!.encodedPath
                    if (files.containsKey(destination)) response(current, 412, ByteArray(0))
                    else { files[destination] = files.remove(path)!!; response(current, 201, ByteArray(0)) }
                }
                "DELETE" -> { files.remove(path); response(current, 204, ByteArray(0)) }
                "PROPFIND" -> {
                    val data = files[path]
                    val type = if (path.endsWith('/')) "<d:collection/>" else ""
                    if (data == null && type.isEmpty()) response(current, 404, ByteArray(0))
                    else response(current, 207, ("<d:multistatus xmlns:d=\"DAV:\"><d:response><d:href>$path</d:href>" +
                        "<d:propstat><d:prop><d:resourcetype>$type</d:resourcetype><d:getcontentlength>${data?.size ?: 0}</d:getcontentlength>" +
                        "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>").toByteArray())
                }
                else -> response(current, 405, ByteArray(0))
            }
        }
    }

    private class FakeCall(private val original: Request, private val handle: (Request) -> Response) : Call {
        private var executed = false
        private var cancelled = false
        override fun request() = original
        override fun execute(): Response { if (cancelled) throw IOException("Cancelled"); executed = true; return handle(original) }
        override fun enqueue(responseCallback: Callback) { error("Only blocking calls are supported by this test.") }
        override fun cancel() { cancelled = true }
        override fun isExecuted() = executed
        override fun isCanceled() = cancelled
        override fun timeout() = Timeout()
        override fun clone(): Call = FakeCall(original, handle)
    }

    companion object {
        private fun response(request: Request, code: Int, bytes: ByteArray): Response = Response.Builder()
            .request(request).protocol(Protocol.HTTP_1_1).code(code).message("test").body(bytes.toResponseBody()).build()
    }
}
