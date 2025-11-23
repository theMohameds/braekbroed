package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Payment
import com.example.android_project_onwe.repository.ExpenseRepository
import com.example.android_project_onwe.repository.PaymentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FinalizedBillViewModel(
    private val paymentRepo: PaymentRepository = PaymentRepository(),
    private val expenseRepo: ExpenseRepository = ExpenseRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _members = MutableStateFlow<Map<String, String>>(emptyMap())
    val members: StateFlow<Map<String, String>> = _members

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val pendingPaid = mutableMapOf<String, Boolean>()

    private fun getCurrentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    fun start(groupId: String) {
        loadGroupName(groupId)
        loadMembers(groupId)
        listenExpenses(groupId)
        listenPayments(groupId)
    }

    private fun loadGroupName(groupId: String) {
        db.collection("group").document(groupId)
            .get()
            .addOnSuccessListener { snap ->
                _groupName.value = snap.getString("name") ?: ""
            }
    }

    private fun loadMembers(groupId: String) {
        db.collection("group").document(groupId)
            .get()
            .addOnSuccessListener { snap ->
                val refs = snap.get("members") as? List<DocumentReference> ?: emptyList()
                val map = mutableMapOf<String, String>()

                var remain = refs.size
                if (refs.isEmpty()) {
                    _members.value = emptyMap()
                    return@addOnSuccessListener
                }

                refs.forEach { dr ->
                    dr.get().addOnSuccessListener {
                        val name = it.getString("firstName") ?: ""
                        map[dr.id] = name
                    }.addOnCompleteListener {
                        remain--
                        if (remain == 0) {
                            _members.value = map.toMap()
                        }
                    }
                }
            }
    }

    private fun listenExpenses(groupId: String) {
        expenseRepo.listenForExpenses(groupId) { list ->
            _expenses.value = list
        }
    }

    private fun listenPayments(groupId: String) {
        paymentRepo.listenForPayments(groupId) { list ->
            val sorted = list.sortedBy { it.timestamp }.map { p ->
                val overridden = pendingPaid[p.id]
                if (overridden != null && overridden != p.isPaid) {
                    p.copy(isPaid = overridden)
                } else p
            }
            _payments.value = sorted
        }
    }

    fun togglePaid(groupId: String, paymentId: String, paid: Boolean) {
        // optimistic local update
        pendingPaid[paymentId] = paid
        _payments.value = _payments.value.map {
            if (it.id == paymentId) it.copy(isPaid = paid) else it
        }

        paymentRepo.setPaymentPaid(groupId, paymentId, paid)
    }

    fun reopenBill(groupId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            paymentRepo.deleteAllPayments(groupId) {
                db.collection("group").document(groupId)
                    .update("billFinalized", false)
                    .addOnSuccessListener {
                        // clear local store
                        _payments.value = emptyList()
                        pendingPaid.clear()
                        onDone()
                    }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentRepo.stopListening()
        expenseRepo.stopListening()
    }

    fun sendPaymentReminder(toUserId: String, amount: Double) {
        // Needs to be implemented.
    }

}
