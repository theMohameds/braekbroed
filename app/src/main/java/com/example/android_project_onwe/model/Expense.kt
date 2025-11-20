package com.example.android_project_onwe.model

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val payerId: String = "",
    val timestamp: Long = 0L
)