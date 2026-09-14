package com.yiqiu.shirohaquiz.security

import android.content.SharedPreferences
import org.json.JSONObject

data class AiKeyConfiguration(val provider: String, val apiBaseUrl: String, val modelName: String)

class LoadedAiKey(val apiKey: String, val warning: String? = null, val configuration: AiKeyConfiguration? = null) {
    override fun toString(): String = "LoadedAiKey(redacted)"
}

data class AiKeyWriteResult(val success: Boolean, val warning: String? = null)

/** Only this boundary migrates/writes credentials; ordinary question persistence never touches them. */
class AiApiKeyStore(private val preferences: SharedPreferences, private val secrets: LocalSecretStore) {
    fun load(): LoadedAiKey {
        val legacy = preferences.getString(LEGACY_KEY, "").orEmpty()
        return try {
            if (secrets.contains(SECRET_KEY)) {
                val secured = secrets.get(SECRET_KEY)
                    ?: return LoadedAiKey(legacy, "AI Key 加密数据读取失败；原明文未删除，请到 AI 设置重新保存密钥。")
                val restored = decode(secured)
                LoadedAiKey(restored.apiKey, cleanupWarning(), restored.configuration)
            } else if (legacy.isBlank()) {
                LoadedAiKey("", cleanupWarning())
            } else {
                val payload = encode(legacy, null)
                secrets.put(SECRET_KEY, payload)
                check(secrets.get(SECRET_KEY) == payload) { "Credential verification failed" }
                LoadedAiKey(legacy, cleanupWarning())
            }
        } catch (_: Exception) {
            LoadedAiKey(legacy, "AI Key 安全迁移未完成；原密钥已保留，请到 AI 设置重新保存。")
        }
    }

    fun save(value: String, configuration: AiKeyConfiguration? = null): AiKeyWriteResult {
        val key = value.trim()
        if (key.isEmpty()) return clear()
        var previous: String? = null
        var written = false
        return try {
            previous = secrets.get(SECRET_KEY)
            val payload = encode(key, configuration)
            secrets.put(SECRET_KEY, payload)
            written = true
            check(secrets.get(SECRET_KEY) == payload) { "Credential verification failed" }
            AiKeyWriteResult(true, cleanupWarning())
        } catch (_: Exception) {
            if (written) previous?.let { old -> runCatching { secrets.put(SECRET_KEY, old) } }
            AiKeyWriteResult(false, "AI Key 安全保存失败，原接口配置未替换，请稍后重试。")
        }
    }

    fun clear(): AiKeyWriteResult = try {
        // Remove legacy storage first; a failed cleanup must not remove the surviving secure copy.
        check(removeLegacy()) { "Legacy cleanup failed" }
        secrets.remove(SECRET_KEY)
        check(!secrets.contains(SECRET_KEY)) { "Credential deletion failed" }
        AiKeyWriteResult(true)
    } catch (_: Exception) {
        AiKeyWriteResult(false, "AI Key 清除未完成，原接口配置未替换，请稍后重试。")
    }

    private fun cleanupWarning(): String? = if (removeLegacy()) null
        else "AI Key 已加密保存，但旧明文清理未完成，请重新保存配置后重试。"

    private fun removeLegacy(): Boolean = runCatching {
        !preferences.contains(LEGACY_KEY) || preferences.edit().remove(LEGACY_KEY).commit()
    }.getOrDefault(false)

    private fun decode(value: String): LoadedAiKey {
        if (!value.startsWith(CONFIG_PREFIX)) return LoadedAiKey(value)
        val json = JSONObject(value.removePrefix(CONFIG_PREFIX))
        val configuration = if (json.has("provider")) AiKeyConfiguration(
            json.getString("provider"), json.getString("apiBaseUrl"), json.getString("modelName")) else null
        return LoadedAiKey(json.getString("apiKey"), configuration = configuration)
    }

    private fun encode(key: String, configuration: AiKeyConfiguration?): String {
        val json = JSONObject().put("apiKey", key)
        configuration?.let { json.put("provider", it.provider).put("apiBaseUrl", it.apiBaseUrl).put("modelName", it.modelName) }
        return CONFIG_PREFIX + json.toString()
    }

    companion object {
        private const val CONFIG_PREFIX = "shiroha-ai-config-v1:"
        const val LEGACY_KEY = "ai_api_key"
        const val SECRET_KEY = "ai_api_key"
    }
}
