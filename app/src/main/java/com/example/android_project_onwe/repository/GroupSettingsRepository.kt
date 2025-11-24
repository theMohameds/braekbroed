package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupSettingsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserRef(): DocumentReference? =
        auth.currentUser?.let { db.collection("user").document(it.uid) }

    suspend fun fetchGroup(groupId: String): Group? {
        val snapshot = db.collection("group")
            .document(groupId)
            .get()
            .await()
        return snapshot.toObject(Group::class.java)
    }

    suspend fun fetchMembers(group: Group): List<Triple<String, String, DocumentReference>> {
        val fetchedMembers = mutableListOf<Triple<String, String, DocumentReference>>()
        for (memberRef in group.members) {
            try {
                val snapshot = memberRef.get().await()
                val firstName = snapshot.getString("firstName") ?: ""
                val lastName = snapshot.getString("lastName") ?: ""
                val email = snapshot.getString("email") ?: memberRef.id
                val displayName = if (firstName.isNotEmpty() || lastName.isNotEmpty())
                    "$firstName $lastName".trim() else email
                fetchedMembers.add(Triple(displayName, email, memberRef))
            } catch (_: Exception) {
                // ignore individual member fetch errors
            }
        }
        return fetchedMembers
    }

    suspend fun updateGroupName(groupId: String, newName: String) {
        db.collection("group").document(groupId).update("name", newName).await()
    }

    suspend fun updateGroupDescription(groupId: String, newDesc: String) {
        db.collection("group").document(groupId).update("description", newDesc).await()
    }

    suspend fun addMemberByEmail(groupId: String, email: String) {
        val query = db.collection("user").whereEqualTo("email", email).get().await()
        val userRef = query.documents.firstOrNull()?.reference ?: return
        val groupSnapshot = db.collection("group").document(groupId).get().await()
        val group = groupSnapshot.toObject(Group::class.java) ?: return
        val updatedMembers = group.members.toMutableList()
        if (!updatedMembers.contains(userRef)) updatedMembers.add(userRef)
        db.collection("group").document(groupId).update("members", updatedMembers).await()
    }

    suspend fun removeMember(groupId: String, memberRef: DocumentReference) {
        val groupSnapshot = db.collection("group").document(groupId).get().await()
        val group = groupSnapshot.toObject(Group::class.java) ?: return
        val updatedMembers = group.members.toMutableList()
        updatedMembers.remove(memberRef)
        db.collection("group").document(groupId).update("members", updatedMembers).await()
    }

    suspend fun leaveGroup(groupId: String) {
        getCurrentUserRef()?.let { removeMember(groupId, it) }
    }

    fun isCurrentUser(memberRef: DocumentReference): Boolean = memberRef == getCurrentUserRef()

    // Listen to real-time updates
    fun listenToGroup(groupId: String, onUpdate: (Group?) -> Unit) =
        db.collection("group").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    onUpdate(snapshot.toObject(Group::class.java))
                }
            }
}
