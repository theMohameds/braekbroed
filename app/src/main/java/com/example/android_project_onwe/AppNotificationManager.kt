package com.example.android_project_onwe

import android.content.Context
import com.example.android_project_onwe.model.Expense
import com.example.android_project_onwe.model.Group
import com.example.android_project_onwe.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class AppNotificationManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationRepo = NotificationRepository(context)
    private var isGroupSnapshotFirst = true
    private val expensesFirstMap = mutableMapOf<String, Boolean>()
    private var currentOpenGroupId: String? = null

    fun setCurrentOpenGroup(groupId: String?) {
        currentOpenGroupId = groupId
    }
    fun start() {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserRef = db.collection("user").document(currentUserId)

        listenForGroups(currentUserId, currentUserRef)
        listenForExistingGroupsExpenses(currentUserId, currentUserRef)
    }

    private fun listenForGroups(currentUserId: String, currentUserRef: Any) {
        db.collection("group")
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

                        // Notify for new groups (skip first snapshot)
                        if (!isGroupSnapshotFirst && creatorId != null && creatorId != currentUserId) {
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

    private fun listenForExistingGroupsExpenses(currentUserId: String, currentUserRef: Any) {
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

        db.collection("group")
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
                            groupId != currentOpenGroupId  // <-- skip if group is open
                        ) {
                            notificationRepo.sendNotification(
                                title = "New expense in $groupName",
                                content = "${expense.description}: ${expense.amount} kr"
                            )
                        }
                    }
            }
    }
}
