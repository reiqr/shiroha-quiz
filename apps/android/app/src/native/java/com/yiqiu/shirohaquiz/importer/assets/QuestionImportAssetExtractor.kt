package com.yiqiu.shirohaquiz.importer.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import com.yiqiu.shirohaquiz.importer.parser.ExcelQuestionTableParser
import com.yiqiu.shirohaquiz.importer.parser.QuestionTextNormalizer
import com.yiqiu.shirohaquiz.importer.parser.TextImportDecoder
import com.yiqiu.shirohaquiz.util.SafeZipReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import java.util.UUID
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
        val images: List<ExtractedImportImage> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    sealed class DecodeResult {
        data class Success(val content: ImportedContent) : DecodeResult()
        data class Failure(
            val userMessage: String,
            val technicalMessage: String? = null
        ) : DecodeResult()
    }

    data class ExtractedImportImage(
        val marker: String,
        val image: QuestionImage
    )

    private data class RawZipEntry(
        val name: String,
        val bytes: ByteArray
    )

    fun decode(context: Context, bytes: ByteArray, fileName: String): ImportedContent? {
        return when (val result = decodeDetailed(context, bytes, fileName)) {
            is DecodeResult.Success -> result.content
            is DecodeResult.Failure -> null
        }
    }

    fun decodeDetailed(context: Context, bytes: ByteArray, fileName: String): DecodeResult {
        if (bytes.isEmpty()) return DecodeResult.Success(ImportedContent(""))
        val lowerName = fileName.lowercase(Locale.ROOT)
        return if (lowerName.endsWith(".docx") || (looksLikeDocx(bytes) && !lowerName.endsWith(".xlsx"))) {
            decodeDocxWithImagesDetailed(context, bytes, fileName)
        } else if (lowerName.endsWith(".xlsx")) {
            decodeXlsxWithImagesDetailed(context, bytes, fileName)
        } else {
            TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult()
        }
    }

    private fun decodeXlsxWithImagesDetailed(context: Context, bytes: ByteArray, fileName: String): DecodeResult {
        val textFallback = { TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult() }
        val entries = runCatching { readZipEntries(bytes) }.getOrElse { return textFallback() }
        val byPath = entries.associateBy { it.name }
        val markersBySheetRow = mutableMapOf<String, MutableMap<Int, MutableList<String>>>()
        val extracted = mutableListOf<ExtractedImportImage>()
        val locations = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()
        val sessionDir = File(context.filesDir, "question_assets/import_${UUID.randomUUID()}").apply { mkdirs() }

        entries.filter { it.name.matches(Regex("""xl/worksheets/sheet\d+\.xml""")) }.forEach { sheet ->
            val sheetXml = sheet.bytes.toString(Charsets.UTF_8)
            val sheetName = sheet.name.substringAfterLast('/')
            val sheetRelsPath = sheet.name.replaceAfterLast('/', "_rels/$sheetName.rels")
            val sheetRels = parseXlsxRelationships(byPath[sheetRelsPath]?.bytes?.toString(Charsets.UTF_8).orEmpty())
            val drawingIds = Regex("""<(?:\w+:)?drawing\b[^>]*\br:id="([^"]+)"""")
                .findAll(sheetXml).map { it.groupValues[1] }.toList()
            drawingIds.forEach drawingLoop@ { drawingId ->
                val relation = sheetRels[drawingId] ?: return@drawingLoop
                if (!relation.first.endsWith("/drawing")) return@drawingLoop
                val drawingPath = resolveXlsxPart(sheet.name, relation.second) ?: return@drawingLoop
                val drawingXml = byPath[drawingPath]?.bytes?.toString(Charsets.UTF_8) ?: return@drawingLoop
                val drawingName = drawingPath.substringAfterLast('/')
                val drawingRelsPath = drawingPath.replaceAfterLast('/', "_rels/$drawingName.rels")
                val mediaRels = parseXlsxRelationships(byPath[drawingRelsPath]?.bytes?.toString(Charsets.UTF_8).orEmpty())
                val anchors = Regex("""<(?:\w+:)?(?:oneCellAnchor|twoCellAnchor)\b[\s\S]*?</(?:\w+:)?(?:oneCellAnchor|twoCellAnchor)>""")
                    .findAll(drawingXml)
                anchors.forEach anchorLoop@ { match ->
                    val anchor = match.value
                    val row = Regex("""<(?:\w+:)?from>\s*[\s\S]*?<(?:\w+:)?row>(\d+)</(?:\w+:)?row>""")
                        .find(anchor)?.groupValues?.get(1)?.toIntOrNull()?.plus(1)
                    val lastRow = Regex("""<(?:\w+:)?to>\s*[\s\S]*?<(?:\w+:)?row>(\d+)</(?:\w+:)?row>""")
                        .find(anchor)?.groupValues?.get(1)?.toIntOrNull()?.plus(1)
                    val location = "$sheetName 第 ${row ?: "?"} 行"
                    val embed = Regex("""<(?:\w+:)?blip\b[^>]*\br:embed="([^"]+)"""")
                        .find(anchor)?.groupValues?.get(1)
                    if (embed == null) {
                        warnings += "$location：图片未内嵌或使用外部链接，需人工核对"
                        return@anchorLoop
                    }
                    if (row == null || row < 1 || (lastRow != null && lastRow > row)) {
                        warnings += "$location：图片锚点跨行或无效，需人工核对"
                        return@anchorLoop
                    }
                    val mediaRel = mediaRels[embed]
                    val mediaPath = mediaRel?.takeIf { it.first.endsWith("/image") }
                        ?.let { resolveXlsxPart(drawingPath, it.second) }
                    val media = mediaPath?.takeIf { it.startsWith("xl/media/") }?.let(byPath::get)
                    if (media == null || !media.name.lowercase(Locale.ROOT).matches(Regex(""".*\.(png|jpe?g|webp)"""))) {
                        warnings += "$location：图片资源缺失或格式不支持"
                        return@anchorLoop
                    }
                    val marker = QuestionImageMarker.canonical("img_${(extracted.size + 1).toString().padStart(4, '0')}")
                    val image = runCatching {
                        saveQuestionImage(sessionDir, File(media.name).name, extracted.size + 1, media.bytes)
                    }.getOrNull()
                    if (image?.width == null || image.height == null) {
                        image?.localPath?.let { File(it).delete() }
                        warnings += "$location：图片无法解码"
                        return@anchorLoop
                    }
                    extracted += ExtractedImportImage(marker, image)
                    locations[marker] = location
                    markersBySheetRow.getOrPut(sheet.name) { mutableMapOf() }
                        .getOrPut(row) { mutableListOf() }.add(marker)
                }
            }
        }
        if (extracted.isEmpty()) {
            sessionDir.delete()
            return when (val fallback = textFallback()) {
                is DecodeResult.Success -> fallback.copy(content = fallback.content.copy(warnings = warnings))
                is DecodeResult.Failure -> fallback
            }
        }
        val text = TextImportDecoder.decodeXlsxWithMarkers(bytes, markersBySheetRow) ?: run {
            extracted.forEach { File(it.image.localPath).delete() }
            sessionDir.delete()
            return textFallback()
        }
        val parsedQuestions = ExcelQuestionTableParser.parse(text)
        val attached = extracted.filter { image -> parsedQuestions.any { it.question.contains(image.marker) } }
        extracted.filterNot { it in attached }.forEach {
            File(it.image.localPath).delete()
            warnings += "${locations[it.marker]}：图片未能对应标准表格题目，需人工核对"
        }
        if (attached.isEmpty()) sessionDir.delete()
        return DecodeResult.Success(ImportedContent(text, attached, warnings))
    }

    private fun parseXlsxRelationships(xml: String): Map<String, Pair<String, String>> =
        Regex("""<Relationship\b[^>]*>""").findAll(xml).mapNotNull { match ->
            val attrs = Regex("""\b([A-Za-z]+)="([^"]*)"""").findAll(match.value)
                .associate { it.groupValues[1] to it.groupValues[2] }
            val id = attrs["Id"] ?: return@mapNotNull null
            if (attrs["TargetMode"].equals("External", ignoreCase = true)) return@mapNotNull null
            id to ((attrs["Type"].orEmpty()) to (attrs["Target"].orEmpty()))
        }.toMap()

    private fun resolveXlsxPart(base: String, target: String): String? {
        if (target.isBlank() || target.contains(':') || target.startsWith("//")) return null
        val raw = if (target.startsWith('/')) target.removePrefix("/") else base.substringBeforeLast('/') + "/" + target
        val parts = mutableListOf<String>()
        raw.replace('\\', '/').split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isEmpty()) return null else parts.removeAt(parts.lastIndex)
                else -> parts += part
            }
        }
        return parts.joinToString("/").takeIf { it.startsWith("xl/") }
    }

    private fun decodeDocxWithImagesDetailed(context: Context, bytes: ByteArray, fileName: String): DecodeResult {
        val entries = runCatching { readZipEntries(bytes) }.getOrElse { error ->
            return DecodeResult.Failure(
                userMessage = "文件无法读取，可能已损坏。请重新导出或另存为 docx 后再试。",
                technicalMessage = error.message
            )
        }
        val documentXml = entries.firstOrNull { it.name == "word/document.xml" }?.bytes?.toString(Charsets.UTF_8)
            ?: return TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult()
        val relationshipsXml = entries.firstOrNull { it.name == "word/_rels/document.xml.rels" }?.bytes?.toString(Charsets.UTF_8).orEmpty()
        val relationshipTargets = parseRelationships(relationshipsXml)
        val mediaByName = entries
            .filter { it.name.startsWith("word/media/") }
            .associateBy { it.name }

        if (mediaByName.isEmpty()) {
            return TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult()
        }

        val sessionDir = File(context.filesDir, "question_assets/import_${System.currentTimeMillis()}").apply { mkdirs() }
        val extracted = mutableListOf<ExtractedImportImage>()
        val builder = StringBuilder()
        var order = 0

        val paragraphRegex = Regex("""<w:p[\s\S]*?</w:p>""")
        val paragraphMatches = paragraphRegex.findAll(documentXml).toList()
        if (paragraphMatches.isEmpty()) {
            return TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult()
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
                val marker = QuestionImageMarker.canonical("img_${order.toString().padStart(4, '0')}")
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
        return if (normalized.isBlank()) {
            TextImportDecoder.decodeDetailed(bytes, fileName).toImportedContentResult()
        } else {
            DecodeResult.Success(ImportedContent(normalized, extracted))
        }
    }

    private fun TextImportDecoder.DecodeResult.toImportedContentResult(): DecodeResult {
        return when (this) {
            is TextImportDecoder.DecodeResult.Success -> DecodeResult.Success(ImportedContent(text))
            is TextImportDecoder.DecodeResult.Failure -> DecodeResult.Failure(userMessage, technicalMessage)
        }
    }

    private fun readZipEntries(bytes: ByteArray): List<RawZipEntry> {
        val maxTotalSize = 50L * 1024 * 1024
        val maxEntrySize = 10L * 1024 * 1024
        val maxEntries = 500
        var totalSize = 0L
        return buildList {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (size >= maxEntries) break
                    val name = SafeZipReader.normalizeEntryName(entry.name)
                    val data = SafeZipReader.readEntryBytes(
                        zip = zip,
                        entry = entry,
                        maxSize = maxEntrySize,
                        maxTotalRemaining = maxTotalSize - totalSize
                    )
                    totalSize += data.size
                    add(RawZipEntry(name, data))
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
        return TextImportDecoder.extractTextFromWordParagraphXml(xml)
    }

    fun saveQuestionImage(
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

        val shouldKeepOriginal = bytes.size <= 800_000 && maxDimension <= 1400
        if (shouldKeepOriginal || originalWidth == null || originalHeight == null) {
            return ProcessedImage(bytes, ext, originalWidth, originalHeight)
        }

        val targetMax = 1200
        var sampleSize = 1
        while (maxDimension / sampleSize > targetMax * 2) sampleSize *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (_: OutOfMemoryError) {
            null
        } ?: return ProcessedImage(bytes, ext, originalWidth, originalHeight)

        val scale = targetMax.toFloat() / max(decoded.width, decoded.height).toFloat()
        val scaled = try {
            if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
        } catch (_: OutOfMemoryError) {
            decoded.recycle()
            return ProcessedImage(bytes, ext, originalWidth, originalHeight)
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
}
