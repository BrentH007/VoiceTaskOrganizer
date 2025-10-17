package com.example.voicenotereminder.util

import java.time.LocalDateTime

object DateTimeUtils {
    fun nowPlusMinutes(m: Long) = LocalDateTime.now().plusMinutes(m)
}
