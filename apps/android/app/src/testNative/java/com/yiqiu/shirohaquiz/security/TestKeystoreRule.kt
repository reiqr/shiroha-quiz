package com.yiqiu.shirohaquiz.security

import android.security.keystore.KeyGenParameterSpec
import org.junit.rules.ExternalResource
import java.io.InputStream
import java.io.OutputStream
import java.security.*
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Only the Android Keystore service is replaced; production AES-GCM and file IO still execute. */
class TestKeystoreRule : ExternalResource() {
    private var previous: Provider? = null
    override fun before() {
        previous = Security.getProvider("AndroidKeyStore")
        Security.removeProvider("AndroidKeyStore")
        TestKeystoreKeys.keys.clear()
        Security.addProvider(TestKeystoreProvider())
    }
    override fun after() {
        Security.removeProvider("AndroidKeyStore")
        previous?.let(Security::addProvider)
        TestKeystoreKeys.keys.clear()
    }
}

internal object TestKeystoreKeys { val keys = linkedMapOf<String, SecretKey>() }

class TestKeystoreProvider : Provider("AndroidKeyStore", 1.0, "Isolated test keystore") {
    init {
        put("KeyStore.AndroidKeyStore", TestKeystoreSpi::class.java.name)
        put("KeyGenerator.AES", TestAesKeyGeneratorSpi::class.java.name)
    }
}

class TestAesKeyGeneratorSpi : KeyGeneratorSpi() {
    private var alias: String? = null
    override fun engineInit(random: SecureRandom?) { }
    override fun engineInit(keysize: Int, random: SecureRandom?) { }
    override fun engineInit(params: AlgorithmParameterSpec, random: SecureRandom?) {
        alias = (params as KeyGenParameterSpec).keystoreAlias
    }
    override fun engineGenerateKey(): SecretKey {
        val name = checkNotNull(alias)
        return SecretKeySpec(ByteArray(32).also { SecureRandom().nextBytes(it) }, "AES")
            .also { TestKeystoreKeys.keys[name] = it }
    }
}

class TestKeystoreSpi : KeyStoreSpi() {
    override fun engineGetKey(alias: String, password: CharArray?): Key? = TestKeystoreKeys.keys[alias]
    override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null
    override fun engineGetCertificate(alias: String): Certificate? = null
    override fun engineGetCreationDate(alias: String): Date = Date(0)
    override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<out Certificate>?) {
        TestKeystoreKeys.keys[alias] = key as SecretKey
    }
    override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) { error("Unsupported test entry") }
    override fun engineSetCertificateEntry(alias: String, cert: Certificate) { error("Unsupported test entry") }
    override fun engineDeleteEntry(alias: String) { TestKeystoreKeys.keys.remove(alias) }
    override fun engineAliases() = Collections.enumeration(TestKeystoreKeys.keys.keys)
    override fun engineContainsAlias(alias: String) = alias in TestKeystoreKeys.keys
    override fun engineSize() = TestKeystoreKeys.keys.size
    override fun engineIsKeyEntry(alias: String) = alias in TestKeystoreKeys.keys
    override fun engineIsCertificateEntry(alias: String) = false
    override fun engineGetCertificateAlias(cert: Certificate): String? = null
    override fun engineStore(stream: OutputStream?, password: CharArray?) { }
    override fun engineLoad(stream: InputStream?, password: CharArray?) { }
}
