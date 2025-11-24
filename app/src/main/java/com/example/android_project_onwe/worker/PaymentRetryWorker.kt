package com.example.android_project_onwe.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.android_project_onwe.data.PendingOperationDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class PaymentRetryWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val db = FirebaseFirestore.getInstance()
    private val dao = PendingOperationDao

    override suspend fun doWork(): Result {

        val groupId = inputData.getString("groupId") ?: return Result.failure()
        val paymentId = inputData.getString("paymentId") ?: return Result.failure()
        val desiredState = inputData.getBoolean("desiredIsPaid", false)

        if (runAttemptCount >= 1) {
            dao.markFailedByPaymentId(paymentId)
            showFailureNotification(paymentId)
            return Result.failure()
        }

        return try {
            withTimeout(5000) {
                db.collection("group")
                    .document(groupId)
                    .collection("payments")
                    .document(paymentId)
                    .update("isPaid", desiredState)
                    .await()
            }

            dao.deleteAllFor(paymentId)
            showSuccessNotification(paymentId, desiredState)
            Result.success()

        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun showSuccessNotification(paymentId: String, desiredState: Boolean) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "payment_updates",
                    "Payment Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val msg = if (desiredState)
            "Payment $paymentId synced successfully."
        else
            "Payment $paymentId marked unpaid."

        val notif = NotificationCompat.Builder(ctx, "payment_updates")
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("Payment Synced")
            .setContentText(msg)
            .setAutoCancel(true)
            .build()

        nm.notify(paymentId.hashCode(), notif)
    }

    private fun showFailureNotification(paymentId: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "payment_updates",
                    "Payment Updates",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notif = NotificationCompat.Builder(ctx, "payment_updates")
            .setSmallIcon(android.R.drawable.ic_delete)
            .setContentTitle("Payment Failed")
            .setContentText("Could not sync payment $paymentId.")
            .setAutoCancel(true)
            .build()

        nm.notify(paymentId.hashCode() + 9999, notif)
    }
}
