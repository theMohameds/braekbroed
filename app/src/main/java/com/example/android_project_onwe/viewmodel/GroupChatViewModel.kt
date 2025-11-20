package com.example.android_project_onwe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_project_onwe.model.Debt
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Message
import com.example.android_project_onwe.repository.ExpenseRepository
import com.example.android_project_onwe.repository.GroupChatRepository
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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {


    // DEV MODE
    private val DEV_MODE = true
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    private fun getUserId(): String? {
        return if (DEV_MODE) DEV_USER_ID else FirebaseAuth.getInstance().currentUser?.uid
    }

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> get() = _groupName
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> get() = _messages
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> get() = _expenses
    private val _membersMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val membersMap: StateFlow<Map<String, String>> get() = _membersMap
    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: StateFlow<List<Debt>> get() = _debts

    fun loadGroupName(groupId: String) {
        db.collection("group").document(groupId).get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("name") ?: "Group"
                _groupName.value = name
            }
            .addOnFailureListener {
                _groupName.value = "Group"
            }
    }

    fun listenForMessages(groupId: String) {
        chatRepo.listenForMessages(groupId) { newMessages ->
            _messages.value = newMessages.sortedBy { it.timestamp }
        }
    }

    fun listenForExpenses(groupId: String) {
        expenseRepo.listenForExpenses(groupId) { newExpenses ->
            _expenses.value = newExpenses.sortedBy { it.timestamp }
            recomputeDebtsIfReady(groupId)
        }
    }

    fun sendMessage(groupId: String, text: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(groupId, text)
        }
    }

    fun addExpense(groupId: String, amount: Double, description: String) {
        viewModelScope.launch {
            expenseRepo.addExpense(groupId, amount, description)
        }
    }

    fun loadGroupMembersAndNames(groupId: String) {
        viewModelScope.launch {
            db.collection("group").document(groupId).get()
                .addOnSuccessListener { snap ->
                    val rawMembers = snap.get("members") as? List<*>
                    val docRefs = rawMembers
                        ?.filterIsInstance<DocumentReference>()
                        ?: emptyList()

                    if (docRefs.isEmpty()) {
                        _membersMap.value = emptyMap()
                        _debts.value = emptyList()
                        return@addOnSuccessListener
                    }

                    val resultMap = mutableMapOf<String, String>()
                    var remaining = docRefs.size

                    docRefs.forEach { dr ->
                        dr.get().addOnSuccessListener { userSnap ->
                            val uid = dr.id
                            val firstName = userSnap.getString("firstName") ?: ""
                            resultMap[uid] = firstName
                        }.addOnCompleteListener {
                            remaining -= 1
                            if (remaining <= 0) {
                                _membersMap.value = resultMap.toMap()
                                recomputeDebts(groupId, resultMap.keys.toList())
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
        if (members.isNotEmpty()) {
            recomputeDebts(groupId, members)
        } else {
            loadGroupMembersAndNames(groupId)
        }
    }

    private fun recomputeDebts(groupId: String, memberIds: List<String>) {
        viewModelScope.launch {
            if (memberIds.isEmpty()) {
                _debts.value = emptyList()
                return@launch
            }

            val currentExpenses = _expenses.value

            val balances = mutableMapOf<String, Double>()
            memberIds.forEach { balances[it] = 0.0 }

            val totalExpenses = currentExpenses.sumOf { it.amount }
            val memberCount = memberIds.size
            val sharePerMember = if (memberCount > 0) totalExpenses / memberCount else 0.0

            val paidBy = mutableMapOf<String, Double>()
            memberIds.forEach { paidBy[it] = 0.0 }

            currentExpenses.forEach { exp ->
                val payer = exp.payerId
                if (paidBy.containsKey(payer)) {
                    paidBy[payer] = (paidBy[payer] ?: 0.0) + exp.amount
                } else {
                    paidBy[payer] = (paidBy[payer] ?: 0.0) + exp.amount
                    if (!balances.containsKey(payer)) balances[payer] = 0.0
                }
            }

            val computedBalances = balances.keys.associateWith { memberId ->
                val paid = paidBy[memberId] ?: 0.0
                val bal = paid - sharePerMember
                BigDecimal(bal).setScale(2, RoundingMode.HALF_UP).toDouble()
            }.toMutableMap()

            val creditors = ArrayList<Pair<String, Double>>()
            val debtors = ArrayList<Pair<String, Double>>()

            for ((uid, bal) in computedBalances) {
                when {
                    bal > 0.009 -> creditors.add(uid to bal)
                    bal < -0.009 -> debtors.add(uid to -bal)
                    else -> { }
                }
            }

            creditors.sortByDescending { it.second }
            debtors.sortByDescending { it.second }

            val settlement = mutableListOf<Debt>()

            var i = 0
            var j = 0
            while (i < debtors.size && j < creditors.size) {
                val (debtorId, debtorAmt) = debtors[i]
                val (creditorId, creditorAmt) = creditors[j]

                val settleAmt = minOf(debtorAmt, creditorAmt)
                val rounded = BigDecimal(settleAmt).setScale(2, RoundingMode.HALF_UP).toDouble()

                if (rounded > 0.0) {
                    settlement.add(Debt(fromUserId = debtorId, toUserId = creditorId, amount = rounded))
                }

                // decrement
                val newDebtor = debtorAmt - settleAmt
                val newCreditor = creditorAmt - settleAmt

                if (newDebtor <= 0.009) i++ else debtors[i] = debtorId to newDebtor
                if (newCreditor <= 0.009) j++ else creditors[j] = creditorId to newCreditor
            }

            _debts.value = settlement
        }
    }
}
