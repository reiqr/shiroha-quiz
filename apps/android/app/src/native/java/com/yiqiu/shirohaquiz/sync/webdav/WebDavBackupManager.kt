package com.yiqiu.shirohaquiz.sync.webdav

import android.content.Context
import com.yiqiu.shirohaquiz.BuildConfig
import com.yiqiu.shirohaquiz.state.QuizRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Application-owned, not navigation/ViewModel-owned. Only one workflow (including cleanup) may
 * hold the slot. Automatic backup scheduling and UI confirmation dialogs belong to the parent.
 * Parent wiring: hooks.previewBackupBytes = { map(QuizRepository.previewBackupBytes(it)) };
 * hooks.replaceContentFromBackupBytes = QuizRepository::replaceContentFromBackupBytes.
 */
class WebDavBackupManager private constructor(
    context: Context,
    private val hooks: WebDavBackupHooks,
    private val settingsStore: WebDavSettingsStore,
    private val clientFactory: (WebDavSettings, WebDavCredentials) -> WebDavClient,
    private val scope: CoroutineScope,
    private val clockMillis: () -> Long
) {
    private val appContext = context.applicationContext
    private val cacheDirectory = File(appContext.noBackupFilesDir, "webdav_cache")
    private val mutableSettings = MutableStateFlow(settingsStore.load())
    private val mutableTask = MutableStateFlow<WebDavTaskState?>(null)
    private val mutableBackups = MutableStateFlow<List<WebDavBackupEntry>>(emptyList())
    private val mutablePrepared = MutableStateFlow<WebDavPreparedRestore?>(null)
    private val mutableDownload = MutableStateFlow<File?>(null)
    private val mutableUpload = MutableStateFlow<WebDavUploadResult?>(null)
    private val mutableDelete = MutableStateFlow<WebDavDeleteResult?>(null)
    private var preparedFile: File? = null
    private var active: ActiveTask? = null

    val settings: StateFlow<WebDavSettings> = mutableSettings.asStateFlow()
    val task: StateFlow<WebDavTaskState?> = mutableTask.asStateFlow()
    val backups: StateFlow<List<WebDavBackupEntry>> = mutableBackups.asStateFlow()
    val preparedRestore: StateFlow<WebDavPreparedRestore?> = mutablePrepared.asStateFlow()
    /** Verified local export; UI must copy/share it before cleanup/expiry, never add it to backups. */
    val downloadedFile: StateFlow<File?> = mutableDownload.asStateFlow()
    val lastUpload: StateFlow<WebDavUploadResult?> = mutableUpload.asStateFlow()
    val lastDelete: StateFlow<WebDavDeleteResult?> = mutableDelete.asStateFlow()

    @Synchronized fun isBusy(): Boolean = active != null
    @Synchronized fun hasPassword(): Boolean = settingsStore.loadPassword() != null

    /** Settings cannot change the origin/account of an in-flight request or prepared restore. */
    @Synchronized fun saveSettings(value: WebDavSettings, password: String? = null): Boolean {
        if (active != null) return false
        mutableSettings.value = settingsStore.save(value, password)
        invalidatePrepared()
        mutableBackups.value = emptyList()
        return true
    }

    @Synchronized fun clearSettings(): Boolean {
        if (active != null) return false
        settingsStore.clear()
        mutableSettings.value = settingsStore.load()
        invalidatePrepared()
        mutableBackups.value = emptyList()
        mutableUpload.value = null
        mutableDelete.value = null
        return true
    }

    fun testConnection(): Boolean = start(WebDavTaskKind.TEST_CONNECTION) { work ->
        val client = clientForSavedSettings()
        withContext(Dispatchers.IO) { client.testConnection(work.cancellation) }
        "连接测试成功，远端备份目录存在。"
    }

    /** Never called by upload, list, testConnection, restore or automatic scheduling. */
    fun createDirectoryByUserRequest(): Boolean = start(WebDavTaskKind.CREATE_DIRECTORY) { work ->
        val client = clientForSavedSettings()
        withContext(Dispatchers.IO) { client.createDirectoryByUserRequest(work.cancellation) }
        "远端备份目录已就绪。"
    }

    fun refreshBackups(): Boolean = start(WebDavTaskKind.LIST) { work ->
        val client = clientForSavedSettings()
        val result = withContext(Dispatchers.IO) { loadRemoteEntries(client, work.cancellation) }
        work.cancellation.throwIfCancelled()
        mutableBackups.value = result
        "已找到 ${result.size} 份云端备份。"
    }

    fun uploadBackup(): Boolean = start(WebDavTaskKind.UPLOAD) { work ->
        uploadMessage(performUpload(work))
    }

    /** Worker API. Shares the UI's task slot; caller cancellation cancels only its own upload. */
    suspend fun uploadBackupAndAwait(): WebDavUploadResult? {
        var result: WebDavUploadResult? = null
        val work = launchWorkflow(WebDavTaskKind.UPLOAD) {
            val uploaded = performUpload(it)
            result = uploaded
            uploadMessage(uploaded)
        } ?: return null
        try {
            work.completion.await()
            return result ?: throw WebDavException("WebDAV 上传未返回结果。")
        } catch (cancelled: CancellationException) {
            synchronized(this) {
                if (active === work && !work.irreversible) cancelCurrentTask()
            }
            throw cancelled
        }
    }

    /** Throwing variant retained for callers that want busy treated as a retryable failure. */
    suspend fun uploadBackupAwait(): WebDavUploadResult = uploadBackupAndAwait()
        ?: throw WebDavException("已有 WebDAV 任务正在执行，请稍后重试。")

    private fun uploadMessage(result: WebDavUploadResult): String =
        if (result.metadataPublished) "备份上传成功，完整文件大小和 SHA256 校验通过。"
        else "备份已上传并通过校验，但附加元数据发布失败，请刷新列表重新扫描。"

    private suspend fun performUpload(work: ActiveTask): WebDavUploadResult {
        mutableUpload.value = null
        val client = clientForSavedSettings()
        val file = newCacheFile("zip")
        var metadataFile: File? = null
        try {
            // QuizRepository is Main-owned. Its existing exporter returns bytes; only this
            // boundary is buffered. Hashing and all network upload/download are streamed on IO.
            val bytes = withContext(Dispatchers.Main.immediate) {
                work.cancellation.throwIfCancelled()
                hooks.exportFullBackupZip?.invoke() ?: QuizRepository.exportFullBackupZip()
            }
            if (bytes.isEmpty() || bytes.size.toLong() > MAX_BACKUP_BYTES) throw WebDavIntegrityException("导出的备份为空或超过大小限制。")
            val preview = withContext(Dispatchers.IO) { hooks.previewBackupBytes(bytes) }
            requireValidPreview(preview)
            val digest = withContext(Dispatchers.IO) {
                writePrivateFile(file, bytes, work.cancellation)
                WebDavClient.hashFile(file, work.cancellation, MAX_BACKUP_BYTES)
            }
            val createdAt = clockMillis()
            val name = WebDavPaths.newBackupName(createdAt, BuildConfig.VERSION_NAME, digest.sha256)
            setFileName(work, name)
            val entry = WebDavBackupEntry(
                fileName = name,
                createdAt = createdAt,
                sizeBytes = digest.sizeBytes,
                source = "android",
                appVersion = BuildConfig.VERSION_NAME,
                sha256 = digest.sha256,
                bankCount = preview.bankCount,
                questionCount = preview.questionCount,
                includesWrongBook = preview.includesWrongBook,
                includesFavorites = preview.includesFavorites,
                includesStudyRecords = preview.includesStudyRecords
            )
            withContext(Dispatchers.IO) { client.uploadAtomic(file, name, work.cancellation, progress(work)) }
            mutableUpload.value = WebDavUploadResult(entry, false)
            mutableBackups.value = (mutableBackups.value.filterNot { it.fileName == name } + entry)
                .sortedByDescending { it.createdAt ?: 0 }
            val metadata = newCacheFile("json")
            metadataFile = metadata
            var metadataPublished = false
            try {
                withContext(Dispatchers.IO) {
                    writePrivateFile(metadata, entry.toMetadataJson().toString().toByteArray(Charsets.UTF_8), work.cancellation)
                    client.uploadAtomic(metadata, "$name.meta.json", work.cancellation)
                }
                metadataPublished = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // The immutable filename still carries SHA256/time/source/version; do not
                // misreport a verified, published backup as missing if optional indexing fails.
            }
            val result = WebDavUploadResult(entry, metadataPublished)
            mutableUpload.value = result
            return result
        } finally {
            withContext(Dispatchers.IO) {
                file.delete()
                metadataFile?.delete()
            }
        }
    }

    /** Download/validate/preview only. It never invokes either mutation callback. */
    fun prepareRestore(entry: WebDavBackupEntry): Boolean = start(WebDavTaskKind.PREVIEW, entry.fileName) { work ->
        invalidatePrepared()
        val client = clientForSavedSettings()
        val file = newCacheFile(if (entry.fileName.endsWith(".zip")) "zip" else "json")
        var retained = false
        try {
            val preview = withContext(Dispatchers.IO) {
                client.download(entry, file, work.cancellation, progress(work))
                val bytes = readVerifiedBytes(file, entry, work.cancellation)
                hooks.previewBackupBytes(bytes).also(::requireValidPreview)
            }
            work.cancellation.throwIfCancelled()
            val prepared = WebDavPreparedRestore(UUID.randomUUID().toString(), entry, preview, clockMillis() + PREVIEW_TTL_MILLIS)
            preparedFile = file
            mutablePrepared.value = prepared
            retained = true
            scope.launch {
                delay(PREVIEW_TTL_MILLIS)
                synchronized(this@WebDavBackupManager) {
                    if (active == null && mutablePrepared.value?.token == prepared.token) invalidatePrepared()
                }
            }
            "备份校验通过，请检查预览并明确选择恢复方式。"
        } finally {
            if (!retained) withContext(Dispatchers.IO) { file.delete() }
        }
    }

    /** Explicit restore confirmation; no default mode and no reliance on import meaning replace. */
    fun restorePrepared(token: String, mode: WebDavRestoreMode, userConfirmed: Boolean): Boolean {
        if (!userConfirmed) return false
        return start(WebDavTaskKind.RESTORE) { work ->
            val prepared = mutablePrepared.value?.takeIf { it.token == token && it.expiresAt > clockMillis() }
                ?: throw WebDavException("恢复预览不存在或已过期，请重新下载并预览。")
            val file = preparedFile ?: throw WebDavException("已校验的恢复缓存不存在。")
            setFileName(work, prepared.entry.fileName)
            requireValidPreview(prepared.preview)
            if (mode == WebDavRestoreMode.REPLACE_CONTENT_KEEP_SETTINGS && !prepared.preview.canReplaceContent) {
                throw WebDavException("此备份不能用于覆盖本地内容。")
            }
            try {
                val bytes = withContext(Dispatchers.IO) { readVerifiedBytes(file, prepared.entry, work.cancellation) }
                val result = withContext(Dispatchers.Main.immediate) {
                    synchronized(this@WebDavBackupManager) {
                        work.cancellation.throwIfCancelled()
                        // Once mutation begins it cannot safely be interrupted or replayed.
                        work.irreversible = true
                    }
                    when (mode) {
                        WebDavRestoreMode.IMPORT_COPY -> hooks.importCopyFromBackupBytes?.invoke(prepared.entry.fileName, bytes)
                            ?: QuizRepository.importBackupBytes(appContext, prepared.entry.fileName, bytes)
                        WebDavRestoreMode.REPLACE_CONTENT_KEEP_SETTINGS -> hooks.replaceContentFromBackupBytes(bytes)
                    }
                }
                if (result.startsWith("导入失败") || result.startsWith("恢复失败") || result.startsWith("覆盖失败")) {
                    throw WebDavException(result.take(300))
                }
                result
            } finally {
                invalidatePrepared()
            }
        }
    }

    /** Keeps a verified private file for the UI's explicit save/share action, without restoring. */
    fun downloadBackup(entry: WebDavBackupEntry): Boolean = start(WebDavTaskKind.DOWNLOAD, entry.fileName) { work ->
        val client = clientForSavedSettings()
        mutableDownload.value?.delete()
        mutableDownload.value = null
        val file = newCacheFile(if (entry.fileName.endsWith(".zip")) "zip" else "json")
        var retained = false
        try {
            withContext(Dispatchers.IO) { client.download(entry, file, work.cancellation, progress(work)) }
            work.cancellation.throwIfCancelled()
            mutableDownload.value = file
            retained = true
            scope.launch {
                delay(CACHE_TTL_MILLIS)
                synchronized(this@WebDavBackupManager) {
                    if (active == null && mutableDownload.value == file) {
                        file.delete()
                        mutableDownload.value = null
                    }
                }
            }
            "云端备份已下载并通过完整校验，可手动保存或分享。"
        } finally {
            if (!retained) withContext(Dispatchers.IO) { file.delete() }
        }
    }

    /** UI must supply true only after its second confirmation; this module has no dialog UI. */
    fun deleteBackup(entry: WebDavBackupEntry, userConfirmedTwice: Boolean): Boolean {
        if (!userConfirmedTwice) return false
        return start(WebDavTaskKind.DELETE, entry.fileName) { work ->
            val client = clientForSavedSettings()
            mutableDelete.value = null
            val deleted = withContext(Dispatchers.IO) { client.deleteBackup(entry, work.cancellation) }
            mutableBackups.value = mutableBackups.value.filterNot { it.fileName == entry.fileName }
            mutableDelete.value = WebDavDeleteResult(deleted, false)
            var metadataDeleted = false
            try {
                withContext(Dispatchers.IO) { client.deleteMetadata(entry.fileName, work.cancellation) }
                metadataDeleted = true // Already absent is also a clean index state.
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                // Listing is directory-authoritative, so stale sidecars/legacy indexes are ignored.
            }
            mutableDelete.value = WebDavDeleteResult(deleted, metadataDeleted)
            if (metadataDeleted) "远端备份已移除；列表会按实际文件过滤旧索引中的残留记录。"
            else "远端备份已移除，但元数据清理失败，请刷新列表重新扫描。"
        }
    }

    /** Sticky token cancellation, not Job.cancel: the task slot is held until IO/finally finishes. */
    @Synchronized fun cancelCurrentTask(taskId: String? = null): Boolean {
        val work = active ?: return false
        if ((taskId != null && work.id != taskId) || work.irreversible) return false
        work.cancellation.cancel()
        mutableTask.value = mutableTask.value?.copy(stage = WebDavTaskStage.CANCELLING, message = "正在取消任务……")
        return true
    }

    fun cancelTask(taskId: String): Boolean = cancelCurrentTask(taskId)

    @Synchronized fun dismissFinishedTask(): Boolean {
        if (active != null) return false
        mutableTask.value = null
        return true
    }

    @Synchronized fun discardPreparedRestore(): Boolean {
        if (active != null) return false
        invalidatePrepared()
        return true
    }

    fun cleanTemporaryCache(): Boolean = start(WebDavTaskKind.CLEAN_CACHE) { work ->
        invalidatePrepared()
        mutableDownload.value?.delete()
        mutableDownload.value = null
        withContext(Dispatchers.IO) { cleanCacheFiles(work.cancellation, removeAll = true) }
        "WebDAV 私有临时缓存已清理。"
    }

    private suspend fun loadRemoteEntries(client: WebDavClient, cancellation: WebDavCancellation): List<WebDavBackupEntry> {
        val resources = client.list(cancellation).filter { !it.isDirectory && WebDavPaths.isBackup(it.fileName) }
        if (resources.size > MAX_LIST_ENTRIES) throw WebDavException("远端备份数量超过安全限制，请减少目录中的备份后再查看。")
        val legacy = mutableMapOf<String, JSONObject>()
        // A manifest is advisory, never a source of unvalidated remote paths or DELETE targets.
        for (read in listOf<() -> ByteArray?>({ client.readMetadata("manifest.json", cancellation) }, { client.readLegacyManifest(cancellation) })) {
            cancellation.throwIfCancelled()
            val bytes = try { read() } catch (_: IOException) { null }
            val root = bytes?.let { runCatching { JSONObject(it.toString(Charsets.UTF_8)) }.getOrNull() }
            val entries = root?.optJSONArray("backups") ?: continue
            if (entries.length() > MAX_LIST_ENTRIES) continue
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val name = item.optString("fileName")
                if (WebDavPaths.isBackup(name)) legacy[name] = item
            }
        }
        return resources.map { resource ->
            cancellation.throwIfCancelled()
            val raw = try { client.readMetadata("${resource.fileName}.meta.json", cancellation) } catch (_: IOException) { null }
            val sidecar = raw?.let { runCatching { JSONObject(it.toString(Charsets.UTF_8)) }.getOrNull() }
            val indexed = sidecar?.let { WebDavBackupEntry.fromMetadataJson(it, resource) }
                ?: legacy[resource.fileName]?.let { WebDavBackupEntry.fromMetadataJson(it, resource) }
            val fallback = WebDavPaths.fallbackEntry(resource)
            // A generated filename's embedded digest must agree with advisory metadata.
            if (indexed != null && (fallback.sha256 == null || WebDavPaths.equalSha256(fallback.sha256, indexed.sha256!!))) indexed else fallback
        }.sortedByDescending { it.createdAt ?: 0 }
    }

    private fun start(
        kind: WebDavTaskKind,
        fileName: String? = null,
        operation: suspend (ActiveTask) -> String
    ): Boolean = launchWorkflow(kind, fileName, operation) != null

    @Synchronized private fun launchWorkflow(
        kind: WebDavTaskKind,
        fileName: String? = null,
        operation: suspend (ActiveTask) -> String
    ): ActiveTask? {
        if (active != null) return null
        val work = ActiveTask(UUID.randomUUID().toString(), WebDavCancellation())
        active = work
        mutableTask.value = WebDavTaskState(id = work.id, kind = kind, fileName = fileName, startedAt = clockMillis())
        work.job = scope.launch(start = CoroutineStart.LAZY) {
            var failure: Throwable? = null
            val watchdog = scope.launch {
                delay(TASK_TIMEOUT_MILLIS)
                synchronized(this@WebDavBackupManager) {
                    if (active === work && !work.irreversible) {
                        work.timedOut = true
                        work.cancellation.cancel()
                    }
                }
            }
            try {
                withContext(Dispatchers.IO) { cleanCacheFiles(work.cancellation, removeAll = false) }
                val message = operation(work)
                if (!work.irreversible) work.cancellation.throwIfCancelled()
                finish(work, WebDavTaskStage.SUCCEEDED, message)
            } catch (cancelled: CancellationException) {
                failure = if (work.timedOut) WebDavException("WebDAV 任务超时。") else cancelled
                finish(work, if (work.timedOut) WebDavTaskStage.FAILED else WebDavTaskStage.CANCELLED,
                    if (work.timedOut) "WebDAV 任务超时，请刷新云端列表后再重试。"
                    else "任务已取消。上传或删除可能已在服务器生效，请刷新列表后再重试。")
            } catch (error: WebDavException) {
                failure = error
                finish(work, WebDavTaskStage.FAILED, error.message ?: "WebDAV 操作失败。")
            } catch (_: Exception) {
                failure = WebDavException("WebDAV 操作失败，请检查配置、备份内容和可用存储空间。")
                // Never surface arbitrary hook/keystore/network exception messages or secrets.
                finish(work, WebDavTaskStage.FAILED, "WebDAV 操作失败，请检查配置、备份内容和可用存储空间。")
            } finally {
                watchdog.cancel()
                synchronized(this@WebDavBackupManager) {
                    if (active === work) {
                        active = null
                        if (mutablePrepared.value?.expiresAt?.let { it <= clockMillis() } == true) invalidatePrepared()
                        mutableTask.value = mutableTask.value?.copy(stage = work.terminalStage, message = work.terminalMessage)
                    }
                }
                val error = failure
                if (error == null) work.completion.complete(Unit) else work.completion.completeExceptionally(error)
            }
        }
        work.job!!.start()
        return work
    }

    @Synchronized private fun finish(work: ActiveTask, stage: WebDavTaskStage, message: String) {
        if (active === work) {
            work.terminalStage = stage
            work.terminalMessage = message
        }
    }

    @Synchronized private fun setFileName(work: ActiveTask, name: String) {
        if (active === work) mutableTask.value = mutableTask.value?.copy(fileName = name)
    }

    private fun progress(work: ActiveTask): (Long, Long) -> Unit = { bytes, total ->
        val now = clockMillis()
        if (bytes == total || now - work.lastProgressAt >= 200) {
            synchronized(this) {
                if (active === work) {
                    work.lastProgressAt = now
                    mutableTask.value = mutableTask.value?.copy(transferredBytes = bytes, totalBytes = total)
                }
            }
        }
    }

    private fun clientForSavedSettings(): WebDavClient = clientFactory(settingsStore.load().validated(), settingsStore.credentials())

    @Synchronized private fun invalidatePrepared() {
        preparedFile?.delete()
        preparedFile = null
        mutablePrepared.value = null
    }

    private fun newCacheFile(extension: String): File {
        require(extension == "zip" || extension == "json")
        check(cacheDirectory.isDirectory || cacheDirectory.mkdirs()) { "无法创建不参与备份的 WebDAV 私有缓存。"
        }
        check(cacheDirectory.canonicalFile.parentFile == appContext.noBackupFilesDir.canonicalFile)
        return File(cacheDirectory, "webdav-${UUID.randomUUID()}.$extension")
    }

    private fun cleanCacheFiles(cancellation: WebDavCancellation, removeAll: Boolean) {
        if (!cacheDirectory.exists()) return
        check(cacheDirectory.canonicalFile.parentFile == appContext.noBackupFilesDir.canonicalFile)
        val protectedFiles = synchronized(this) { listOfNotNull(preparedFile?.canonicalPath, mutableDownload.value?.canonicalPath).toSet() }
        cacheDirectory.listFiles()?.forEach { file ->
            cancellation.throwIfCancelled()
            if (file.isFile && file.name.startsWith("webdav-") &&
                file.canonicalFile.parentFile == cacheDirectory.canonicalFile &&
                file.canonicalPath !in protectedFiles && (removeAll || file.lastModified() <= clockMillis() - CACHE_TTL_MILLIS)) {
                if (!file.delete()) throw WebDavException("无法删除过期的 WebDAV 缓存。")
            }
        }
    }

    private fun writePrivateFile(file: File, bytes: ByteArray, cancellation: WebDavCancellation) {
        FileOutputStream(file).use { output ->
            var offset = 0
            while (offset < bytes.size) {
                cancellation.throwIfCancelled()
                val count = minOf(DEFAULT_BUFFER_SIZE, bytes.size - offset)
                output.write(bytes, offset, count)
                offset += count
            }
            output.fd.sync()
        }
    }

    private fun readVerifiedBytes(file: File, entry: WebDavBackupEntry, cancellation: WebDavCancellation): ByteArray {
        if (!entry.integrityAvailable || entry.sizeBytes > MAX_BACKUP_BYTES || !file.isFile || file.length() != entry.sizeBytes) {
            throw WebDavIntegrityException("已校验的备份缓存不存在、发生变化或超过大小限制。")
        }
        val bytes = ByteArray(entry.sizeBytes.toInt())
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            var offset = 0
            while (offset < bytes.size) {
                cancellation.throwIfCancelled()
                val count = input.read(bytes, offset, minOf(DEFAULT_BUFFER_SIZE, bytes.size - offset))
                if (count < 0) throw WebDavIntegrityException("备份缓存被截断，内容不完整。")
                if (count == 0) continue
                digest.update(bytes, offset, count)
                offset += count
            }
            if (input.read() != -1) throw WebDavIntegrityException("备份缓存大小在下载后发生变化。")
        }
        cancellation.throwIfCancelled()
        if (!WebDavPaths.equalSha256(WebDavPaths.digestHex(digest.digest()), entry.sha256!!)) {
            throw WebDavIntegrityException("恢复前备份缓存的 SHA256 校验失败。")
        }
        return bytes
    }

    private fun requireValidPreview(preview: WebDavBackupPreview) {
        val counts = listOf(preview.bankCount, preview.questionCount, preview.wrongCount, preview.favoriteCount,
            preview.recordCount, preview.assetCount, preview.slashedCount)
        val hasContent = preview.bankCount > 0 || preview.wrongCount > 0 || preview.favoriteCount > 0 || preview.recordCount > 0
        if (!preview.canRestore || counts.any { it < 0 } || !hasContent) {
            throw WebDavException("备份内容校验失败，未修改任何本地内容。")
        }
    }

    private class ActiveTask(val id: String, val cancellation: WebDavCancellation) {
        val completion = CompletableDeferred<Unit>()
        var job: Job? = null
        var irreversible = false
        var timedOut = false
        var lastProgressAt = 0L
        var terminalStage = WebDavTaskStage.FAILED
        var terminalMessage = "WebDAV 任务未完成。"
    }

    companion object {
        private const val MAX_BACKUP_BYTES = WebDavClient.DEFAULT_MAX_BACKUP_BYTES
        private const val MAX_LIST_ENTRIES = 500
        private const val PREVIEW_TTL_MILLIS = 30L * 60 * 1000
        private const val CACHE_TTL_MILLIS = 24L * 60 * 60 * 1000
        private const val TASK_TIMEOUT_MILLIS = 15L * 60 * 1000
        @Volatile private var instance: WebDavBackupManager? = null

        /** Initialize once at application level, then collect StateFlows from any destination. */
        fun getInstance(
            context: Context,
            hooks: WebDavBackupHooks = nativeHooks(),
            settingsStore: WebDavSettingsStore = WebDavSettingsStore(context),
            clientFactory: (WebDavSettings, WebDavCredentials) -> WebDavClient = { settings, credentials ->
                WebDavClient(settings.baseUrl, settings.remoteDirectory, credentials)
            }
        ): WebDavBackupManager = instance ?: synchronized(this) {
            instance ?: WebDavBackupManager(
                context, hooks, settingsStore, clientFactory,
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), System::currentTimeMillis
            ).also { instance = it }
        }

        private fun nativeHooks() = WebDavBackupHooks(
            previewBackupBytes = { bytes ->
                val preview = QuizRepository.previewBackupBytes(bytes)
                WebDavBackupPreview(
                    bankCount = preview.bankCount,
                    questionCount = preview.questionCount,
                    includesWrongBook = preview.wrongCount > 0,
                    includesFavorites = preview.favoriteCount > 0,
                    includesStudyRecords = preview.recordCount > 0,
                    source = preview.exportedBy ?: "unknown",
                    wrongCount = preview.wrongCount,
                    favoriteCount = preview.favoriteCount,
                    recordCount = preview.recordCount,
                    assetCount = preview.assetCount,
                    slashedCount = preview.slashedCount,
                    sourceVersion = preview.sourceVersion,
                    sourceKind = preview.sourceKind,
                    exportedAt = preview.exportedAt
                )
            },
            replaceContentFromBackupBytes = QuizRepository::replaceContentFromBackupBytes
        )

        /** Isolated test instance; supplied scope/clock/transport can be driven by a mock harness. */
        fun createForTesting(
            context: Context,
            hooks: WebDavBackupHooks,
            settingsStore: WebDavSettingsStore,
            clientFactory: (WebDavSettings, WebDavCredentials) -> WebDavClient,
            scope: CoroutineScope,
            clockMillis: () -> Long = System::currentTimeMillis
        ): WebDavBackupManager = WebDavBackupManager(context, hooks, settingsStore, clientFactory, scope, clockMillis)
    }
}
