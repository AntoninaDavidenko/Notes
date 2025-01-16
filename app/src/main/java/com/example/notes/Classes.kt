package com.example.notes

import java.util.Date

data class Note(
    val title: String = "",
    val modifiedAt: Date = Date(),
    val id: String = "",
    val records: List<Record> = emptyList()
)

data class Record(
    val type: String = "text", // "text" або "checkbox"
    val content: String = "",
    var isChecked: Boolean? = null,
    val order: Int = 0,
    val styles: List<TextStyle> = emptyList()
)

enum class TextStyle {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH
}