package com.example.uniflow

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

data class Task(
    val vrsta: String,
    val naziv: String,
    val boja: Color,
    val datum: LocalDate,
    val vrijeme: String? = null
)
