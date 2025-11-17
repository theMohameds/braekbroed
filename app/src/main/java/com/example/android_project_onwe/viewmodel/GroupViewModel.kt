package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android_project_onwe.model.Group
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GroupViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    fun loadGroupsForCurrentUser() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            _groups.value = emptyList()
            return
        }

        db.collection("group")
            .whereArrayContains("members", db.collection("users").document(currentUserId))
            .get()
            .addOnSuccessListener { snap ->
                _groups.value = snap.documents.mapNotNull { it.toObject(Group::class.java) }
            }
            .addOnFailureListener {
                _groups.value = emptyList()
            }
    }

    fun createGroup(name: String, description: String, members: List<DocumentReference>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        val updatedMembers = members.toMutableList()
        updatedMembers.add(currentUserRef)

        val groupData = Group(
            name = name,
            description = description,
            createdBy = currentUserId,
            createdAt = Timestamp.now(),
            members = updatedMembers
        )

        db.collection("group")
            .document()
            .set(groupData)
            .addOnSuccessListener {
                // Group created successfully
            }
            .addOnFailureListener { e ->
                // Handle error
                e.printStackTrace()
            }
    }
}
