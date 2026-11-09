package com.hitunguang.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hitunguang.MainActivity
import com.hitunguang.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_REMINDER = "reminder_channel"
        const val CHANNEL_BUDGET = "budget_channel"
        const val CHANNEL_REVIEW = "review_channel"
        const val CHANNEL_RECURRING = "recurring_channel"

        const val ID_REMINDER = 1001
        const val ID_BUDGET = 1002
        const val ID_REVIEW = 1003
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.notif_channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_reminder_desc)
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                context.getString(R.string.notif_channel_budget_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_budget_desc)
            }

            val reviewChannel = NotificationChannel(
                CHANNEL_REVIEW,
                context.getString(R.string.notif_channel_review_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_review_desc)
            }

            val recurringChannel = NotificationChannel(
                CHANNEL_RECURRING,
                context.getString(R.string.notif_channel_recurring_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_recurring_desc)
            }

            manager.createNotificationChannels(listOf(reminderChannel, budgetChannel, reviewChannel, recurringChannel))
        }
    }

    fun showNotification(channelId: String, notificationId: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(
                if (channelId == CHANNEL_BUDGET) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
