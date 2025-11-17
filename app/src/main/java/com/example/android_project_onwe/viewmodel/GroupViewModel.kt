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
            .whereArrayContains("members", db.collection("user").document(currentUserId))
            .get()
            .addOnSuccessListener { snap ->
                _groups.value = snap.documents.mapNotNull { it.toObject(Group::class.java) }
            }
            .addOnFailureListener {
                _groups.value = emptyList()
            }
    }


    fun createGroup(name: String, description: String, memberEmails: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        if (memberEmails.isEmpty()) {

            val groupData = Group(
                name = name,
                description = description,
                createdBy = currentUserId,
                createdAt = Timestamp.now(),
                members = listOf(currentUserRef)
            )
            db.collection("group").document().set(groupData)
            return
        }

        // Convert emails to DocumentReferences asynchronously
        val memberRefs = mutableListOf<DocumentReference>()
        var completed = 0

        memberEmails.forEach { email ->
            db.collection("user")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.firstOrNull()?.let { memberRefs.add(it.reference) }
                    completed++
                    if (completed == memberEmails.size) {
                        finishGroupCreation(name, description, currentUserRef, memberRefs)
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == memberEmails.size) {
                        finishGroupCreation(name, description, currentUserRef, memberRefs)
                    }
                }
        }
    }


    private fun finishGroupCreation(
        name: String,
        description: String,
        currentUserRef: DocumentReference,
        memberRefs: List<DocumentReference>
    ) {
        val allMembers = memberRefs.toMutableList()
        allMembers.add(currentUserRef)

        val groupData = Group(
            name = name,
            description = description,
            createdBy = currentUserRef.id,
            createdAt = Timestamp.now(),
            members = allMembers
        )

        db.collection("group").document()
            .set(groupData)
            .addOnSuccessListener {
                // Group created successfully
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
