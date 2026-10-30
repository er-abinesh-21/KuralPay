package com.kuralpay.app.domain.model

data class ParsedPayment(
    val amount: Double,
    val sender: String,
    val source: PaymentSource,
    val rawText: String,
    /** Row in the announcement queue, so the speaker can mark it heard. 0 until saved. */
    val announcementId: Long = 0,
    /** Set when another KuralPay user forwarded this payment; such payments are announced but never saved or re-forwarded. */
    val forwardedFrom: String? = null
)
