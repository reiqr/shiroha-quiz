package com.yiqiu.shirohaquiz.importer.parser

import android.graphics.Bitmap
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageMarker
import com.yiqiu.shirohaquiz.importer.assets.QuestionImageBinder
import com.yiqiu.shirohaquiz.importer.assets.QuestionImportAssetExtractor
import com.yiqiu.shirohaquiz.importer.model.ImportDiagnostics
import com.yiqiu.shirohaquiz.importer.model.ImportResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class XlsxImageRowBindingTest {
    @Test
    fun imageMarkerFollowsOriginalSheetRowAcrossBlankRows() {
        val xml = """
            <worksheet><sheetData>
              <row r="1"><c r="A1" t="inlineStr"><is><t>题型</t></is></c><c r="B1" t="inlineStr"><is><t>题干</t></is></c><c r="C1" t="inlineStr"><is><t>答案</t></is></c></row>
              <row r="3"><c r="A3" t="inlineStr"><is><t>判断题</t></is></c><c r="B3" t="inlineStr"><is><t>有图题</t></is></c><c r="C3" t="inlineStr"><is><t>正确</t></is></c></row>
              <row r="4"><c r="A4" t="inlineStr"><is><t>判断题</t></is></c><c r="B4" t="inlineStr"><is><t>无图题</t></is></c><c r="C4" t="inlineStr"><is><t>错误</t></is></c></row>
            </sheetData></worksheet>
        """.trimIndent()
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
        val marker = QuestionImageMarker.canonical("img_0001")
        val text = TextImportDecoder.decodeXlsxWithMarkers(
            bytes, mapOf("xl/worksheets/sheet1.xml" to mapOf(3 to listOf(marker)))
        ).orEmpty()
        assertEquals(1, text.split(marker).size - 1)
        assertTrue(text.lineSequence().any { it.contains("有图题") && it.contains(marker) })
        assertFalse(text.lineSequence().any { it.contains("无图题") && it.contains(marker) })
        val questions = ExcelQuestionTableParser.parse(text)
        assertEquals(2, questions.size)
        assertTrue(questions[0].question.contains(marker))
        assertFalse(questions[1].question.contains(marker))
        val image = QuestionImportAssetExtractor.ExtractedImportImage(marker,
            com.yiqiu.shirohaquiz.importer.model.QuestionImage(id = "xlsx-image", localPath = "image.png"))
        val bound = QuestionImageBinder.attach(ImportResult(questions, "standard", emptyList(), ImportDiagnostics()), listOf(image))
        assertEquals(listOf("xlsx-image"), bound.questions[0].images.map { it.id })
        assertTrue(bound.questions[1].images.isEmpty())
        assertFalse(bound.questions[0].question.contains(marker))
    }

    @Test
    fun imageOnHeaderDoesNotChangeTableHeaders() {
        val xml = """
            <worksheet><sheetData>
              <row r="1"><c r="A1" t="inlineStr"><is><t>题型</t></is></c><c r="B1" t="inlineStr"><is><t>题干</t></is></c><c r="C1" t="inlineStr"><is><t>答案</t></is></c></row>
              <row r="2"><c r="A2" t="inlineStr"><is><t>判断题</t></is></c><c r="B2" t="inlineStr"><is><t>正常题目</t></is></c><c r="C2" t="inlineStr"><is><t>正确</t></is></c></row>
            </sheetData></worksheet>
        """.trimIndent()
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
        val marker = QuestionImageMarker.canonical("img_0001")
        val text = TextImportDecoder.decodeXlsxWithMarkers(
            bytes, mapOf("xl/worksheets/sheet1.xml" to mapOf(1 to listOf(marker)))
        ).orEmpty()
        assertFalse(text.contains(marker))
        assertEquals(1, ExcelQuestionTableParser.parse(text).size)
    }

    @Test
    fun drawingImageBindsOnlyToMatchingQuestionRow() {
        val sheet = """
            <worksheet xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheetData>
              <row r="1"><c r="A1" t="inlineStr"><is><t>题型</t></is></c><c r="B1" t="inlineStr"><is><t>题干</t></is></c><c r="C1" t="inlineStr"><is><t>答案</t></is></c></row>
              <row r="3"><c r="A3" t="inlineStr"><is><t>判断题</t></is></c><c r="B3" t="inlineStr"><is><t>有图题</t></is></c><c r="C3" t="inlineStr"><is><t>正确</t></is></c></row>
              <row r="4"><c r="A4" t="inlineStr"><is><t>判断题</t></is></c><c r="B4" t="inlineStr"><is><t>无图题</t></is></c><c r="C4" t="inlineStr"><is><t>错误</t></is></c></row>
            </sheetData><drawing r:id="rId1"/></worksheet>
        """.trimIndent()
        val sheetRels = """<Relationships><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/></Relationships>"""
        val drawing = """<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <xdr:oneCellAnchor><xdr:from><xdr:row>0</xdr:row></xdr:from><a:blip r:embed="rId1"/></xdr:oneCellAnchor>
            <xdr:oneCellAnchor><xdr:from><xdr:row>2</xdr:row></xdr:from><a:blip r:embed="rId2"/></xdr:oneCellAnchor>
            </xdr:wsDr>"""
        val drawingRels = """<Relationships><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/header.png"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/question.png"/></Relationships>"""
        val png = ByteArrayOutputStream().also { stream ->
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, stream)
        }.toByteArray()
        val parts = linkedMapOf(
            "xl/worksheets/sheet1.xml" to sheet.toByteArray(),
            "xl/worksheets/_rels/sheet1.xml.rels" to sheetRels.toByteArray(),
            "xl/drawings/drawing1.xml" to drawing.toByteArray(),
            "xl/drawings/_rels/drawing1.xml.rels" to drawingRels.toByteArray(),
            "xl/media/header.png" to png,
            "xl/media/question.png" to png
        )
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                parts.forEach { (name, data) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
        val decoded = QuestionImportAssetExtractor.decodeDetailed(RuntimeEnvironment.getApplication(), bytes, "sample.xlsx")
        assertTrue(decoded is QuestionImportAssetExtractor.DecodeResult.Success)
        val content = (decoded as QuestionImportAssetExtractor.DecodeResult.Success).content
        try {
            assertEquals(1, content.images.size)
            assertEquals("img_0002", QuestionImageMarker.markerId(content.images.single().marker))
            assertTrue(File(content.images.single().image.localPath).isFile)
            assertTrue(content.warnings.any { it.contains("第 1 行") })
            val questions = ExcelQuestionTableParser.parse(content.text)
            val bound = QuestionImageBinder.attach(ImportResult(questions, "standard", emptyList(), ImportDiagnostics()), content.images)
            assertEquals(1, bound.questions[0].images.size)
            assertTrue(bound.questions[1].images.isEmpty())
        } finally {
            content.images.forEach { File(it.image.localPath).delete() }
        }
    }
}
