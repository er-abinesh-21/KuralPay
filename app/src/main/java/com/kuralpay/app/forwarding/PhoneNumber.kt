package com.kuralpay.app.forwarding

/** Indian mobile numbers, stored as +91XXXXXXXXXX so the same person always has the same key. */
object PhoneNumber {

    /** Returns +91XXXXXXXXXX, or null when the input is not a valid 10-digit Indian mobile number. */
    fun normalize(raw: String): String? {
        var digits = raw.filter { it.isDigit() }
        if (digits.length == 12 && digits.startsWith("91")) digits = digits.drop(2)
        else if (digits.length == 11 && digits.startsWith("0")) digits = digits.drop(1)
        return if (digits.length == 10 && digits.first() in '6'..'9') "+91$digits" else null
    }

    /** +919876543210 -> +91 98765 43210 */
    fun pretty(e164: String): String =
        if (e164.length == 13 && e164.startsWith("+91")) "+91 ${e164.substring(3, 8)} ${e164.substring(8)}" else e164

    /** +919876543210 -> +91 ••••• 43210, for lists where the full number isn't needed. */
    fun masked(e164: String): String =
        if (e164.length == 13 && e164.startsWith("+91")) "+91 ••••• ${e164.substring(8)}" else e164
}
