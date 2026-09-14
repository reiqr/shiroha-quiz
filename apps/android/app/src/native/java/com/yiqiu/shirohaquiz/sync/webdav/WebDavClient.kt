package com.yiqiu.shirohaquiz.sync.webdav

import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Blocking API: invoke on IO. A supplied Call.Factory is a mock seam and MUST NOT follow redirects
 * or perform authentication itself. Production always uses the hardened OkHttp client below.
 * HTTPS remains mandatory for injected base URLs (use TLS MockWebServer or a fake Call.Factory).
 */
class WebDavClient(
    baseUrl: String,
    remoteDirectory: String,
    private val credentials: WebDavCredentials,
    httpClient: OkHttpClient = OkHttpClient(),
    callFactory: Call.Factory? = null,
    private val maxBackupBytes: Long = DEFAULT_MAX_BACKUP_BYTES,
    private val xmlParserFactory: () -> XmlPullParser = { XmlPullParserFactory.newInstance().newPullParser() }
) {
    private val base = WebDavPaths.baseUrl(baseUrl)
    private val directoryParts = WebDavPaths.directory(remoteDirectory).trim('/').split('/')
    private val directoryUrl = collectionUrl(directoryParts)
    private val transport: Call.Factory = callFactory ?: httpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .authenticator(Authenticator.NONE)
        .proxyAuthenticator(Authenticator.NONE)
        .cache(null)
        .build()

    init { require(maxBackupBytes > 0) }

    /** Depth 0 only, never writes anything and never implicitly creates a collection. */
    fun testConnection(cancellation: WebDavCancellation = WebDavCancellation()): WebDavResource {
        probeCollection(base, cancellation) ?: throw WebDavException("WebDAV 服务地址不存在，请检查地址。", 404)
        return probeCollection(directoryUrl, cancellation)
            ?: throw WebDavException("远端备份目录不存在，请先点击创建目录。", 404)
    }

    /** The sole MKCOL entry point. UI must invoke this in response to an explicit user action. */
    fun createDirectoryByUserRequest(cancellation: WebDavCancellation = WebDavCancellation()) {
        for (count in 1..directoryParts.size) {
            cancellation.throwIfCancelled()
            val url = collectionUrl(directoryParts.take(count))
            if (probeCollection(url, cancellation) != null) continue
            withResponse(request(url, "MKCOL"), cancellation) { response ->
                when (response.code) {
                    201 -> Unit
                    405 -> if (probeCollection(url, cancellation) == null) failHttp(response.code)
                    else -> failHttp(response.code)
                }
            }
        }
    }

    /** Only direct child resources are returned; foreign origins, traversal and nested hrefs fail closed. */
    fun list(cancellation: WebDavCancellation = WebDavCancellation()): List<WebDavResource> {
        return propfind(directoryUrl, 1, cancellation)
            .filter { it.fileName.isNotEmpty() }
            .distinctBy { it.fileName }
    }

    fun statFile(fileName: String, cancellation: WebDavCancellation = WebDavCancellation()): WebDavResource? {
        val url = fileUrl(fileName)
        return try {
            propfind(url, 0, cancellation).singleOrNull()
                ?: throw WebDavException("WebDAV 返回的文件信息无效。")
        } catch (error: WebDavException) {
            if (error.httpCode == 404) null else throw error
        }
    }

    /** PUT never targets a final name. A complete read-back SHA256 precedes MOVE/Overwrite:F. */
    fun uploadAtomic(
        source: File,
        fileName: String,
        cancellation: WebDavCancellation = WebDavCancellation(),
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): WebDavTransferResult {
        requireOwnedName(fileName)
        require(source.isFile && source.length() in 1..maxBackupBytes) { "待上传备份不存在、为空或超过大小限制。"
        }
        val expected = hashFile(source, cancellation, maxBackupBytes)
        val temporaryName = WebDavPaths.safeFileName("$fileName.uploading")
        val body = object : RequestBody() {
            override fun contentType() = BINARY
            override fun contentLength(): Long = expected.sizeBytes
            override fun isOneShot(): Boolean = true
            override fun writeTo(sink: BufferedSink) {
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                source.inputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        cancellation.throwIfCancelled()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > expected.sizeBytes) throw WebDavIntegrityException("备份在上传过程中发生变化，完整性校验失败。")
                        digest.update(buffer, 0, count)
                        sink.write(buffer, 0, count)
                        onProgress(total, expected.sizeBytes)
                    }
                }
                if (total != expected.sizeBytes || !WebDavPaths.equalSha256(WebDavPaths.digestHex(digest.digest()), expected.sha256)) {
                    throw WebDavIntegrityException("备份在上传过程中发生变化，完整性校验失败。")
                }
            }
        }
        var ownsTemporary = false
        var moved = false
        try {
            withResponse(request(fileUrl(temporaryName), "PUT", body).newBuilder()
                .header("If-None-Match", "*").build(), cancellation) { response ->
                if (response.code !in setOf(200, 201, 204)) failHttp(response.code)
                ownsTemporary = true
            }
            verifyRemote(temporaryName, expected, cancellation)
            cancellation.throwIfCancelled()
            val move = request(fileUrl(temporaryName), "MOVE").newBuilder()
                .header("Destination", fileUrl(fileName).toString())
                .header("Overwrite", "F")
                .build()
            withResponse(move, cancellation) { response ->
                // 204 signifies replacement, which violates the immutable backup contract.
                if (response.code != 201) failHttp(response.code)
                moved = true
            }
            verifyRemote(fileName, expected, cancellation)
            return expected
        } finally {
            // Cancellation stays prompt. Cancelled/ambiguous uploads may leave hidden .uploading
            // files, never visible backups. Do not start uncancellable cleanup network requests.
            if (ownsTemporary && !moved && !cancellation.isCancelled) {
                runCatching { deleteOwnedFile(temporaryName, cancellation) }
            }
        }
    }

    /** No unverified partial file survives failure; caller supplies a private, non-existing destination. */
    fun download(
        entry: WebDavBackupEntry,
        destination: File,
        cancellation: WebDavCancellation = WebDavCancellation(),
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): WebDavTransferResult {
        require(WebDavPaths.isBackup(entry.fileName))
        if (!entry.integrityAvailable) throw WebDavIntegrityException("备份缺少 SHA256 摘要或完整大小信息，无法安全下载恢复。")
        require(entry.sizeBytes <= maxBackupBytes && !destination.exists())
        val parent = destination.parentFile ?: throw WebDavException("缺少私有下载缓存目录。")
        check(parent.isDirectory || parent.mkdirs()) { "无法创建私有下载缓存目录。"
        }
        var complete = false
        try {
            val result = withResponse(downloadRequest(entry.fileName, entry.etag), cancellation) { response ->
                requireDownloadResponse(response, entry.sizeBytes)
                FileOutputStream(destination).use { output ->
                    val result = transfer(response.body!!.byteStream(), output, entry.sizeBytes, entry.sha256!!, cancellation, onProgress)
                    output.fd.sync()
                    result
                }
            }
            complete = true
            return result
        } finally {
            if (!complete) destination.delete()
        }
    }

    /** Used only for bounded sidecar/index data, never for backup file bodies. */
    fun readMetadata(fileName: String, cancellation: WebDavCancellation = WebDavCancellation()): ByteArray? {
        require(fileName == "manifest.json" || (fileName.startsWith("shiroha-quiz-backup-") && fileName.endsWith(".meta.json")))
        return readSmall(fileUrl(fileName), cancellation)
    }

    /** Compatibility read for the original plan's parent-directory manifest; it is not overwritten. */
    fun readLegacyManifest(cancellation: WebDavCancellation = WebDavCancellation()): ByteArray? =
        readSmall(collectionUrl(directoryParts.dropLast(1)).newBuilder().addPathSegment("manifest.json").build(), cancellation)

    /** UI owns the second confirmation. This API accepts only owned files, never collections. */
    fun deleteBackup(entry: WebDavBackupEntry, cancellation: WebDavCancellation = WebDavCancellation()): Boolean {
        require(WebDavPaths.isBackup(entry.fileName))
        return deleteOwnedFile(entry.fileName, cancellation, entry.etag)
    }

    fun deleteMetadata(fileName: String, cancellation: WebDavCancellation = WebDavCancellation()): Boolean {
        require(WebDavPaths.isBackup(fileName))
        return deleteOwnedFile("$fileName.meta.json", cancellation)
    }

    private fun deleteOwnedFile(name: String, cancellation: WebDavCancellation, expectedEtag: String? = null): Boolean {
        requireOwnedName(name.removeSuffix(".uploading"))
        val resource = statFile(name, cancellation) ?: return false
        if (resource.isDirectory) throw WebDavException("不允许删除远端目录。")
        if (expectedEtag != null && resource.etag != expectedEtag) throw WebDavException("远端备份已发生变化，请刷新列表后再删除。", 412)
        val builder = request(fileUrl(name), "DELETE").newBuilder().header("Depth", "0")
        (expectedEtag ?: resource.etag)?.takeIf { !it.startsWith("W/") }?.let { builder.header("If-Match", it) }
        return withResponse(builder.build(), cancellation) { response ->
            when (response.code) {
                200, 204 -> true
                404 -> false
                else -> failHttp(response.code) // 207 is not success without per-resource verification.
            }
        }
    }

    private fun probeCollection(url: HttpUrl, cancellation: WebDavCancellation): WebDavResource? = try {
        val resource = propfind(url, 0, cancellation).singleOrNull()
            ?: throw WebDavException("WebDAV 返回的目录信息无效。")
        if (!resource.isDirectory) throw WebDavException("远端目录路径已被同名文件占用。")
        resource
    } catch (error: WebDavException) {
        if (error.httpCode == 404) null else throw error
    }

    private fun propfind(url: HttpUrl, depth: Int, cancellation: WebDavCancellation): List<WebDavResource> {
        require(depth in 0..1)
        val query = request(url, "PROPFIND", PROPFIND_BODY.toRequestBody(XML)).newBuilder()
            .header("Depth", depth.toString()).build()
        return withResponse(query, cancellation) { response ->
            if (response.code != 207) failHttp(response.code)
            val bytes = boundedBytes(response, MAX_METADATA_BYTES, cancellation)
            parseResources(bytes, response.request.url, depth, cancellation)
        }
    }

    private fun parseResources(bytes: ByteArray, requested: HttpUrl, depth: Int, cancellation: WebDavCancellation): List<WebDavResource> = try {
        val root = parseXml(bytes, cancellation)
        if (root.namespaceURI != DAV || root.localName != "multistatus") throw WebDavException("WebDAV 返回的 XML 数据无效。")
        root.children("response").mapNotNull { item ->
            val href = item.children("href").singleOrNull()?.textContent?.trim() ?: return@mapNotNull null
            val uri = runCatching { URI(href) }.getOrNull() ?: return@mapNotNull null
            if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return@mapNotNull null
            if (runCatching { WebDavPaths.requireSafeEncodedPath(uri.rawPath.orEmpty()) }.isFailure) return@mapNotNull null
            val target = requested.resolve(href) ?: return@mapNotNull null
            if (!WebDavPaths.sameOrigin(base, target)) return@mapNotNull null
            val requestedPath = requested.encodedPath.trimEnd('/')
            val targetPath = target.encodedPath.trimEnd('/')
            val self = targetPath == requestedPath
            if (!self && (depth != 1 || targetPath.substringBeforeLast('/') != requestedPath)) return@mapNotNull null
            val responseStatus = item.children("status").firstOrNull()?.textContent?.let(::httpStatus)
            if (responseStatus != null && responseStatus !in 200..299) return@mapNotNull null
            val props = item.children("propstat")
                .filter { httpStatus(it.children("status").firstOrNull()?.textContent.orEmpty()) in 200..299 }
                .flatMap { it.children("prop") }
            val type = props.flatMap { it.children("resourcetype") }.firstOrNull() ?: return@mapNotNull null
            val isDirectory = type.children("collection").isNotEmpty()
            val name = if (self && isDirectory) "" else target.pathSegments.lastOrNull { it.isNotEmpty() }.orEmpty()
            if (name.isNotEmpty() && runCatching { WebDavPaths.safeFileName(name) }.isFailure) return@mapNotNull null
            fun property(name: String): String? = props.flatMap { it.children(name) }.firstOrNull()?.textContent?.trim()
            WebDavResource(
                fileName = name,
                isDirectory = isDirectory,
                sizeBytes = property("getcontentlength")?.toLongOrNull()?.takeIf { it >= 0 },
                modifiedAt = property("getlastmodified")?.let(::parseHttpDate),
                etag = property("getetag")?.takeIf { it.length <= 512 && it.none(Char::isISOControl) }
            )
        }
    } catch (error: WebDavException) {
        throw error
    } catch (cancelled: java.util.concurrent.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        throw WebDavException("无法安全解析 WebDAV 返回的 XML 数据。", cause = error)
    }

    private class XmlNode(val namespaceURI: String, val localName: String) {
        val nodes = mutableListOf<XmlNode>()
        val text = StringBuilder()
        val textContent: String get() = text.toString()
        fun children(name: String): List<XmlNode> = nodes.filter { it.namespaceURI == DAV && it.localName == name }
    }

    /** Android's DOM factory does not support Xerces security features. Use a bounded pull
     * parser with DOCDECL processing off and reject declaration/custom-entity tokens outright. */
    private fun parseXml(bytes: ByteArray, cancellation: WebDavCancellation): XmlNode {
        val parser = xmlParserFactory().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            try { setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
            catch (_: org.xmlpull.v1.XmlPullParserException) {
                require(!getFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL)) { "XML 解析器不允许关闭文档类型处理。" }
            }
        }
        val stack = mutableListOf<XmlNode>()
        var root: XmlNode? = null
        var elementCount = 0
        bytes.inputStream().use { input ->
            parser.setInput(input, null)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                cancellation.throwIfCancelled()
                when (parser.eventType) {
                    XmlPullParser.DOCDECL -> throw WebDavException("WebDAV XML 含不允许的文档类型声明或实体。")
                    XmlPullParser.START_TAG -> {
                        elementCount++
                        if (stack.size >= 32 || elementCount > 20_000) throw WebDavException("WebDAV XML 数据过于复杂，已停止解析。")
                        val node = XmlNode(parser.namespace.orEmpty(), parser.name)
                        if (stack.isEmpty()) {
                            if (root != null) throw WebDavException("WebDAV XML 含多个根节点。")
                            root = node
                        } else stack.last().nodes.add(node)
                        stack.add(node)
                    }
                    XmlPullParser.END_TAG -> {
                        if (stack.isEmpty()) throw WebDavException("WebDAV XML 格式错误。")
                        stack.removeAt(stack.lastIndex)
                    }
                    XmlPullParser.ENTITY_REF -> {
                        if (parser.name !in setOf("amp", "lt", "gt", "quot", "apos") || parser.text == null) {
                            throw WebDavException("WebDAV XML 含不允许的自定义实体。")
                        }
                        stack.lastOrNull()?.text?.append(parser.text)
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.IGNORABLE_WHITESPACE ->
                        stack.lastOrNull()?.text?.append(parser.text.orEmpty())
                }
                parser.nextToken()
            }
        }
        if (stack.isNotEmpty()) throw WebDavException("WebDAV XML 数据不完整。")
        return root ?: throw WebDavException("WebDAV 返回的 XML 为空。")
    }

    private fun readSmall(url: HttpUrl, cancellation: WebDavCancellation): ByteArray? =
        withResponse(request(url, "GET"), cancellation) { response ->
            if (response.code == 404) null else {
                if (response.code != 200) failHttp(response.code)
                boundedBytes(response, MAX_METADATA_BYTES, cancellation)
            }
        }

    private fun boundedBytes(response: Response, maximum: Long, cancellation: WebDavCancellation): ByteArray {
        val body = response.body ?: throw WebDavException("WebDAV 返回的内容为空。")
        if (body.contentLength() > maximum) throw WebDavException("WebDAV 返回的元数据超过安全大小限制。")
        val output = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                cancellation.throwIfCancelled()
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                if (output.size().toLong() + count > maximum) throw WebDavException("WebDAV 返回的元数据超过安全大小限制。")
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    private fun verifyRemote(name: String, expected: WebDavTransferResult, cancellation: WebDavCancellation) {
        withResponse(downloadRequest(name, null), cancellation) { response ->
            requireDownloadResponse(response, expected.sizeBytes)
            transfer(response.body!!.byteStream(), null, expected.sizeBytes, expected.sha256, cancellation) { _, _ -> }
        }
    }

    private fun downloadRequest(name: String, etag: String?): Request {
        val builder = request(fileUrl(name), "GET").newBuilder().header("Accept-Encoding", "identity")
        etag?.takeIf { !it.startsWith("W/") }?.let { builder.header("If-Match", it) }
        return builder.build()
    }

    private fun requireDownloadResponse(response: Response, size: Long) {
        if (response.code != 200) failHttp(response.code)
        val body = response.body ?: throw WebDavException("备份下载内容为空。")
        if (size !in 1..maxBackupBytes || (body.contentLength() >= 0 && body.contentLength() != size)) {
            throw WebDavIntegrityException("远端备份大小与元数据不一致。")
        }
        if (response.header("Content-Encoding")?.let { !it.equals("identity", true) } == true) {
            throw WebDavIntegrityException("备份响应经过额外编码，无法逐字节校验。")
        }
    }

    private fun transfer(
        input: InputStream,
        output: OutputStream?,
        expectedSize: Long,
        expectedSha256: String,
        cancellation: WebDavCancellation,
        onProgress: (Long, Long) -> Unit
    ): WebDavTransferResult {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        input.use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                cancellation.throwIfCancelled()
                val count = it.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                total += count
                if (total > expectedSize || total > maxBackupBytes) throw WebDavIntegrityException("备份超过元数据记录的完整大小。")
                digest.update(buffer, 0, count)
                output?.write(buffer, 0, count)
                onProgress(total, expectedSize)
            }
        }
        cancellation.throwIfCancelled()
        val sha = WebDavPaths.digestHex(digest.digest())
        if (total != expectedSize || !WebDavPaths.equalSha256(sha, expectedSha256)) {
            throw WebDavIntegrityException("备份 SHA256 摘要或完整文件大小校验失败。")
        }
        return WebDavTransferResult(total, sha)
    }

    private fun collectionUrl(parts: List<String>): HttpUrl {
        val builder = base.newBuilder()
        parts.forEach { builder.addPathSegment(it) }
        if (parts.isNotEmpty()) builder.addPathSegment("")
        return builder.build()
    }

    private fun fileUrl(name: String): HttpUrl = directoryUrl.newBuilder()
        .addPathSegment(WebDavPaths.safeFileName(name)).build()

    private fun requireOwnedName(name: String) {
        WebDavPaths.safeFileName(name)
        require(WebDavPaths.isBackup(name) ||
            (name.endsWith(".meta.json") && WebDavPaths.isBackup(name.removeSuffix(".meta.json")))) {
            "只能操作 Shiroha 备份文件及其元数据。"
        }
    }

    private fun request(url: HttpUrl, method: String, body: RequestBody? = null): Request {
        require(WebDavPaths.sameOrigin(base, url) && url.isHttps)
        return Request.Builder().url(url).method(method, body)
            .header("Authorization", Credentials.basic(credentials.username, credentials.password, Charsets.UTF_8))
            .header("Cache-Control", "no-store")
            .build()
    }

    private fun <T> withResponse(initial: Request, cancellation: WebDavCancellation, block: (Response) -> T): T {
        var current = initial
        for (redirects in 0..MAX_REDIRECTS) {
            cancellation.throwIfCancelled()
            val call = transport.newCall(current)
            cancellation.register(call)
            try {
                call.execute().use { response ->
                    if (!WebDavPaths.sameOrigin(base, response.request.url)) throw WebDavException("已阻止来自其他服务器的 WebDAV 响应。")
                    if (response.code !in setOf(301, 302, 303, 307, 308)) return block(response)
                    if (redirects == MAX_REDIRECTS) throw WebDavException("WebDAV 重定向次数过多。")
                    if (response.code == 303 || (current.method !in setOf("GET", "PROPFIND") && response.code !in setOf(307, 308))) {
                        throw WebDavException("已阻止可能改变请求方法的 WebDAV 重定向。")
                    }
                    val location = response.header("Location") ?: throw WebDavException("WebDAV 重定向缺少目标地址。")
                    val uri = runCatching { URI(location) }.getOrNull() ?: throw WebDavException("WebDAV 重定向地址无效。")
                    if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
                        runCatching { WebDavPaths.requireSafeEncodedPath(uri.rawPath.orEmpty()) }.isFailure) {
                        throw WebDavException("已阻止不安全的 WebDAV 重定向。")
                    }
                    val next = current.url.resolve(location) ?: throw WebDavException("WebDAV 重定向地址无效。")
                    // Restrict not just origin but resource identity. A redirect cannot turn DELETE
                    // or an authenticated GET into access to another account/path on the same host.
                    if (!WebDavPaths.sameOrigin(base, next) || !next.isHttps ||
                        next.encodedPath.trimEnd('/') != current.url.encodedPath.trimEnd('/')) {
                        throw WebDavException("WebDAV 重定向改变了服务器或资源路径，已阻止转发账号凭据。")
                    }
                    if (current.body?.isOneShot() == true) throw WebDavException("流式上传不支持重放重定向，请填写最终的 WebDAV 地址。")
                    current = current.newBuilder().url(next).build()
                }
            } catch (error: IOException) {
                cancellation.throwIfCancelled()
                if (error is WebDavException) throw error
                // Raw transport/server errors can contain URLs, query strings or credentials.
                throw WebDavException("WebDAV 网络请求失败或超时，请检查网络后重试。")
            } finally {
                cancellation.unregister(call)
            }
        }
        throw WebDavException("WebDAV 重定向次数过多。")
    }

    private fun failHttp(code: Int): Nothing = throw WebDavException(when (code) {
        401, 403 -> "WebDAV 账号或授权码无效，或没有访问权限。"
        404 -> "远端 WebDAV 资源不存在。"
        409 -> "远端父目录不存在，请先点击创建目录。"
        412 -> "远端文件已存在或发生变化，未执行覆盖。"
        423 -> "远端 WebDAV 资源已被锁定。"
        507 -> "远端 WebDAV 存储空间不足。"
        else -> "WebDAV 请求失败（HTTP $code）。"
    }, code)

    private fun httpStatus(value: String): Int = Regex("HTTP/\\S+\\s+(\\d{3})").find(value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

    private fun parseHttpDate(value: String): Long? = runCatching {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
            isLenient = false
        }.parse(value)?.time
    }.getOrNull()

    companion object {
        const val DEFAULT_MAX_BACKUP_BYTES = 150L * 1024 * 1024
        private const val MAX_METADATA_BYTES = 2L * 1024 * 1024
        private const val MAX_REDIRECTS = 5
        private const val DAV = "DAV:"
        private val BINARY = "application/octet-stream".toMediaType()
        private val XML = "application/xml; charset=utf-8".toMediaType()
        private const val PROPFIND_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/><d:getcontentlength/><d:getlastmodified/><d:getetag/></d:prop></d:propfind>"

        fun hashFile(
            file: File,
            cancellation: WebDavCancellation = WebDavCancellation(),
            maxBytes: Long = DEFAULT_MAX_BACKUP_BYTES
        ): WebDavTransferResult {
            require(file.isFile && maxBytes > 0)
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    cancellation.throwIfCancelled()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    if (total > maxBytes) throw WebDavIntegrityException("本地备份超过安全大小限制。")
                    digest.update(buffer, 0, count)
                }
            }
            cancellation.throwIfCancelled()
            return WebDavTransferResult(total, WebDavPaths.digestHex(digest.digest()))
        }
    }
}
