package com.yiqiu.shirohaquiz.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small device-local secret store. Ciphertext lives under noBackupFilesDir and the AES key stays
 * in Android Keystore, so API tokens are excluded from Shiroha backups and Android Auto Backup.
 */
class SecureSecretStore(context: Context) {
    private val appContext = context.applicationContext
    private val secretFile = File(appContext.noBackupFilesDir, SECRET_FILE_NAME)

    @Synchronized
    fun put(key: String, value: String) {
        require(key.isNotBlank()) { "Secret key must not be blank." }
        if (value.isBlank()) {
            remove(key)
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val root = readRoot()
        root.put(
            key,
            JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        )
        writeRoot(root)
    }

    @Synchronized
    fun get(key: String): String? {
        val entry = readRoot().optJSONObject(key) ?: return null
        return runCatching {
            val iv = Base64.decode(entry.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(entry.getString("value"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    fun remove(key: String) {
        val root = readRoot()
        if (root.has(key)) {
            root.remove(key)
            writeRoot(root)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun readRoot(): JSONObject {
        if (!secretFile.isFile) return JSONObject()
        return runCatching { JSONObject(secretFile.readText(Charsets.UTF_8)) }.getOrDefault(JSONObject())
    }

    private fun writeRoot(root: JSONObject) {
        secretFile.parentFile?.mkdirs()
        val tempFile = File(secretFile.parentFile, "${secretFile.name}.tmp")
        tempFile.writeText(root.toString(), Charsets.UTF_8)
        if (!tempFile.renameTo(secretFile)) {
            secretFile.writeText(root.toString(), Charsets.UTF_8)
            tempFile.delete()
        }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "shiroha_secure_secrets_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SECRET_FILE_NAME = "shiroha_secure_secrets.json"
    }
}
