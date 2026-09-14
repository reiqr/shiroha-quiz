package com.yiqiu.shirohaquiz.security

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiApiKeyStoreTest {
    private val preferences get() = RuntimeEnvironment.getApplication().getSharedPreferences("key-store-test", Context.MODE_PRIVATE)

    @Test fun secureValueWinsOverStalePlaintextAndCleansIt() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "stale-key").commit()
        val secrets = FakeSecrets().apply { values[AiApiKeyStore.SECRET_KEY] = "secure-key" }
        val loaded = AiApiKeyStore(preferences, secrets).load()
        assertEquals("secure-key", loaded.apiKey)
        assertFalse(preferences.contains(AiApiKeyStore.LEGACY_KEY))
        assertEquals(0, secrets.writes)
    }

    @Test fun failedMigrationKeepsOriginalPlaintextAndWarns() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "legacy-key").commit()
        val secrets = FakeSecrets().apply { failWrite = true }
        val loaded = AiApiKeyStore(preferences, secrets).load()
        assertEquals("legacy-key", loaded.apiKey)
        assertNotNull(loaded.warning)
        assertEquals("legacy-key", preferences.getString(AiApiKeyStore.LEGACY_KEY, null))
    }

    @Test fun failedReadbackCannotDeleteLegacyValue() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "legacy-key").commit()
        val secrets = FakeSecrets().apply { unreadableValue = "legacy-key" }
        val loaded = AiApiKeyStore(preferences, secrets).load()
        assertEquals("legacy-key", loaded.apiKey)
        assertNotNull(loaded.warning)
        assertTrue(preferences.contains(AiApiKeyStore.LEGACY_KEY))
    }

    @Test fun unreadableSecureEntryIsNeverAutomaticallyOverwritten() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "legacy-key").commit()
        val secrets = FakeSecrets().apply { values[AiApiKeyStore.SECRET_KEY] = "broken"; unreadableValue = "broken" }
        val loaded = AiApiKeyStore(preferences, secrets).load()
        assertEquals("legacy-key", loaded.apiKey)
        assertNotNull(loaded.warning)
        assertEquals(0, secrets.writes)
        assertTrue(preferences.contains(AiApiKeyStore.LEGACY_KEY))
    }

    @Test fun failedReplacementRestoresPreviouslyReadableSecret() {
        val secrets = FakeSecrets().apply { values[AiApiKeyStore.SECRET_KEY] = "original"; unreadableValue = "new" }
        val result = AiApiKeyStore(preferences, secrets).save("new")
        assertFalse(result.success)
        assertEquals("original", secrets.get(AiApiKeyStore.SECRET_KEY))
        assertNotNull(result.warning)
    }

    @Test fun failedLegacyCleanupDoesNotLoseTheSecureCopy() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "legacy").commit()
        val secrets = FakeSecrets()
        val loaded = AiApiKeyStore(commitFailingPreferences(), secrets).load()
        assertEquals("legacy", loaded.apiKey)
        assertNotNull(loaded.warning)
        assertEquals("legacy", AiApiKeyStore(commitFailingPreferences(), secrets).load().apiKey)
        assertTrue(preferences.contains(AiApiKeyStore.LEGACY_KEY))
    }

    @Test fun failedClearLeavesSurvivingSecretUntouched() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "legacy").commit()
        val secrets = FakeSecrets().apply { values[AiApiKeyStore.SECRET_KEY] = "secure" }
        assertFalse(AiApiKeyStore(commitFailingPreferences(), secrets).clear().success)
        assertEquals("secure", secrets.get(AiApiKeyStore.SECRET_KEY))
    }

    @Test fun loadResultsDoNotExposeKeyInDiagnostics() {
        preferences.edit().putString(AiApiKeyStore.LEGACY_KEY, "sensitive-test-value").commit()
        val loaded = AiApiKeyStore(preferences, FakeSecrets()).load()
        assertFalse(loaded.toString().contains("sensitive-test-value"))
    }

    private fun commitFailingPreferences(): SharedPreferences = object : SharedPreferences by preferences {
        override fun edit(): SharedPreferences.Editor {
            val real = preferences.edit()
            return object : SharedPreferences.Editor by real {
                override fun remove(key: String): SharedPreferences.Editor { real.remove(key); return this }
                override fun commit() = false
            }
        }
    }

    private class FakeSecrets : LocalSecretStore {
        val values = mutableMapOf<String, String>()
        var writes = 0
        var failWrite = false
        var unreadableValue: String? = null
        override fun put(key: String, value: String) { if (failWrite) error("Test write failure"); writes++; values[key] = value }
        override fun get(key: String) = values[key]?.takeUnless {
            it == unreadableValue || unreadableValue != null && it.contains("\"apiKey\":\"$unreadableValue\"")
        }
        override fun contains(key: String) = values.containsKey(key)
        override fun remove(key: String) { values.remove(key) }
    }
}
