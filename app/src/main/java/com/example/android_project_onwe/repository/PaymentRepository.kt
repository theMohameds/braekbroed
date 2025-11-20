package com.example.android_project_onwe.repository

import com.example.android_project_onwe.model.Payment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PaymentRepository {
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    fun listenForPayments(groupId: String, onPayments: (List<Payment>) -> Unit) {
        listener?.remove()
        listener = db.collection("group")
            .document(groupId)
            .collection("payments")
            .orderBy("timestamp")
            .addSnapshotListener { snap, _ ->
                val list = snap?.toObjects(Payment::class.java) ?: emptyList()
                onPayments(list)
            }
    }

    fun createPayments(groupId: String, payments: List<Payment>, onComplete: (() -> Unit)? = null) {
        val batch = db.batch()
        val col = db.collection("group").document(groupId).collection("payments")

        payments.forEach { p ->
            val doc = if (p.id.isBlank()) col.document() else col.document(p.id)

            val data = hashMapOf(
                "id" to doc.id,
                "fromUser" to p.fromUser,
                "toUser" to p.toUser,
                "amount" to p.amount,
                "isPaid" to p.isPaid,
                "timestamp" to (if (p.timestamp == 0L) System.currentTimeMillis() else p.timestamp)
            )

            batch.set(doc, data)
        }

        batch.commit()
            .addOnSuccessListener { onComplete?.invoke() }
            .addOnFailureListener { onComplete?.invoke() }
    }

    fun setPaymentPaid(groupId: String, paymentId: String, isPaid: Boolean) {
        db.collection("group")
            .document(groupId)
            .collection("payments")
            .document(paymentId)
            .update("isPaid", isPaid)
    }

    fun deleteAllPayments(groupId: String, onComplete: (() -> Unit)? = null) {
        db.collection("group")
            .document(groupId)
            .collection("payments")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { d -> batch.delete(d.reference) }

                batch.commit().addOnSuccessListener { onComplete?.invoke() }
            }
            .addOnFailureListener { onComplete?.invoke() }
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }
}
