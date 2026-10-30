package com.kuralpay.app.forwarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {
    @Test fun plainTenDigits() = assertEquals("+919876543210", PhoneNumber.normalize("9876543210"))
    @Test fun withCountryCode() = assertEquals("+919876543210", PhoneNumber.normalize("+91 98765 43210"))
    @Test fun withLeadingZero() = assertEquals("+919876543210", PhoneNumber.normalize("09876543210"))
    @Test fun withDashesAndBrackets() = assertEquals("+919876543210", PhoneNumber.normalize("(98765)-43210"))
    @Test fun tooShort() = assertNull(PhoneNumber.normalize("98765"))
    @Test fun tooLong() = assertNull(PhoneNumber.normalize("98765432101"))
    @Test fun cannotStartBelowSix() = assertNull(PhoneNumber.normalize("5876543210"))
    @Test fun empty() = assertNull(PhoneNumber.normalize(""))
    @Test fun lettersOnly() = assertNull(PhoneNumber.normalize("abcdefghij"))
    @Test fun prettyFormat() = assertEquals("+91 98765 43210", PhoneNumber.pretty("+919876543210"))
    @Test fun maskedFormat() = assertEquals("+91 ••••• 43210", PhoneNumber.masked("+919876543210"))
}
