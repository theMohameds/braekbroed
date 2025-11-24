package com.example.android_project_onwe.model

import com.google.firebase.firestore.DocumentReference

data class Payment(
    val id: String = "",
    val fromUser: DocumentReference? = null,
    val toUser: DocumentReference? = null,
    val amount: Double = 0.0,
    val isPaid: Boolean = false,
    val timestamp: Long = 0L,
    //local used for payment failure, to reset slider.
    val failed: Boolean = false
)
