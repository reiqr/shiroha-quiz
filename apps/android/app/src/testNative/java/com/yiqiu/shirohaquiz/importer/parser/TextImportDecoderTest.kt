package com.yiqiu.shirohaquiz.importer.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * DOCX 基础解码的快速单元测试。
 *
 * 其余文本解析场景由 test/native-parser-regression 的外部回归覆盖；这里只保留
 * Word 文档解码这一条 —— 外部回归的样例都是纯文本，不经过 TextImportDecoder。
 */
class TextImportDecoderTest {
    @Test
    fun `docx decoder should extract readable text from word xml`() {
        val xml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>1. 安全帽的主要作用是（A）</w:t></w:r></w:p>
                <w:p><w:r><w:t>A. 保护头部</w:t></w:r></w:p>
                <w:p><w:r><w:t>答案：A</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()

        val docxBytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("word/document.xml"))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            output.toByteArray()
        }

        val text = TextImportDecoder.decode(docxBytes, "sample.docx")

        assertTrue(text?.contains("安全帽的主要作用") == true)
        assertTrue(text?.contains("答案:A") == true)
    }
}
