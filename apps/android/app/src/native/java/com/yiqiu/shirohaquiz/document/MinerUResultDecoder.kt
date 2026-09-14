package com.yiqiu.shirohaquiz.document

import com.yiqiu.shirohaquiz.util.SafeZipReader
import java.io.File
import java.util.zip.ZipInputStream

object MinerUResultDecoder {
    data class DecodedResult(
        val importText: String,
        val hasImageReferences: Boolean
    )

    fun decodeFreeMarkdown(file: File): DecodedResult {
        require(file.isFile) { "Markdown 识别结果不存在。" }
        require(file.length() <= MAX_MARKDOWN_BYTES) { "Markdown 识别结果过大。" }
        return decodeMarkdown(file.readText(Charsets.UTF_8))
    }

    fun decodePreciseZip(file: File): DecodedResult {
        require(file.isFile) { "精准识别结果不存在。" }
        var entryCount = 0
        var markdown: String? = null
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount += 1
                require(entryCount <= MAX_ZIP_ENTRIES) { "精准识别结果包含过多文件。" }
                if (entry.isDirectory) continue
                val safeName = SafeZipReader.normalizeEntryName(entry.name)
                if (safeName == "full.md" || safeName.endsWith("/full.md")) {
                    val bytes = SafeZipReader.readEntryBytes(
                        zip = zip,
                        entry = entry,
                        maxSize = MAX_MARKDOWN_BYTES
                    )
                    markdown = bytes.toString(Charsets.UTF_8)
                    break
                }
            }
        }
        val value = markdown ?: throw IllegalArgumentException("精准识别结果中没有找到 full.md。")
        return decodeMarkdown(value)
    }

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
            .filterNot { it.trim().matches(FENCE_LINE) || it.trim().matches(HORIZONTAL_RULE) }
            .joinToString("\n")
            .replace(MARKDOWN_LINK) { it.groupValues[1] }
            .replace("**", "")
            .replace("__", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        require(text.isNotBlank()) { "识别完成，但没有得到可用文本。" }
        return DecodedResult(importText = text, hasImageReferences = hasImages)
    }

    private const val MAX_MARKDOWN_BYTES = 24L * 1024L * 1024L
    private const val MAX_ZIP_ENTRIES = 2_000
    private val MARKDOWN_IMAGE = Regex("!\\[([^]]*)]\\(([^)]+)\\)")
    private val MARKDOWN_LINK = Regex("(?<!!)\\[([^]]+)]\\(([^)]+)\\)")
    private val HEADING_PREFIX = Regex("^\\s{0,3}#{1,6}\\s+")
    private val BLOCK_QUOTE_PREFIX = Regex("^\\s{0,3}>\\s?")
    private val UNORDERED_LIST_PREFIX = Regex("^\\s{0,3}[-+*]\\s+")
    private val FENCE_LINE = Regex("^(```|~~~).*$")
    private val HORIZONTAL_RULE = Regex("^([-*_])(?:\\s*\\1){2,}\\s*$")
}
