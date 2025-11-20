package com.example.android_project_onwe.model

data class Message(
    @Transient var id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L
)