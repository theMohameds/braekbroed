package com.example.android_project_onwe.model

data class ProfileModel(
    var firstName: String = "",
    var lastName: String = "",
    var phone: String = "",
    var address: String = "",
    var notificationsEnabled: Boolean = false
)