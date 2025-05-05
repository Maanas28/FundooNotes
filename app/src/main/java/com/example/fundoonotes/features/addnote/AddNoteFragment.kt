package com.example.fundoonotes.features.addnote

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.interfaces.AddNoteListener
import com.example.fundoonotes.common.util.interfaces.DrawerToggleListener
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.common.util.managers.PermissionManager
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.databinding.FragmentAddNoteBinding
import com.example.fundoonotes.features.addnote.util.ReminderManagerUI

class AddNoteFragment : Fragment() {

    private var _binding: FragmentAddNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by lazy {
        val parent = parentFragment
        if (parent != null) {
            ViewModelProvider(parent)[NotesViewModel::class.java]
        } else {
            ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        }
    }

    private val reminderManagerUI: ReminderManagerUI by lazy {
        ReminderManagerUI(requireContext(), binding.reminderBadge) {
            selectedReminderTime = it
        }
    }

    private val permissionManager: PermissionManager by lazy {
        PermissionManager(requireContext())
    }

    private var existingNote: Note? = null
    private var selectedReminderTime: Long? = null
    private var addNoteListener: AddNoteListener? = null
    private var isArchivedInitially = false
    private var isArchiveToggled = false

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> }

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
        reminderManagerUI // triggers lazy init
        permissionManager.requestNotificationPermissionIfNeeded(requestNotificationPermissionLauncher)
        populateIfEditing()
    }

    private fun setupListeners() {
        binding.btnBackFAB.setOnClickListener { saveNoteAndReturn() }
        binding.btnReminder.setOnClickListener { reminderManagerUI.showPicker() }
        binding.reminderBadge.setOnClickListener { showReminderOptionsDialog() }

        binding.btnArchive.setOnClickListener {
            if (existingNote != null) {
                existingNote = existingNote?.copy(archived = !existingNote!!.archived)
                viewModel.archiveNote(existingNote!!, onSuccess = {}, onFailure = {})

                val message = if (existingNote!!.archived) "Note archived" else "Note unarchived"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                isArchiveToggled = true
            } else {
                isArchivedInitially = !isArchivedInitially
                val msg = if (isArchivedInitially) "Note will be archived on save" else "Note will not be archived"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            renderLabelPills(note.labels)
        }

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
        labels.forEach { label ->
            val pill = inflater.inflate(R.layout.item_note_label, container, false) as TextView
            pill.text = if (label.length > 20) label.take(18) + "…" else label
            container.addView(pill)
        }
    }

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
            archived = existingNote?.archived ?: isArchivedInitially,
            inBin = existingNote?.inBin ?: false,
            deleted = existingNote?.deleted ?: false
        )

        if (noteId.isEmpty()) {
            viewModel.saveNote(finalNote, requireContext())
        } else {
            val changed = existingNote?.title != finalNote.title ||
                    existingNote?.content != finalNote.content ||
                    existingNote?.reminderTime != finalNote.reminderTime ||
                    isArchiveToggled

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