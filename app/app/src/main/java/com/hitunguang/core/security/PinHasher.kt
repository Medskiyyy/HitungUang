package com.hitunguang.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val PREFIX = "pbkdf2"
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private val random = SecureRandom()

    fun hash(input: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val key = derive(input, salt, iterations, KEY_BITS)
        val encoder = Base64.getEncoder()
        return "$PREFIX$$iterations$${encoder.encodeToString(salt)}$${encoder.encodeToString(key)}"
    }

    fun verify(input: String, stored: String): Boolean {
        if (isLegacy(stored)) {
            return MessageDigest.isEqual(
                legacySha256(input).toByteArray(),
                stored.lowercase().toByteArray()
            )
        }
        val parts = stored.split("$")
        if (parts.size != 4) return false
        val iterations = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return false
        return try {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(parts[2])
            val expected = decoder.decode(parts[3])
            if (expected.isEmpty()) return false
            MessageDigest.isEqual(derive(input, salt, iterations, expected.size * 8), expected)
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun isLegacy(stored: String): Boolean = !stored.startsWith("$PREFIX$")

    private fun derive(input: String, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
        val spec = PBEKeySpec(input.toCharArray(), salt, iterations, keyBits)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacySha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
