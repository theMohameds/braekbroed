package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ExpenseRepository {

    private val DEV_MODE = false
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var listenerRegistration: ListenerRegistration? = null

    private fun getCurrentUserId(): String? {
        return if (DEV_MODE) DEV_USER_ID else auth.currentUser?.uid
    }

    fun listenForExpenses(groupId: String, onExpenses: (List<Expense>) -> Unit) {
        db.collection("group")
            .document(groupId)
            .collection("expenses")
            .orderBy("timestamp")
            .addSnapshotListener { value, _ ->
                val expenses = value?.toObjects(Expense::class.java) ?: emptyList()
                onExpenses(expenses)
            }
    }

    fun listenForBillFinalized(groupId: String, onChange: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("group")
            .document(groupId)
            .addSnapshotListener { snapshot, _ ->
                val finalized = snapshot?.getBoolean("billFinalized") ?: false
                onChange(finalized)
            }
    }



    fun addExpense(groupId: String, amount: Double, description: String) {
        val currentUserId = getCurrentUserId() ?: return

        val expenseDoc = db.collection("group")
            .document(groupId)
            .collection("expenses")
            .document()

        val expenseData = hashMapOf(
            "id" to expenseDoc.id,
            "amount" to amount,
            "description" to description,
            "payerId" to currentUserId,
            "timestamp" to System.currentTimeMillis()
        )

        expenseDoc.set(expenseData)
    }

    fun updateExpense(groupId: String, expenseId: String, amount: Double, description: String) {
        val expenseRef = db.collection("group")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)

        expenseRef.update(
            mapOf(
                "amount" to amount,
                "description" to description
            )
        )
    }

    fun deleteExpense(groupId: String, expenseId: String) {
        db.collection("group")
            .document(groupId)
            .collection("expenses")
            .document(expenseId)
            .delete()
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
