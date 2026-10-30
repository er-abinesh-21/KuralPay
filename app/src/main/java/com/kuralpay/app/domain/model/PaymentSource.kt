package com.kuralpay.app.domain.model

enum class PaymentSource(val displayName: String) {
    GOOGLE_PAY("Google Pay"),
    PHONEPE("PhonePe"),
    PAYTM("Paytm"),
    AMAZON_PAY("Amazon Pay"),
    BHIM("BHIM"),
    BANK_TRANSFER("Bank Transfer"),
    MERCHANT_APP("Merchant App"),
    UNKNOWN("Unknown")
}
