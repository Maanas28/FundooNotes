package com.example.fundoonotes.common.database.repository.firebase

import android.util.Log
import com.example.fundoonotes.common.data.model.ArrayContent
import com.example.fundoonotes.common.data.model.FirestoreArrayValue
import com.example.fundoonotes.common.data.model.FirestoreNoteFields
import com.example.fundoonotes.common.data.model.FirestoreNoteRequest
import com.example.fundoonotes.common.data.model.FirestoreValue
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.database.repository.interfaces.NotesRepository
import com.example.fundoonotes.common.util.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class FirebaseNotesRepository : NotesRepository {

    private val firestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }
    private val auth = FirebaseAuth.getInstance()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val archivedNotes: StateFlow<List<Note>> = _archivedNotes

    private val _deletedNotes = MutableStateFlow<List<Note>>(emptyList())
    override val binNotes: StateFlow<List<Note>> = _deletedNotes

    private val _reminderNotes = MutableStateFlow<List<Note>>(emptyList())
    override val reminderNotes: StateFlow<List<Note>> = _reminderNotes

    private val _notesByLabel = MutableStateFlow<List<Note>>(emptyList())
    override val notesByLabel: StateFlow<List<Note>> = _notesByLabel

    private val _labelsForNote = MutableStateFlow<List<String>>(emptyList())
    override val labelsForNote : MutableStateFlow<List<String>> = _labelsForNote

    private val _filteredNotes = MutableStateFlow<List<Note>>(emptyList())
    override val filteredNotes: StateFlow<List<Note>> = _filteredNotes

    override fun fetchNotes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("archived", false)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _notes.value = emptyList()
                    return@addSnapshotListener
                }
                _notes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchArchivedNotes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("archived", true)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _archivedNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _archivedNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchBinNotes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _deletedNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _deletedNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    override fun fetchReminderNotes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("hasReminder", true)
            .whereEqualTo("inBin", false)
            .whereEqualTo("deleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _reminderNotes.value = emptyList()
                    return@addSnapshotListener
                }
                _reminderNotes.value = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
            }
    }

    fun formatToIsoString(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(date)
    }


    fun addNoteWithRetrofit(
        note: Note,
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firestoreNote = FirestoreNoteRequest(
            fields = FirestoreNoteFields(
                userId = FirestoreValue(stringValue = note.userId),
                id = FirestoreValue(stringValue = note.id),
                title = FirestoreValue(stringValue = note.title),
                content = FirestoreValue(stringValue = note.content),
                timestamp = FirestoreValue(timestampValue = formatToIsoString(note.timestamp.toDate())),
                archived = FirestoreValue(booleanValue = note.archived),
                deleted = FirestoreValue(booleanValue = note.deleted),
                inBin = FirestoreValue(booleanValue = note.inBin),
                hasReminder = FirestoreValue(booleanValue = note.hasReminder),
                reminderTime = FirestoreValue(integerValue = note.reminderTime?.toString() ?: "0"),
                labels = FirestoreArrayValue(
                    arrayValue = ArrayContent(
                        values = note.labels.map { FirestoreValue(stringValue = it) }
                    )
                )
            )
        )

        RetrofitClient.firestoreApi.addNote(
            authHeader = "Bearer $idToken",
            projectId = "fundoo-kotlin",
            body = firestoreNote
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(Exception("Failed: ${response.errorBody()?.string()}"))
                }
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                onFailure(Exception(t))
            }
        })
    }


    // Refactored addNote using client-generated ID.
    override fun addNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        val noteToAdd = note.copy(
            id = if (note.id.isBlank()) UUID.randomUUID().toString() else note.id,
            userId = userId,
            archived = false,
            deleted = false,
            inBin = false
        )
        firestore.collection("notes")
            .document(noteToAdd.id)
            .set(noteToAdd)
            .addOnSuccessListener {
                Log.d("FirestoreSave", "Saved successfully with ID ${noteToAdd.id}")
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun updateNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update(
                mapOf(
                    "title" to note.title,
                    "content" to note.content,
                    "hasReminder" to note.hasReminder,
                    "reminderTime" to note.reminderTime,
                    "timestamp" to note.timestamp,
                    "labels" to note.labels
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun archiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("archived", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun unarchiveNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("archived", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun deleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("inBin", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun permanentlyDeleteNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun restoreNote(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("notes")
            .document(note.id)
            .update("inBin", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    override fun setReminder(note: Note, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        note.reminderTime?.let { time ->
            firestore.collection("notes")
                .document(note.id)
                .update(
                    mapOf(
                        "hasReminder" to true,
                        "reminderTime" to time
                    )
                )
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } ?: onFailure(Exception("Reminder time is null"))
    }

    suspend fun getAllNotesForUser(userId: String): List<Note>
            = suspendCancellableCoroutine { cont ->
        firestore.collection("notes")
            .whereEqualTo("userId",userId)
            .whereEqualTo("archived", false)
            .whereEqualTo("deleted", false)
            .whereEqualTo("inBin", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val notes = snapshot.documents.mapNotNull {
                    it.toObject(Note::class.java)?.copy(id = it.id)
                }.sortedByDescending { it.timestamp }
                cont.resume(notes, null)
            }
            .addOnFailureListener { exception ->
                cont.resume(emptyList(), null) // or handle differently if needed
            }
    }
}