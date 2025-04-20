package com.example.fundoonotes.features.addnote

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.interfaces.AddNoteListener
import com.example.fundoonotes.common.util.managers.PermissionManager
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.databinding.FragmentAddNoteBinding
import com.example.fundoonotes.features.addnote.util.ReminderManagerUI

class AddNoteFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    // ViewModel
    private val viewModel by activityViewModels<NotesViewModel>()


    // External components
    private lateinit var reminderManagerUI: ReminderManagerUI
    private lateinit var permissionManager: PermissionManager

    // Note-related data
    private var existingNote: Note? = null
    private var selectedReminderTime: Long? = null
    private var addNoteListener: AddNoteListener? = null

    // Register permission launcher
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("Permission", if (isGranted) "Notification granted" else "Notification denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingNote = arguments?.getParcelable(ARG_NOTE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        setupListeners()
        setupReminderManager()
        setupPermissionManager()
        populateIfEditing()
    }

    /** Initialize all UI event listeners */
    private fun setupListeners() {
        binding.btnBackFAB.setOnClickListener { saveNoteAndReturn() }
        binding.btnReminder.setOnClickListener { reminderManagerUI.showPicker() }
        binding.reminderBadge.setOnClickListener { showReminderOptionsDialog() }
    }

    /** Attach ReminderManager to control reminder logic and badge UI */
    private fun setupReminderManager() {
        reminderManagerUI = ReminderManagerUI(requireContext(), binding.reminderBadge) {
            selectedReminderTime = it
        }
    }

    /** Request permission via centralized manager */
    private fun setupPermissionManager() {
        permissionManager = PermissionManager(requireContext())
        permissionManager.requestNotificationPermissionIfNeeded(
            launcher = requestNotificationPermissionLauncher
        )
    }

    /** Populate fields if editing an existing note */
    private fun populateIfEditing() {
        existingNote?.let { note ->
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)

            if (note.hasReminder && note.reminderTime != null) {
                selectedReminderTime = note.reminderTime
                binding.reminderBadge.apply {
                    visibility = View.VISIBLE
                    text = reminderManagerUI.format(note.reminderTime)
                }
            }
        }

        if (parentFragment is AddNoteListener) {
            addNoteListener = parentFragment as AddNoteListener
        }
    }

    /** Save note or update existing, and call listener callbacks */
    private fun saveNoteAndReturn() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            addNoteListener?.onAddNoteCancelled()
            requireActivity().onBackPressedDispatcher.onBackPressed()
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

        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /** Show dialog for changing/removing existing reminder */
    private fun showReminderOptionsDialog() {
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

    /** Set AddNoteListener for callback support */
    fun setAddNoteListener(listener: AddNoteListener) {
        this.addNoteListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NOTE = "arg_note"

        fun newInstance(note: Note? = null): AddNoteFragment {
            return AddNoteFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
        }
    }
}