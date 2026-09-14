package com.yiqiu.shirohaquiz.security

import android.content.Context
import com.yiqiu.shirohaquiz.state.QuizRepository
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RepositoryAiKeyTest {
    @get:Rule val keystore = TestKeystoreRule()
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("shiroha_quiz_native", Context.MODE_PRIVATE)

    @Before fun initialize() { QuizRepository.resetForTesting(); preferences.edit().clear().commit(); QuizRepository.init(context) }
    @After fun reset() { QuizRepository.resetForTesting() }

    @Test fun savingAndOrdinaryPersistenceNeverWritesPlaintextKey() {
        QuizRepository.setAiInterfaceConfig(context, "DeepSeek", "https://example.com/v1", "test-only-ai-key", "model")
        assertEquals("test-only-ai-key", QuizRepository.aiApiKey)
        val ciphertext = java.io.File(context.noBackupFilesDir, "shiroha_secure_secrets.json")
        assertTrue(ciphertext.isFile)
        assertFalse(ciphertext.readText().contains("test-only-ai-key"))
        assertFalse(preferences.contains("ai_api_key"))
        QuizRepository.setDarkThemeEnabled(context, true)
        assertFalse(preferences.contains("ai_api_key"))
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("test-only-ai-key", QuizRepository.aiApiKey)
    }

    @Test fun legacyKeyMigratesWithoutChangingExistingConfiguration() {
        preferences.edit().putString("ai_api_key", "legacy-test-key").putString("ai_model_name", "old-model").commit()
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("legacy-test-key", QuizRepository.aiApiKey)
        assertEquals("old-model", QuizRepository.aiModelName)
        assertFalse(preferences.contains("ai_api_key"))
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("legacy-test-key", QuizRepository.aiApiKey)
    }

    @Test fun explicitClearRemovesKeyWithoutTouchingOtherSecrets() {
        val secrets = SecureSecretStore(context)
        secrets.put("mineru_precise_token", "test-mineru-token")
        secrets.put("webdav_password", "test-webdav-password")
        QuizRepository.setAiInterfaceConfig(context, "DeepSeek", "https://example.com/v1", "test-only-ai-key", "model")
        QuizRepository.clearAiConfig(context)
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("", QuizRepository.aiApiKey)
        assertFalse(preferences.contains("ai_api_key"))
        assertEquals("test-mineru-token", secrets.get("mineru_precise_token"))
        assertEquals("test-webdav-password", secrets.get("webdav_password"))
    }

    @Test fun failedKeySaveDoesNotReplaceExistingSettings() {
        QuizRepository.setAiInterfaceConfig(context, "DeepSeek", "https://old.example.com/v1", "original-key", "old-model")
        val provider = java.security.Security.getProvider("AndroidKeyStore")
        java.security.Security.removeProvider("AndroidKeyStore")
        try {
            assertFalse(QuizRepository.setAiInterfaceConfig(context, "OpenAI", "https://new.example.com/v1", "new-key", "new-model"))
            assertEquals("original-key", QuizRepository.aiApiKey)
            assertEquals("old-model", QuizRepository.aiModelName)
            assertEquals("https://old.example.com/v1", QuizRepository.aiApiBaseUrl)
            assertNotNull(QuizRepository.aiCredentialWarning)
        } finally { java.security.Security.addProvider(provider) }
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("original-key", QuizRepository.aiApiKey)
    }

    @Test fun restartKeepsKeyAssociatedWithItsSavedProvider() {
        QuizRepository.setAiInterfaceConfig(context, "OpenAI", "https://new.example.com/v1", "new-test-key", "new-model")
        preferences.edit().putString("ai_provider", "DeepSeek").putString("ai_api_base_url", "https://old.example.com/v1")
            .putString("ai_model_name", "old-model").commit()
        QuizRepository.resetForTesting()
        QuizRepository.init(context)
        assertEquals("new-test-key", QuizRepository.aiApiKey)
        assertEquals("OpenAI", QuizRepository.aiProvider)
        assertEquals("https://new.example.com/v1", QuizRepository.aiApiBaseUrl)
        assertEquals("new-model", QuizRepository.aiModelName)
    }
}
