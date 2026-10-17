package com.example

import com.example.security.AuthSecurityUtil
import com.example.security.PasswordStrengthLevel
import org.junit.Assert.*
import org.junit.Test

class AuthSecurityTest {

    @Test
    fun testPasswordHashingAndVerification() {
        val password = "SuperSecretPassword@2026"
        val salt = AuthSecurityUtil.generateSalt()

        val hash1 = AuthSecurityUtil.hashPassword(password, salt)
        val hash2 = AuthSecurityUtil.hashPassword(password, salt)

        // Deterministic with same salt
        assertEquals(hash1, hash2)

        // Verify matches
        assertTrue(AuthSecurityUtil.verifyPassword(password, salt, hash1))

        // Wrong password fails
        assertFalse(AuthSecurityUtil.verifyPassword("WrongPassword123", salt, hash1))
    }

    @Test
    fun testOtpGeneration() {
        val otp1 = AuthSecurityUtil.generate6DigitOtp()
        assertEquals(6, otp1.length)
        assertTrue(otp1.all { it.isDigit() })

        val otp2 = AuthSecurityUtil.generate6DigitOtp()
        assertNotEquals(otp1, otp2)
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val weak = AuthSecurityUtil.evaluatePasswordStrength("12345")
        assertEquals(PasswordStrengthLevel.WEAK, weak.level)

        val strong = AuthSecurityUtil.evaluatePasswordStrength("Misha#QuantumSecure_2026!")
        assertTrue(strong.level == PasswordStrengthLevel.GOOD || strong.level == PasswordStrengthLevel.STRONG)
        assertTrue(strong.score >= 3)
    }
}
