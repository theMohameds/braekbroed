package com.example.android_project_onwe.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.ProfileModel
import com.example.android_project_onwe.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    var profile = mutableStateOf(ProfileModel())
        private set

    var isSaved = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            isLoading.value = true
            val loaded = repository.loadProfile()
            if (loaded != null) {
                profile.value = loaded
            }
            isLoading.value = false
        }
    }

    fun updateProfileField(field: String, value: String) {
        val updated = profile.value.copy()

        when (field) {
            "firstName" -> updated.firstName = value
            "lastName" -> updated.lastName = value
            "phone" -> updated.phone = value
            "address" -> updated.address = value
        }

        profile.value = updated
        isSaved.value = false
        errorMessage.value = null
    }

    fun toggleNotifications(enabled: Boolean) {
        val updated = profile.value.copy(notificationsEnabled = enabled)
        profile.value = updated
        isSaved.value = false
        errorMessage.value = null
    }

    fun saveProfile() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            isSaved.value = false

            val result = repository.saveProfile(profile.value)

            if (result.isSuccess) {
                isSaved.value = true
            } else {
                errorMessage.value = "Connection error. Try again later."
            }

            isLoading.value = false
        }
    }
}
