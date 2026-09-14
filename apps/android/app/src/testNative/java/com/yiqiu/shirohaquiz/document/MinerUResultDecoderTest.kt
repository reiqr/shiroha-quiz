package com.yiqiu.shirohaquiz.document

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MinerUResultDecoderTest {
    @Test fun preservesBlankMarkersAndCodeBlocks() {
        val value = "# 填空题\r\n1. 写入____。\r\n答案：文本\r\n```bash\r\nvalue=\u0024((a + b))\r\ncmd1 || cmd2\r\n```"
        val decoded = MinerUResultDecoder.decodeMarkdown(value)
        assertTrue(decoded.importText.contains("____"))
        assertTrue(decoded.importText.contains("```bash\nvalue="))
        assertTrue(decoded.importText.contains("cmd1 || cmd2\n```"))
        assertFalse(decoded.importText.contains('\r'))
    }

    @Test fun freeResultKeepsMissingImageNotice() {
        val decoded = MinerUResultDecoder.decodeMarkdown("1. 看图作答。\n![图表](images/test.jpg)\n答案：A")
        assertTrue(decoded.hasImageReferences)
        assertTrue(decoded.importText.contains("【图片位置：图表】"))
    }

    @Test fun rejectsUnsafeEntryEvenAfterMarkdown() {
        withZip(listOf("full.md" to "1. 正文", "../evil.txt" to "恶意路径")) { file ->
            assertThrows(IllegalArgumentException::class.java) { MinerUResultDecoder.decodePreciseZip(file) }
        }
    }

    @Test fun keepsValidTextFromNestedResultDirectory() {
        withZip(listOf("result/full.md" to "# 单选题\n1. 题目\nA. 内容\nB. 内容\n答案：A")) { file ->
            val decoded = MinerUResultDecoder.decodePreciseZip(file)
            assertTrue(decoded.importText.startsWith("单选题"))
            assertTrue(decoded.images.isEmpty())
        }
    }

    private fun withZip(entries: List<Pair<String, String>>, block: (File) -> Unit) {
        val dir = kotlin.io.path.createTempDirectory("mineru-test").toFile()
        try {
            val file = File(dir, "result.zip")
            ZipOutputStream(file.outputStream()).use { zip -> entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            } }
            block(file)
        } finally { dir.deleteRecursively() }
    }
}
