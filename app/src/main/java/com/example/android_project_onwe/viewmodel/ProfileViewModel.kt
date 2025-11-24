package com.example.android_project_onwe.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.AppNavigation
import com.example.android_project_onwe.model.ProfileModel
import com.example.android_project_onwe.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel(

) {

    private val repo = ProfileRepository()
    var profile = mutableStateOf(ProfileModel())
        private set

    var originalProfile = mutableStateOf(ProfileModel())
        private set
    var isSaved = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isLoggedOut = mutableStateOf(false)
        private set

    fun loadProfile() {
        viewModelScope.launch {
            isLoading.value = true
            val loaded = repo.loadProfile()
            if (loaded != null) {
                profile.value = loaded
                originalProfile.value = loaded
            }
            isLoading.value = false
        }
    }

    fun updateProfileField(field: String, value: String) {
        val updated = profile.value.copy()
        when (field) {
            "firstName" -> updated.firstName = value
            "lastName" -> updated.lastName = value
            "email" -> updated.lastName = value
            "phone" -> updated.phone = value
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

            val profileData = profile.value

            runCatching {
                repo.saveProfile(profileData)

            }.onSuccess { result ->
                if (result.isSuccess) {
                    isSaved.value = true
                    originalProfile.value = profile.value
                } else {
                    errorMessage.value = result.exceptionOrNull()?.message
                        ?: "Failed to save profile."
                }
            }.onFailure { e ->
                errorMessage.value = e.message ?: "Failed to save profile."
            }

            isLoading.value = false
        }
    }

    fun resetProfile() {
        profile.value = originalProfile.value.copy()
        isSaved.value = false
        errorMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            runCatching {
                repo.logout()
            }.onSuccess {
                profile.value = ProfileModel()
                isSaved.value = false
                isLoggedOut.value = true
            }.onFailure { e ->
                errorMessage.value = e.message ?: "Logout failed. Try again."
            }

            isLoading.value = false
        }
    }

}
