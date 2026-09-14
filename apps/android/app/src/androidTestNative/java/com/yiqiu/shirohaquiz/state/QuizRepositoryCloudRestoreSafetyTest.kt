package com.yiqiu.shirohaquiz.state

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Bundle
import com.yiqiu.shirohaquiz.importer.model.Question
import com.yiqiu.shirohaquiz.importer.model.QuestionImage
import com.yiqiu.shirohaquiz.importer.model.QuestionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Real Android APIs, without adding a JUnit/Robolectric/AndroidX dependency to shared builds. */
class QuizRepositoryCloudRestoreSafetyInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            val count = QuizRepositoryCloudRestoreSafetyTest(targetContext).runAll()
            result.putString("stream", "$count cloud restore safety tests passed\n")
            finish(Activity.RESULT_OK, result)
        } catch (failure: Throwable) {
            result.putString("stream", "Cloud restore safety test failed: ${failure.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}

/** Each case uses isolated preferences and file roots, never the user's repository. */
class QuizRepositoryCloudRestoreSafetyTest(private val context: Context) {
    private lateinit var isolated: RestoreContext

    fun runAll(): Int {
        val cases = listOf(
            ::testPreviewIsReadOnlyAndCountsIndependentContent,
            ::testReplacementPreservesSettingsAndIndependentContentAcrossRestart,
            ::testCommitFailureRollsBackObjectsAssetsAndPreferences,
            ::testFailedRollbackIsRecoveredBeforeLoadingOnRestart,
            ::testSafetyBackupFailureNeverStartsReplacement,
            ::testSafetyCopiesAreReadableAndBounded,
            ::testLossyAndMalformedJsonIsRejectedBeforeAnyWrite,
            ::testZipTraversalTruncationDuplicatesAndMissingAssetsAreRejected,
            ::testSameBasenameAssetsRemainDistinctAndOldAssetsRemainAvailable,
            ::testEmptyFullBackupAndRecordOnlyBackupAreLegal,
            ::testCanonicalIdRepairPreservesNestedQuestionSnapshots,
            ::testLegacyUnmappableStateAndNonSelfContainedImagesAreRejected,
            ::testZipCountSizeCrcAndUtf8LimitsAreEnforced,
            ::testSafetyBackupIncludesSharedAssetReferencedByDifferentQuestionIds
        )
        cases.forEach { test ->
            setUp()
            try { test() } finally { tearDown() }
        }
        return cases.size
    }

    private fun setUp() {
        QuizRepository.resetForTesting()
        isolated = RestoreContext(context)
        QuizRepository.init(isolated)
    }

    private fun tearDown() {
        QuizRepository.resetForTesting()
        isolated.root.deleteRecursively()
        isolated.preferences.edit().clear().commit()
    }

    private fun assertTrue(value: Boolean) { check(value) { "Expected true" } }
    private fun assertFalse(value: Boolean) { check(!value) { "Expected false" } }
    private fun assertEquals(expected: Any?, actual: Any?) { check(expected == actual) { "Expected $expected, got $actual" } }
    private fun assertSame(expected: Any?, actual: Any?) { check(expected === actual) { "Object identity changed" } }
    private fun fail(message: String): Nothing = throw AssertionError(message)

    fun testPreviewIsReadOnlyAndCountsIndependentContent() {
        seedLocal()
        val before = QuizRepository.banks.toList()
        val preferences = isolated.preferences.all.toMap()
        val files = isolated.root.walkTopDown().map { it.relativeTo(isolated.root).path }.toList()
        val preview = QuizRepository.previewBackupBytes(independentBackup())
        assertEquals(0, preview.bankCount)
        assertEquals(0, preview.questionCount)
        assertEquals(1, preview.wrongCount)
        assertEquals(1, preview.favoriteCount)
        assertEquals(1, preview.recordCount)
        assertEquals(3, preview.sourceVersion)
        assertEquals(before, QuizRepository.banks.toList())
        assertEquals(preferences, isolated.preferences.all)
        assertEquals(files, isolated.root.walkTopDown().map { it.relativeTo(isolated.root).path }.toList())
    }

    fun testReplacementPreservesSettingsAndIndependentContentAcrossRestart() {
        seedLocal()
        QuizRepository.setDarkThemeEnabled(isolated, true)
        QuizRepository.setAiInterfaceConfig(isolated, "DeepSeek", "https://example.com", "local-secret", "local-model")
        isolated.preferences.edit().putString("other_platform_secret", "unchanged").commit()
        assertTrue(QuizRepository.replaceContentFromBackupBytes(independentBackup()).startsWith("已覆盖恢复"))
        assertTrue(QuizRepository.banks.isEmpty())
        assertEquals(1, QuizRepository.wrongBook.size)
        assertEquals(1, QuizRepository.favoriteQuestions.size)
        assertEquals(1, QuizRepository.studyRecords.size)
        assertTrue(QuizRepository.darkThemeEnabled)
        assertEquals("local-secret", QuizRepository.aiApiKey)
        assertEquals("unchanged", isolated.preferences.getString("other_platform_secret", null))
        QuizRepository.resetForTesting()
        QuizRepository.init(isolated)
        assertEquals(1, QuizRepository.favoriteQuestions.size)
        assertEquals(1, QuizRepository.wrongBook.size)
        assertEquals(1, QuizRepository.studyRecords.size)
        assertEquals("local-secret", QuizRepository.aiApiKey)
    }

    fun testCommitFailureRollsBackObjectsAssetsAndPreferences() {
        val old = seedLocal(withImage = true)
        val oldPreferences = isolated.preferences.all.toMap()
        val image = File(old.questions.single().images.single().localPath)
        val originalBytes = image.readBytes()
        QuizRepository.startExam(questionCount = 1, durationMinutes = 1)
        isolated.failedCommits = 1
        val result = QuizRepository.replaceContentFromBackupBytes(independentBackup())
        assertTrue(result.startsWith("恢复失败"))
        assertSame(old, QuizRepository.banks.single())
        assertEquals(oldPreferences, isolated.preferences.all)
        assertTrue(originalBytes.contentEquals(image.readBytes()))
        assertEquals(1, QuizRepository.examQuestions.size)
        assertFalse(File(isolated.noBackupFilesDir, "cloud_restore_safety/pending.json").exists())
    }

    fun testFailedRollbackIsRecoveredBeforeLoadingOnRestart() {
        val old = seedLocal()
        val oldPreferences = isolated.preferences.all.toMap()
        isolated.failedCommits = 2
        assertTrue(QuizRepository.replaceContentFromBackupBytes(independentBackup()).startsWith("恢复失败"))
        assertSame(old, QuizRepository.banks.single())
        assertTrue(File(isolated.noBackupFilesDir, "cloud_restore_safety/pending.json").exists())
        QuizRepository.resetForTesting()
        QuizRepository.init(isolated)
        assertEquals(oldPreferences, isolated.preferences.all)
        assertEquals(old.id, QuizRepository.banks.single().id)
    }

    fun testSafetyBackupFailureNeverStartsReplacement() {
        val old = seedLocal()
        val safetyDir = File(isolated.noBackupFilesDir, "cloud_restore_safety")
        safetyDir.writeText("not a directory")
        assertTrue(QuizRepository.replaceContentFromBackupBytes(independentBackup()).startsWith("恢复失败"))
        assertSame(old, QuizRepository.banks.single())
    }

    fun testSafetyCopiesAreReadableAndBounded() {
        seedLocal(withImage = true)
        repeat(5) {
            assertTrue(QuizRepository.replaceContentFromBackupBytes(independentBackup()).startsWith("已覆盖恢复"))
        }
        val backups = File(isolated.noBackupFilesDir, "cloud_restore_safety").listFiles()!!
            .filter { it.extension == "zip" }
        assertEquals(3, backups.size)
        backups.forEach { assertEquals(1, QuizRepository.previewBackupBytes(it.readBytes()).favoriteCount) }
    }

    fun testLossyAndMalformedJsonIsRejectedBeforeAnyWrite() {
        val old = seedLocal()
        val invalid = listOf(
            "{\"banks\":[null]}",
            "{\"banks\":[{\"id\":\"b\",\"questions\":[null]}]}",
            "{\"banks\":[],\"wrongBook\":[{\"bankId\":\"gone\"}]}",
            "{\"banks\":[],\"favoriteQuestions\":[{\"bankId\":\"\",\"question\":{\"question\":\"q\"}}]}",
            "{\"banks\":[],\"studyRecords\":[{\"id\":\"r\",\"questionResults\":[null]}]}",
            "{\"banks\":[],\"banks\":[]}", "{\"banks\":[],}", "{\"banks\":[]} trailing",
            "{\"banks\":[],\"version\":999}", "{\"settings\":{}}"
        )
        invalid.forEach { raw ->
            assertRejected(raw.toByteArray())
            assertTrue(QuizRepository.replaceContentFromBackupBytes(raw.toByteArray()).startsWith("恢复失败"))
            assertSame(old, QuizRepository.banks.single())
        }
        assertFalse(File(isolated.noBackupFilesDir, "cloud_restore_safety").exists())
    }

    fun testZipTraversalTruncationDuplicatesAndMissingAssetsAreRejected() {
        val json = independentBackup()
        assertRejected(zip(mapOf("backup.json" to json, "../escape.png" to byteArrayOf(1))))
        val valid = zip(mapOf("backup.json" to json))
        assertRejected(valid.copyOf(valid.size - 8))
        val duplicate = zip(linkedMapOf("backup.json" to json, "second.json" to json))
        val renamed = duplicate.copyOf()
        val from = "second.json".toByteArray()
        val to = "backup.json".toByteArray()
        for (i in 0..renamed.size - from.size) {
            if (from.indices.all { renamed[i + it] == from[it] }) to.copyInto(renamed, i)
        }
        assertRejected(renamed)
        val root = JSONObject(String(json))
        root.getJSONArray("favoriteQuestions").getJSONObject(0).getJSONObject("question")
            .put("images", JSONArray().put(JSONObject().put("id", "img").put("localPath", "assets/missing.png")))
        assertRejected(zip(mapOf("backup.json" to root.toString().toByteArray())))
    }

    fun testSameBasenameAssetsRemainDistinctAndOldAssetsRemainAvailable() {
        val old = seedLocal(withImage = true)
        val root = JSONObject(String(independentBackup()))
        root.getJSONArray("favoriteQuestions").getJSONObject(0).getJSONObject("question")
            .put("images", JSONArray()
                .put(JSONObject().put("id", "a").put("localPath", "assets/a/image.png").put("order", 1))
                .put(JSONObject().put("id", "b").put("localPath", "assets/b/image.png").put("order", 2)))
        val bytes = zip(mapOf("backup.json" to root.toString().toByteArray(),
            "assets/a/image.png" to byteArrayOf(1, 2), "assets/b/image.png" to byteArrayOf(3, 4)))
        assertTrue(QuizRepository.replaceContentFromBackupBytes(bytes).startsWith("已覆盖恢复"))
        val images = QuizRepository.favoriteQuestions.single().question.images
        assertFalse(images[0].localPath == images[1].localPath)
        assertTrue(File(images[0].localPath).readBytes().contentEquals(byteArrayOf(1, 2)))
        assertTrue(File(images[1].localPath).readBytes().contentEquals(byteArrayOf(3, 4)))
        assertTrue(File(old.questions.single().images.single().localPath).exists())
    }

    fun testEmptyFullBackupAndRecordOnlyBackupAreLegal() {
        val empty = JSONObject().put("kind", "shiroha_quiz_full_backup").put("version", 3).put("banks", JSONArray())
        assertEquals(0, QuizRepository.previewBackupBytes(empty.toString().toByteArray()).bankCount)
        val records = JSONObject(String(independentBackup())).getJSONArray("studyRecords")
        val onlyRecords = JSONObject().put("version", 3).put("studyRecords", records).toString().toByteArray()
        val preview = QuizRepository.previewBackupBytes(onlyRecords)
        assertEquals(0, preview.bankCount)
        assertEquals(1, preview.recordCount)
        assertTrue(QuizRepository.replaceContentFromBackupBytes(onlyRecords).startsWith("已覆盖恢复"))
        assertEquals(1, QuizRepository.studyRecords.size)
    }

    fun testCanonicalIdRepairPreservesNestedQuestionSnapshots() {
        val first = JSONObject().put("id", "duplicate").put("type", "SHORT").put("question", "first")
        val second = JSONObject().put("id", "duplicate").put("type", "SHORT").put("question", "second")
        val root = JSONObject().put("version", 3).put("kind", "shiroha_quiz_full_backup")
            .put("banks", JSONArray().put(JSONObject().put("id", "b").put("name", "bank")
                .put("questions", JSONArray().put(first).put(second))))
            .put("favoriteQuestions", JSONArray().put(JSONObject().put("bankId", "b").put("question", second)))
        assertTrue(QuizRepository.replaceContentFromBackupBytes(root.toString().toByteArray()).startsWith("已覆盖恢复"))
        val questions = QuizRepository.banks.single().questions
        assertFalse(questions[0].id == questions[1].id)
        assertEquals(questions[1].id, QuizRepository.favoriteQuestions.single().question.id)
        assertEquals("second", QuizRepository.favoriteQuestions.single().question.question)
    }

    fun testLegacyUnmappableStateAndNonSelfContainedImagesAreRejected() {
        val legacy = JSONObject().put("kind", "shiroha_quiz_web_backup").put("banks", JSONArray())
            .put("favorites", JSONObject().put("missing", JSONArray().put("q")))
        assertRejected(legacy.toString().toByteArray())
        val local = JSONObject(String(independentBackup()))
        local.getJSONArray("favoriteQuestions").getJSONObject(0).getJSONObject("question")
            .put("images", JSONArray().put(JSONObject().put("localPath", "/private/untrusted.png")))
        assertRejected(local.toString().toByteArray())
        val blank = JSONObject(String(independentBackup()))
        blank.getJSONArray("favoriteQuestions").getJSONObject(0).getJSONObject("question")
            .put("type", "BLANK").put("blankAnswers", JSONArray().put(JSONArray().put("a").put("b").put("c").put("d")))
        assertRejected(blank.toString().toByteArray())
    }

    fun testZipCountSizeCrcAndUtf8LimitsAreEnforced() {
        assertRejected(byteArrayOf('{'.code.toByte(), 0xc3.toByte(), 0x28, '}'.code.toByte()))
        val tooMany = linkedMapOf("backup.json" to independentBackup())
        repeat(1000) { tooMany["assets/$it.bin"] = byteArrayOf(1) }
        assertRejected(zip(tooMany))
        assertRejected(zip(mapOf("backup.json" to independentBackup(), "assets/large.bin" to ByteArray(20 * 1024 * 1024 + 1))))
        val corrupt = zip(mapOf("backup.json" to independentBackup())).copyOf()
        val central = (0..corrupt.size - 4).first { i ->
            corrupt[i] == 0x50.toByte() && corrupt[i + 1] == 0x4b.toByte() &&
                corrupt[i + 2] == 0x01.toByte() && corrupt[i + 3] == 0x02.toByte()
        }
        corrupt[central + 16] = (corrupt[central + 16].toInt() xor 1).toByte()
        assertRejected(corrupt)
    }

    fun testSafetyBackupIncludesSharedAssetReferencedByDifferentQuestionIds() {
        val old = seedLocal(withImage = true)
        QuizRepository.importBank(isolated, "Second", listOf(old.questions.single().copy(id = "second-q")))
        val zip = QuizRepository.exportFullBackupZip()
        assertEquals(2, QuizRepository.previewBackupBytes(zip).questionCount)
        assertEquals(1, QuizRepository.previewBackupBytes(zip).assetCount)
        assertTrue(QuizRepository.replaceContentFromBackupBytes(independentBackup()).startsWith("已覆盖恢复"))
    }

    private fun assertRejected(bytes: ByteArray) {
        try {
            QuizRepository.previewBackupBytes(bytes)
            fail("Unsafe backup accepted")
        } catch (_: IllegalArgumentException) {
            // Expected strict preflight rejection.
        }
    }

    private fun seedLocal(withImage: Boolean = false): QuizBank {
        val images = if (withImage) {
            val file = File(isolated.filesDir, "original.png").apply { writeBytes(byteArrayOf(9, 8, 7)) }
            listOf(QuestionImage("local-image", file.absolutePath, "original.png", 1))
        } else emptyList()
        QuizRepository.importBank(isolated, "Local", listOf(
            Question(id = "local-q", type = QuestionType.SHORT, question = "local question", images = images)
        ))
        return QuizRepository.banks.single()
    }

    private fun independentBackup(): ByteArray {
        val q = JSONObject().put("id", "independent-q").put("type", "SHORT")
            .put("question", "independent question").put("answer", JSONArray().put("answer"))
        return JSONObject().put("kind", "shiroha_quiz_full_backup").put("version", 3)
            .put("banks", JSONArray())
            .put("wrongBook", JSONArray().put(JSONObject().put("bankId", "removed-bank").put("question", q)))
            .put("favoriteQuestions", JSONArray().put(JSONObject().put("bankId", "removed-bank").put("question", q)))
            .put("studyRecords", JSONArray().put(JSONObject().put("id", "independent-record")
                .put("total", 1).put("correct", 0).put("questionResults", JSONArray()
                    .put(JSONObject().put("question", q).put("correct", false)))))
            .toString().toByteArray()
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private class RestoreContext(base: Context) : ContextWrapper(base) {
        val root = File(base.cacheDir, "cloud_restore_test_${UUID.randomUUID()}").apply { mkdirs() }
        val preferences: SharedPreferences = base.getSharedPreferences(root.name, MODE_PRIVATE)
        var failedCommits = 0
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
        override fun getNoBackupFilesDir(): File = File(root, "no_backup").apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            object : SharedPreferences by preferences {
                override fun edit(): SharedPreferences.Editor {
                    val real = preferences.edit()
                    return object : SharedPreferences.Editor by real {
                        override fun commit(): Boolean {
                            val committed = real.commit()
                            if (failedCommits <= 0) return committed
                            failedCommits--
                            return false
                        }
                    }
                }
            }
    }
}
