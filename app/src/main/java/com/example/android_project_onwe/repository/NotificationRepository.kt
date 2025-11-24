package com.example.android_project_onwe.repository

import android.content.Context
import com.example.android_project_onwe.utils.NotificationUtils

class NotificationRepository(private val context: Context) {
    fun sendNotification(title: String, content: String) {
        val builder = NotificationUtils.buildBasicNotification(context, title, content)
        NotificationUtils.showNotification(context, builder)
    }
}