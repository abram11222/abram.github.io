package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "deacons_service_channel"
    const val CHANNEL_NAME = "إشعارات خدمة الشمامسة"
    const val CHANNEL_DESC = "تنبيهات الحضور، الألحان، والامتحانات لخدمة الشمامسة"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (!hasNotificationPermission(context)) {
            false
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setColor(0xFF800020.toInt()) // Burgundy
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        return true
    }

    /**
     * Dispatch an overdue follow-up notification for an individual member.
     * Clicking the notification opens the member's profile directly.
     */
    fun showMemberOverdueNotification(
        context: Context,
        memberId: Long,
        memberName: String,
        overdueReason: String,
        notificationId: Int = (2000 + (memberId % 10000)).toInt()
    ): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "member_profile")
            putExtra("member_id", memberId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val title = "تنبيه متابعة - خدمة الشمامسة 🔔"
        val message = "المخدوم $memberName: $overdueReason"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setColor(0xFF800020.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        return true
    }

    /**
     * Dispatch a summary notification when multiple members have overdue records.
     * Clicking opens the "مخدومين يحتاجون متابعة" screen on the Dashboard.
     */
    fun showSummaryOverdueNotification(
        context: Context,
        overdueMembers: List<MemberOverdueInfo>,
        notificationId: Int = 1000
    ): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context) || overdueMembers.isEmpty()) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "dashboard_followup")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "تنبيه متابعة - خدمة الشمامسة 🔔"
        val mainText = "يوجد ${overdueMembers.size} مخدومين بحاجة لمتابعة (غياب / ألحان / امتحانات)"

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("متابعة مدرسة الشمامسة (${overdueMembers.size} مخدومين)")
            .setSummaryText("متابعة الغياب والألحان والامتحانات")

        overdueMembers.take(5).forEach { item ->
            inboxStyle.addLine("• ${item.member.fullName}: ${item.primaryReason}")
        }
        if (overdueMembers.size > 5) {
            inboxStyle.addLine("• و${overdueMembers.size - 5} مخدومين آخرين...")
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(mainText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setColor(0xFF800020.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        return true
    }

    fun sendTestNotification(context: Context): Boolean {
        return showNotification(
            context = context,
            notificationId = 1001,
            title = "اختبار إشعارات خدمة الشمامسة",
            message = "تم تفعيل نظام التنبيهات وفترات السماح بنجاح! مدرسة القديس اسطفانوس للشمامسة."
        )
    }
}
