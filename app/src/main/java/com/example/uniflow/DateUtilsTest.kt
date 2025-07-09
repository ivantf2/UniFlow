package com.example.uniflow

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
