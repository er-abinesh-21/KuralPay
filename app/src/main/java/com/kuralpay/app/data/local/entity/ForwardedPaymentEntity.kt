package com.kuralpay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kuralpay.app.forwarding.ForwardedPayment

/**
 * A payment a linked KuralPay user forwarded to this phone. Kept apart from [TransactionEntity] so it
 * is listed, but never counted in this phone's own collection, analytics or cloud backup.
 */
@Entity(tableName = "forwarded_payments")
data class ForwardedPaymentEntity(
    /** The Firestore inbox document id, so a payment is stored once even if delivered twice. */
    @PrimaryKey val id: String,
    val fromName: String,
    val amount: Double,
    val payer: String,
    val timestamp: Long
) {
    fun toDomain() = ForwardedPayment(id = id, fromName = fromName, amount = amount, payer = payer, timestamp = timestamp)

    companion object {
        fun fromDomain(p: ForwardedPayment) =
            ForwardedPaymentEntity(id = p.id, fromName = p.fromName, amount = p.amount, payer = p.payer, timestamp = p.timestamp)
    }
}
