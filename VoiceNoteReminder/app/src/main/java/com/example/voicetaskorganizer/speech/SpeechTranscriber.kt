package com.example.voicetaskorganizer.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.MutableLiveData

class SpeechTranscriber(
    private val context: Context,
    private val liveText: MutableLiveData<String>,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var bufferText: String = ""

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on device.")
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechTranscriber)
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // If offline packages installed, Android can perform on-device; otherwise may fallback
        }
        bufferText = ""
        speechRecognizer?.startListening(intent)
        onStart()
    }

    fun stop() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        onStop()
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        onError("Speech error: $error")
        onStop()
    }

    override fun onResults(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        val text = list.joinToString(" ")
        bufferText = text
        liveText.postValue(bufferText)
        onStop()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!partial.isNullOrBlank()) {
            liveText.postValue(partial)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
