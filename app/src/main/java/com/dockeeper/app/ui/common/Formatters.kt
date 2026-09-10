package com.dockeeper.app.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es"))
private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("es"))

fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

fun formatDateTime(millis: Long): String = dateTimeFormatter.format(Date(millis))

fun pluralItems(count: Int): String =
    if (count == 1) "1 registro" else "$count registros"

fun pluralAttachments(count: Int): String =
    if (count == 1) "1 archivo" else "$count archivos"
