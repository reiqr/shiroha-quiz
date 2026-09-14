package com.yiqiu.shirohaquiz.document

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit

class MinerUClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        // 签名地址由服务端响应给出，禁止重定向可避免被跳转到任意主机或降级到明文 HTTP。
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) {
    fun requestFreeUpload(
        fileName: String,
        settings: MinerUSettings
    ): MinerUUploadTicket {
        val payload = JSONObject()
            .put("file_name", fileName)
            .put("language", settings.language)
            .put("enable_table", settings.enableTable)
            .put("is_ocr", settings.isOcr)
            .put("enable_formula", settings.enableFormula)
        settings.pageRange.trim().takeIf { it.isNotBlank() }?.let { payload.put("page_range", it) }

        val root = executeJson(
            Request.Builder()
                .url(FREE_UPLOAD_ENDPOINT)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        )
        val data = requireSuccess(root)
        return MinerUUploadTicket(
            taskId = data.requireText("task_id"),
            uploadUrl = data.requireHttpsUrl("file_url")
        )
    }

    fun requestPreciseUpload(
        fileName: String,
        token: String,
        settings: MinerUSettings,
        dataId: String
    ): MinerUUploadTicket {
        val file = JSONObject()
            .put("name", fileName)
            .put("data_id", dataId)
            .put("is_ocr", settings.isOcr)
        settings.pageRange.trim().takeIf { it.isNotBlank() }?.let { file.put("page_ranges", it) }

        val payload = JSONObject()
            .put("files", JSONArray().put(file))
            .put("model_version", settings.modelVersion.apiValue)
            .put("extra_formats", JSONArray(settings.extraFormats.filter { it in listOf("docx", "html", "latex") }))
            .put("language", settings.language)
            .put("enable_table", settings.enableTable)
            .put("enable_formula", settings.enableFormula)

        val root = executeJson(
            Request.Builder()
                .url(PRECISE_UPLOAD_ENDPOINT)
                .header("Authorization", "Bearer ${token.trim()}")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        )
        val data = requireSuccess(root)
        val urls = data.optJSONArray("file_urls")
            ?: throw MinerUException("MinerU 未返回文件上传地址。")
        val uploadUrl = urls.optString(0).takeIf { it.startsWith("https://") }
            ?: throw MinerUException("MinerU 返回的上传地址无效。")
        return MinerUUploadTicket(
            batchId = data.requireText("batch_id"),
            uploadUrl = uploadUrl
        )
    }

    /** The signed MinerU upload explicitly requires no Content-Type header. */
    fun uploadSignedFile(uploadUrl: String, source: File, onProgress: (Long) -> Unit = {}) {
        require(source.isFile && source.length() > 0L) { "待上传 PDF 不存在或为空。" }
        requirePublicHttpsUrl(uploadUrl, "MinerU 返回的上传地址")
        val request = Request.Builder()
            .url(uploadUrl)
            .put(object : RequestBody() {
                override fun contentType() = null
                override fun contentLength() = source.length()
                override fun writeTo(sink: BufferedSink) {
                    source.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var uploaded = 0L
                        var lastReportedAt = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            sink.write(buffer, 0, read)
                            uploaded += read
                            val now = System.nanoTime()
                            if (now - lastReportedAt >= 200_000_000 || uploaded == source.length()) {
                                onProgress(uploaded)
                                lastReportedAt = now
                            }
                        }
                    }
                }
            })
            .build()
        execute(request).use { response ->
            if (response.code !in setOf(200, 201, 204)) {
                throw MinerUException("PDF 上传失败（HTTP ${response.code}）。${response.body?.string().orEmpty().take(180)}")
            }
        }
    }

    fun queryFree(taskId: String): MinerUTaskStatus {
        val root = executeJson(
            Request.Builder()
                .url("$FREE_QUERY_ENDPOINT/${encodePathSegment(taskId)}")
                .get()
                .build()
        )
        val data = requireSuccess(root)
        return MinerUTaskStatus(
            state = data.optString("state", "pending"),
            resultUrl = data.optNullableHttpsUrl("markdown_url"),
            errorCode = data.opt("err_code")?.toString(),
            errorMessage = data.optString("err_msg").takeIf { it.isNotBlank() }
        )
    }

    fun queryPrecise(batchId: String, token: String, fileName: String): MinerUTaskStatus {
        val root = executeJson(
            Request.Builder()
                .url("$PRECISE_QUERY_ENDPOINT/${encodePathSegment(batchId)}")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()
        )
        val data = requireSuccess(root)
        val result = findPreciseResult(data, fileName)
            ?: return MinerUTaskStatus(state = "pending")
        val progress = result.optJSONObject("extract_progress")
        return MinerUTaskStatus(
            state = result.optString("state", "pending"),
            resultUrl = result.optNullableHttpsUrl("full_zip_url"),
            errorMessage = result.optString("err_msg").takeIf { it.isNotBlank() },
            extractedPages = progress?.optInt("extracted_pages", 0) ?: 0,
            totalPages = progress?.optInt("total_pages", 0) ?: 0
        )
    }

    fun download(url: String, destination: File, maxBytes: Long) {
        requirePublicHttpsUrl(url, "MinerU 返回的结果地址")
        require(maxBytes > 0L) { "下载大小限制必须大于 0。" }
        val request = Request.Builder().url(url).get().build()
        execute(request).use { response ->
            if (!response.isSuccessful) {
                val message = "识别结果下载失败（HTTP ${response.code}）。"
                // 4xx 说明结果地址或权限已失效，重复拉取不会有不同结果。
                if (response.code in 400..499) throw MinerUResultException(message)
                throw MinerUException(message)
            }
            val body = response.body ?: throw MinerUException("识别结果为空。")
            if (body.contentLength() > maxBytes) {
                throw MinerUResultException("识别结果超过本地安全限制。")
            }
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        total += count
                        if (total > maxBytes) {
                            destination.delete()
                            throw MinerUResultException("识别结果超过本地安全限制。")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    private fun findPreciseResult(data: JSONObject, fileName: String): JSONObject? {
        val raw = data.opt("extract_result")
        return when (raw) {
            is JSONObject -> raw
            is JSONArray -> {
                (0 until raw.length())
                    .mapNotNull { raw.optJSONObject(it) }
                    .firstOrNull { it.optString("file_name") == fileName }
                    ?: raw.optJSONObject(0)
            }
            else -> null
        }
    }

    private fun executeJson(request: Request): JSONObject {
        execute(request).use { response ->
            // 先做有界读取再判断长度，避免超大响应先把内存吃满。
            val raw = response.peekBody(MAX_JSON_CHARS.toLong() + 1L).string()
            if (!response.isSuccessful) {
                val apiMessage = runCatching { JSONObject(raw).optString("msg") }.getOrNull().orEmpty()
                val detail = apiMessage.takeIf { it.isNotBlank() } ?: raw.take(180)
                throw MinerUException(httpErrorMessage(response.code, detail))
            }
            if (raw.length > MAX_JSON_CHARS) throw MinerUException("MinerU 返回的数据异常过大。")
            return runCatching { JSONObject(raw) }
                .getOrElse { throw MinerUException("MinerU 返回了无法识别的数据。") }
        }
    }

    private fun execute(request: Request) = try {
        client.newCall(request).execute()
    } catch (_: SocketTimeoutException) {
        throw MinerUException("连接 MinerU 超时，请稍后继续查询。")
    } catch (error: IOException) {
        throw MinerUException("无法连接 MinerU：${error.message ?: "请检查网络"}")
    }

    fun cancelActiveRequests() { client.dispatcher.cancelAll() }

    private fun requireSuccess(root: JSONObject): JSONObject {
        val code = root.opt("code")?.toString().orEmpty()
        if (code != "0") {
            val message = root.optString("msg").ifBlank { minerUErrorMessage(code) }
            throw MinerUException("MinerU 请求失败（$code）：$message")
        }
        return root.optJSONObject("data") ?: throw MinerUException("MinerU 返回数据不完整。")
    }

    private fun JSONObject.requireText(key: String): String {
        return optString(key).takeIf { it.isNotBlank() }
            ?: throw MinerUException("MinerU 返回数据缺少 $key。")
    }

    private fun JSONObject.requireHttpsUrl(key: String): String {
        return requirePublicHttpsUrl(requireText(key), "MinerU 返回的 $key")
    }

    private fun JSONObject.optNullableHttpsUrl(key: String): String? {
        val value = optString(key)
        return runCatching { requirePublicHttpsUrl(value, "MinerU 返回的 $key") }.getOrNull()
    }

    /**
     * 服务端给出的地址一律要求是公网 HTTPS：拒绝明文、内网/环回/链路本地字面量与空主机，
     * 避免签名地址被篡改后把本地 PDF 上传到攻击者主机或形成 SSRF。
     */
    private fun requirePublicHttpsUrl(url: String, subject: String): String {
        if (!url.startsWith("https://", ignoreCase = true)) {
            throw MinerUException("$subject 不是 HTTPS 地址。")
        }
        val host = runCatching { URI(url).host }.getOrNull()?.trim()?.lowercase().orEmpty()
        if (host.isBlank()) throw MinerUException("$subject 缺少有效主机名。")
        if (isPrivateOrLocalHost(host)) throw MinerUException("$subject 指向内网地址，已拒绝。")
        return url
    }

    private fun isPrivateOrLocalHost(host: String): Boolean {
        val literal = host.removePrefix("[").removeSuffix("]")
        IPV4_LITERAL.matchEntire(literal)?.let { match ->
            val parts = match.groupValues.drop(1).map { it.toIntOrNull() ?: return true }
            if (parts.any { it !in 0..255 }) return true
            val first = parts[0]
            val second = parts[1]
            return first == 0 || first == 10 || first == 127 ||
                (first == 172 && second in 16..31) ||
                (first == 192 && second == 168) ||
                (first == 169 && second == 254) ||
                (first == 100 && second in 64..127)
        }
        if (literal.contains(':')) {
            return literal == "::1" ||
                literal.startsWith("fc") || literal.startsWith("fd") ||
                literal.startsWith("fe80") ||
                literal.startsWith("::ffff:127") || literal.startsWith("::ffff:10.")
        }
        return literal == "localhost"
    }

    private fun encodePathSegment(value: String): String {
        require(value.matches(Regex("[A-Za-z0-9._-]+"))) { "任务 ID 格式无效。" }
        return value
    }

    private fun httpErrorMessage(code: Int, detail: String): String {
        val prefix = when (code) {
            401, 403 -> "MinerU Token 无效或无权限"
            413 -> "上传文件超过服务限制"
            429 -> "免费接口请求过于频繁，请稍后再试"
            in 500..599 -> "MinerU 服务暂时不可用"
            else -> "MinerU 请求失败（HTTP $code）"
        }
        return if (detail.isBlank()) "$prefix。" else "$prefix：$detail"
    }

    private fun minerUErrorMessage(code: String): String = when (code) {
        "-30001" -> "文件超过免费模式 10MB 限制。"
        "-30002" -> "免费模式不支持该文件类型。"
        "-30003" -> "页数超过免费模式限制，请指定不超过 20 页的范围或使用精准模式。"
        "-30004" -> "识别参数无效。"
        "A0202", "A0211" -> "Token 无效或已过期。"
        "-60005" -> "文件超过精准模式 200MB 限制。"
        "-60006" -> "页数超过精准模式 200 页限制。"
        "-60009" -> "识别队列已满，请稍后重试。"
        else -> "请稍后重试。"
    }

    companion object {
        private const val FREE_UPLOAD_ENDPOINT = "https://mineru.net/api/v1/agent/parse/file"
        private const val FREE_QUERY_ENDPOINT = "https://mineru.net/api/v1/agent/parse"
        private const val PRECISE_UPLOAD_ENDPOINT = "https://mineru.net/api/v4/file-urls/batch"
        private const val PRECISE_QUERY_ENDPOINT = "https://mineru.net/api/v4/extract-results/batch"
        private const val MAX_JSON_CHARS = 2_000_000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val IPV4_LITERAL = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
    }
}

open class MinerUException(message: String) : IOException(message)

/**
 * 结果本身有问题的确定性失败（超限、解不出正文等）。重复拉取同一个结果不会有不同结局，
 * 因此调用方应直接判定任务失败，而不是当作可恢复的瞬时中断。
 */
class MinerUResultException(message: String) : MinerUException(message)
