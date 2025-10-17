package com.example.voicenotereminder.sched

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.voicenotereminder.data.Reminder
import java.time.ZoneId

object ReminderScheduler {
    fun schedule(context: Context, reminder: Reminder) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = reminder.dueAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", reminder.id)
        }
        val pi = PendingIntent.getBroadcast(
            context, reminder.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Doze aware exact alarm
        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    fun cancel(context: Context, reminderId: Long) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", reminderId)
        }
        val pi = PendingIntent.getBroadcast(
            context, reminderId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mgr.cancel(pi)
    }
}
