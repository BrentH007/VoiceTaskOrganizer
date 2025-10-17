package com.example.voicenotereminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voicenotereminder.databinding.ActivityMainBinding
import com.example.voicenotereminder.nlp.ParsedReminder
import com.example.voicenotereminder.ui.ReminderAdapter
import com.example.voicenotereminder.ui.SpaceItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: viewmodel.MainViewModel by viewModels()
    private val adapter by lazy { ReminderAdapter(
        onToggle = { reminder, checked -> vm.setCompleted(reminder.id, checked) }
    ) }

    private val permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= 33) result[Manifest.permission.POST_NOTIFICATIONS] == true else true
        if (micGranted && notifGranted) {
            startDictation()
        } else {
            Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Recycler
        binding.recyclerReminders.layoutManager = LinearLayoutManager(this)
        binding.recyclerReminders.adapter = adapter
        binding.recyclerReminders.itemAnimator?.apply {
            changeDuration = 180
            addDuration = 150
            removeDuration = 150
        }
        binding.recyclerReminders.addItemDecoration(SpaceItemDecoration(16))

        // Observe
        runBlocking {
            vm.reminders.collectLatest { list -> adapter.submitList(list) }
        }
        vm.transcription.observe(this) { text ->
            binding.transcriptionText.setText(text)
        }
        vm.isRecording.observe(this) { recording ->
            binding.micPulse.isVisible = recording
            binding.btnDictate.text = if (recording) getString(R.string.stop_dictation) else getString(R.string.start_dictation)
        }

        // FAB / Large button
        val pulseAnim = AnimationUtils.loadAnimation(this, R.animator.mtrl_extended_fab_state_list_animator) // subtle
        binding.btnDictate.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
            if (vm.isRecording.value == true) stopDictation() else requestPermissionsAndStart()
        }
        binding.micPulse.startAnimation(pulseAnim)

        // Confirm button to parse and schedule from current transcription
        binding.btnConfirmParse.setOnClickListener {
            val text = binding.transcriptionText.text?.toString().orEmpty().trim()
            if (text.isBlank()) {
                Toast.makeText(this, R.string.no_text_to_parse, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val parsed = vm.parse(text)
            if (parsed == null) {
                Toast.makeText(this, R.string.parse_failed, Toast.LENGTH_LONG).show()
            } else {
                showConfirmDialog(parsed)
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val toRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            toRequest += Manifest.permission.RECORD_AUDIO
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            toRequest += Manifest.permission.POST_NOTIFICATIONS
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        } else {
            startDictation()
        }
    }

    private fun startDictation() {
        vm.startTranscription(this)
    }

    private fun stopDictation() {
        vm.stopTranscription()
    }

    private fun showConfirmDialog(parsed: ParsedReminder) {
        val message = getString(
            R.string.confirm_details_fmt,
            parsed.task.ifBlank { getString(R.string.task_unknown) },
            parsed.dueDateTimeString(this),
            if (parsed.isRecurringDaily) getString(R.string.recurring_daily) else getString(R.string.one_time)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_reminder)
            .setMessage(message)
            .setPositiveButton(R.string.save) { _, _ ->
                vm.saveAndSchedule(parsed, this)
                Toast.makeText(this, R.string.reminder_scheduled, Toast.LENGTH_SHORT).show()
                binding.transcriptionText.setText("")
            }
            .setNeutralButton(R.string.edit) { _, _ ->
                // simple flow: let them edit the editText then press Confirm again
                Toast.makeText(this, R.string.edit_in_textbox, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
