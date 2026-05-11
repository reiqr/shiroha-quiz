package com.yiqiu.shirohaquiz.importer.parser

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream

object TextImportDecoder {
    fun decode(bytes: ByteArray, fileName: String): String? {
        if (bytes.isEmpty()) return ""
        val lowerName = fileName.lowercase(Locale.ROOT)

        return when {
            lowerName.endsWith(".docx") || looksLikeDocx(bytes) -> decodeDocx(bytes)
            else -> decodePlainText(bytes)
        }?.let(QuestionTextNormalizer::normalize)
    }

    private fun decodePlainText(bytes: ByteArray): String? {
        val utf8 = bytes.toString(Charsets.UTF_8)
        if ('�' !in utf8) return utf8
        return try {
            bytes.toString(Charset.forName("GB18030"))
        } catch (_: Exception) {
            utf8
        }
    }

    private fun looksLikeDocx(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()
    }

    private fun decodeDocx(bytes: ByteArray): String? {
        return runCatching {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return@runCatching extractTextFromWordXml(xml)
                    }
                }
                null
            }
        }.getOrNull()
    }

    internal fun extractTextFromWordXml(xml: String): String {
        val withBreaks = xml
            .replace("</w:p>", "\n")
            .replace("</w:tr>", "\n")
            .replace("</w:tc>", " ")
            .replace("<w:tab/>", "\t")
            .replace("<w:br/>", "\n")

        val text = Regex("""<w:t[^>]*>([\s\S]*?)</w:t>""")
            .replace(withBreaks) { match ->
                decodeXmlEntities(match.groupValues[1])
            }
            .replace(Regex("""<[^>]+>"""), "")

        return text
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun ZipInputStream.readBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
