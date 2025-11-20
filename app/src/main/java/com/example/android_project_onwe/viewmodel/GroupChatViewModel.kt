package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Debt
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Message
import com.example.android_project_onwe.model.Payment
import com.example.android_project_onwe.repository.ExpenseRepository
import com.example.android_project_onwe.repository.GroupChatRepository
import com.example.android_project_onwe.repository.PaymentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class GroupChatViewModel(
    private val chatRepo: GroupChatRepository = GroupChatRepository(),
    private val expenseRepo: ExpenseRepository = ExpenseRepository(),
    private val paymentRepo: PaymentRepository = PaymentRepository(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    // DEV MODE
    private val DEV_MODE: Boolean = false
    private val DEV_USER_ID: String = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    private fun getUserId(): String? =
        if (DEV_MODE) DEV_USER_ID else FirebaseAuth.getInstance().currentUser?.uid

    private val _groupName = MutableStateFlow<String>("")
    val groupName: StateFlow<String> = _groupName

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments

    private val _membersMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val membersMap: StateFlow<Map<String, String>> = _membersMap

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> = _debts

    private val _billFinalized = MutableStateFlow<Boolean>(false)
    val billFinalized: StateFlow<Boolean> = _billFinalized

    fun loadGroupName(groupId: String) {
        db.collection("group").document(groupId).get()
            .addOnSuccessListener { snap ->
                _groupName.value = snap.getString("name") ?: "Group"
                _billFinalized.value = snap.getBoolean("billFinalized") ?: false
            }
            .addOnFailureListener {
                _groupName.value = "Group"
                _billFinalized.value = false
            }
    }

    fun listenForMessages(groupId: String) {
        chatRepo.listenForMessages(groupId) { list ->
            _messages.value = list.sortedBy { it.timestamp }
        }
    }

    fun listenForExpenses(groupId: String) {
        expenseRepo.listenForExpenses(groupId) { list ->
            _expenses.value = list.sortedBy { it.timestamp }
            recomputeDebtsIfReady(groupId)
        }
    }

    fun listenForPayments(groupId: String) {
        paymentRepo.listenForPayments(groupId) { p ->
            _payments.value = p.sortedBy { it.timestamp }
        }
    }

    fun sendMessage(groupId: String, text: String) = viewModelScope.launch {
        chatRepo.sendMessage(groupId, text)
    }

    fun addExpense(groupId: String, amount: Double, description: String) {
        if (_billFinalized.value) return
        viewModelScope.launch {
            expenseRepo.addExpense(groupId, amount, description)
        }
    }

    fun loadGroupMembersAndNames(groupId: String) {
        viewModelScope.launch {
            db.collection("group").document(groupId).get()
                .addOnSuccessListener { snap ->
                    val memberRefs = (snap.get("members") as? List<*>)?.filterIsInstance<DocumentReference>()
                        ?: emptyList()

                    if (memberRefs.isEmpty()) {
                        _membersMap.value = emptyMap()
                        _debts.value = emptyList()
                        return@addOnSuccessListener
                    }

                    val nameMap = mutableMapOf<String, String>()
                    var remaining = memberRefs.size

                    memberRefs.forEach { ref ->
                        ref.get().addOnSuccessListener { userSnap ->
                            nameMap[ref.id] = userSnap.getString("firstName") ?: ""
                        }.addOnCompleteListener {
                            remaining--
                            if (remaining == 0) {
                                _membersMap.value = nameMap.toMap()
                                recomputeDebts(groupId, nameMap.keys.toList())
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    _membersMap.value = emptyMap()
                    _debts.value = emptyList()
                }
        }
    }

    private fun recomputeDebtsIfReady(groupId: String) {
        val members = _membersMap.value.keys.toList()
        if (members.isNotEmpty()) recomputeDebts(groupId, members)
    }

    private fun recomputeDebts(groupId: String, memberIds: List<String>) {
        viewModelScope.launch {
            if (memberIds.isEmpty()) {
                _debts.value = emptyList()
                return@launch
            }

            val currentExpenses = _expenses.value
            val total = currentExpenses.sumOf { it.amount }
            val sharePerMember = if (memberIds.isNotEmpty()) total / memberIds.size else 0.0

            val paidBy = mutableMapOf<String, Double>()
            memberIds.forEach { paidBy[it] = 0.0 }
            currentExpenses.forEach { exp ->
                paidBy[exp.payerId] = (paidBy[exp.payerId] ?: 0.0) + exp.amount
                if (!paidBy.containsKey(exp.payerId)) {
                    paidBy[exp.payerId] = (paidBy[exp.payerId] ?: 0.0)
                }
            }

            val balances: Map<String, Double> = paidBy.mapValues { (_, paid) ->
                BigDecimal(paid - sharePerMember).setScale(2, RoundingMode.HALF_UP).toDouble()
            }

            val creditors = balances.filter { it.value > 0.009 }
                .toList()
                .sortedByDescending { it.second }
                .toMutableList()

            val debtors = balances.filter { it.value < -0.009 }
                .map { it.key to -it.value }
                .toList()
                .sortedByDescending { it.second }
                .toMutableList()

            val settlement = mutableListOf<Debt>()

            var ci = 0
            var di = 0

            while (di < debtors.size && ci < creditors.size) {
                val (debtorId, debtorAmt) = debtors[di]
                val (creditorId, creditorAmt) = creditors[ci]

                val settleAmt = minOf(debtorAmt, creditorAmt)
                val rounded = BigDecimal(settleAmt).setScale(2, RoundingMode.HALF_UP).toDouble()
                if (rounded > 0.0) {
                    settlement.add(Debt(fromUserId = debtorId, toUserId = creditorId, amount = rounded))
                }

                val remainingDebtor = debtorAmt - settleAmt
                val remainingCreditor = creditorAmt - settleAmt

                // update lists safely
                if (remainingDebtor <= 0.009) {
                    di++
                } else {
                    debtors[di] = debtorId to remainingDebtor
                }

                if (remainingCreditor <= 0.009) {
                    ci++
                } else {
                    creditors[ci] = creditorId to remainingCreditor
                }
            }

            _debts.value = settlement
        }
    }


    fun finalizeBill(groupId: String) {
        viewModelScope.launch {
            if (_billFinalized.value) return@launch

            recomputeDebtsIfReady(groupId)
            val currentDebts = _debts.value
            val userCol = db.collection("user")

            val payments = currentDebts.map { d ->
                Payment(
                    id = "",
                    fromUser = userCol.document(d.fromUserId),
                    toUser = userCol.document(d.toUserId),
                    amount = d.amount,
                    isPaid = false,
                    timestamp = System.currentTimeMillis()
                )
            }

            paymentRepo.createPayments(groupId, payments) {
                db.collection("group")
                    .document(groupId)
                    .update("billFinalized", true)
                    .addOnSuccessListener {
                        _billFinalized.value = true
                    }

                listenForPayments(groupId)
            }
        }
    }


    fun reopenBill(groupId: String) {
        viewModelScope.launch {
            paymentRepo.deleteAllPayments(groupId) {
                db.collection("group")
                    .document(groupId)
                    .update("billFinalized", false)
                    .addOnSuccessListener {
                        _billFinalized.value = false
                        _payments.value = emptyList()
                        paymentRepo.stopListening()
                    }
            }
        }
    }


    fun togglePaymentPaid(groupId: String, paymentId: String, isPaid: Boolean) {
        paymentRepo.setPaymentPaid(groupId, paymentId, isPaid)
    }


    fun stopAllListeners() {
        chatRepo.stopListening()
        expenseRepo.stopListening()
        paymentRepo.stopListening()
    }

    fun resetValueGroupState() {
        stopAllListeners()
        _debts.value = emptyList()
        _expenses.value = emptyList()
        _membersMap.value = emptyMap()
        _payments.value = emptyList()
        _groupName.value = ""
        _billFinalized.value = false
    }

    fun editExpense(groupId: String, expenseId: String, amount: Double, description: String) {
        viewModelScope.launch {
            expenseRepo.updateExpense(groupId, expenseId, amount, description)
        }
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        viewModelScope.launch {
            expenseRepo.deleteExpense(groupId, expenseId)
        }
    }
}
