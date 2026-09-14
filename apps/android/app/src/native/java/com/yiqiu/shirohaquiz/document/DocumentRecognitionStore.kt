package com.yiqiu.shirohaquiz.document

import android.content.Context
import com.yiqiu.shirohaquiz.security.SecureSecretStore
import org.json.JSONObject
import org.json.JSONArray
import com.yiqiu.shirohaquiz.importer.model.QuestionImage

class DocumentRecognitionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureSecrets = SecureSecretStore(context.applicationContext)

    fun loadSettings(): MinerUSettings {
        val raw = preferences.getString(KEY_SETTINGS, null) ?: return MinerUSettings()
        return runCatching {
            val json = JSONObject(raw)
            MinerUSettings(
                mode = json.optEnum("mode", MinerUMode.FREE),
                language = json.optString("language", "ch").ifBlank { "ch" },
                isOcr = json.optBoolean("isOcr", true),
                enableTable = json.optBoolean("enableTable", true),
                enableFormula = json.optBoolean("enableFormula", true),
                modelVersion = json.optEnum("modelVersion", MinerUModelVersion.VLM),
                extraFormats = json.optJSONArray("extraFormats")?.let { rows ->
                    (0 until rows.length()).map { rows.optString(it) }.filter { it in listOf("docx", "html", "latex") }
                }.orEmpty(),
                pageRange = json.optString("pageRange", "")
            )
        }.getOrDefault(MinerUSettings())
    }

    fun saveSettings(settings: MinerUSettings) {
        val json = JSONObject()
            .put("mode", settings.mode.name)
            .put("language", settings.language)
            .put("isOcr", settings.isOcr)
            .put("enableTable", settings.enableTable)
            .put("enableFormula", settings.enableFormula)
            .put("modelVersion", settings.modelVersion.name)
            .put("extraFormats", JSONArray(settings.extraFormats))
            .put("pageRange", settings.pageRange)
        preferences.edit().putString(KEY_SETTINGS, json.toString()).apply()
    }

    fun loadSelectedDocument(): SelectedDocument? {
        val raw = preferences.getString(KEY_SELECTED_DOCUMENT, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            SelectedDocument(
                fileName = json.getString("fileName"),
                localPath = json.getString("localPath"),
                sizeBytes = json.optLong("sizeBytes", 0L),
                pageCount = json.optInt("pageCount", 0)
            )
        }.getOrNull()
    }

    fun saveSelectedDocument(document: SelectedDocument?) {
        if (document == null) {
            preferences.edit().remove(KEY_SELECTED_DOCUMENT).apply()
            return
        }
        val json = JSONObject()
            .put("fileName", document.fileName)
            .put("localPath", document.localPath)
            .put("sizeBytes", document.sizeBytes)
            .put("pageCount", document.pageCount)
        preferences.edit().putString(KEY_SELECTED_DOCUMENT, json.toString()).apply()
    }

    fun loadTask(): DocumentRecognitionTask? {
        val raw = preferences.getString(KEY_TASK, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            DocumentRecognitionTask(
                id = json.getString("id"),
                mode = json.optEnum("mode", MinerUMode.FREE),
                fileName = json.getString("fileName"),
                localPdfPath = json.getString("localPdfPath"),
                sizeBytes = json.optLong("sizeBytes", 0L),
                pageCount = json.optInt("pageCount", 0),
                pageRange = json.optString("pageRange", ""),
                taskId = json.optNullableString("taskId"),
                batchId = json.optNullableString("batchId"),
                stage = json.optEnum("stage", DocumentTaskStage.FAILED),
                serviceState = json.optString("serviceState", ""),
                extractedPages = json.optInt("extractedPages", 0),
                uploadedBytes = json.optLong("uploadedBytes", 0),
                totalPages = json.optInt("totalPages", 0),
                rawResultPath = json.optNullableString("rawResultPath"),
                resultPath = json.optNullableString("resultPath"),
                resultSourceUrl = json.optNullableString("resultSourceUrl"),
                hasImageReferences = json.optBoolean("hasImageReferences", false),
                message = json.optString("message", ""),
                errorMessage = json.optNullableString("errorMessage"),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun saveTask(task: DocumentRecognitionTask?) {
        if (task == null) {
            preferences.edit().remove(KEY_TASK).apply()
            return
        }
        val json = JSONObject()
            .put("id", task.id)
            .put("mode", task.mode.name)
            .put("fileName", task.fileName)
            .put("localPdfPath", task.localPdfPath)
            .put("sizeBytes", task.sizeBytes)
            .put("pageCount", task.pageCount)
            .put("pageRange", task.pageRange)
            .put("taskId", task.taskId)
            .put("batchId", task.batchId)
            .put("stage", task.stage.name)
            .put("serviceState", task.serviceState)
            .put("extractedPages", task.extractedPages)
            .put("uploadedBytes", task.uploadedBytes)
            .put("totalPages", task.totalPages)
            .put("rawResultPath", task.rawResultPath)
            .put("resultPath", task.resultPath)
            .put("resultSourceUrl", task.resultSourceUrl)
            .put("hasImageReferences", task.hasImageReferences)
            .put("message", task.message)
            .put("errorMessage", task.errorMessage)
            .put("updatedAt", task.updatedAt)
        check(preferences.edit().putString(KEY_TASK, json.toString()).commit()) { "识别任务暂存失败。" }
    }

    fun hasPreciseToken(): Boolean = !secureSecrets.get(SECRET_PRECISE_TOKEN).isNullOrBlank()

    fun loadImages(taskId: String): List<DocumentImageAsset> {
        val raw = preferences.getString("images_$taskId", null) ?: return emptyList()
        return runCatching {
            val rows = JSONArray(raw)
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    add(DocumentImageAsset(
                        marker = row.getString("marker"),
                        image = QuestionImage(id = row.getString("id"), localPath = row.getString("path"),
                            sourceName = row.optString("name"), order = row.optInt("order"),
                            width = row.optInt("width").takeIf { it > 0 }, height = row.optInt("height").takeIf { it > 0 },
                            sizeBytes = row.optLong("size")),
                        excluded = row.optBoolean("excluded"),
                        targetQuestionIndex = row.optInt("target", -1).takeIf { it >= 0 }
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveImages(taskId: String, images: List<DocumentImageAsset>) {
        val rows = JSONArray()
        images.forEach { asset -> rows.put(JSONObject().put("marker", asset.marker)
            .put("id", asset.image.id).put("path", asset.image.localPath).put("name", asset.image.sourceName)
            .put("order", asset.image.order).put("width", asset.image.width).put("height", asset.image.height)
            .put("size", asset.image.sizeBytes).put("excluded", asset.excluded).put("target", asset.targetQuestionIndex ?: -1)) }
        preferences.edit().putString("images_$taskId", rows.toString()).commit()
    }

    fun clearImages(taskId: String) { preferences.edit().remove("images_$taskId").commit() }

    fun loadPreciseToken(): String = secureSecrets.get(SECRET_PRECISE_TOKEN).orEmpty()

    fun savePreciseToken(token: String) {
        secureSecrets.put(SECRET_PRECISE_TOKEN, token.trim())
    }

    fun clearPreciseToken() {
        secureSecrets.remove(SECRET_PRECISE_TOKEN)
    }

    private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
        return runCatching { enumValueOf<T>(optString(key, fallback.name)) }.getOrDefault(fallback)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }

    companion object {
        private const val PREFS_NAME = "shiroha_document_recognition"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_SELECTED_DOCUMENT = "selected_document"
        private const val KEY_TASK = "active_task"
        private const val SECRET_PRECISE_TOKEN = "mineru_precise_token"
    }
}
