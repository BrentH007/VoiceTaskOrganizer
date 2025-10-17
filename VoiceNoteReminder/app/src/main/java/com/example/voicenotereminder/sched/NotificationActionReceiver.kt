package com.example.voicenotereminder.sched

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.voicenotereminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_ACTION = "action"
        const val ACTION_SNOOZE = "snooze"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_DONE = "done"

        fun pendingIntent(context: Context, id: Long, action: String): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_ACTION, action)
            }
            return PendingIntent.getBroadcast(
                context, (id.toString() + action).hashCode(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val action = intent.getStringExtra(EXTRA_ACTION)
        if (id <= 0L || action.isNullOrBlank()) return

        // Dismiss notification UI
        NotificationManagerCompat.from(context).cancel(id.hashCode())

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(context).reminderDao()
            val reminder = dao.findById(id) ?: return@launch
            when (action) {
                ACTION_SNOOZE -> {
                    val snoozed = reminder.copy(dueAt = reminder.dueAt.plusMinutes(5))
                    dao.insert(snoozed)
                    ReminderScheduler.schedule(context, snoozed)
                }
                ACTION_DONE -> {
                    dao.setCompleted(id, true)
                }
                ACTION_DISMISS -> {
                    // no-op; user dismissed, do not reschedule (unless recurring handled at fire time)
                }
            }
        }
    }
}
