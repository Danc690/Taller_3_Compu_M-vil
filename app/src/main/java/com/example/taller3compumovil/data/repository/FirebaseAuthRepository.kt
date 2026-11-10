package com.example.taller3compumovil.data.repository

import com.example.taller3compumovil.domain.repository.AuthRepository
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override suspend fun register(email: String, password: String): Result<String> =
        executeAuthOperation {
            auth.createUserWithEmailAndPassword(email, password)
                .awaitResult()
                .user
                ?.uid
                ?: error("Firebase Authentication returned no user after registration")
        }

    override suspend fun signIn(email: String, password: String): Result<String> =
        executeAuthOperation {
            auth.signInWithEmailAndPassword(email, password)
                .awaitResult()
                .user
                ?.uid
                ?: error("Firebase Authentication returned no user after sign-in")
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUid(): String? = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAuthState(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private suspend fun executeAuthOperation(operation: suspend () -> String): Result<String> =
        try {
            Result.success(operation())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        val listener = OnCompleteListener<T> { task ->
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
        }

        addOnCompleteListener(listener)
    }
}
