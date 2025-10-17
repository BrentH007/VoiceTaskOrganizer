package com.example.voicetaskorganizer.sched

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetaskorganizer.repo.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Re-schedule future reminders on boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val repo = ReminderRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val now = LocalDateTime.now()
                repo.allOnce().filter { it.dueAt.isAfter(now) }.forEach {
                    ReminderScheduler.schedule(context, it)
                }
            }
        }
    }
}
