package com.lifeos.app.domain

import com.lifeos.app.core.security.BackupCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test fun roundTripsAndRejectsWrongPassword() {
        val data = "SQLite format 3\u0000 some database bytes".toByteArray()
        val enc = BackupCrypto.encrypt(data, "correct horse".toCharArray())
        assertTrue(enc.size > data.size)
        assertEquals(String(data), String(BackupCrypto.decrypt(enc, "correct horse".toCharArray())))
        var failed = false
        try { BackupCrypto.decrypt(enc, "wrong password".toCharArray()) } catch (e: Exception) { failed = true }
        assertTrue(failed)
        var tampered = false
        val bad = enc.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        try { BackupCrypto.decrypt(bad, "correct horse".toCharArray()) } catch (e: Exception) { tampered = true }
        assertTrue(tampered)
    }
}
