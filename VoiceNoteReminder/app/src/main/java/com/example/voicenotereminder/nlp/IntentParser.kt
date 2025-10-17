package com.example.voicenotereminder.nlp

interface IntentParser {
    /**
     * Parse free-form text into a structured reminder.
     * Return null if cannot parse.
     */
    fun parse(text: String): ParsedReminder?
}
