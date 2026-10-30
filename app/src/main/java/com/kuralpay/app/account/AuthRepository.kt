package com.kuralpay.app.account

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** A failure with a message that is safe to show the person. */
class AuthException(message: String) : Exception(message)

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** False until the owner adds google-services.json; nothing here touches Firebase before then. */
    val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    private val auth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = if (isConfigured) auth.currentUser else null

    val user: Flow<FirebaseUser?> = callbackFlow {
        if (!isConfigured) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(name: String, email: String, password: String) = guarded {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw AuthException("Could not create the account. Try again.")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        saveProfile(user, name.trim())
    }

    suspend fun signIn(email: String, password: String) = guarded {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
        if (user != null) saveProfile(user, user.displayName.orEmpty())
    }

    suspend fun resetPassword(email: String) = guarded {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** [activityContext] must be an Activity: Credential Manager shows its sheet over it. */
    suspend fun signInWithGoogle(activityContext: Context) = guarded {
        val clientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (clientIdRes == 0) throw AuthException("Google sign-in is not set up yet. Add your SHA-1 in Firebase and download google-services.json again.")

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(clientIdRes))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (_: GetCredentialCancellationException) {
            return@guarded
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException("That wasn't a Google account. Try again.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
        if (user != null) saveProfile(user, user.displayName.orEmpty())
    }

    fun signOut() {
        if (isConfigured) auth.signOut()
    }

    private suspend fun saveProfile(user: FirebaseUser, name: String) {
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .set(mapOf("displayName" to name.ifBlank { user.email.orEmpty().substringBefore('@') }, "email" to user.email.orEmpty()), SetOptions.merge())
            .await()
    }

    private suspend fun guarded(block: suspend () -> Unit) {
        if (!isConfigured) throw AuthException("Firebase isn't set up in this build yet.")
        try {
            block()
        } catch (e: AuthException) {
            throw e
        } catch (e: FirebaseAuthException) {
            throw AuthException(friendly(e.errorCode))
        } catch (e: Exception) {
            val offline = e.message.orEmpty().contains("network", ignoreCase = true)
            throw AuthException(if (offline) "No internet connection. Check your network and try again." else "Something went wrong. Please try again.")
        }
    }

    private fun friendly(code: String): String = when (code) {
        "ERROR_INVALID_EMAIL" -> "That email address doesn't look right."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Email or password is incorrect."
        "ERROR_USER_NOT_FOUND" -> "No account found with that email."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Sign in instead."
        "ERROR_WEAK_PASSWORD" -> "Choose a password with at least 6 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a few minutes and try again."
        "ERROR_NETWORK_REQUEST_FAILED" -> "No internet connection. Check your network and try again."
        else -> "Could not sign in. Please try again."
    }
}
