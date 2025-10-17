package com.example.voicenotereminder.sched

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.voicenotereminder.App
import com.example.voicenotereminder.R
import com.example.voicenotereminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id <= 0L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(context).reminderDao()
            val reminder = dao.findById(id) ?: return@launch

            // Notification actions
            val snooze = NotificationActionReceiver.pendingIntent(context, id, NotificationActionReceiver.ACTION_SNOOZE)
            val done = NotificationActionReceiver.pendingIntent(context, id, NotificationActionReceiver.ACTION_DONE)
            val dismiss = NotificationActionReceiver.pendingIntent(context, id, NotificationActionReceiver.ACTION_DISMISS)

            val notif = NotificationCompat.Builder(context, App.CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_stat_name) // add a proper small icon in /drawable
                .setContentTitle(reminder.task.ifBlank { context.getString(R.string.reminder) })
                .setContentText(reminder.originalText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.originalText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_snooze, context.getString(R.string.snooze), snooze)
                .addAction(R.drawable.ic_check, context.getString(R.string.done), done)
                .addAction(R.drawable.ic_close, context.getString(R.string.dismiss), dismiss)
                .build()

            NotificationManagerCompat.from(context).notify(id.hashCode(), notif)

            // Reschedule if recurring daily
            if (reminder.isRecurringDaily) {
                val next = reminder.copy(dueAt = reminder.dueAt.plusDays(1))
                dao.insert(next) // new row for next occurrence
                ReminderScheduler.schedule(context, next)
            }
        }
    }
}
