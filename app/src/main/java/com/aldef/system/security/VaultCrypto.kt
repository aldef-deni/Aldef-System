package com.aldef.system.security

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enkripsi brankas: AES-256-GCM dengan kunci turunan PBKDF2 dari PIN.
 *
 * Kunci tidak pernah ditulis ke disk. Yang tersimpan hanya salt; kunci
 * diturunkan ulang setiap kali brankas dibuka dan hanya hidup di memori
 * selama sesi berlangsung.
 */
object VaultCrypto {

    private const val KEY_ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    const val IV_BYTES = 12
    const val SALT_BYTES = 16

    fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }

    fun deriveKey(pin: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val secret = SecretKeyFactory.getInstance(KEY_ALGO).generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }

    fun encrypt(key: SecretKey, plain: ByteArray): ByteArray {
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(key: SecretKey, payload: ByteArray): ByteArray {
        require(payload.size > IV_BYTES) { "Payload terlalu pendek" }
        val iv = payload.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(payload, IV_BYTES, payload.size - IV_BYTES)
    }

    /**
     * Menyalin [input] ke [output] sambil mengenkripsi, tanpa memuat seluruh
     * berkas ke memori. IV ditulis lebih dulu sebagai header.
     */
    fun encryptStream(key: SecretKey, input: InputStream, output: OutputStream): Long {
        val iv = randomBytes(IV_BYTES)
        output.write(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            cipher.update(buffer, 0, read)?.let { output.write(it) }
        }
        output.write(cipher.doFinal())
        output.flush()
        return total
    }

    /** Kebalikan [encryptStream]; IV dibaca dari awal [input]. */
    fun decryptStream(key: SecretKey, input: InputStream, output: OutputStream) {
        val iv = ByteArray(IV_BYTES)
        var offset = 0
        while (offset < IV_BYTES) {
            val read = input.read(iv, offset, IV_BYTES - offset)
            if (read <= 0) error("Berkas brankas rusak: header tidak lengkap")
            offset += read
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        CipherInputStream(input, cipher).use { it.copyTo(output, 64 * 1024) }
        output.flush()
    }
}
