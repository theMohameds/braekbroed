package com.example.android_project_onwe.data

import com.example.android_project_onwe.model.PendingOperation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PendingOperationDao {
    private val mutex = Mutex()
    private val memoryList = mutableListOf<PendingOperation>()

    suspend fun insert(op: PendingOperation) = mutex.withLock {
        memoryList.add(op)
    }

    suspend fun get(opId: String): PendingOperation? = mutex.withLock {
        memoryList.firstOrNull { it.opId == opId }
    }

    suspend fun markFailedByPaymentId(paymentId: String) = mutex.withLock {
        memoryList.filter { it.paymentId == paymentId }.forEach { it.failed = true }
    }

    suspend fun getFailure(paymentId: String): Boolean = mutex.withLock {
        memoryList.any { it.paymentId == paymentId && it.failed }
    }

    suspend fun deleteAllFor(paymentId: String) = mutex.withLock {
        memoryList.removeIf { it.paymentId == paymentId }
    }
}
