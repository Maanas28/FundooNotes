package com.example.fundoonotes.UI.features.addnote

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.fundoonotes.UI.util.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddNoteFragment : Fragment() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var reminderBadge: TextView
    private lateinit var viewModel: NotesViewModel

    private var selectedReminderTime: Long? = null

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("Permission", "Notification permission granted")
            } else {
                Log.d("Permission", "Notification permission denied")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_note, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireParentFragment())[NotesViewModel::class.java]

        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        reminderBadge = view.findViewById(R.id.reminderBadge)

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackFAB)
        val reminderBtn = view.findViewById<ImageButton>(R.id.btnReminder)

        requestNotificationPermissionIfNeeded() // ✅ ask on load

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

        if (title.isNotEmpty() || content.isNotEmpty()) {
            val note = Note(
                title = title,
                content = content,
                hasReminder = selectedReminderTime != null,
                reminderTime = selectedReminderTime
            )

            viewModel.saveNote(note, requireContext())

        }

        (parentFragment as? NotesFragment)?.restoreMainNotesLayout()
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
}
