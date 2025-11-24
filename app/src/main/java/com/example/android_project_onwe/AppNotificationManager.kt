package com.example.android_project_onwe

import android.content.Context
import android.util.Log
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

import java.math.BigDecimal
import java.math.RoundingMode

class AppNotificationManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationRepo = NotificationRepository(context)

    private var isGroupSnapshotFirst = true
    private val expensesFirstMap = mutableMapOf<String, Boolean>()
    private var currentOpenGroupId: String? = null

    private var notificationsEnabled: Boolean = true
    private var userNotificationsListener: ListenerRegistration? = null

    private var groupListener: ListenerRegistration? = null
    private val expenseListeners = mutableMapOf<String, ListenerRegistration>()
    private var reminderListener: ListenerRegistration? = null

    fun setCurrentOpenGroup(groupId: String?) {
        currentOpenGroupId = groupId
    }

    fun start() {
        val currentUserId = auth.currentUser?.uid ?: return

        userNotificationsListener = db.collection("user")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true
            }

        val currentUserRef = db.collection("user").document(currentUserId)

        listenForGroups(currentUserId, currentUserRef)
        listenForExistingGroupsExpenses(currentUserId, currentUserRef)
        listenForReminderEvents(currentUserId)
    }

    private fun shouldNotify(): Boolean = notificationsEnabled

    private fun listenForGroups(currentUserId: String, currentUserRef: DocumentReference) {
        groupListener = db.collection("group")
            .whereArrayContains("members", currentUserRef)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { docChange ->
                        val groupDoc = docChange.document
                        val groupId = groupDoc.id
                        val group = groupDoc.toObject(Group::class.java)
                        val groupName = group?.name ?: "Group"
                        val creatorId = group?.createdBy?.id

                        if (!isGroupSnapshotFirst &&
                            creatorId != null &&
                            creatorId != currentUserId &&
                            shouldNotify()
                        ) {
                            notificationRepo.sendNotification(
                                title = "You've been added to a group",
                                content = groupName
                            )
                        }

                        listenForGroupExpenses(groupId, groupName, currentUserId)
                    }

                isGroupSnapshotFirst = false
            }
    }

    private fun listenForExistingGroupsExpenses(currentUserId: String, currentUserRef: DocumentReference) {
        db.collection("group")
            .whereArrayContains("members", currentUserRef)
            .get()
            .addOnSuccessListener { groupSnap ->
                groupSnap.documents.forEach { groupDoc ->
                    val groupId = groupDoc.id
                    val groupName = groupDoc.getString("name") ?: "Group"
                    listenForGroupExpenses(groupId, groupName, currentUserId)
                }
            }
    }

    private fun listenForGroupExpenses(groupId: String, groupName: String, currentUserId: String) {
        expensesFirstMap[groupId] = true

        val listener = db.collection("group")
            .document(groupId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val isFirstSnapshot = expensesFirstMap[groupId] ?: true
                if (isFirstSnapshot) {
                    expensesFirstMap[groupId] = false
                    return@addSnapshotListener
                }

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val expense = change.document.toObject(Expense::class.java)
                        if (expense.payerId != currentUserId &&
                            groupId != currentOpenGroupId &&
                            shouldNotify()
                        ) {
                            notificationRepo.sendNotification(
                                title = "New expense in $groupName",
                                content = "${expense.description}: ${expense.amount} kr"
                            )
                        }
                    }
            }

        expenseListeners[groupId] = listener
    }

    private fun listenForReminderEvents(currentUserId: String) {
        reminderListener = db.collectionGroup("reminders")
            .whereEqualTo("toUserId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val amount = change.document.getDouble("amount") ?: return@forEach
                        val groupName = change.document.getString("groupName") ?: "Group"

                        if (shouldNotify()) {
                            notificationRepo.sendNotification(
                                title = "You owe in $groupName",
                                content = "You owe %.2f kr".format(amount)
                            )
                        }
                    }
            }
    }


    fun sendPaymentRemindersToFirestore(
        groupId: String,
        groupName: String,
        members: List<DocumentReference>,
        expenses: List<Expense>
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        val total = expenses.sumOf { it.amount }
        val equalShare = if (members.isNotEmpty()) total / members.size else 0.0

        val paidMap = members.associate { it.id to 0.0 }.toMutableMap()
        expenses.forEach { exp ->
            paidMap[exp.payerId] = (paidMap[exp.payerId] ?: 0.0) + exp.amount
        }

        members.forEach { memberRef ->
            val userId = memberRef.id
            if (userId == currentUserId) return@forEach

            val balance = BigDecimal(equalShare - (paidMap[userId] ?: 0.0))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            if (balance > 0.0) {
                val reminder = mapOf(
                    "toUserId" to userId,
                    "amount" to balance,
                    "groupName" to groupName,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("group")
                    .document(groupId)
                    .collection("reminders")
                    .add(reminder)
                    .addOnSuccessListener { println("Reminder added for $userId: $balance kr") }
                    .addOnFailureListener { e -> println("Failed to add reminder: ${e.message}") }
            }
        }
    }
}
