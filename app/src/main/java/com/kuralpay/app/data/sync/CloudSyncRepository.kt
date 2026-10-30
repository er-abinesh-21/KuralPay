package com.kuralpay.app.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kuralpay.app.account.AuthRepository
import com.kuralpay.app.data.local.dao.TransactionDao
import com.kuralpay.app.data.local.entity.TransactionEntity
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.domain.model.Transaction
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The person's payment history and settings backup in Firestore, under users/{uid}.
 * Raw notification text (which can hold account fragments) stays on the phone.
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val auth: AuthRepository,
    private val transactionDao: TransactionDao
) {
    private val db get() = FirebaseFirestore.getInstance()

    private fun transactions(uid: String) = db.collection("users").document(uid).collection("transactions")
    private fun settingsDoc(uid: String) = db.collection("users").document(uid).collection("data").document("settings")

    /** Same payment always gets the same id, so uploading twice never duplicates it. */
    private fun docId(timestamp: Long, amount: Double) = "${timestamp}_${Math.round(amount * 100)}"

    /** Fire and forget: Firestore queues the write while offline and sends it when the network returns. */
    fun uploadTransaction(tx: Transaction) {
        val user = auth.currentUser ?: return
        transactions(user.uid).document(docId(tx.timestamp, tx.amount)).set(
            mapOf(
                "amount" to tx.amount,
                "sender" to tx.sender,
                "sourceApp" to tx.sourceApp.name,
                "timestamp" to tx.timestamp
            )
        ).addOnFailureListener { Log.w(TAG, "Upload failed", it) }
    }

    /** Pushes payments this phone has and the cloud lacks, then pulls the ones this phone lacks. */
    suspend fun restoreAndBackfill(uid: String) {
        val col = transactions(uid)
        val remote = col.get().await().documents
        val remoteIds = remote.map { it.id }.toHashSet()

        val local = transactionDao.getAllOnce()
        val missingInCloud = local.filter { docId(it.timestamp, it.amount) !in remoteIds }
        missingInCloud.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { t ->
                batch.set(
                    col.document(docId(t.timestamp, t.amount)),
                    mapOf("amount" to t.amount, "sender" to t.sender, "sourceApp" to t.sourceApp, "timestamp" to t.timestamp)
                )
            }
            batch.commit().await()
        }

        remote.forEach { d ->
            val amount = d.getDouble("amount") ?: return@forEach
            val timestamp = d.getLong("timestamp") ?: return@forEach
            if (transactionDao.countMatching(timestamp, amount) == 0) {
                transactionDao.insert(
                    TransactionEntity.fromDomain(
                        Transaction(
                            amount = amount,
                            sender = d.getString("sender").orEmpty(),
                            sourceApp = runCatching { PaymentSource.valueOf(d.getString("sourceApp").orEmpty()) }.getOrDefault(PaymentSource.UNKNOWN),
                            timestamp = timestamp,
                            rawNotification = ""
                        )
                    )
                )
            }
        }
        Log.d(TAG, "Synced: ${missingInCloud.size} uploaded, ${remote.size} in cloud")
    }

    /** Deletes cloud payments older than [cutoff]; returns how many were removed. */
    suspend fun deleteOlderThan(cutoff: Long): Int {
        val user = auth.currentUser ?: return 0
        var removed = 0
        while (true) {
            val old = transactions(user.uid).whereLessThan("timestamp", cutoff).limit(400).get().await().documents
            if (old.isEmpty()) break
            val batch = db.batch()
            old.forEach { batch.delete(it.reference) }
            batch.commit().await()
            removed += old.size
        }
        return removed
    }

    fun uploadSettings(values: Map<String, Any>) {
        val user = auth.currentUser ?: return
        settingsDoc(user.uid).set(values, SetOptions.merge()).addOnFailureListener { Log.w(TAG, "Settings upload failed", it) }
    }

    /** The backed-up settings, or null when this account has none yet. */
    suspend fun fetchSettings(uid: String): Map<String, Any?>? {
        val snap = settingsDoc(uid).get().await()
        return if (snap.exists()) snap.data else null
    }

    private companion object {
        const val TAG = "CloudSync"
    }
}
