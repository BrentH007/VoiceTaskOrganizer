package com.example.voicetaskorganizer.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.voicetaskorganizer.data.Reminder
import com.example.voicetaskorganizer.nlp.IntentParser
import com.example.voicetaskorganizer.nlp.LocalLlmParser
import com.example.voicetaskorganizer.nlp.ParsedReminder
import com.example.voicetaskorganizer.nlp.RuleBasedParser
import com.example.voicetaskorganizer.repo.ReminderRepository
import com.example.voicetaskorganizer.sched.ReminderScheduler
import com.example.voicetaskorganizer.speech.SpeechTranscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _transcription = MutableLiveData("")
    val transcription: LiveData<String> = _transcription

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private var transcriber: SpeechTranscriber? = null
    private var repo: ReminderRepository? = null
    private var parsers: List<IntentParser> = emptyList()

    // Expose reminders as StateFlow<List<Reminder>>
    val reminders by lazy {
        kotlinx.coroutines.flow.flowOf<List<Reminder>>(emptyList())
    }

    // We need a context to init repo/flows
    fun startTranscription(context: Context) {
        ensureInit(context)
        transcriber?.start()
    }

    fun stopTranscription() {
        transcriber?.stop()
    }

    fun parse(text: String): ParsedReminder? {
        parsers.forEach { parser ->
            parser.parse(text)?.let { return it }
        }
        return null
    }

    fun saveAndSchedule(parsed: ParsedReminder, context: Context) {
        ensureInit(context)
        val r = Reminder(
            originalText = parsed.originalText,
            task = parsed.task,
            dueAt = parsed.dueAt,
            isRecurringDaily = parsed.isRecurringDaily
        )
        CoroutineScope(Dispatchers.IO).launch {
            val id = repo!!.save(r)
            ReminderScheduler.schedule(context, r.copy(id = id))
        }
    }

    fun setCompleted(id: Long, completed: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            repo?.setCompleted(id, completed)
        }
    }

    private fun ensureInit(context: Context) {
        if (repo == null) {
            repo = ReminderRepository(context.applicationContext)
            // replace dummy flow holder with real flow
            val flow = repo!!.getReminders()
            // Hot StateFlow exposed via property
            val state = flow.stateIn(
                scope = CoroutineScope(Dispatchers.IO),
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )
            // Expose via delegated property
            val field = this::class.java.getDeclaredField("reminders")
            field.isAccessible = true
            field.set(this, state)
        }
        if (transcriber == null) {
            transcriber = SpeechTranscriber(
                context = context,
                liveText = _transcription,
                onStart = { _isRecording.postValue(true) },
                onStop = { _isRecording.postValue(false) },
                onError = { _ ->
                    _isRecording.postValue(false)
                }
            )
        }
        if (parsers.isEmpty()) {
            parsers = listOf(
                LocalLlmParser(context), // may return null; then fallback
                RuleBasedParser()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        transcriber?.release()
    }
}
