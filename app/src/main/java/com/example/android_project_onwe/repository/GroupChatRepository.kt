package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class GroupChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
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

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun loadLatestMessages(
        groupId: String,
        limit: Long = 20,
        onLoaded: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("group")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING) // newest first
            .limit(limit)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(Message::class.java) }
                onLoaded(list.reversed())
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }


    fun loadOlderMessages(
        groupId: String,
        oldestTimestamp: Long,
        limit: Long = 20,
        onLoaded: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("group")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(oldestTimestamp)
            .limit(limit)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(Message::class.java) }
                onLoaded(list.sortedBy { it.timestamp })
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }


}
