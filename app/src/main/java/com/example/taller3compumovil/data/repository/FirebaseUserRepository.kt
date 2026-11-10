package com.example.taller3compumovil.data.repository

import com.example.taller3compumovil.data.model.UserProfile
import com.example.taller3compumovil.domain.repository.UserRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun createProfile(profile: UserProfile): Result<Unit> =
        executeFirestoreOperation {
            val document = usersCollection.document(profile.uid)

            firestore.runTransaction { transaction ->
                val existingDocument = transaction.get(document)
                if (!existingDocument.exists()) {
                    transaction.set(document, profile.toFirestoreMap())
                }
                existingDocument.exists()
            }.awaitResult()

            Unit
        }

    override suspend fun getProfile(uid: String): Result<UserProfile?> =
        executeFirestoreOperation {
            val snapshot = usersCollection.document(uid).get().awaitResult()
            if (snapshot.exists()) snapshot.toUserProfile() else null
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = usersCollection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    close(IllegalStateException("Firestore returned no snapshot for user $uid"))
                    return@addSnapshotListener
                }

                try {
                    val profile = if (snapshot.exists()) snapshot.toUserProfile() else null
                    trySend(profile)
                } catch (exception: Exception) {
                    close(exception)
                }
            }

        awaitClose { registration.remove() }
    }

    override suspend fun updateName(uid: String, name: String): Result<Unit> =
        executeFirestoreOperation {
            usersCollection.document(uid)
                .update(FIELD_NAME, name)
                .awaitResult()
            Unit
        }

    override suspend fun updatePhone(uid: String, phone: String): Result<Unit> =
        executeFirestoreOperation {
            usersCollection.document(uid)
                .update(FIELD_PHONE, phone)
                .awaitResult()
            Unit
        }

    private val usersCollection
        get() = firestore.collection(COLLECTION_USERS)

    private fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
        FIELD_UID to uid,
        FIELD_NAME to name,
        FIELD_EMAIL to email,
        FIELD_PHONE to phone,
        FIELD_PHOTO_URL to photoUrl
    )

    private fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
        uid = id,
        name = getString(FIELD_NAME)
            ?: throw IllegalStateException("User document $id is missing '$FIELD_NAME'"),
        email = getString(FIELD_EMAIL)
            ?: throw IllegalStateException("User document $id is missing '$FIELD_EMAIL'"),
        phone = getString(FIELD_PHONE)
            ?: throw IllegalStateException("User document $id is missing '$FIELD_PHONE'"),
        photoUrl = getString(FIELD_PHOTO_URL)
    )

    private suspend fun <T> executeFirestoreOperation(
        operation: suspend () -> T
    ): Result<T> = try {
        Result.success(operation())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        Result.failure(exception)
    }

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
        const val FIELD_UID = "uid"
        const val FIELD_NAME = "name"
        const val FIELD_EMAIL = "email"
        const val FIELD_PHONE = "phone"
        const val FIELD_PHOTO_URL = "photoUrl"
    }
}
