package com.yiqiu.shirohaquiz.importer.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import com.yiqiu.shirohaquiz.importer.parser.QuestionTextNormalizer
import com.yiqiu.shirohaquiz.importer.parser.TextImportDecoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android-only importer helper for DOCX files that contain inline images.
 *
 * The normal parser still receives text. Images are represented in the text as stable markers,
 * then QuestionImageBinder removes those markers and attaches the extracted files to questions.
 */
object QuestionImportAssetExtractor {
    data class ImportedContent(
        val text: String,
        val images: List<ExtractedImportImage> = emptyList()
    )

    data class ExtractedImportImage(
        val marker: String,
        val image: QuestionImage
    )

    private data class RawZipEntry(
        val name: String,
        val bytes: ByteArray
    )

    fun decode(context: Context, bytes: ByteArray, fileName: String): ImportedContent? {
        if (bytes.isEmpty()) return ImportedContent("")
        val lowerName = fileName.lowercase(Locale.ROOT)
        return if (lowerName.endsWith(".docx") || looksLikeDocx(bytes)) {
            decodeDocxWithImages(context, bytes, fileName)
        } else {
            TextImportDecoder.decode(bytes, fileName)?.let { ImportedContent(it) }
        }
    }

    private fun decodeDocxWithImages(context: Context, bytes: ByteArray, fileName: String): ImportedContent? {
        val entries = readZipEntries(bytes)
        val documentXml = entries.firstOrNull { it.name == "word/document.xml" }?.bytes?.toString(Charsets.UTF_8)
            ?: return TextImportDecoder.decode(bytes, fileName)?.let { ImportedContent(it) }
        val relationshipsXml = entries.firstOrNull { it.name == "word/_rels/document.xml.rels" }?.bytes?.toString(Charsets.UTF_8).orEmpty()
        val relationshipTargets = parseRelationships(relationshipsXml)
        val mediaByName = entries
            .filter { it.name.startsWith("word/media/") }
            .associateBy { it.name }

        if (mediaByName.isEmpty()) {
            return TextImportDecoder.decode(bytes, fileName)?.let { ImportedContent(it) }
        }

        val sessionDir = File(context.filesDir, "question_assets/import_${System.currentTimeMillis()}").apply { mkdirs() }
        val extracted = mutableListOf<ExtractedImportImage>()
        val builder = StringBuilder()
        var order = 0

        val paragraphRegex = Regex("""<w:p[\s\S]*?</w:p>""")
        val paragraphMatches = paragraphRegex.findAll(documentXml).toList()
        if (paragraphMatches.isEmpty()) {
            return TextImportDecoder.decode(bytes, fileName)?.let { ImportedContent(it) }
        }

        paragraphMatches.forEach { paragraphMatch ->
            val paragraphXml = paragraphMatch.value
            val paragraphText = extractTextFromParagraph(paragraphXml).trimEnd()
            if (paragraphText.isNotBlank()) {
                builder.append(paragraphText).append('\n')
            }
            extractEmbedIds(paragraphXml).forEach { relationId ->
                val target = relationshipTargets[relationId] ?: return@forEach
                val mediaName = normalizeWordTarget(target)
                val imageBytes = mediaByName[mediaName]?.bytes ?: return@forEach
                order += 1
                val marker = "[[SHIROHA_IMAGE:img_${order.toString().padStart(4, '0')}]]"
                val image = saveQuestionImage(
                    dir = sessionDir,
                    sourceName = File(mediaName).name,
                    order = order,
                    bytes = imageBytes
                )
                extracted += ExtractedImportImage(marker = marker, image = image)
                builder.append(marker).append('\n')
            }
        }

        val normalized = QuestionTextNormalizer.normalize(builder.toString())
        return ImportedContent(normalized, extracted)
    }

