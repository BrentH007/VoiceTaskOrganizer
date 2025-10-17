package com.example.voicenotereminder.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.voicenotereminder.data.Reminder
import com.example.voicenotereminder.databinding.ItemReminderBinding
import java.time.format.DateTimeFormatter

class ReminderAdapter(
    private val onToggle: (Reminder, Boolean) -> Unit
) : ListAdapter<Reminder, ReminderAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder) = oldItem == newItem
    }

    inner class VH(val b: ItemReminderBinding) : RecyclerView.ViewHolder(b.root)

    private val dtfDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dtfTime = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.txtTask.text = item.task
        holder.b.txtOriginal.text = item.originalText
        holder.b.txtWhen.text = "${item.dueAt.toLocalDate().format(dtfDate)} • ${item.dueAt.toLocalTime().format(dtfTime)}" +
                if (item.isRecurringDaily) " • Daily" else ""

        holder.b.chkCompleted.setOnCheckedChangeListener(null)
        holder.b.chkCompleted.isChecked = item.completed
        holder.b.chkCompleted.setOnCheckedChangeListener { _, checked ->
            onToggle(item, checked)
        }
    }
}
