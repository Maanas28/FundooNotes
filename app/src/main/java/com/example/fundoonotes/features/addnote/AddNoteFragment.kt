package com.example.fundoonotes.features.addnote

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.interfaces.AddNoteListener
import com.example.fundoonotes.common.util.managers.PermissionManager
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.databinding.FragmentAddNoteBinding
import com.example.fundoonotes.features.addnote.util.ReminderManagerUI

class AddNoteFragment : Fragment() {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel to interact with notes
    private val viewModel by activityViewModels<NotesViewModel>()

    // Reminder and permission managers
    private lateinit var reminderManagerUI: ReminderManagerUI
    private lateinit var permissionManager: PermissionManager

    // Existing note (if editing)
    private var existingNote: Note? = null

    // Currently selected reminder timestamp
    private var selectedReminderTime: Long? = null

    // Listener to notify parent fragment of add/edit actions
    private var addNoteListener: AddNoteListener? = null

    // Launcher to request notification permission
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("Permission", if (isGranted) "Notification granted" else "Notification denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the existing note from arguments if present
        existingNote = arguments?.getParcelable(ARG_NOTE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()            // Set up UI event listeners
        setupReminderManager()     // Initialize reminder picker
        setupPermissionManager()   // Handle notification permissions
        populateIfEditing()        // Pre-fill UI if editing an existing note
    }

    private fun setupListeners() {
        binding.btnBackFAB.setOnClickListener { saveNoteAndReturn() }           // Save note on back FAB
        binding.btnReminder.setOnClickListener { reminderManagerUI.showPicker() } // Open reminder picker
        binding.reminderBadge.setOnClickListener { showReminderOptionsDialog() } // Show options to modify/remove reminder
    }

    private fun setupReminderManager() {
        // Initialize the reminder UI logic
        reminderManagerUI = ReminderManagerUI(requireContext(), binding.reminderBadge) {
            selectedReminderTime = it
        }
    }

    private fun setupPermissionManager() {
        // Request notification permission if needed
        permissionManager = PermissionManager(requireContext())
        permissionManager.requestNotificationPermissionIfNeeded(
            launcher = requestNotificationPermissionLauncher
        )
    }

    private fun populateIfEditing() {
        existingNote?.let { note ->
            // Populate title and content if editing
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)

            // Display existing reminder if available
            if (note.hasReminder && note.reminderTime != null) {
                selectedReminderTime = note.reminderTime
                binding.reminderBadge.apply {
                    visibility = View.VISIBLE
                    text = reminderManagerUI.format(note.reminderTime)
                }
            }

            // Render assigned labels
            renderLabelPills(note.labels)
        }

        // Initialize listener if parent implements AddNoteListener
        if (parentFragment is AddNoteListener) {
            addNoteListener = parentFragment as AddNoteListener
        }
    }

    private fun renderLabelPills(labels: List<String>) {
        val container = binding.labelPillsContainer
        container.removeAllViews()

        if (labels.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(requireContext())

        // Create pill UI for each label
        labels.forEach { label ->
            val pill = inflater.inflate(R.layout.item_note_label, container, false) as TextView
            pill.text = if (label.length > 20) label.take(18) + "…" else label
            container.addView(pill)
        }
    }

    private fun saveNoteAndReturn() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        // If no title/content, treat it as cancel
        if (title.isEmpty() && content.isEmpty()) {
            addNoteListener?.onAddNoteCancelled()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        // Build the final note object
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

        // Save or update note based on presence of ID
        if (noteId.isEmpty()) {
            viewModel.saveNote(finalNote, requireContext())
        } else {
            val changed = existingNote?.title != title ||
                    existingNote?.content != content ||
                    existingNote?.reminderTime != selectedReminderTime

            if (changed) {
                viewModel.updateNote(
                    finalNote,
                    existingNote!!,
                    requireContext(),
                    { addNoteListener?.onNoteUpdated(finalNote) },
                    { Log.e("NoteUpdate", "Failed to update", it) }
                )
            }
        }

        requireActivity().onBackPressedDispatcher.onBackPressed() // Navigate back
    }

    private fun showReminderOptionsDialog() {
        // Show dialog with options to modify or remove reminder
        val options = arrayOf("Change Reminder", "Remove Reminder")

        AlertDialog.Builder(requireContext())
            .setTitle("Reminder Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> reminderManagerUI.showPicker()
                    1 -> reminderManagerUI.removeReminder()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun setAddNoteListener(listener: AddNoteListener) {
        this.addNoteListener = listener // Set listener to communicate with parent
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }

    companion object {
        private const val ARG_NOTE = "arg_note"

        // Factory method to create an instance with optional note argument
        fun newInstance(note: Note? = null): AddNoteFragment {
            return AddNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
        }
    }
}
