package com.example.android_project_onwe.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Payment
import com.example.android_project_onwe.model.PendingOperation
import com.example.android_project_onwe.repository.ExpenseRepository
import com.example.android_project_onwe.repository.PaymentRepository
import com.example.android_project_onwe.data.PendingOperationDao
import com.example.android_project_onwe.worker.PaymentRetryWorker
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class FinalizedBillViewModel(
    private val paymentRepo: PaymentRepository = PaymentRepository(),
    private val expenseRepo: ExpenseRepository = ExpenseRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    // Use the singleton DAO (object) so Worker / ViewModel share same backing store
    private val pendingOperationDao = PendingOperationDao

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _members = MutableStateFlow<Map<String, String>>(emptyMap())
    val members: StateFlow<Map<String, String>> = _members

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

    private val pendingPaid = mutableMapOf<String, Boolean>()

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
                        map[dr.id] = it.getString("firstName") ?: ""
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
            viewModelScope.launch {
                val mapped = list.sortedBy { it.timestamp }.map { p ->

                    val optimisticPaid = pendingPaid[p.id]
                    val paidStatus = optimisticPaid ?: p.isPaid

                    val failed = pendingOperationDao.getFailure(p.id)

                    p.copy(
                        isPaid = paidStatus,
                        failed = failed
                    )
                }

                _payments.value = mapped
            }
        }
    }

    fun togglePaid(
        context: Context,
        groupId: String,
        paymentId: String,
        paid: Boolean
    ) {
        // optimistic UI
        pendingPaid[paymentId] = paid

        _payments.value = _payments.value.map {
            if (it.id == paymentId)
                it.copy(isPaid = paid, failed = false)
            else it
        }

        val op = PendingOperation(
            opId = UUID.randomUUID().toString(),
            groupId = groupId,
            paymentId = paymentId,
            desiredIsPaid = paid,
            createdAt = System.currentTimeMillis(),
            attemptCount = 0,
            failed = false
        )

        viewModelScope.launch {
            pendingOperationDao.insert(op)
            enqueuePaymentRetry(context, groupId, paymentId, paid)
        }
    }

    fun markPaymentFailed(paymentId: String) {
        viewModelScope.launch {
            pendingOperationDao.markFailedByPaymentId(paymentId)

            pendingPaid.remove(paymentId)

            _payments.value = _payments.value.map {
                if (it.id == paymentId)
                    it.copy(isPaid = false, failed = true)
                else it
            }
        }
    }

    fun clearPending(paymentId: String) {
        viewModelScope.launch {
            pendingOperationDao.deleteAllFor(paymentId)
            pendingPaid.remove(paymentId)
        }
    }

    private fun enqueuePaymentRetry(
        context: Context,
        groupId: String,
        paymentId: String,
        desiredState: Boolean
    ) {
        val data = workDataOf(
            "groupId" to groupId,
            "paymentId" to paymentId,
            "desiredIsPaid" to desiredState
        )

        val work = OneTimeWorkRequestBuilder<PaymentRetryWorker>()
            .setInputData(data)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .build()

        val wm = WorkManager.getInstance(context)

        wm.enqueueUniqueWork(
            "payment_retry_$paymentId",
            ExistingWorkPolicy.KEEP,
            work
        )

        wm.getWorkInfoByIdLiveData(work.id).observeForever { info ->
            if (info == null) return@observeForever

            when (info.state) {

                WorkInfo.State.SUCCEEDED -> {
                    clearPending(paymentId)
                    wm.getWorkInfoByIdLiveData(work.id).removeObserver { }
                }

                WorkInfo.State.FAILED -> {
                    markPaymentFailed(paymentId)
                    wm.getWorkInfoByIdLiveData(work.id).removeObserver { }
                }

                else -> {}
            }
        }
    }

    fun reopenBill(groupId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            paymentRepo.deleteAllPayments(groupId) {
                db.collection("group").document(groupId)
                    .update("billFinalized", false)
                    .addOnSuccessListener {
                        _payments.value = emptyList()
                        pendingPaid.clear()
                        onDone()
                    }
            }
        }
    }

    fun sendPaymentReminder(toUserId: String, amount: Double) {
        // todo
    }

    override fun onCleared() {
        paymentRepo.stopListening()
        expenseRepo.stopListening()
        super.onCleared()
    }
}
