package com.yiqiu.shirohaquiz.state

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import com.yiqiu.shirohaquiz.security.TestKeystoreRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CloudRestoreJvmTest {
    @get:Rule val keystore = TestKeystoreRule()
    @Test fun isolatedCloudRestoreSafetyCases() {
        assertEquals(14, QuizRepositoryCloudRestoreSafetyTest(RuntimeEnvironment.getApplication()).runAll())
    }

    @Test fun completeJsonLargerThanImageEntryLimitIsAccepted() {
        val bytes = ("{\"schemaVersion\":3,\"banks\":[]}" + " ".repeat(21 * 1024 * 1024)).toByteArray()
        assertEquals(0, QuizRepository.previewBackupBytes(bytes).bankCount)
    }
}
