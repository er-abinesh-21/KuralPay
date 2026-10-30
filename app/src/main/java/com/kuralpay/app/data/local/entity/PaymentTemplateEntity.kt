package com.kuralpay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_templates")
data class PaymentTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val templateRegex: String,
    val fields: String,
    val confidenceScore: Float = 0.0f,
    val createdAt: Long = System.currentTimeMillis()
)
