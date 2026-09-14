package com.yiqiu.shirohaquiz.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.io.File
import android.util.AtomicFile
import java.security.MessageDigest
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small device-local secret store. Ciphertext lives under noBackupFilesDir and the AES key stays
 * in Android Keystore, so API tokens are excluded from Shiroha backups and Android Auto Backup.
 */
interface LocalSecretStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun contains(key: String): Boolean
    fun remove(key: String)
}

class SecureSecretStore(context: Context) : LocalSecretStore {
    private val appContext = context.applicationContext
    private val secretFile = File(appContext.noBackupFilesDir, SECRET_FILE_NAME)

    @Synchronized
    override fun put(key: String, value: String): Unit = synchronized(STORE_LOCK) {
        require(key.isNotBlank()) { "Secret key must not be blank." }
        if (value.isBlank()) {
            remove(key)
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val alias = KEY_ALIAS + "_" + MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it.toInt() and 255) }
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(alias))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val root = readRoot()
        root.put(
            key,
            JSONObject()
                .put("alias", alias)
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        )
        writeRoot(root)
    }

    @Synchronized
    override fun get(key: String): String? = synchronized(STORE_LOCK) {
        val root = runCatching { readRoot() }.getOrNull() ?: return@synchronized null
        val entry = root.optJSONObject(key) ?: return@synchronized null
        runCatching {
            val iv = Base64.decode(entry.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(entry.getString("value"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(entry.optString("alias", KEY_ALIAS)), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    override fun contains(key: String): Boolean = synchronized(STORE_LOCK) { readRoot().has(key) }

    @Synchronized
    override fun remove(key: String): Unit = synchronized(STORE_LOCK) {
        val root = readRoot()
        if (root.has(key)) {
            root.remove(key)
            writeRoot(root)
        }
    }

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
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
        if (!secretFile.isFile && !File(secretFile.path + ".bak").isFile) return JSONObject()
        return JSONObject(AtomicFile(secretFile).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    private fun writeRoot(root: JSONObject) {
        secretFile.parentFile?.mkdirs()
        val atomic = AtomicFile(secretFile)
        val output = atomic.startWrite()
        try {
            output.write(root.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (error: Exception) { atomic.failWrite(output); throw error }
    }

    companion object {
        private val STORE_LOCK = Any()
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "shiroha_secure_secrets_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SECRET_FILE_NAME = "shiroha_secure_secrets.json"
    }
}
