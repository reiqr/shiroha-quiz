package com.yiqiu.shirohaquiz.sync.webdav

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class WebDavBackupManagerTest {
    @Test fun downloadPreviewRequiresExplicitConfirmationBeforeMutation() = runBlocking {
        withManager { manager, counters ->
            val uploaded = manager.uploadBackupAndAwait()!!
            assertTrue(manager.prepareRestore(uploaded.entry))
            awaitTerminal(manager)
            val preview = manager.preparedRestore.value!!
            assertEquals(0, counters[0])
            assertFalse(manager.restorePrepared(preview.token, WebDavRestoreMode.REPLACE_CONTENT_KEEP_SETTINGS, false))
            assertEquals(0, counters[0])
            assertTrue(manager.restorePrepared(preview.token, WebDavRestoreMode.REPLACE_CONTENT_KEEP_SETTINGS, true))
            awaitTerminal(manager)
            assertEquals(1, counters[0])
            assertNull(manager.preparedRestore.value)
        }
    }

    @Test fun completedUploadLeavesSlotFreeAndNoPrivateZipCache() = runBlocking {
        withManager { manager, _ ->
            val uploaded = manager.uploadBackupAndAwait()!!
            assertTrue(uploaded.entry.integrityAvailable)
            assertFalse(manager.isBusy())
            assertEquals(WebDavTaskStage.SUCCEEDED, manager.task.value!!.stage)
            val cache = java.io.File(RuntimeEnvironment.getApplication().noBackupFilesDir, "webdav_cache")
            assertTrue(cache.listFiles().orEmpty().none { it.extension == "zip" || it.extension == "json" })
            assertFalse(manager.cancelTask("a-different-task"))
        }
    }

    @Test fun zeroBankSnapshotWithIndependentRecordsIsAccepted() = runBlocking {
        withManager(preview = WebDavBackupPreview(0, 0, recordCount = 1, includesStudyRecords = true)) { manager, _ ->
            assertNotNull(manager.uploadBackupAndAwait())
        }
    }

    @Test fun automaticBackupDefaultsRemainOff() {
        val context = RuntimeEnvironment.getApplication()
        val defaults = WebDavAutoBackup.load(context)
        assertEquals(0, defaults.intervalDays)
        assertFalse(defaults.deleteOldBackups)
        assertThrows(IllegalArgumentException::class.java) {
            WebDavAutoBackup.save(context, defaults.copy(intervalDays = 1))
        }
        assertEquals(0, WebDavAutoBackup.load(context).intervalDays)
    }

    @Test fun periodicBackupUsesOneJobAndCanBeDisabled() {
        val context = RuntimeEnvironment.getApplication()
        if (runCatching { androidx.work.WorkManager.getInstance(context) }.isFailure) {
            androidx.work.WorkManager.initialize(context, androidx.work.Configuration.Builder().build())
        }
        val workManager = androidx.work.WorkManager.getInstance(context)
        val settings = WebDavAutoBackupSettings(intervalDays = 1)
        WebDavAutoBackup.schedule(context, settings)
        val first = workManager.getWorkInfosForUniqueWork("shiroha_webdav_auto_backup").get(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(1, first.size)
        WebDavAutoBackup.schedule(context, settings)
        val updated = workManager.getWorkInfosForUniqueWork("shiroha_webdav_auto_backup").get(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(1, updated.size)
        assertEquals(first.single().id, updated.single().id)
        WebDavAutoBackup.schedule(context, settings.copy(intervalDays = 0))
        val cancelled = workManager.getWorkInfosForUniqueWork("shiroha_webdav_auto_backup").get(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(androidx.work.WorkInfo.State.CANCELLED, cancelled.single().state)
    }

    private suspend fun awaitTerminal(manager: WebDavBackupManager) {
        val id = manager.task.value!!.id
        withTimeout(10_000) {
            val terminal = manager.task.first { it?.id == id && it.stage !in setOf(WebDavTaskStage.RUNNING, WebDavTaskStage.CANCELLING) }
            assertEquals(terminal?.message, WebDavTaskStage.SUCCEEDED, terminal?.stage)
        }
    }

    private suspend fun withManager(preview: WebDavBackupPreview = WebDavBackupPreview(1, 1),
                                    test: suspend (WebDavBackupManager, IntArray) -> Unit) {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val counters = intArrayOf(0)
        val store = WebDavSettingsStore(context)
        val dav = WebDavClientTest.FakeDav()
        val manager = WebDavBackupManager.createForTesting(context,
            WebDavBackupHooks(previewBackupBytes = { preview }, replaceContentFromBackupBytes = { counters[0]++; "已覆盖恢复" },
                exportFullBackupZip = { "native-backup-fixture".toByteArray() }, importCopyFromBackupBytes = { _, _ -> counters[0]++; "已导入" }),
            store, clientFactory = { settings, credentials -> WebDavClient(settings.baseUrl, settings.remoteDirectory,
                credentials, callFactory = dav, xmlParserFactory = { org.kxml2.io.KXmlParser() }) }, scope = scope)
        try {
            manager.saveSettings(WebDavSettings("https://dav.example.com/dav/", "user"), "password")
            withTimeout(20_000) { test(manager, counters) }
        } finally { manager.cancelCurrentTask(); scope.cancel(); Dispatchers.resetMain() }
    }
}
