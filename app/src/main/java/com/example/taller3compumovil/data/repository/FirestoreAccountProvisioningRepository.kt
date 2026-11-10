package com.example.taller3compumovil.data.repository

import com.example.taller3compumovil.data.model.Presence
import com.example.taller3compumovil.data.model.UserProfile
import com.example.taller3compumovil.domain.exception.ProvisioningConflictException
import com.example.taller3compumovil.domain.repository.AccountProvisioningRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreAccountProvisioningRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AccountProvisioningRepository {

    override suspend fun ensureAccountDocuments(
        profile: UserProfile,
        presence: Presence
    ): Result<Unit> = try {
        if (profile.uid != presence.uid) {
            throw ProvisioningConflictException(
                "Los UID del perfil privado y de la presencia no coinciden."
            )
        }

        val userDocument = firestore.collection(COLLECTION_USERS).document(profile.uid)
        val presenceDocument = firestore.collection(COLLECTION_PRESENCE).document(presence.uid)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userDocument)
            val presenceSnapshot = transaction.get(presenceDocument)

            validateUid(userSnapshot, profile.uid, "users/${profile.uid}")
            validateUid(presenceSnapshot, presence.uid, "presence/${presence.uid}")

            if (!userSnapshot.exists()) {
                transaction.set(userDocument, profile.toFirestoreMap())
            }
            if (!presenceSnapshot.exists()) {
                transaction.set(presenceDocument, presence.toFirestoreMap())
            }

            true
        }.awaitResult()

        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun validateUid(snapshot: DocumentSnapshot, expectedUid: String, path: String) {
        if (snapshot.exists() && snapshot.getString(FIELD_UID) != expectedUid) {
            throw ProvisioningConflictException(
                "El UID guardado en '$path' no coincide con el UID esperado."
            )
        }
    }

    private fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_UID to uid,
        FIELD_NAME to name,
        FIELD_EMAIL to email,
        FIELD_PHONE to phone,
        FIELD_PHOTO_URL to photoUrl
    )

    private fun Presence.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_UID to uid,
        FIELD_NAME to name,
        FIELD_PHOTO_URL to photoUrl,
        FIELD_IS_ONLINE to isOnline,
        FIELD_LATITUDE to latitude,
        FIELD_LONGITUDE to longitude,
        FIELD_LAST_LOCATION_AT to lastLocationAt
    )

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener(OnCompleteListener { task ->
            if (!continuation.isActive) return@OnCompleteListener

            val exception = task.exception
            if (exception != null) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(exception))
                }
            } else {
                try {
                    val result = task.result
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.success(result))
                    }
                } catch (resultException: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(resultException))
                    }
                }
            }
        })
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_PRESENCE = "presence"
        const val FIELD_UID = "uid"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PHONE = "phone"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_IS_ONLINE = "isOnline"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_LAST_LOCATION_AT = "lastLocationAt"
    }
}
