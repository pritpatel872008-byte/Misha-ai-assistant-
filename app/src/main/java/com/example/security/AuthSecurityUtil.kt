package com.example.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.regex.Pattern
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PasswordStrengthLevel {
    WEAK,
    FAIR,
    GOOD,
    STRONG
}

data class PasswordStrengthResult(
    val level: PasswordStrengthLevel,
    val score: Int, // 0 to 4
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
    val feedbackMessage: String
)

object AuthSecurityUtil {

    private val secureRandom = SecureRandom()
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )

    fun generateSalt(byteLength: Int = 16): String {
        val saltBytes = ByteArray(byteLength)
        secureRandom.nextBytes(saltBytes)
        return bytesToHex(saltBytes)
    }

    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = hexToBytes(saltHex)
        return try {
            val spec: KeySpec = PBEKeySpec(password.toCharArray(), saltBytes, 10000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            bytesToHex(hash)
        } catch (e: NoSuchAlgorithmException) {
            // Fallback to SHA-256 with iterated salt
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(saltBytes)
            var current = digest.digest(password.toByteArray(Charsets.UTF_8))
            for (i in 0 until 1000) {
                digest.reset()
                digest.update(saltBytes)
                current = digest.digest(current)
            }
            bytesToHex(current)
        }
    }

    fun verifyPassword(password: String, saltHex: String, expectedHash: String): Boolean {
        val calculatedHash = hashPassword(password, saltHex)
        return slowEquals(calculatedHash, expectedHash)
    }

    fun generate6DigitOtp(): String {
        val otp = secureRandom.nextInt(900000) + 100000
        return otp.toString()
    }

    fun hashSecurityAnswer(answer: String, saltHex: String): String {
        val normalized = answer.trim().lowercase()
        return hashPassword(normalized, saltHex)
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotBlank() && EMAIL_PATTERN.matcher(trimmed).matches()
    }

    fun evaluatePasswordStrength(password: String): PasswordStrengthResult {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        var score = 0
        if (hasMinLength) score++
        if (hasUppercase && hasLowercase) score++
        if (hasDigit) score++
        if (hasSpecialChar) score++

        val (level, message) = when (score) {
            0, 1 -> PasswordStrengthLevel.WEAK to "Weak: Use at least 8 characters with letters, numbers & symbols."
            2 -> PasswordStrengthLevel.FAIR to "Fair: Add uppercase letters or special characters."
            3 -> PasswordStrengthLevel.GOOD to "Good: Add symbols like !@#$% for maximum protection."
            else -> PasswordStrengthLevel.STRONG to "Strong: Excellent passphrase entropy."
        }

        return PasswordStrengthResult(
            level = level,
            score = score,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar,
            feedbackMessage = message
        )
    }

    /**
     * Constant-time string comparison to mitigate timing attacks
     */
    private fun slowEquals(a: String, b: String): Boolean {
        var diff = a.length xor b.length
        val minLen = minOf(a.length, b.length)
        for (i in 0 until minLen) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
