package com.example.fundoonotes.UI.features.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.notes.adapter.NotesAdapter
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

class NotesGridFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    lateinit var viewModel: NotesViewModel
    lateinit var adapter: NotesAdapter

    val selectedNotes = mutableSetOf<Note>()

    var onSelectionChanged: ((Int) -> Unit)? = null
    var onSelectionModeEnabled: (() -> Unit)? = null
    var onSelectionModeDisabled: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.notes_grid_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.notesRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note ->
                if (selectedNotes.isNotEmpty()) toggleSelection(note)
            },
            onNoteLongClick = { note ->
                toggleSelection(note)
            },
            selectedNotes = selectedNotes
        )
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        // 👉 Explicitly fetch the active notes
        viewModel.fetchNotes()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notesFlow.collect { notes ->
                    // 🔍 Add these logs
                    android.util.Log.d("NotesGridFragment", "Collected ${notes.size} notes")
                    notes.forEach {
                        android.util.Log.d("NotesGridFragment", "Note title: ${it.title}, content: ${it.content}, archived: ${it.archived}")
                    }

                    adapter.updateList(notes)
                }
            }
        }
    }



    private fun toggleSelection(note: Note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note)
        } else {
            selectedNotes.add(note)
        }

        if (selectedNotes.isNotEmpty()) {
            onSelectionModeEnabled?.invoke()
        } else {
            onSelectionModeDisabled?.invoke()
        }

        updateSelectedCount()
        adapter.notifyDataSetChanged()
    }

    fun toggleView(isGrid: Boolean) {
        recyclerView.layoutManager = if (isGrid)
            GridLayoutManager(requireContext(), 2)
        else
            LinearLayoutManager(requireContext())
    }

    private fun updateSelectedCount() {
        onSelectionChanged?.invoke(selectedNotes.size)
    }

    fun clearSelection() {
        selectedNotes.clear()
        adapter.notifyDataSetChanged()
        onSelectionModeDisabled?.invoke()
    }

}
