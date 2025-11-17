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

    // Dev mode
    private val DEV_MODE = true
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private fun getCurrentUserId(): String? {
        return if (DEV_MODE) DEV_USER_ID else auth.currentUser?.uid
    }

    fun loadGroupsForCurrentUser() {
        val currentUserId = getCurrentUserId() ?: run {
            _groups.value = emptyList()
            return
        }

        val currentUserRef = db.collection("user").document(currentUserId)

        db.collection("group")
            .whereArrayContains("members", currentUserRef)
            .get()
            .addOnSuccessListener { snap ->
                _groups.value = snap.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(id = doc.id)
                }
            }
            .addOnFailureListener {
                _groups.value = emptyList()
            }
    }

    fun createGroup(name: String, description: String, members: List<DocumentReference>) {
        val currentUserId = getCurrentUserId() ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        val updatedMembers = members.toMutableList()
        if (!updatedMembers.contains(currentUserRef)) {
            updatedMembers.add(currentUserRef)
        }

        val groupData = Group(
            name = name,
            description = description,
            createdBy = currentUserRef,
            createdAt = Timestamp.now(),
            members = updatedMembers
        )

        db.collection("group")
            .document()
            .set(groupData)
            .addOnSuccessListener {
                loadGroupsForCurrentUser()
            }
    }
}
