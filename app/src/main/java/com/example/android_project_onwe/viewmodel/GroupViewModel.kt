package com.example.android_project_onwe.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.GroupRepository
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val repo = GroupRepository()

    var groups = mutableStateOf<List<Group>>(emptyList())
        private set

    var groupEvent = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    init {
        loadGroupsForCurrentUser()
        startListeningToGroups()
    }

    fun loadGroupsForCurrentUser() {
        viewModelScope.launch {
            isLoading.value = true
            repo.loadGroupsForCurrentUser() // suspend function
            groups.value = repo.groups.value
            isLoading.value = false
        }
    }

    fun startListeningToGroups() {
        repo.startListeningToGroups()
        viewModelScope.launch {
            repo.groups.collect { updated ->
                groups.value = updated
            }
        }
    }

    fun createGroup(name: String, description: String, memberEmails: List<String>) {
        viewModelScope.launch {
            isLoading.value = true
            val result = repo.createGroup(name, description, memberEmails)
            result.onSuccess { group ->
                groupEvent.value = "Group created successfully!"
            }.onFailure { e ->
                groupEvent.value = "Failed to create group: ${e.message}"
            }
            isLoading.value = false
        }
    }
}
