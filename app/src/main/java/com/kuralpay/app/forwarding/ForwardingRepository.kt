package com.kuralpay.app.forwarding

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kuralpay.app.account.AuthException
import com.kuralpay.app.account.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class LinkStatus { PENDING, ACCEPTED, DECLINED }

/** One direction of a link: [fromUid] sends their payments to [toUid]. */
data class Link(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val fromName: String,
    val toName: String,
    val status: LinkStatus,
    val muted: Boolean,
    val fromPhone: String? = null,
    val toPhone: String? = null
)

data class ForwardedPayment(
    val id: String,
    val fromName: String,
    val amount: Double,
    val payer: String,
    val timestamp: Long
)

data class MyProfile(val phone: String?, val name: String? = null)

@Singleton
class ForwardingRepository @Inject constructor(
    private val auth: AuthRepository
) {
    private val db get() = FirebaseFirestore.getInstance()
    private val me get() = auth.currentUser ?: throw AuthException("Sign in to use payment forwarding.")

    // ---- My number --------------------------------------------------------------------------

    val myProfile: Flow<MyProfile> = auth.user.flatMapLatest { user ->
        if (user == null) flowOf(MyProfile(null))
        else callbackFlow {
            val reg = db.collection("users").document(user.uid).addSnapshotListener { snap, _ ->
                trySend(MyProfile(snap?.getString("phone"), snap?.getString("displayName")?.takeIf { it.isNotBlank() }))
            }
            awaitClose { reg.remove() }
        }
    }

    /** Claims [raw] as my number, shown to others as [displayName]. Fails if someone else already holds the number. */
    suspend fun setMyPhone(raw: String, displayName: String) {
        val phone = PhoneNumber.normalize(raw) ?: throw AuthException("Enter a valid 10-digit mobile number.")
        val user = me
        val name = displayName.trim().take(40).ifBlank { defaultName(user) }
        val userRef = db.collection("users").document(user.uid)
        val newIndex = db.collection("phoneIndex").document(phone)
        try {
            db.runTransaction { tx ->
                val existing = tx.get(newIndex)
                if (existing.exists() && existing.getString("uid") != user.uid) {
                    throw AuthException("That number is already linked to another KuralPay account.")
                }
                val oldPhone = tx.get(userRef).getString("phone")
                tx.set(newIndex, mapOf("uid" to user.uid, "name" to name))
                tx.set(userRef, mapOf("phone" to phone, "displayName" to name, "email" to user.email.orEmpty()), com.google.firebase.firestore.SetOptions.merge())
                if (oldPhone != null && oldPhone != phone) tx.delete(db.collection("phoneIndex").document(oldPhone))
            }.await()
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "setMyPhone failed", e)
            throw AuthException("Couldn't save your number. Check your connection and try again.")
        }
    }

    // ---- Links ------------------------------------------------------------------------------

    /** Requests I sent, and people I forward to. */
    val outgoing: Flow<List<Link>> = linksWhere("fromUid")

    /** Requests sent to me, and people who forward to me. */
    val incoming: Flow<List<Link>> = linksWhere("toUid")

    private fun linksWhere(field: String): Flow<List<Link>> = auth.user.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else callbackFlow {
            val reg = db.collection("links").whereEqualTo(field, user.uid).addSnapshotListener { snap, err ->
                if (err != null) Log.w(TAG, "links listener ($field)", err)
                trySend(snap?.documents?.mapNotNull { d ->
                    Link(
                        id = d.id,
                        fromUid = d.getString("fromUid") ?: return@mapNotNull null,
                        toUid = d.getString("toUid") ?: return@mapNotNull null,
                        fromName = d.getString("fromName").orEmpty(),
                        toName = d.getString("toName").orEmpty(),
                        status = runCatching { LinkStatus.valueOf(d.getString("status").orEmpty().uppercase()) }.getOrDefault(LinkStatus.PENDING),
                        muted = d.getBoolean("muted") ?: false,
                        fromPhone = d.getString("fromPhone"),
                        toPhone = d.getString("toPhone")
                    )
                }.orEmpty().sortedBy { it.fromName + it.toName })
            }
            awaitClose { reg.remove() }
        }
    }

    /**
     * Asks the person with this number to receive my payments. [label] is the name I want to see for
     * them; if blank, the name they gave themselves is used.
     */
    suspend fun requestLink(rawPhone: String, label: String) {
        val phone = PhoneNumber.normalize(rawPhone) ?: throw AuthException("Enter a valid 10-digit mobile number.")
        val user = me
        try {
            val target = db.collection("phoneIndex").document(phone).get().await()
            val toUid = target.getString("uid") ?: throw AuthException("No KuralPay user found with that number. Ask them to sign in and add it.")
            if (toUid == user.uid) throw AuthException("That's your own number.")
            val myDoc = db.collection("users").document(user.uid).get().await()
            val myName = myDoc.getString("displayName")?.takeIf { it.isNotBlank() } ?: defaultName(user)
            val ref = db.collection("links").document("${user.uid}_$toUid")
            // A declined request is replaced by a fresh one; only the recipient may edit it.
            if (ref.get().await().exists()) ref.delete().await()
            ref.set(
                mapOf(
                    "fromUid" to user.uid, "toUid" to toUid,
                    "fromName" to myName, "toName" to label.trim().take(40).ifBlank { target.getString("name").orEmpty() },
                    "fromPhone" to myDoc.getString("phone"), "toPhone" to phone,
                    "status" to "pending", "muted" to false, "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "requestLink failed", e)
            throw AuthException("Couldn't send the request. Check your connection and try again.")
        }
    }

    private fun defaultName(user: com.google.firebase.auth.FirebaseUser): String =
        user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty().substringBefore('@')

    suspend fun respond(link: Link, accept: Boolean) {
        db.collection("links").document(link.id).update("status", if (accept) "accepted" else "declined").await()
    }

    suspend fun setMuted(link: Link, muted: Boolean) {
        db.collection("links").document(link.id).update("muted", muted).await()
    }

    suspend fun unlink(link: Link) {
        db.collection("links").document(link.id).delete().await()
    }

    // ---- Sending ----------------------------------------------------------------------------

    /** Sends one detected payment to everyone who accepted me and hasn't muted me. Only amount and payer leave the phone. */
    suspend fun forward(amount: Double, payer: String) {
        val user = auth.currentUser ?: return
        // The name I saved in KuralPay, so the announcement matches what linked people see in their list.
        val myName = runCatching { db.collection("users").document(user.uid).get().await().getString("displayName") }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: defaultName(user)
        val targets = db.collection("links").whereEqualTo("fromUid", user.uid).whereEqualTo("status", "accepted").get().await()
            .documents.filter { it.getBoolean("muted") != true }
        for (target in targets) {
            val toUid = target.getString("toUid") ?: continue
            try {
                db.collection("inbox").document(toUid).collection("forwards").add(
                    mapOf(
                        "fromUid" to user.uid, "fromName" to myName,
                        "amount" to amount, "payer" to payer,
                        "ts" to System.currentTimeMillis(), "heard" to false
                    )
                ).await()
            } catch (e: Exception) {
                Log.w(TAG, "forward to $toUid failed", e)
            }
        }
    }

    // ---- Receiving --------------------------------------------------------------------------

    /** New forwarded payments for me. Each is emitted once, then marked heard. */
    val incomingPayments: Flow<ForwardedPayment> = auth.user.flatMapLatest { user ->
        if (user == null) emptyFlow()
        else callbackFlow<ForwardedPayment> {
            val col = db.collection("inbox").document(user.uid).collection("forwards")
            // Sorted on the phone: filtering by heard and ordering by ts would need a composite index.
            val reg = col.whereEqualTo("heard", false)
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.w(TAG, "inbox listener", err); return@addSnapshotListener }
                    val now = System.currentTimeMillis()
                    snap?.documentChanges?.filter { it.type == DocumentChange.Type.ADDED }
                        ?.sortedBy { it.document.getLong("ts") ?: now }
                        ?.forEach { change ->
                        val d = change.document
                        val ts = d.getLong("ts") ?: now
                        d.reference.update("heard", true)
                        // Anything older than an hour waited while this phone was off; don't read out stale news.
                        if (now - ts <= STALE_AFTER_MS) {
                            trySend(
                                ForwardedPayment(
                                    id = d.id,
                                    fromName = d.getString("fromName").orEmpty(),
                                    amount = d.getDouble("amount") ?: return@forEach,
                                    payer = d.getString("payer").orEmpty(),
                                    timestamp = ts
                                )
                            )
                        }
                    }
                }
            awaitClose { reg.remove() }
        }
    }

    private companion object {
        const val TAG = "ForwardingRepository"
        const val STALE_AFTER_MS = 60 * 60 * 1000L
    }
}
