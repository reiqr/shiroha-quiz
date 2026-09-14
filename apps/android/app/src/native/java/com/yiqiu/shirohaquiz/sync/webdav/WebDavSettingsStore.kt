package com.yiqiu.shirohaquiz.sync.webdav

import android.content.Context
import com.yiqiu.shirohaquiz.security.SecureSecretStore

/** Non-secret configuration is separate from QuizRepository preferences and exported content. */
class WebDavSettingsStore(context: Context, private val secrets: SecureSecretStore = SecureSecretStore(context)) {
    private val preferences = context.applicationContext.getSharedPreferences("shiroha_webdav_settings", Context.MODE_PRIVATE)
    private var sessionPassword: String? = null

    @Synchronized fun load(): WebDavSettings = WebDavSettings(
        baseUrl = preferences.getString("base_url", "").orEmpty(),
        username = preferences.getString("username", "").orEmpty(),
        remoteDirectory = preferences.getString("remote_directory", WebDavSettings.DEFAULT_REMOTE_DIRECTORY)
            ?: WebDavSettings.DEFAULT_REMOTE_DIRECTORY,
        rememberPassword = preferences.getBoolean("remember_password", false)
    )

    /** Null retains the password only when the origin AND account are unchanged. Empty clears it. */
    @Synchronized fun save(settings: WebDavSettings, password: String? = null): WebDavSettings {
        val clean = settings.validated()
        val previous = load()
        val sameAccount = runCatching {
            WebDavPaths.sameOrigin(WebDavPaths.baseUrl(previous.baseUrl), WebDavPaths.baseUrl(clean.baseUrl)) &&
                previous.username == clean.username
        }.getOrDefault(false)
        val effective = password ?: if (sameAccount) loadPassword() else null
        effective?.takeIf { it.isNotEmpty() }?.let { WebDavCredentials(clean.username, it) }
        if (clean.rememberPassword && !effective.isNullOrEmpty()) {
            secrets.put(PASSWORD_SECRET_KEY, effective)
            sessionPassword = null
        } else {
            secrets.remove(PASSWORD_SECRET_KEY)
            sessionPassword = effective?.takeIf { it.isNotEmpty() }
        }
        check(preferences.edit()
            .putString("base_url", clean.baseUrl)
            .putString("username", clean.username)
            .putString("remote_directory", clean.remoteDirectory)
            .putBoolean("remember_password", clean.rememberPassword)
            .commit()) { "无法保存 WebDAV 配置。"
        }
        return clean
    }

    @Synchronized fun loadPassword(): String? = if (load().rememberPassword) {
        secrets.get(PASSWORD_SECRET_KEY)
    } else sessionPassword

    @Synchronized fun credentials(): WebDavCredentials {
        val settings = load().validated()
        val password = loadPassword() ?: throw WebDavException("请填写 WebDAV 密码或应用专用授权码。")
        return WebDavCredentials(settings.username, password)
    }

    @Synchronized fun clear() {
        secrets.remove(PASSWORD_SECRET_KEY)
        sessionPassword = null
        check(preferences.edit().clear().commit()) { "无法清除 WebDAV 配置。"
        }
    }

    @Synchronized fun forgetSessionPassword() { sessionPassword = null }

    companion object {
        // Independent logical secret; the parent owns any SecureSecretStore Keystore-alias change.
        const val PASSWORD_SECRET_KEY = "webdav_password"
    }
}
