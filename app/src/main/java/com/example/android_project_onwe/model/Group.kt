package com.example.android_project_onwe.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

/*
data class Group(
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val members: List<String> = emptyList(),
    val paidBy: String = "",
    val totalExpense: Double = 0.0,
    val currentBalances: Map<String, Double> = emptyMap(),
    val oldBalances: Map<String, Double> = emptyMap()
)
 */

data class Group(
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val members: List<DocumentReference> = listOf()
)