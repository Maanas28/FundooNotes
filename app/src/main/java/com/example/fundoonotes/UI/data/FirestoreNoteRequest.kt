package com.example.fundoonotes.UI.data

data class FirestoreNoteRequest(
    val fields: FirestoreNoteFields
)

data class FirestoreNoteFields(
    val userId: FirestoreValue,
    val id: FirestoreValue,
    val title: FirestoreValue,
    val content: FirestoreValue,
    val timestamp: FirestoreValue,
    val archived: FirestoreValue,
    val deleted: FirestoreValue,
    val inBin: FirestoreValue,
    val hasReminder: FirestoreValue,
    val reminderTime: FirestoreValue,
    val labels: FirestoreArrayValue
)

data class FirestoreValue(
    val stringValue: String? = null,
    val booleanValue: Boolean? = null,
    val integerValue: String? = null,
    val timestampValue: String? = null
)

data class FirestoreArrayValue(
    val arrayValue: ArrayContent
)

data class ArrayContent(
    val values: List<FirestoreValue>
)
