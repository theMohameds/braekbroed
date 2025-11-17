package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupChatRepository {

    private val DEV_MODE = true
    private val DEV_USER_ID = "b1aGkqyYBqR9GSIEB1FnbjBMrWt1"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return if (DEV_MODE) DEV_USER_ID else auth.currentUser?.uid
    }

    fun listenForMessages(groupId: String, onMessages: (List<Message>) -> Unit) {
        db.collection("group")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener

                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.apply {
                        id = doc.id
                    }
                } ?: emptyList()

                onMessages(list)
            }
    }

    fun sendMessage(groupId: String, text: String) {
        val senderId = getCurrentUserId() ?: return

        val doc = db.collection("group")
            .document(groupId)
            .collection("messages")
            .document()

        val message = hashMapOf(
            "id" to doc.id,
            "text" to text,
            "senderId" to senderId,
            "timestamp" to System.currentTimeMillis()
        )

        doc.set(message)
    }
}
