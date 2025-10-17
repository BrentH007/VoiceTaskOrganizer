package com.example.voicenotereminder.nlp

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Example stub for local LLM intent parsing using a small TFLite model.
 * Replace with a real model and input/output processing as needed.
 * If loading fails, this parser should be skipped by caller.
 */
class LocalLlmParser(private val context: Context) : IntentParser {

    private var interpreter: Interpreter? = null
    private var loadError: Throwable? = null

    init {
        try {
            interpreter = Interpreter(loadModelFile("intent_parser.tflite"))
        } catch (t: Throwable) {
            loadError = t
            Log.w("LocalLlmParser", "Failed to load TFLite model: ${t.message}")
        }
    }

    override fun parse(text: String): ParsedReminder? {
        if (interpreter == null) return null
        // TODO: implement real tokenization, input tensor feeds, and output parsing.
        // For demonstration, we return null so the caller uses the fallback RuleBasedParser.
        return null
    }

    private fun loadModelFile(assetName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetName)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.length)
        }
    }
}
