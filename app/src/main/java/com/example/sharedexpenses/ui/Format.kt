package com.example.sharedexpenses.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** 15000 øre -> "150 DKK", 7550 -> "75,50 DKK". Sign is dropped; screens say "owe" / "owed" instead. */
fun formatDkk(ore: Long): String {
    val value = abs(ore)
    val kroner = value / 100
    val rest = value % 100
    return if (rest == 0L) "$kroner DKK" else "$kroner,${rest.toString().padStart(2, '0')} DKK"
}

/** "150", "150,5", "150.50" -> øre. Returns null if it is not a positive amount. */
fun parseDkk(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    val value = normalized.toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.scale() > 2) return null
    return value.movePointRight(2).toLong()
}

fun formatTime(millis: Long): String = SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(millis))
