package com.example.android_project_onwe.model

import com.google.firebase.Timestamp

data class Payment(
    val fromUser: String = "",
    val amount: Double = 0.0,
    val createdAt: Timestamp? = null,
    val groupId: String = ""
)