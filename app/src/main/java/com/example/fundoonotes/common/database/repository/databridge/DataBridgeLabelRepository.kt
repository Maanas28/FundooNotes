package com.example.fundoonotes.common.database.repository.databridge

import android.content.Context
import android.util.Log
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
        val userId = firebaseAuth.getCurrentUserId() ?: return onFailure(Exception("User not logged in"))
        Log.d("DataBridge", "Adding label with name: ${label.name}, userId: $userId")

        if (isOnline(context)) {
            // Online: Update both Firebase and SQLite
            firebaseLabel.addNewLabel(label, {
                Log.d("DataBridge", "Firebase success, now retrieving label details")

                // Retrieve the newly created label to get the proper ID
                firebaseLabel.getLabelByName(label.name, userId, { retrievedLabel ->
                    Log.d("DataBridge", "Retrieved label: id=${retrievedLabel.id}, userId=${retrievedLabel.userId}")

                    // Add to SQLite with the complete information
                    Log.d("DataBridge", "Adding to SQLite: id=${retrievedLabel.id}, userId=${retrievedLabel.userId}")
                    sqliteLabels.addNewLabel(retrievedLabel, {
                        Log.d("DataBridge", "SQLite success")
                        onSuccess()
                    }, { e ->
                        Log.e("DataBridge", "SQLite failure", e)
                        onFailure(e)
                    })
                }, { e ->
                    Log.e("DataBridge", "Failed to retrieve label", e)
                    onFailure(e)
                })
            }, { e ->
                Log.e("DataBridge", "Firebase failure", e)
                onFailure(e)
            })
        } else {
            // Offline: Update SQLite and track the change
            val offlineLabel = Label(
                id = UUID.randomUUID().toString(),
                name = label.name,
                userId = userId
            )

            Log.d("DataBridge", "Offline mode: Adding to SQLite only: id=${offlineLabel.id}, userId=${offlineLabel.userId}")
            sqliteLabels.addNewLabel(offlineLabel, {
                Log.d("DataBridge", "SQLite success (offline mode)")
                trackOfflineOperation("ADD", "LABEL", offlineLabel.id, offlineLabel)
                onSuccess()
            }, { e ->
                Log.e("DataBridge", "SQLite failure (offline mode)", e)
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
                    Log.e("DataBridge", "Firebase update label failed", e)
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
                    Log.e("DataBridge", "Firebase delete label failed", e)
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
                    Log.e("DataBridge", "Firebase toggle label failed", e)
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