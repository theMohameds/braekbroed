package com.example.android_project_onwe.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.GroupSettingsRepository
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupSettingsViewModel : ViewModel() {

    private val repo = GroupSettingsRepository()

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
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val grp = repo.fetchGroup(groupId)
                _group.value = grp
                _membersData.value = grp?.let { repo.fetchMembers(it) } ?: emptyList()
            } catch (e: Exception) {
                Log.e("GroupSettingsVM", "Error fetching group", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateGroupName(newName: String) = groupId?.let {
        viewModelScope.launch { repo.updateGroupName(it, newName) }
    }

    fun updateGroupDescription(newDesc: String) = groupId?.let {
        viewModelScope.launch { repo.updateGroupDescription(it, newDesc) }
    }

    fun addMemberByEmail(email: String) = groupId?.let {
        viewModelScope.launch { repo.addMemberByEmail(it, email) }
    }

    fun removeMember(memberRef: DocumentReference) = groupId?.let {
        viewModelScope.launch { repo.removeMember(it, memberRef) }
    }

    fun leaveGroup() = groupId?.let {
        viewModelScope.launch { repo.leaveGroup(it) }
    }

    fun isCurrentUser(memberRef: DocumentReference) = repo.isCurrentUser(memberRef)
}
