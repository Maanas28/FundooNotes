package com.example.fundoonotes.UI.features.addnote

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddNoteFragment : Fragment() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var reminderBadge: TextView
    private lateinit var viewModel: NotesViewModel
    private var existingNote: Note? = null
    private var selectedReminderTime: Long? = null
    private var addNoteListener: AddNoteListener? = null

    interface AddNoteListener {
        fun onNoteAdded(note: Note)
        fun onNoteUpdated(note: Note)
        fun onAddNoteCancelled()
    }

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
        existingNote = arguments?.getParcelable(ARG_NOTE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is AddNoteListener) {
            addNoteListener = parentFragment as AddNoteListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]

        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        reminderBadge = view.findViewById(R.id.reminderBadge)

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackFAB)
        val reminderBtn = view.findViewById<ImageButton>(R.id.btnReminder)

        requestNotificationPermissionIfNeeded() // ✅ ask on load

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

        backBtn.setOnClickListener {
            saveNoteAndReturn()
        }

        reminderBtn.setOnClickListener {
            showReminderPicker()
        }

        reminderBadge.setOnClickListener {
            showReminderOptionsDialog()
        }
    }

    private fun saveNoteAndReturn() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
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
            // Add new note
            viewModel.saveNote(finalNote, requireContext())
            addNoteListener?.onNoteAdded(finalNote)
        } else {
            // Update existing note
            viewModel.updateNote(finalNote, requireContext(), {
                addNoteListener?.onNoteUpdated(finalNote)
            }, {
                Log.e("NoteUpdate", "Failed to update note", it)
            })

        }

        // Make sure to call restoreMainNotesLayout BEFORE popping the back stack
        if (parentFragment is NotesFragment) {
            (parentFragment as NotesFragment).restoreMainNotesLayout()
        }

        parentFragmentManager.popBackStack()
    }


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

    private fun showReminderOptionsDialog() {
        val options = arrayOf("Change Reminder", "Remove Reminder")

        android.app.AlertDialog.Builder(requireContext())
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

    fun setAddNoteListener(listener: AddNoteListener) {
        this.addNoteListener = listener
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