package com.yiqiu.shirohaquiz.document

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object DocumentRecognitionManager {
    var settings by mutableStateOf(MinerUSettings())
        private set
    var selectedDocument by mutableStateOf<SelectedDocument?>(null)
        private set
    var task by mutableStateOf<DocumentRecognitionTask?>(null)
        private set
    var resultText by mutableStateOf("")
        private set
    var pendingImportDraft by mutableStateOf<DocumentImportDraft?>(null)
        private set
    var isPreparingDocument by mutableStateOf(false)
        private set
    var uiMessage by mutableStateOf<String?>(null)
        private set
    var preciseTokenConfigured by mutableStateOf(false)
        private set
    val shouldShowGlobalTaskCapsule: Boolean
        get() {
            val current = task ?: return false
            if (current.isWorking) return true
            if (current.stage == DocumentTaskStage.PAUSED) return current.id != acknowledgedTerminalTaskId
            if (current.stage == DocumentTaskStage.READY) return current.id != acknowledgedTerminalTaskId
            return current.stage == DocumentTaskStage.FAILED &&
                (current.taskId != null || current.batchId != null) &&
                current.id != acknowledgedTerminalTaskId
        }

    private lateinit var appContext: Context
    private lateinit var store: DocumentRecognitionStore
    private val managerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                uiMessage = "识别任务内部错误：${error.message ?: "未知错误"}"
            }
    )
    private val client = MinerUClient()
    private var workflowJob: Job? = null
    private var resultSaveJob: Job? = null
    private var acknowledgedTerminalTaskId by mutableStateOf<String?>(null)
    private var cleanupAfterImportTaskId: String? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        store = DocumentRecognitionStore(appContext)
        settings = store.loadSettings()
        preciseTokenConfigured = store.hasPreciseToken()
        selectedDocument = store.loadSelectedDocument()?.takeIf { File(it.localPath).isFile }
        if (selectedDocument == null) store.saveSelectedDocument(null)

        task = store.loadTask()
        resultText = ""
        initialized = true
        val restored = task
        when (restored?.stage) {
            DocumentTaskStage.REQUESTING_UPLOAD,
            DocumentTaskStage.UPLOADING -> updateTask(
                restored.copy(
                    stage = DocumentTaskStage.FAILED,
                    message = "上次上传未完成，请重新提交识别。",
                    errorMessage = "应用在 PDF 上传完成前退出，签名上传地址已失效。"
                )
            )
            DocumentTaskStage.QUEUED,
            DocumentTaskStage.RUNNING,
            DocumentTaskStage.DOWNLOADING -> resumePolling()
            DocumentTaskStage.READY -> {
                val resultFile = restored.resultPath?.let(::File)
                if (resultFile?.isFile == true && resultFile.length() <= MAX_RESULT_TEXT_BYTES) {
                    managerScope.launch {
                        val text = try {
                            withContext(Dispatchers.IO) { resultFile.readText(Charsets.UTF_8) }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Throwable) {
                            uiMessage = "本地识别文本读取失败：${error.message ?: "文件不可读"}"
                            return@launch
                        }
                        if (task?.id == restored.id) resultText = text
                    }
                } else {
                    updateTask(
                        restored.copy(
                            stage = DocumentTaskStage.FAILED,
                            message = "本地识别文本已丢失，请重新识别。",
                            errorMessage = "未找到本地结果文件。"
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    fun updateSettings(value: MinerUSettings) {
        ensureInitialized()
        settings = value.copy(pageRange = value.pageRange.take(80))
        store.saveSettings(settings)
    }

    fun savePreciseToken(token: String): Boolean {
        ensureInitialized()
        val clean = token.trim().replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "").trim()
        if (clean.isBlank()) {
            uiMessage = "Token 不能为空。"
            return false
        }
        return runCatching {
            store.savePreciseToken(clean)
            preciseTokenConfigured = true
            uiMessage = "MinerU Token 已安全保存在本机。"
            true
        }.getOrElse {
            uiMessage = "Token 保存失败：${it.message ?: "设备密钥不可用"}"
            false
        }
    }

    fun clearPreciseToken() {
        ensureInitialized()
        store.clearPreciseToken()
        preciseTokenConfigured = false
        uiMessage = "已清除 MinerU Token。"
    }

    fun selectPdf(uri: Uri) {
        ensureInitialized()
        if (task?.isWorking == true || isPreparingDocument) {
            uiMessage = "当前识别任务尚未结束，暂时不能更换 PDF。"
            return
        }
        val replacingTaskId = task?.id
        managerScope.launch {
            isPreparingDocument = true
            uiMessage = "正在读取 PDF 信息……"
            val result = runCatching {
                withContext(Dispatchers.IO) { copyPdfToPrivateStorage(uri) }
            }
            isPreparingDocument = false
            result.onSuccess { document ->
                val replaced = task?.id == replacingTaskId
                if (replaced) {
                    deleteResultFiles(task)
                    task = null
                    resultText = ""
                    store.saveTask(null)
                }
                val previous = selectedDocument
                selectedDocument = document
                store.saveSelectedDocument(document)
                // 复制期间若已有新任务接管（replaced 为 false），旧 PDF 仍被它引用，不能删。
                val previousPath = previous?.localPath
                if (replaced && previousPath != null && previousPath != document.localPath) {
                    File(previousPath).delete()
                }
                uiMessage = if (replaced) {
                    "已选择 ${document.fileName}。"
                } else {
                    "已选择 ${document.fileName}；原识别任务已保留，如需继续请先结束它。"
                }
            }.onFailure { error ->
                uiMessage = error.message ?: "PDF 读取失败。"
            }
        }
    }

    fun startRecognition() {
        ensureInitialized()
        if (isPreparingDocument) {
            uiMessage = "正在准备 PDF，请稍候。"
            return
        }
        if (workflowJob?.isActive == true || task?.isWorking == true) {
            uiMessage = "已有识别任务正在运行。"
            return
        }
        val document = selectedDocument
        if (document == null || !File(document.localPath).isFile) {
            uiMessage = "请先选择 PDF 文件。"
            return
        }
        val validationError = validateRequest(document, settings)
        if (validationError != null) {
            uiMessage = validationError
            return
        }
        if (settings.mode == MinerUMode.PRECISE && !preciseTokenConfigured) {
            uiMessage = "精准模式需要先保存 MinerU Token。"
            return
        }

        val taskId = UUID.randomUUID().toString()
        val cleanRange = settings.pageRange.filterNot(Char::isWhitespace)
        val initialTask = DocumentRecognitionTask(
            id = taskId,
            mode = settings.mode,
            fileName = document.fileName,
            localPdfPath = document.localPath,
            sizeBytes = document.sizeBytes,
            pageCount = document.pageCount,
            pageRange = cleanRange,
            stage = DocumentTaskStage.REQUESTING_UPLOAD,
            message = "正在向 MinerU 申请安全上传地址……"
        )
        resultText = ""
        pendingImportDraft = null
        updateTask(initialTask)
        uiMessage = null

        workflowJob = managerScope.launch {
            try {
                val settingsSnapshot = settings.copy(pageRange = cleanRange)
                val ticket = withContext(Dispatchers.IO) {
                    if (settingsSnapshot.mode == MinerUMode.FREE) {
                        client.requestFreeUpload(document.fileName, settingsSnapshot)
                    } else {
                        client.requestPreciseUpload(
                            fileName = document.fileName,
                            token = store.loadPreciseToken(),
                            settings = settingsSnapshot,
                            dataId = "shiroha_${System.currentTimeMillis()}"
                        )
                    }
                }
                updateTask(
                    requireTask(taskId).copy(
                        taskId = ticket.taskId,
                        batchId = ticket.batchId,
                        stage = DocumentTaskStage.UPLOADING,
                        message = "正在上传 PDF，请保持网络连接……",
                        errorMessage = null
                    )
                )
                withContext(Dispatchers.IO) {
                    client.uploadSignedFile(ticket.uploadUrl, File(document.localPath))
                }
                updateTask(
                    requireTask(taskId).copy(
                        stage = DocumentTaskStage.QUEUED,
                        serviceState = "pending",
                        message = "PDF 已上传，正在等待 MinerU 解析。"
                    )
                )
                pollUntilTerminal(taskId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val current = task?.takeIf { it.id == taskId } ?: return@launch
                val canContinue = current.stage in setOf(
                    DocumentTaskStage.QUEUED,
                    DocumentTaskStage.RUNNING,
                    DocumentTaskStage.DOWNLOADING
                ) && (current.taskId != null || current.batchId != null)
                updateTask(
                    current.copy(
                        stage = if (canContinue) DocumentTaskStage.PAUSED else DocumentTaskStage.FAILED,
                        message = if (canContinue) "查询暂时中断，可稍后继续。" else "识别任务未能提交。",
                        errorMessage = error.message ?: "未知错误"
                    )
                )
            }
        }
    }

    fun pausePolling() {
        val current = task ?: return
        if (current.stage !in setOf(DocumentTaskStage.QUEUED, DocumentTaskStage.RUNNING)) return
        workflowJob?.cancel()
        workflowJob = null
        updateTask(
            current.copy(
                stage = DocumentTaskStage.PAUSED,
                message = "已暂停状态查询；MinerU 服务器可能仍在继续解析。"
            )
        )
    }

    fun resumePolling() {
        ensureInitialized()
        val current = task ?: return
        if (workflowJob?.isActive == true) return
        if (current.taskId == null && current.batchId == null) {
            updateTask(
                current.copy(
                    stage = DocumentTaskStage.FAILED,
                    message = "任务无法继续，请重新提交。",
                    errorMessage = "缺少 MinerU 任务 ID。"
                )
            )
            return
        }
        if (current.mode == MinerUMode.PRECISE && store.loadPreciseToken().isBlank()) {
            updateTask(
                current.copy(
                    stage = DocumentTaskStage.PAUSED,
                    message = "请重新保存 MinerU Token 后继续查询。",
                    errorMessage = "精准任务查询需要原 Token。"
                )
            )
            return
        }
        updateTask(current.copy(stage = DocumentTaskStage.QUEUED, message = "正在恢复任务查询……", errorMessage = null))
        workflowJob = managerScope.launch {
            try {
                pollUntilTerminal(current.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                task?.takeIf { it.id == current.id }?.let {
                    updateTask(
                        it.copy(
                            stage = DocumentTaskStage.PAUSED,
                            message = "查询暂时中断，可稍后继续。",
                            errorMessage = error.message ?: "网络异常"
                        )
                    )
                }
            }
        }
    }

    fun retryRecognition() {
        if (isPreparingDocument) {
            uiMessage = "正在准备 PDF，请稍候。"
            return
        }
        val current = task ?: return
        if (current.isWorking) return
        clearTask(keepSelectedDocument = true)
        startRecognition()
    }

    fun updateResultText(text: String) {
        val current = task?.takeIf { it.stage == DocumentTaskStage.READY } ?: return
        resultText = text
        resultSaveJob?.cancel()
        resultSaveJob = managerScope.launch {
            delay(350)
            val resultFile = current.resultPath?.let(::File) ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    resultFile.parentFile?.mkdirs()
                    resultFile.writeText(text, Charsets.UTF_8)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                uiMessage = "识别文本暂存失败：${error.message ?: "本地存储不可用"}"
            }
        }
    }

    fun prepareImportDraft(text: String = resultText): Boolean {
        val current = task?.takeIf { it.stage == DocumentTaskStage.READY } ?: return false
        val clean = text.trim()
        if (clean.isBlank()) {
            uiMessage = "识别文本为空，无法进入导入。"
            return false
        }
        updateResultText(clean)
        pendingImportDraft = DocumentImportDraft(
            id = current.id,
            sourceFileName = current.fileName,
            text = clean,
            hasImageReferences = current.hasImageReferences
        )
        cleanupAfterImportTaskId = current.id
        return true
    }

    fun consumePendingImportDraft(id: String) {
        if (pendingImportDraft?.id == id) pendingImportDraft = null
    }

    fun onQuestionBankImportSaved() {
        val cleanupId = cleanupAfterImportTaskId ?: return
        if (task?.id == cleanupId) clearTask(keepSelectedDocument = false)
        cleanupAfterImportTaskId = null
    }

    fun clearTask(keepSelectedDocument: Boolean = true) {
        ensureInitialized()
        workflowJob?.cancel()
        workflowJob = null
        resultSaveJob?.cancel()
        resultSaveJob = null
        deleteResultFiles(task)
        task = null
        resultText = ""
        pendingImportDraft = null
        cleanupAfterImportTaskId = null
        store.saveTask(null)
        if (!keepSelectedDocument) {
            selectedDocument?.localPath?.let(::File)?.delete()
            selectedDocument = null
            store.saveSelectedDocument(null)
        }
        uiMessage = null
    }

    fun clearUiMessage() {
        uiMessage = null
    }

    fun acknowledgeVisibleTask() {
        val current = task ?: return
        if (!current.isWorking) acknowledgedTerminalTaskId = current.id
    }

    private suspend fun pollUntilTerminal(localTaskId: String) {
        while (true) {
            val current = requireTask(localTaskId)
            val status = withContext(Dispatchers.IO) {
                if (current.mode == MinerUMode.FREE) {
                    client.queryFree(current.taskId ?: error("缺少 MinerU task_id。"))
                } else {
                    client.queryPrecise(
                        batchId = current.batchId ?: error("缺少 MinerU batch_id。"),
                        token = store.loadPreciseToken(),
                        fileName = current.fileName
                    )
                }
            }
            when (status.state.lowercase()) {
                "done" -> {
                    val resultUrl = status.resultUrl
                    if (resultUrl == null) {
                        updateTask(
                            current.copy(
                                stage = DocumentTaskStage.FAILED,
                                serviceState = status.state,
                                message = "MinerU 未返回识别结果地址。",
                                errorMessage = "任务已完成，但没有可下载的结果文件。"
                            )
                        )
                        return
                    }
                    downloadAndDecode(localTaskId, resultUrl)
                    return
                }
                "failed" -> {
                    val message = status.errorMessage ?: "MinerU 未返回具体原因。"
                    updateTask(
                        current.copy(
                            stage = DocumentTaskStage.FAILED,
                            serviceState = status.state,
                            message = "MinerU 解析失败。",
                            errorMessage = listOfNotNull(status.errorCode, message).joinToString("：")
                        )
                    )
                    return
                }
                "running", "converting" -> updateTask(
                    current.copy(
                        stage = DocumentTaskStage.RUNNING,
                        serviceState = status.state,
                        extractedPages = status.extractedPages,
                        totalPages = status.totalPages,
                        message = when {
                            status.totalPages > 0 -> "正在解析：${status.extractedPages}/${status.totalPages} 页"
                            status.state == "converting" -> "正文已识别，正在生成结果文件……"
                            else -> "MinerU 正在识别文档内容……"
                        },
                        errorMessage = null
                    )
                )
                "waiting-file", "uploading", "pending" -> updateTask(
                    current.copy(
                        stage = DocumentTaskStage.QUEUED,
                        serviceState = status.state,
                        message = when (status.state) {
                            "waiting-file" -> "正在等待 MinerU 接收上传文件……"
                            "uploading" -> "MinerU 正在读取上传文件……"
                            else -> "任务已进入解析队列……"
                        },
                        errorMessage = null
                    )
                )
                else -> updateTask(
                    current.copy(
                        stage = DocumentTaskStage.QUEUED,
                        serviceState = status.state,
                        message = "MinerU 正在处理任务（${status.state.ifBlank { "pending" }}）……"
                    )
                )
            }
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private suspend fun downloadAndDecode(localTaskId: String, resultUrl: String) {
        val current = requireTask(localTaskId)
        val resultDir = recognitionDir()
        val rawResultFile = File(
            resultDir,
            if (current.mode == MinerUMode.FREE) "${current.id}_full.md" else "${current.id}_result.zip"
        )
        updateTask(
            current.copy(
                stage = DocumentTaskStage.DOWNLOADING,
                serviceState = "done",
                rawResultPath = rawResultFile.absolutePath,
                resultSourceUrl = resultUrl,
                message = "识别完成，正在下载并整理文本……",
                errorMessage = null
            )
        )
        val decoded = try {
            withContext(Dispatchers.IO) {
                client.download(
                    url = resultUrl,
                    destination = rawResultFile,
                    maxBytes = if (current.mode == MinerUMode.FREE) MAX_RESULT_TEXT_BYTES else MAX_PRECISE_ZIP_BYTES
                )
                if (current.mode == MinerUMode.FREE) {
                    MinerUResultDecoder.decodeFreeMarkdown(rawResultFile)
                } else {
                    MinerUResultDecoder.decodePreciseZip(rawResultFile)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            // 下载中断是瞬时故障，仍交给上层按“可继续查询”处理；
            // 结果本身有问题（超限、解不出正文、地址失效）时重试结局相同，直接判定失败。
            if (error !is MinerUResultException && error !is IllegalArgumentException) throw error
            rawResultFile.delete()
            updateTask(
                requireTask(localTaskId).copy(
                    stage = DocumentTaskStage.FAILED,
                    message = "识别结果无法解析，请重新提交识别。",
                    errorMessage = error.message ?: "结果文件处理失败"
                )
            )
            return
        }
        val resultFile = File(resultDir, "${current.id}_import.txt")
        withContext(Dispatchers.IO) { resultFile.writeText(decoded.importText, Charsets.UTF_8) }
        resultText = decoded.importText
        updateTask(
            requireTask(localTaskId).copy(
                stage = DocumentTaskStage.READY,
                rawResultPath = rawResultFile.absolutePath,
                resultPath = resultFile.absolutePath,
                resultSourceUrl = resultUrl,
                hasImageReferences = decoded.hasImageReferences,
                message = "文本已就绪，请核对后进入题库导入。",
                errorMessage = null
            )
        )
    }

    private fun copyPdfToPrivateStorage(uri: Uri): SelectedDocument {
        val resolver = appContext.contentResolver
        var displayName = "document.pdf"
        var declaredSize = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { index ->
                    displayName = cursor.getString(index).orEmpty().ifBlank { displayName }
                }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { index ->
                    if (!cursor.isNull(index)) declaredSize = cursor.getLong(index)
                }
            }
        }
        if (!displayName.lowercase().endsWith(".pdf")) throw IllegalArgumentException("目前在线识别只支持 PDF 文件。")
        if (declaredSize > MAX_PRECISE_PDF_BYTES) throw IllegalArgumentException("PDF 超过 200MB，无法提交 MinerU。")

        val destination = File(recognitionDir(), "source_${UUID.randomUUID()}.pdf")
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        total += count
                        if (total > MAX_PRECISE_PDF_BYTES) {
                            throw IllegalArgumentException("PDF 超过 200MB，无法提交 MinerU。")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: throw IllegalArgumentException("无法读取所选 PDF。")
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
        if (destination.length() <= 0L) {
            destination.delete()
            throw IllegalArgumentException("所选 PDF 为空。")
        }
        val pageCount = readPdfPageCount(destination)
        return SelectedDocument(
            fileName = sanitizeFileName(displayName),
            localPath = destination.absolutePath,
            sizeBytes = destination.length(),
            pageCount = pageCount
        )
    }

    private fun readPdfPageCount(file: File): Int {
        return runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
            }
        }.getOrDefault(0)
    }

    private fun validateRequest(document: SelectedDocument, value: MinerUSettings): String? {
        val pageRange = value.pageRange.filterNot(Char::isWhitespace)
        if (value.mode == MinerUMode.FREE) {
            if (document.sizeBytes > MAX_FREE_PDF_BYTES) return "免费模式仅支持不超过 10MB 的文件，请改用精准模式或压缩 PDF。"
            if (pageRange.isNotBlank() && !pageRange.matches(Regex("\\d+(?:-\\d+)?"))) {
                return "免费模式页码范围只支持单页（如 5）或连续范围（如 1-20）。"
            }
            val range = parsePositivePageRange(pageRange)
            if (range != null) {
                if (range.first <= 0 || range.last < range.first) return "页码范围无效。"
                if (document.pageCount > 0 && range.last > document.pageCount) return "页码范围超过 PDF 总页数。"
                if (range.last - range.first + 1 > 20) return "免费模式一次最多识别 20 页。"
            } else if (document.pageCount > 20) {
                return "该 PDF 共 ${document.pageCount} 页；免费模式请填写不超过 20 页的连续范围。"
            }
        } else {
            if (document.sizeBytes > MAX_PRECISE_PDF_BYTES) return "精准模式仅支持不超过 200MB 的文件。"
            if (pageRange.isNotBlank() && !pageRange.matches(PRECISE_PAGE_RANGE_PATTERN)) {
                return "精准模式页码范围格式无效，例如：1-20 或 2,4-6。"
            }
            if (pageRange.isBlank() && document.pageCount > 200) {
                return "该 PDF 共 ${document.pageCount} 页；精准模式一次最多识别 200 页，请填写页码范围。"
            }
        }
        return null
    }

    private fun parsePositivePageRange(value: String): IntRange? {
        if (value.isBlank()) return null
        val parts = value.split('-', limit = 2)
        val start = parts.firstOrNull()?.toIntOrNull() ?: return null
        val end = parts.getOrNull(1)?.toIntOrNull() ?: start
        return start..end
    }

    private fun sanitizeFileName(value: String): String {
        val clean = value.replace(Regex("[\\r\\n/\\\\]"), "_").trim().take(120)
        return clean.ifBlank { "document.pdf" }.let { if (it.lowercase().endsWith(".pdf")) it else "$it.pdf" }
    }

    private fun recognitionDir(): File {
        return File(appContext.noBackupFilesDir, "document_recognition").apply { mkdirs() }
    }

    private fun requireTask(id: String): DocumentRecognitionTask {
        return task?.takeIf { it.id == id } ?: throw CancellationException("Task replaced")
    }

    private fun updateTask(value: DocumentRecognitionTask) {
        val previousStage = task?.takeIf { it.id == value.id }?.stage
        val updated = value.copy(updatedAt = System.currentTimeMillis())
        if (
            updated.stage in setOf(DocumentTaskStage.READY, DocumentTaskStage.FAILED, DocumentTaskStage.PAUSED) &&
            previousStage != updated.stage
        ) {
            acknowledgedTerminalTaskId = null
        }
        task = updated
        store.saveTask(updated)
    }

    private fun deleteResultFiles(value: DocumentRecognitionTask?) {
        value?.rawResultPath?.let(::File)?.delete()
        value?.resultPath?.let(::File)?.delete()
    }

    private fun ensureInitialized() {
        check(initialized) { "DocumentRecognitionManager.init() must be called first." }
    }

    private const val POLL_INTERVAL_MILLIS = 3_000L
    private const val MAX_FREE_PDF_BYTES = 10_000_000L
    private const val MAX_PRECISE_PDF_BYTES = 200_000_000L
    private const val MAX_RESULT_TEXT_BYTES = 24L * 1024L * 1024L
    private const val MAX_PRECISE_ZIP_BYTES = 250L * 1024L * 1024L
    private val PRECISE_PAGE_RANGE_PATTERN = Regex("-?\\d+(?:--?\\d+)?(?:,-?\\d+(?:--?\\d+)?)*")
}
