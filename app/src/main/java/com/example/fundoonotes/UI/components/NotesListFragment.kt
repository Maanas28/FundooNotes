package com.example.fundoonotes.UI.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SelectionBarListener
import kotlinx.coroutines.launch

class NotesListFragment : Fragment() {

    companion object {
        private const val ARG_CONTEXT = "notes_context"

        fun newInstance(context: NotesGridContext): NotesListFragment {
            return NotesListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTEXT, context)
                }
            }
        }
    }

    private lateinit var recyclerView: RecyclerView
    lateinit var adapter: NotesAdapter
    private lateinit var viewModel: NotesViewModel

    private var notesContext: NotesGridContext? = null
    val selectedNotes = mutableSetOf<Note>()

    var selectionBarListener: SelectionBarListener? = null
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onSelectionModeEnabled: (() -> Unit)? = null
    var onSelectionModeDisabled: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesContext = arguments?.getParcelable(ARG_CONTEXT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.notes_grid_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.notesRecyclerView)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

        adapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note -> if (selectedNotes.isNotEmpty()) toggleSelection(note) },
            onNoteLongClick = { note -> toggleSelection(note) },
            selectedNotes = selectedNotes
        )
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        notesContext?.let { context ->
            viewModel.fetchForContext(context)

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    viewModel.getFlowForContext(context).collect { notes ->
                        val filtered = viewModel.filterNotesForContext(notes, context)
                        adapter.updateList(filtered)
                    }
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
            selectionBarListener?.onSelectionCancelled()
        }

        onSelectionChanged?.invoke(selectedNotes.size)
        adapter.notifyDataSetChanged()
    }

    fun toggleView(isGrid: Boolean) {
        recyclerView.layoutManager = if (isGrid)
            GridLayoutManager(requireContext(), 2)
        else
            LinearLayoutManager(requireContext())
    }

    fun clearSelection() {
        selectedNotes.clear()
        adapter.notifyDataSetChanged()
        onSelectionModeDisabled?.invoke()
        selectionBarListener?.onSelectionCancelled()
    }


}
