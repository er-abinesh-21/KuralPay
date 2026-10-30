package com.kuralpay.app.paymentdetection.parser

import com.kuralpay.app.domain.model.ParsedPayment
import com.kuralpay.app.domain.model.PaymentSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentParserEngine @Inject constructor() {

    private val paymentApps = mapOf(
        "com.google.android.apps.nbu.paisa.user" to PaymentSource.GOOGLE_PAY,
        "com.phonepe.app" to PaymentSource.PHONEPE,
        "net.one97.paytm" to PaymentSource.PAYTM,
        "com.amazon.wallet" to PaymentSource.AMAZON_PAY,
        "in.amazon.mShop.android.shopping" to PaymentSource.AMAZON_PAY,
        "in.org.npci.upiapp" to PaymentSource.BHIM
    )

    /** Bank credit alerts arrive as SMS through these apps. */
    private val smsApps = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.messaging"
    )

    /** People chat about money; a friend's message must never be announced as a payment. */
    private val chatApps = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "com.instagram.android",
        "com.facebook.orca", "com.facebook.katana", "com.snapchat.android", "com.discord",
        "com.google.android.gm", "com.microsoft.office.outlook", "com.samsung.android.email.provider"
    )

    fun parse(packageName: String, title: String, text: String): ParsedPayment? {
        if (packageName in chatApps) return null
        val combined = listOf(title, text).filter { it.isNotBlank() }.joinToString(". ")
        val result = PaymentMessageParser.parse(combined) ?: return null

        val source = paymentApps[packageName]
            ?: if (packageName in smsApps || BANK_WORDS.containsMatchIn(combined)) PaymentSource.BANK_TRANSFER
            else PaymentSource.UNKNOWN

        return ParsedPayment(
            amount = result.amount,
            sender = result.sender,
            source = source,
            rawText = combined
        )
    }

    private companion object {
        val BANK_WORDS = Regex("""\ba/c\b|\bacct\b|\baccount\b""", RegexOption.IGNORE_CASE)
    }
}
