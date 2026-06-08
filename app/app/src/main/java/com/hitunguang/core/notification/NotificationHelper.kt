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

        const val ID_REMINDER = 1001
        const val ID_BUDGET = 1002
        const val ID_REVIEW = 1003
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Pengingat Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mengirimkan pengingat harian untuk mencatat keuangan"
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Peringatan Anggaran",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mengirimkan peringatan jika pengeluaran melebihi batas anggaran"
            }

            val reviewChannel = NotificationChannel(
                CHANNEL_REVIEW,
                "Review Laporan Keuangan",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mengirimkan ringkasan keuangan mingguan dan bulanan"
            }

            manager.createNotificationChannels(listOf(reminderChannel, budgetChannel, reviewChannel))
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
