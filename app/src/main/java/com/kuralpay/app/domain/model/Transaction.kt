package com.kuralpay.app.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val sender: String,
    val sourceApp: PaymentSource,
    val timestamp: Long,
    val rawNotification: String
)
