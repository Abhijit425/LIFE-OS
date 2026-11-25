package com.lifeos.app.core

import com.lifeos.app.core.format.IndianFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class IndianFormatTest {

    @Test fun groupsSmallNumbersUnchanged() {
        assertEquals("0", IndianFormat.groupDigits("0"))
        assertEquals("999", IndianFormat.groupDigits("999"))
    }

    @Test fun groupsInLakhsAndCrores() {
        assertEquals("1,000", IndianFormat.groupDigits("1000"))
        assertEquals("50,000", IndianFormat.groupDigits("50000"))
        assertEquals("1,00,000", IndianFormat.groupDigits("100000"))
        assertEquals("12,34,567", IndianFormat.groupDigits("1234567"))
        assertEquals("1,23,45,67,890", IndianFormat.groupDigits("1234567890"))
    }

    @Test fun formatsRupeesWithPaise() {
        assertEquals("₹0.00", IndianFormat.rupees(0))
        assertEquals("₹0.05", IndianFormat.rupees(5))
        assertEquals("₹50,000.00", IndianFormat.rupees(50_000_00))
        assertEquals("₹1,00,000.50", IndianFormat.rupees(1_00_000_50))
        assertEquals("-₹1,234.56", IndianFormat.rupees(-1_234_56))
    }

    @Test fun roundsWholeRupeesHalfUp() {
        assertEquals("₹1,235", IndianFormat.rupees(1_234_50, showPaise = false))
        assertEquals("₹1,234", IndianFormat.rupees(1_234_49, showPaise = false))
    }

    @Test fun compactUsesIndianUnits() {
        assertEquals("₹950", IndianFormat.rupeesCompact(950_00))
        assertEquals("₹12.5K", IndianFormat.rupeesCompact(12_500_00))
        assertEquals("₹1L", IndianFormat.rupeesCompact(1_00_000_00))
        assertEquals("₹3.2L", IndianFormat.rupeesCompact(3_20_000_00))
        assertEquals("₹1.4Cr", IndianFormat.rupeesCompact(1_40_00_000_00))
        assertEquals("-₹2K", IndianFormat.rupeesCompact(-2_000_00))
    }
}
