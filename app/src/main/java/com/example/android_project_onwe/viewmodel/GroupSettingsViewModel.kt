package com.example.android_project_onwe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupSettingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserRef: DocumentReference? =
        auth.currentUser?.let { db.collection("user").document(it.uid) }

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group

    private val _membersData =
        MutableStateFlow<List<Triple<String, String, DocumentReference>>>(emptyList())
    val membersData: StateFlow<List<Triple<String, String, DocumentReference>>> = _membersData
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private var groupId: String? = null

    fun fetchGroup(groupId: String) {
        this.groupId = groupId

        db.collection("group").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                _group.value = snapshot?.toObject(Group::class.java)
                _isLoading.value = false
                fetchMembers()
            }
    }

    private fun fetchMembers() {
        val grp = _group.value ?: return
        viewModelScope.launch {
            val fetchedMembers = mutableListOf<Triple<String, String, DocumentReference>>()

            for (memberRef in grp.members) {
                try {
                    val snapshot = memberRef.get().await()

                    val firstName = snapshot.getString("firstName") ?: ""
                    val lastName = snapshot.getString("lastName") ?: ""
                    val email = snapshot.getString("email") ?: memberRef.id // fallback to UID

                    if (snapshot.getString("email") == null) {
                        Log.w("GroupSettingsVM", "Missing email for user ${memberRef.id}")
                    }

                    val displayName = if (firstName.isNotEmpty() || lastName.isNotEmpty())
                        "$firstName $lastName".trim()
                    else email

                    fetchedMembers.add(Triple(displayName, email, memberRef))

                } catch (e: Exception) {
                    Log.e("GroupSettingsVM", "Error fetching member ${memberRef.id}", e)
                }
            }

            _membersData.value = fetchedMembers
        }
    }

    fun updateGroupName(newName: String) {
        val gid = groupId ?: return
        db.collection("group").document(gid).update("name", newName)
    }

    fun updateGroupDescription(newDesc: String) {
        val gid = groupId ?: return
        db.collection("group").document(gid).update("description", newDesc)
    }

    fun addMemberByEmail(email: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                val query = db.collection("user").whereEqualTo("email", email).get().await()
                val userRef = query.documents.firstOrNull()?.reference
                userRef?.let {
                    val updatedMembers = _group.value?.members?.toMutableList() ?: mutableListOf()
                    if (!updatedMembers.contains(it)) {
                        updatedMembers.add(it)
                        db.collection("group").document(gid).update("members", updatedMembers)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeMember(memberRef: DocumentReference) {
        val gid = groupId ?: return
        viewModelScope.launch {
            val updatedMembers = _group.value?.members?.toMutableList() ?: return@launch
            updatedMembers.remove(memberRef)
            db.collection("group").document(gid).update("members", updatedMembers)
        }
    }

    fun leaveGroup() {
        currentUserRef?.let { removeMember(it) }
    }

    fun isCurrentUser(memberRef: DocumentReference) = memberRef == currentUserRef
}
