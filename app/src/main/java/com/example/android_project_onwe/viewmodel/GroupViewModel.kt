package com.example.android_project_onwe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupViewModel() : ViewModel() {

    // Dev mode
    private val DEV_MODE = false
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups
    private val _groupEvent = MutableStateFlow("")

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

    fun createGroup(
        name: String,
        description: String,
        memberEmailsOrRefs: List<Any>
    ) {
        val currentUserId = getCurrentUserId() ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        if (DEV_MODE) {
            val refs = memberEmailsOrRefs.filterIsInstance<DocumentReference>().toMutableList()

            if (!refs.contains(currentUserRef)) {
                refs.add(currentUserRef)
            }

            finishGroupCreation(name, description, currentUserRef, refs)
            return
        }

        val memberEmails = memberEmailsOrRefs.filterIsInstance<String>()

        viewModelScope.launch {
            try {
                val memberRefs = memberEmails.mapNotNull { email ->
                    val normalizedEmail = email.trim().lowercase()
                    val snap = db.collection("user")
                        .whereEqualTo("email", normalizedEmail)
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

    fun startListeningToGroups() {
        val currentUserId = getCurrentUserId() ?: return
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
    private fun finishGroupCreation(
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
            createdBy = currentUserRef,
            createdAt = Timestamp.now(),
            members = allMembers,
            billFinalized = false
        )

        val newDocRef = db.collection("group").document()
        val newDocId = newDocRef.id

        newDocRef.set(groupData)
            .addOnSuccessListener {
                _groups.update { current ->
                    current + groupData.copy(id = newDocId)
                }
                _groupEvent.value = "Group created successfully!"
            }
            .addOnFailureListener { e ->
                _groupEvent.value = "Failed to save group: ${e.message}"
            }
    }
}
