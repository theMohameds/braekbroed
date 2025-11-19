package com.example.android_project_onwe.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.android_project_onwe.model.ProfileModel

class ProfileViewModel : ViewModel() {

    // Holds user profile data as observable state for the UI.
    var profile = mutableStateOf(ProfileModel())
        private set

    // Indicates whether the profile is saved (used for feedback in the UI).
    var isSaved = mutableStateOf(false)
        private set

    // Updates the specific profile field when the user edits a text input.
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
    }

    fun toggleNotifications(enabled: Boolean) {
        val updated = profile.value.copy(notificationsEnabled = enabled)
        profile.value = updated
        isSaved.value = false
    }

    // Called when the user presses "Save" and marks the profile as saved.
    fun saveProfile() {
        isSaved.value = true
    }
}