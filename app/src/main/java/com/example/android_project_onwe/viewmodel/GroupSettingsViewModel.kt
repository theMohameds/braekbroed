package com.example.android_project_onwe.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.GroupSettingsRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration

class GroupSettingsViewModel : ViewModel() {

    private val repo = GroupSettingsRepository()

    var group = mutableStateOf<Group?>(null)
        private set

    var membersData = mutableStateOf<List<Triple<String, String, DocumentReference>>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSaved = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private var groupId: String? = null
    private var groupListener: ListenerRegistration? = null

    fun fetchGroup(groupId: String) {
        this.groupId = groupId
        isLoading.value = true
        errorMessage.value = null

        // Remove previous listener if exists
        groupListener?.remove()

        // Listen for real-time updates
        groupListener = repo.listenToGroup(groupId) { updatedGroup ->
            group.value = updatedGroup
            viewModelScope.launch {
                membersData.value = updatedGroup?.let { repo.fetchMembers(it) } ?: emptyList()
            }
            isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupListener?.remove()
    }

    fun saveGroupChanges(newName: String, newDesc: String) {
        val id = groupId ?: return

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            try {
                // Run both updates sequentially
                runCatching { repo.updateGroupName(id, newName) }.onFailure { e ->
                    errorMessage.value = e.message ?: "Failed to update name."
                    return@launch
                }

                runCatching { repo.updateGroupDescription(id, newDesc) }.onFailure { e ->
                    errorMessage.value = e.message ?: "Failed to update description."
                    return@launch
                }

                // Update local state
                group.value = group.value?.copy(name = newName, description = newDesc)
                isSaved.value = true // triggers toast once
            } finally {
                isLoading.value = false
            }
        }
    }


    fun updateGroupName(newName: String) {
        groupId?.let { id ->
            viewModelScope.launch {
                runCatching { repo.updateGroupName(id, newName) }
                    .onSuccess { group.value = group.value?.copy(name = newName);}
                    .onFailure { e -> errorMessage.value = e.message ?: "Failed to update name." }
            }
        }
    }

    fun updateGroupDescription(newDesc: String) {
        groupId?.let { id ->
            viewModelScope.launch {
                runCatching { repo.updateGroupDescription(id, newDesc) }
                    .onSuccess { group.value = group.value?.copy(description = newDesc);}
                    .onFailure { e -> errorMessage.value = e.message ?: "Failed to update description." }
            }
        }
    }

    fun addMemberByEmail(email: String) {
        groupId?.let { id ->
            viewModelScope.launch {
                runCatching { repo.addMemberByEmail(id, email) }
                    .onFailure { e -> errorMessage.value = e.message ?: "Failed to add member." }
            }
        }
    }

    fun removeMember(memberRef: DocumentReference) {
        groupId?.let { id ->
            viewModelScope.launch {
                runCatching { repo.removeMember(id, memberRef) }
                    .onFailure { e -> errorMessage.value = e.message ?: "Failed to remove member." }
            }
        }
    }

    fun leaveGroup() {
        groupId?.let { id ->
            viewModelScope.launch {
                runCatching { repo.leaveGroup(id) }
                    .onSuccess { group.value = null; membersData.value = emptyList(); isSaved.value = true }
                    .onFailure { e -> errorMessage.value = e.message ?: "Failed to leave group." }
            }
        }
    }

    fun isCurrentUser(memberRef: DocumentReference) = repo.isCurrentUser(memberRef)
    fun clearSavedFlag() { isSaved.value = false }
}
