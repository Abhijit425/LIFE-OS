package com.lifeos.app.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Document encryption with a hardware-backed (where available) Android Keystore key.
 * The key never leaves the Keystore. File format: [12-byte IV][AES-256-GCM ciphertext+tag].
 */
object VaultCrypto {
    private const val ALIAS = "lifeos_vault_key_v1"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    fun encrypt(input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        output.write(cipher.iv)
        CipherOutputStream(output, cipher).use { input.copyTo(it) }
    }

    fun decryptStream(input: InputStream): InputStream {
        val iv = ByteArray(12)
        var read = 0
        while (read < 12) {
            val n = input.read(iv, read, 12 - read)
            if (n < 0) error("Corrupt encrypted file")
            read += n
        }
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return CipherInputStream(input, cipher)
    }
}

/**
 * Password-based encryption for backups, so a backup can be restored on a new device.
 * PBKDF2-HMAC-SHA256 (210k iterations) → AES-256-GCM.
 * Format: "LIFEOSBK1" | 16-byte salt | 12-byte IV | ciphertext+tag.
 */
object BackupCrypto {
    val MAGIC = "LIFEOSBK1".toByteArray(Charsets.US_ASCII)
    private const val ITERATIONS = 210_000

    private fun derive(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also(rnd::nextBytes)
        val iv = ByteArray(12).also(rnd::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv)) }
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    /** Throws if the password is wrong or the file was altered (GCM authentication). */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        require(data.size > MAGIC.size + 28 && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "Not a LIFE OS backup file" }
        var p = MAGIC.size
        val salt = data.copyOfRange(p, p + 16); p += 16
        val iv = data.copyOfRange(p, p + 12); p += 12
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv)) }
        return cipher.doFinal(data, p, data.size - p)
    }
}
