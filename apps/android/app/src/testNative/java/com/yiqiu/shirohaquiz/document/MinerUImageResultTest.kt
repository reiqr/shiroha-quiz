package com.yiqiu.shirohaquiz.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MinerUImageResultTest {
    @Test fun extractsAndCanonicalizesImageForExistingParser() {
        withResult { directory ->
            val result = decode(directory, "1. 看图选择。\n![](images/chart.png)\nA. 是\nB. 否\n答案：A")
            val image = result.images.single()
            assertEquals("img_0001", QuestionImageMarker.markerId(image.marker))
            assertTrue(result.importText.contains(image.marker))
            assertTrue(File(image.image.localPath).isFile)
            assertEquals(20, image.image.width)
            assertEquals(12, image.image.height)
        }
    }

    @Test fun missingImageIsVisibleAndNotBoundToAnotherFile() {
        withResult { directory ->
            val result = decode(directory, "1. 看图作答。\n![](images/missing.png)\n答案：A")
            assertTrue(result.importText.contains("图片缺失"))
            assertTrue(result.notes.isNotEmpty())
        }
    }

    @Test fun mergesTwoImagesOnlyWhenExplicitlyRequested() {
        withResult { directory ->
            val first = decode(directory, "1. 看图\n![](images/chart.png)").images.single()
            val merged = DocumentImageEditor.merge(first, first.copy(marker = QuestionImageMarker.canonical("img_0002")), directory)
            assertEquals(20, merged.image.width)
            assertEquals(24, merged.image.height)
            assertEquals(first.marker, merged.marker)
            assertTrue(File(first.image.localPath).isFile)
            assertEquals(24, BitmapFactory.decodeFile(merged.image.localPath).height)
        }
    }

    @Test fun structuredContentFallbackIncludesTextAndImage() {
        withResult { directory ->
            val file = File(directory, "result.zip")
            val json = "[{\"type\":\"text\",\"text\":\"1. 看图作答。\"},{\"type\":\"image\",\"img_path\":\"images/chart.png\"}]"
            writeZip(file, mapOf("content_list.json" to json.toByteArray(), "images/chart.png" to png()))
            val decoded = MinerUResultDecoder.decodePreciseZip(file)
            assertTrue(decoded.importText.contains("看图作答"))
            assertTrue(decoded.importText.contains("[[SHIROHA_IMAGE:img_0001]]"))
        }
    }

    @Test fun imageOrderFollowsMarkdownRatherThanZipEntryOrder() {
        withResult { directory ->
            val file = File(directory, "result.zip")
            writeZip(file, linkedMapOf("full.md" to "1. 看图\n![](images/second.png)\n![](images/first.png)".toByteArray(),
                "images/first.png" to png(), "images/second.png" to png()))
            val result = MinerUResultDecoder.decodePreciseZip(file)
            assertEquals(listOf("images/second.png", "images/first.png"), result.images.map { it.image.sourceName })
            assertEquals(listOf(1, 2), result.images.map { it.image.order })
        }
    }

    private fun decode(directory: File, markdown: String): MinerUResultDecoder.DecodedResult {
        val zip = File(directory, "result.zip")
        writeZip(zip, mapOf("full.md" to markdown.toByteArray(), "images/chart.png" to png()))
        return MinerUResultDecoder.decodePreciseZip(zip)
    }

    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(20, 12, Bitmap.Config.ARGB_8888)
        return try { ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray() }
        finally { bitmap.recycle() }
    }

    private fun writeZip(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(file.outputStream()).use { zip -> entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
        } }
    }
    private fun withResult(block: (File) -> Unit) {
        val directory = kotlin.io.path.createTempDirectory("mineru-image-test").toFile()
        try { block(directory) } finally { directory.deleteRecursively() }
    }
}
