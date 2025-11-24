package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.ProfileModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid
    suspend fun loadProfile(): ProfileModel? {
        val userId = getUserId() ?: return null

        val doc = db.collection("user")
            .document(userId)
            .get()
            .await()

        return doc.toObject(ProfileModel::class.java)
    }

    suspend fun saveProfile(profile: ProfileModel): Result<Unit> {
        val userId = getUserId() ?: return Result.failure(Exception("Not logged in"))

        return try {
            val updates = mutableMapOf<String, Any>()

            profile.firstName.takeIf { it.isNotBlank() }?.let { updates["firstName"] = it }
            profile.lastName.takeIf { it.isNotBlank() }?.let { updates["lastName"] = it }
            profile.phone.takeIf { it.isNotBlank() }?.let { updates["phone"] = it }
            profile.email.takeIf { it.isNotBlank() }?.let { updates["email"] = it }
            updates["notificationsEnabled"] = profile.notificationsEnabled

            if (updates.isNotEmpty()) {
                db.collection("user")
                    .document(userId)
                    .update(updates)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun logout() {
        auth.signOut()
    }
}
