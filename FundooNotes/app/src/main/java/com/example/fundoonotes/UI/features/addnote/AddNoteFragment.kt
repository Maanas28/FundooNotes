package com.example.fundoonotes.UI.features.addnote

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel


class AddNoteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(requireParentFragment())[NotesViewModel::class.java]

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackFAB)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etContent = view.findViewById<EditText>(R.id.etContent)

        backBtn.setOnClickListener {
            Log.d("AddNoteFragment", "Back button clicked")

            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            Log.d("AddNoteFragment", "Title: $title, Content: $content")

            if (title.isNotEmpty() || content.isNotEmpty()) {
                val note = Note(
                    title = title,
                    content = content,
                    archived = false
                )

                viewModel.saveNote(note,
                    onSuccess = {
                        Log.d("Firestore", "Note saved successfully")
                    },
                    onFailure = {
                        Log.e("Firestore", "Failed to save note", it)
                    }
                )
            }

            parentFragment?.let {
                if (it is NotesFragment) it.restoreMainNotesLayout()
            }
            parentFragmentManager.popBackStack()
        }

    }
}