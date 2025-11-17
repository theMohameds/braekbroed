package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups
    private val _groupEvent = MutableStateFlow("")
    // Lets the UI listen for messages from the ViewModel, like “Group created!” or errors
    val groupEvent: StateFlow<String> = _groupEvent

    init {
        loadGroupsForCurrentUser()
    }

    fun loadGroupsForCurrentUser() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val snap = db.collection("group")
                    .whereArrayContains("members", db.collection("user").document(currentUserId))
                    .get()
                    .await()

                val loadedGroups = snap.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Group::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                _groups.value = loadedGroups
            } catch (e: Exception) {
                e.printStackTrace()
                _groups.value = emptyList()
            }
        }
    }


    fun createGroup(name: String, description: String, memberEmails: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: run {
            _groupEvent.value = "User not logged in."
            return
        }

        val currentUserRef = db.collection("user").document(currentUserId)

        viewModelScope.launch {
            try {
                // Convert emails to DocumentReferences
                val memberRefs = memberEmails.mapNotNull { email ->
                    val snap = db.collection("user")
                        .whereEqualTo("email", email)
                        .get()
                        .await()

                    snap.documents.firstOrNull()?.reference
                }.toMutableList()

                finishGroupCreation(name, description, currentUserRef, memberRefs)
                _groupEvent.value = "Group created successfully!"
            } catch (e: Exception) {
                e.printStackTrace()
                _groupEvent.value = "Failed to create group: ${e.message}"
            }
        }
    }

    private suspend fun finishGroupCreation(
        name: String,
        description: String,
        currentUserRef: DocumentReference,
        memberRefs: List<DocumentReference>
    ) {
        val allMembers = (memberRefs + currentUserRef).distinct()

        if (allMembers.size <= 1) {
            _groupEvent.value = "A group must have at least 2 members."
            return
        }

        val groupData = Group(
            name = name,
            description = description,
            createdBy = currentUserRef.id,
            createdAt = Timestamp.now(),
            members = allMembers
        )

        try {
            db.collection("group").document()
                .set(groupData)
                .await()
            _groupEvent.value = "Group created successfully!"
        } catch (e: Exception) {
            e.printStackTrace()
            _groupEvent.value = "Failed to create group: ${e.message}"
        }
    }

}
