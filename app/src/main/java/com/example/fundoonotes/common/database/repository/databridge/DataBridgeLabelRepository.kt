package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.database.repository.interfaces.LabelsRepository
import com.example.fundoonotes.common.util.managers.NetworkUtils.isOnline
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class DataBridgeLabelRepository (
    context: Context,
) : DataBridge<Label>(context), LabelsRepository{


    override val labels: StateFlow<List<Label>> get() = if (isOnline(context)) firebaseLabel.labels else sqliteLabels.labels


    override fun fetchLabels() = if (isOnline(context)) firebaseLabel.fetchLabels() else sqliteLabels.fetchLabels()


    // Add label operation: Refactored to track offline operations
    override fun addNewLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = firebaseAuth.getCurrentUserId() ?: return onFailure(Exception(context.getString(R.string.user_not_found)))

        if (isOnline(context)) {
            // Online: Update both Firebase and SQLite
            firebaseLabel.addNewLabel(label, {

                // Retrieve the newly created label to get the proper ID
                firebaseLabel.getLabelByName(label.name, userId, { retrievedLabel ->

                    // Add to SQLite with the complete information
                    sqliteLabels.addNewLabel(retrievedLabel, {
                        onSuccess()
                    }, { e ->
                        onFailure(e)
                    })
                }, { e ->
                    onFailure(e)
                })
            }, { e ->
                onFailure(e)
            })
        } else {
            // Offline: Update SQLite and track the change
            val offlineLabel = Label(
                id = UUID.randomUUID().toString(),
                name = label.name,
                userId = userId
            )

            sqliteLabels.addNewLabel(offlineLabel, {
                trackOfflineOperation("ADD", "LABEL", offlineLabel.id, offlineLabel)
                onSuccess()
            }, { e ->
                onFailure(e)
            })
        }
    }

    // Update label operation: Refactored to track offline operations
    override fun updateLabel(oldLabel: Label, newLabel: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebaseLabel.updateLabel(oldLabel, newLabel, {
                    // Once Firebase succeeds, update SQLite
                    sqliteLabels.updateLabel(oldLabel, newLabel, onSuccess, onFailure)
                }, { e ->
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                firebaseLabel.updateLabel(oldLabel, newLabel, {
                    trackOfflineOperation("UPDATE", "LABEL", oldLabel.id, newLabel)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Delete label operation: Refactored to track offline operations
    override fun deleteLabel(label: Label, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebaseLabel.deleteLabel(label, {
                    // Once Firebase succeeds, update SQLite
                    sqliteLabels.deleteLabel(label, onSuccess, onFailure)
                }, { e ->
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqliteLabels.deleteLabel(label, {
                    trackOfflineOperation("DELETE", "LABEL", label.id, label)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Toggle label operation: Refactored to track offline operations
    override fun toggleLabelForNotes(
        label: Label,
        isChecked: Boolean,
        noteIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            if (isOnline(context)) {
                // Online: Update both Firebase and SQLite
                firebaseLabel.toggleLabelForNotes(label, isChecked, noteIds, {
                    // Once Firebase succeeds, update SQLite
                    sqliteLabels.toggleLabelForNotes(label, isChecked, noteIds, onSuccess, onFailure)
                }, { e ->
                    onFailure(e)
                })
            } else {
                // Offline: Update SQLite and track the change
                sqliteLabels.toggleLabelForNotes(label, isChecked, noteIds, {
                    // Create a data object to store the toggle operation details
                    val toggleData = mapOf(
                        "labelId" to label.id,
                        "isChecked" to isChecked,
                        "noteIds" to noteIds
                    )
                    trackOfflineOperation("TOGGLE_LABEL", "LABEL_NOTE", label.id, toggleData)
                    onSuccess()
                }, onFailure)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

}