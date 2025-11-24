package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GroupRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    suspend fun loadGroupsForCurrentUser() {
        val currentUserId = getUserId() ?: run {
            _groups.value = emptyList()
            return
        }

        val currentUserRef = db.collection("user").document(currentUserId)

        try {
            val snap = db.collection("group")
                .whereArrayContains("members", currentUserRef)
                .get()
                .await()

            val groupList = snap.documents.mapNotNull { doc ->
                doc.toObject(Group::class.java)?.copy(id = doc.id)
            }

            _groups.value = groupList
        } catch (e: Exception) {
            _groups.value = emptyList()
        }
    }

    fun startListeningToGroups() {
        val currentUserId = getUserId() ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        db.collection("group")
            .whereArrayContains("members", currentUserRef)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val updatedGroups = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                }

                _groups.value = updatedGroups
            }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        memberRefs: List<String>
    ): Result<Group> {
        val currentUserId = getUserId() ?: return Result.failure(Exception("Not logged in"))
        val currentUserRef = db.collection("user").document(currentUserId)

        return try {
            val memberDocRefs = memberRefs.mapNotNull { email ->
                val normalizedEmail = email.trim().lowercase()
                val snap = db.collection("user")
                    .whereEqualTo("email", normalizedEmail)
                    .get()
                    .await()
                snap.documents.firstOrNull()?.reference
            }.toMutableList()

            val allMembers = (memberDocRefs + currentUserRef).distinct()
            if (allMembers.size <= 1) return Result.failure(Exception("A group must have at least 2 members."))

            val group = Group(
                name = name,
                description = description,
                createdBy = currentUserRef,
                createdAt = Timestamp.now(),
                members = allMembers,
                billFinalized = false
            )

            val newDocRef = db.collection("group").document()
            newDocRef.set(group).await()

            Result.success(group.copy(id = newDocRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
