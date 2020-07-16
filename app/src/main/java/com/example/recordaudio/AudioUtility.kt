package com.example.recordaudio

import java.util.*
import java.util.concurrent.TimeUnit

object AudioUtility {
    fun convertMillisToDuration(millis: Long): String {
        val date = Date()

        val realDuration = date.time - millis
        val days: Long = TimeUnit.MILLISECONDS.toDays(realDuration)
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(realDuration)
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(realDuration)
        val hours: Long = TimeUnit.MILLISECONDS.toHours(realDuration)

        return when {
            (seconds <= 60) -> "Just a Minute Ago"
            ((minutes > 1) && (minutes <= 60)) -> "$minutes minutes ago"
            ((hours >= 1) && (hours >= 2)) -> "An Hour ago"
            ((hours >= 1) && (hours <= 24)) -> "$hours Hours ago"
            (days.toInt() == 1) -> "A day ago"
            (days.toInt() > 1) -> "$days days ago"
            else -> "Data N/A"
        }
    }
}