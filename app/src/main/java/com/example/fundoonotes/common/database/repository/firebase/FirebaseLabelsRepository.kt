package com.example.fundoonotes.common.database.repository.firebase

import android.util.Log
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.database.repository.interfaces.LabelsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID

class FirebaseLabelsRepository : LabelsRepository {

    private val firestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
    }
    private val auth = FirebaseAuth.getInstance()

    private val _labels = MutableStateFlow<List<Label>>(emptyList())
    override val labels: StateFlow<List<Label>> = _labels


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

    suspend fun getAllLabelsForUser(userId: String): List<Label> = suspendCancellableCoroutine { cont ->
        firestore.collection("labels")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val labels = snapshot.documents.mapNotNull {
                    it.toObject(Label::class.java)?.copy(id = it.id)
                }
                cont.resume(labels, null)
            }
            .addOnFailureListener { cont.resume(emptyList(), null) }
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