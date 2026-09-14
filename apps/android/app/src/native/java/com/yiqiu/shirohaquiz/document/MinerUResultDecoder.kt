package com.yiqiu.shirohaquiz.document

import com.yiqiu.shirohaquiz.util.SafeZipReader
import android.graphics.BitmapFactory
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import org.json.JSONArray
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipInputStream

object MinerUResultDecoder {
    data class DecodedResult(
        val importText: String,
        val hasImageReferences: Boolean,
        val images: List<DocumentImageAsset> = emptyList(),
        val notes: List<String> = emptyList()
    )

    fun decodeFreeMarkdown(file: File): DecodedResult {
        require(file.isFile) { "Markdown 识别结果不存在。" }
        require(file.length() <= MAX_MARKDOWN_BYTES) { "Markdown 识别结果过大。" }
        return decodeMarkdown(file.readText(Charsets.UTF_8))
    }

    fun decodePreciseZip(file: File, imageDir: File = File(file.parentFile, "${file.nameWithoutExtension}_images")): DecodedResult {
        require(file.isFile) { "精准识别结果不存在。" }
        var entryCount = 0
        var markdown: String? = null
        var contentList: String? = null
        var totalBytes = 0L
        val seen = mutableSetOf<String>()
        val extracted = linkedMapOf<String, DocumentImageAsset>()
        val notes = mutableListOf<String>()
        imageDir.mkdirs()
        try {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ZIP_ENTRIES) { "精准识别结果包含过多文件。" }
                val safeName = SafeZipReader.normalizeEntryName(entry.name.trimEnd('/'))
                if (entry.isDirectory) continue
                require(seen.add(safeName)) { "识别 ZIP 包含重复文件：$safeName" }
                val isImage = safeName.substringAfterLast('/').contains(IMAGE_EXTENSION)
                val isText = safeName == "full.md" || safeName.endsWith("/full.md") || safeName.endsWith("content_list.json")
                if (!isImage && !isText) {
                    totalBytes += drainEntry(zip, MAX_TOTAL_BYTES - totalBytes)
                    continue
                }
                val limit = if (isImage) MAX_IMAGE_BYTES else MAX_MARKDOWN_BYTES
                val bytes = SafeZipReader.readEntryBytes(zip, entry, limit, MAX_TOTAL_BYTES - totalBytes)
                totalBytes += bytes.size
                if (safeName == "full.md" || safeName.endsWith("/full.md")) {
                    markdown = bytes.toString(Charsets.UTF_8)
                } else if (safeName.endsWith("content_list.json")) {
                    contentList = bytes.toString(Charsets.UTF_8)
                } else if (isImage) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                        bounds.outWidth.toLong() * bounds.outHeight > MAX_IMAGE_PIXELS) {
                        notes += "图片 ${safeName.substringAfterLast('/')} 无法解码或尺寸过大，未导入。"
                        continue
                    }
                    val order = extracted.size + 1
                    val image = QuestionImportAssetExtractor.saveQuestionImage(imageDir, safeName, order, bytes)
                    extracted[safeName] = DocumentImageAsset(QuestionImageMarker.canonical("img_${order.toString().padStart(4, '0')}"), image)
                }
            }
        }
        val entries = contentList?.let { runCatching { JSONArray(it) }.getOrNull() }
        if (contentList != null && entries == null) notes += "content_list.json 无法读取，已使用 Markdown 正文。"
        val value = markdown?.takeIf { it.isNotBlank() } ?: entries?.let(::contentListMarkdown)
            ?: throw IllegalArgumentException("精准识别结果中没有找到可用正文。")
        val referenced = mutableListOf<String>()
        val replaced = MARKDOWN_IMAGE.replace(value) { match ->
            val reference = match.groupValues[2].trim().substringBefore(" \"")
            val asset = resolveImage(reference, extracted)
            if (asset != null) {
                if (asset.marker !in referenced) referenced += asset.marker
                asset.marker
            } else {
                notes += "图片引用未找到文件：${reference.take(100)}"
                "【图片缺失：请核对】"
            }
        }
        val decoded = decodeMarkdown(replaced)
        val images = extracted.values.sortedBy { referenced.indexOf(it.marker).takeIf { position -> position >= 0 } ?: Int.MAX_VALUE }
            .mapIndexed { index, asset -> asset.copy(image = asset.image.copy(order = index + 1)) }
        return decoded.copy(hasImageReferences = MARKDOWN_IMAGE.containsMatchIn(value) || images.isNotEmpty(), images = images, notes = notes.distinct())
        } catch (error: Throwable) {
            imageDir.deleteRecursively()
            throw error
        }
    }

    private fun resolveImage(reference: String, assets: Map<String, DocumentImageAsset>): DocumentImageAsset? {
        val decoded = runCatching { URLDecoder.decode(reference.replace("+", "%2B"), "UTF-8") }.getOrDefault(reference)
            .removePrefix("./").substringBefore('?').substringBefore('#')
        if (!SafeZipReader.isSafeEntryName(decoded)) return null
        assets[decoded]?.let { return it }
        return assets.filterKeys { it.endsWith("/$decoded") || it.substringAfterLast('/') == decoded.substringAfterLast('/') }
            .values.singleOrNull()
    }

    private fun drainEntry(zip: ZipInputStream, maximum: Long): Long {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maximum) { "精准结果 ZIP 解压后超过总大小限制。" }
        }
        return total
    }

    private fun contentListMarkdown(entries: JSONArray): String = buildList {
        for (index in 0 until entries.length()) {
            val item = entries.optJSONObject(index) ?: continue
            val body = item.optString("text").ifBlank { item.optString("table_body") }
            if (body.isNotBlank()) add(body)
            val path = item.optString("img_path")
            if (path.isNotBlank()) add("![]($path)")
            listOf("image_caption", "table_caption", "table_footnote").forEach { key ->
                val captions = item.optJSONArray(key)
                if (captions != null) for (i in 0 until captions.length()) add(captions.optString(i))
            }
        }
    }.joinToString("\n\n")

    fun decodeMarkdown(markdown: String): DecodedResult {
        val normalized = markdown
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val hasImages = MARKDOWN_IMAGE.containsMatchIn(normalized)
        val withoutImages = MARKDOWN_IMAGE.replace(normalized) { match ->
            val alt = match.groupValues.getOrNull(1).orEmpty().trim()
            if (alt.isBlank()) "【图片位置：请在导入前核对】" else "【图片位置：$alt】"
        }
        val text = withoutImages
            .lineSequence()
            .map { line ->
                line
                    .replace(HEADING_PREFIX, "")
                    .replace(BLOCK_QUOTE_PREFIX, "")
                    .replace(UNORDERED_LIST_PREFIX, "")
            }
            .filterNot { it.trim().matches(HORIZONTAL_RULE) }
            .joinToString("\n")
            .replace(MARKDOWN_LINK) { it.groupValues[1] }
            .replace("**", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        require(text.isNotBlank()) { "识别完成，但没有得到可用文本。" }
        return DecodedResult(importText = text, hasImageReferences = hasImages)
    }

    private const val MAX_MARKDOWN_BYTES = 24L * 1024L * 1024L
    private const val MAX_ZIP_ENTRIES = 2_000
    private const val MAX_IMAGE_BYTES = 16L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 300L * 1024L * 1024L
    private const val MAX_IMAGE_PIXELS = 40_000_000L
    private val IMAGE_EXTENSION = Regex("\\.(png|jpe?g|webp|gif|bmp)$", RegexOption.IGNORE_CASE)
    private val MARKDOWN_IMAGE = Regex("!\\[([^]]*)]\\(([^)]+)\\)")
    private val MARKDOWN_LINK = Regex("(?<!!)\\[([^]]+)]\\(([^)]+)\\)")
    private val HEADING_PREFIX = Regex("^\\s{0,3}#{1,6}\\s+")
    private val BLOCK_QUOTE_PREFIX = Regex("^\\s{0,3}>\\s?")
    private val UNORDERED_LIST_PREFIX = Regex("^\\s{0,3}[-+*]\\s+")
    private val HORIZONTAL_RULE = Regex("^([-*_])(?:\\s*\\1){2,}\\s*$")
}
