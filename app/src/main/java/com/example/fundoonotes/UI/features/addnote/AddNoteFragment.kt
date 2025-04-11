package com.example.fundoonotes.UI.features.addnote

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.archive.ArchiveFragment
import com.example.fundoonotes.UI.features.labels.LabelFragment
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.features.reminders.RemindersFragment
import com.example.fundoonotes.UI.util.AddNoteListener
import java.text.SimpleDateFormat
import java.util.*

class AddNoteFragment : Fragment() {

    // UI Elements
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var reminderBadge: TextView

    // ViewModel
    private lateinit var viewModel: NotesViewModel

    // Note data
    private var existingNote: Note? = null
    private var selectedReminderTime: Long? = null

    // Listener for communication with parent
    private var addNoteListener: AddNoteListener? = null

    // Notification permission launcher for Android 13+
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "Notification permission granted")
            } else {
                Log.d("Permission", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingNote = BundleCompat.getParcelable(arguments, ARG_NOTE, Note::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Attach listener if parentFragment implements AddNoteListener
        if (parentFragment is AddNoteListener) {
            addNoteListener = parentFragment as AddNoteListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]

        // Initialize views
        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        reminderBadge = view.findViewById(R.id.reminderBadge)

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackFAB)
        val reminderBtn = view.findViewById<ImageButton>(R.id.btnReminder)

        requestNotificationPermissionIfNeeded()

        // Populate data if editing existing note
        existingNote?.let { note ->
            etTitle.setText(note.title)
            etContent.setText(note.content)

            if (note.hasReminder && note.reminderTime != null) {
                selectedReminderTime = note.reminderTime
                val formatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    .format(note.reminderTime)
                reminderBadge.visibility = View.VISIBLE
                reminderBadge.text = formatted
            }
        }

        // Save and return when back is clicked
        backBtn.setOnClickListener { saveNoteAndReturn() }

        // Show reminder picker dialog
        reminderBtn.setOnClickListener { showReminderPicker() }

        // Show dialog to modify/remove reminder
        reminderBadge.setOnClickListener { showReminderOptionsDialog() }
    }

    /**
     * Saves the note or updates existing one, handles all logic and callback
     */
    private fun saveNoteAndReturn() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        // No content entered; just cancel
        if (title.isEmpty() && content.isEmpty()) {
            when (parentFragment) {
                is NotesFragment -> (parentFragment as NotesFragment).restoreMainLayout()
            }
            addNoteListener?.onAddNoteCancelled()
            parentFragmentManager.popBackStack()
            return
        }

        val noteId = existingNote?.id ?: ""
        val finalNote = Note(
            id = noteId,
            title = title,
            content = content,
            reminderTime = selectedReminderTime,
            hasReminder = selectedReminderTime != null,
            userId = existingNote?.userId ?: "",
            labels = existingNote?.labels ?: emptyList(),
            archived = existingNote?.archived ?: false,
            inBin = existingNote?.inBin ?: false,
            deleted = existingNote?.deleted ?: false
        )

        if (noteId.isEmpty()) {
            // New note
            viewModel.saveNote(finalNote, requireContext())
            addNoteListener?.onNoteAdded(finalNote)
        } else {
            // Check if anything has changed
            val noChange =
                existingNote?.title == title &&
                        existingNote?.content == content &&
                        existingNote?.reminderTime == selectedReminderTime

            if (noChange) {
                Log.d("AddNoteFragment", "No changes detected, skipping update")
                when (val parent = parentFragment) {
                    is NotesFragment -> parent.restoreMainLayout()
                    is ArchiveFragment -> parent.restoreMainLayout()
                    is LabelFragment -> parent.restoreMainLayout()
                    is RemindersFragment -> parent.restoreMainLayout()
                }

                parentFragmentManager.popBackStack()
                return
            }

            // Update note
            viewModel.updateNote(finalNote, existingNote!!, requireContext(), {
                addNoteListener?.onNoteUpdated(finalNote)
            }, {
                Log.e("NoteUpdate", "Failed to update note", it)
            })
        }

        // Restore main layout and close fragment
        when (val parent = parentFragment) {
            is NotesFragment -> parent.restoreMainLayout()
            is ArchiveFragment -> parent.restoreMainLayout()
            is LabelFragment -> parent.restoreMainLayout()
            is RemindersFragment -> parent.restoreMainLayout()
        }

        parentFragmentManager.popBackStack()
    }

    /**
     * Displays date and time picker dialogs for reminder selection
     */
    private fun showReminderPicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                selectedReminderTime = calendar.timeInMillis

                val formatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    .format(calendar.time)

                reminderBadge.apply {
                    visibility = View.VISIBLE
                    text = formatted
                }
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Shows options to change or remove reminder
     */
    private fun showReminderOptionsDialog() {
        val options = arrayOf("Change Reminder", "Remove Reminder")

        AlertDialog.Builder(requireContext())
            .setTitle("Reminder Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showReminderPicker()
                    1 -> {
                        selectedReminderTime = null
                        reminderBadge.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Requests POST_NOTIFICATIONS permission if needed (Android 13+)
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Sets the AddNoteListener used for callbacks
     */
    fun setAddNoteListener(listener: AddNoteListener) {
        this.addNoteListener = listener
    }

    companion object {
        private const val ARG_NOTE = "arg_note"

        /**
         * Creates a new instance of AddNoteFragment with optional existing note
         */
        fun newInstance(note: Note? = null): AddNoteFragment {
            return AddNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
        }
    }
}
