package com.kuralpay.app.domain.model

data class Announcement(
    val id: Long = 0,
    val amount: Double,
    val sender: String,
    val sourceApp: PaymentSource,
    val timestamp: Long,
    val isReplayed: Boolean = false
)
