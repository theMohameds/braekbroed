package com.example.android_project_onwe.model

data class PendingOperation(
    val opId: String,
    val groupId: String,
    val paymentId: String,
    val desiredIsPaid: Boolean,
    val createdAt: Long,
    var attemptCount: Int = 0,
    var failed: Boolean = false
)
