package com.example.fundoonotes.UI.data.repository

import android.util.Log
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import java.util.UUID

class FirebaseNotesRepository : NotesRepository {

    // Configure Firestore to disable offline persistence.
    private val firestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
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

    private val _labels = MutableStateFlow<List<Label>>(emptyList())
    override val labels: StateFlow<List<Label>> = _labels

    private val _notesByLabel = MutableStateFlow<List<Note>>(emptyList())
    override val notesByLabel: StateFlow<List<Note>> = _notesByLabel

    private val _labelsForNote = MutableStateFlow<List<String>>(emptyList())
    override val labelsForNote : MutableStateFlow<List<String>> = _labelsForNote

    private val _accountDetails = MutableStateFlow<User?>(null)
    override val accountDetails: StateFlow<User?> get() = _accountDetails

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

    override fun fetchLabels() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("labels")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _labels.value = emptyList()
                    return@addSnapshotListener
                }

                val labelList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Label::class.java)?.copy(id = doc.id)
                }

                Log.d("FirebaseDebug", "Fetched labels for $userId: $labelList")
                _labels.value = labelList
            }
    }

    override fun fetchAccountDetails() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    _accountDetails.value = null
                    return@addSnapshotListener
                }
                // Assume only one document per userId
                val user = snapshot.documents.first().toObject(User::class.java)
                _accountDetails.value = user
            }
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

    // Refactored addNewLabel using client-generated ID.
    override fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        val labelToAdd = label.copy(
            id = if (label.id.isBlank()) UUID.randomUUID().toString() else label.id,
            userId = userId
        )
        firestore.collection("labels")
            .document(labelToAdd.id)
            .set(labelToAdd)
            .addOnSuccessListener {
                Log.d("Firestore Label", "Saved successfully with ID ${labelToAdd.id}")
                onSuccess()
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        firestore.collection("labels")
            .document(oldLabel.id)
            .get()
            .addOnSuccessListener { doc ->
                val label = doc.toObject(Label::class.java)
                if (label?.userId != userId) {
                    return@addOnSuccessListener onFailure(Exception("Unauthorized operation"))
                }
                doc.reference.update("name", newLabel.name)
                    .addOnSuccessListener {
                        firestore.collection("notes")
                            .whereEqualTo("userId", userId)
                            .whereArrayContains("labels", oldLabel.name)
                            .get()
                            .addOnSuccessListener { notes ->
                                for (noteDoc in notes.documents) {
                                    val labelList = noteDoc.get("labels") as? List<*> ?: continue
                                    val updatedLabels = labelList.map {
                                        if (it == oldLabel.name) newLabel.name else it
                                    }
                                    noteDoc.reference.update("labels", updatedLabels)
                                }
                                onSuccess()
                            }
                            .addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun deleteLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        firestore.collection("labels")
            .document(label.id)
            .get()
            .addOnSuccessListener { doc ->
                val labelDoc = doc.toObject(Label::class.java)
                if (labelDoc?.userId != userId) {
                    return@addOnSuccessListener onFailure(Exception("Unauthorized operation"))
                }
                doc.reference.delete()
                    .addOnSuccessListener {
                        firestore.collection("notes")
                            .whereEqualTo("userId", userId)
                            .whereArrayContains("labels", label.name)
                            .get()
                            .addOnSuccessListener { notes ->
                                for (noteDoc in notes.documents) {
                                    val labelList = noteDoc.get("labels") as? List<*> ?: continue
                                    val updatedLabels = labelList.filterNot { it == label.name }
                                    noteDoc.reference.update("labels", updatedLabels)
                                }
                                onSuccess()
                            }
                            .addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    override fun toggleLabelForNotes(
        label: Label,
        isChecked: Boolean,
        noteIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        val batch = firestore.batch()
        val notesRef = firestore.collection("notes")

        notesRef.whereIn(FieldPath.documentId(), noteIds)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("toggleLabelForNotes", "Found ${snapshot.size()} docs for IDs = $noteIds")
                snapshot.documents.forEach { doc ->
                    val noteLabels = (doc.get("labels") as? MutableList<String>) ?: mutableListOf()
                    val updatedLabels = if (isChecked) {
                        if (!noteLabels.contains(label.name)) noteLabels + label.name else noteLabels
                    } else {
                        noteLabels.filterNot { it == label.name }
                    }
                    batch.update(doc.reference, "labels", updatedLabels)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getLabelByName(name: String, userId: String, onSuccess: (Label) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("labels")
            .whereEqualTo("name", name)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val label = Label(
                        id = document.getString("id") ?: "",
                        name = document.getString("name") ?: "",
                        userId = document.getString("userId") ?: ""
                    )
                    onSuccess(label)
                } else {
                    onFailure(Exception("Label not found"))
                }
            }
            .addOnFailureListener(onFailure)
    }
}
