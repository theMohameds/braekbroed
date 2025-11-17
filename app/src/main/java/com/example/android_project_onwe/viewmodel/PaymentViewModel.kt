package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import com.example.android_project_onwe.model.Payment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PaymentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    fun addPayment(groupId: String, payment: Payment) {
        db.collection("groups").document(groupId)
            .collection("payments")
            .add(payment.copy(createdAt = Timestamp.now()))
    }

    fun loadPayments(groupId: String) {
        db.collection("groups").document(groupId)
            .collection("payments")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snap ->
                _payments.value = snap.documents.mapNotNull { it.toObject(Payment::class.java) }
            }
    }
}