    private fun readZipEntries(bytes: ByteArray): List<RawZipEntry> {
        return buildList {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        add(RawZipEntry(entry.name, zip.readBytes()))
                    }
                }
            }
        }
    }

    private fun looksLikeDocx(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 'P'.code.toByte() &&
            bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() &&
            bytes[3] == 4.toByte()
    }

    private fun parseRelationships(xml: String): Map<String, String> {
        if (xml.isBlank()) return emptyMap()
        val regex = Regex("""<Relationship[^>]*Id="([^"]+)"[^>]*Target="([^"]+)"[^>]*/?>""")
        return regex.findAll(xml).associate { match ->
            match.groupValues[1] to match.groupValues[2]
        }
    }

    private fun normalizeWordTarget(target: String): String {
        val clean = target.replace("\\", "/").removePrefix("/")
        return when {
            clean.startsWith("word/") -> clean
            clean.startsWith("media/") -> "word/$clean"
            clean.startsWith("../") -> "word/${clean.removePrefix("../")}".replace("word/word/", "word/")
            else -> "word/$clean"
        }
    }

    private fun extractEmbedIds(xml: String): List<String> {
        val embedRegex = Regex("""r:embed="([^"]+)"""")
        return embedRegex.findAll(xml).map { it.groupValues[1] }.distinct().toList()
    }

    private fun extractTextFromParagraph(xml: String): String {
        val withBreaks = xml
            .replace("</w:tc>", " ")
            .replace("<w:tab/>", "\t")
            .replace("<w:br/>", "\n")

        return Regex("""<w:t[^>]*>([\s\S]*?)</w:t>""")
            .replace(withBreaks) { match ->
                decodeXmlEntities(match.groupValues[1])
            }
            .replace(Regex("""<[^>]+>"""), "")
            .replace(Regex("""[ \t]{2,}"""), " ")
    }

    private fun decodeXmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun saveQuestionImage(
        dir: File,
        sourceName: String,
        order: Int,
        bytes: ByteArray
    ): QuestionImage {
        val lower = sourceName.lowercase(Locale.ROOT)
        val ext = when {
            lower.endsWith(".png") -> "png"
            lower.endsWith(".webp") -> "webp"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "jpg"
            else -> "png"
        }
        val processed = processImageBytes(bytes, ext)
        val outFile = File(dir, "img_${order.toString().padStart(4, '0')}.${processed.extension}")
        outFile.writeBytes(processed.bytes)
        return QuestionImage(
            localPath = outFile.absolutePath,
            sourceName = sourceName,
            order = order,
            width = processed.width,
            height = processed.height,
            sizeBytes = outFile.length()
        )
    }

    private data class ProcessedImage(
        val bytes: ByteArray,
        val extension: String,
        val width: Int?,
        val height: Int?
    )

    private fun processImageBytes(bytes: ByteArray, ext: String): ProcessedImage {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val originalWidth = bounds.outWidth.takeIf { it > 0 }
        val originalHeight = bounds.outHeight.takeIf { it > 0 }
        val maxDimension = max(originalWidth ?: 0, originalHeight ?: 0)

        val shouldKeepOriginal = bytes.size <= 1_200_000 && maxDimension <= 1800
        if (shouldKeepOriginal || originalWidth == null || originalHeight == null) {
            return ProcessedImage(bytes, ext, originalWidth, originalHeight)
        }

        val targetMax = 1600
        var sampleSize = 1
        while (maxDimension / sampleSize > targetMax * 2) sampleSize *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: return ProcessedImage(bytes, ext, originalWidth, originalHeight)

        val scale = targetMax.toFloat() / max(decoded.width, decoded.height).toFloat()
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt().coerceAtLeast(1),
                (decoded.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val output = ByteArrayOutputStream()
        val format = if (ext == "png" && decoded.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val quality = if (format == Bitmap.CompressFormat.PNG) 100 else 88
        scaled.compress(format, quality, output)
        val finalWidth = scaled.width
        val finalHeight = scaled.height
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        return ProcessedImage(
            bytes = output.toByteArray(),
            extension = extension,
            width = finalWidth,
            height = finalHeight
        )
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
