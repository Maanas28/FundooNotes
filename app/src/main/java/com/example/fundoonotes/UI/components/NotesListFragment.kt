package com.example.fundoonotes.UI.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.example.fundoonotes.UI.util.EditNoteHandler
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
    var editNoteHandler: EditNoteHandler? = null
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onSelectionModeEnabled: (() -> Unit)? = null
    var onSelectionModeDisabled: (() -> Unit)? = null

    private lateinit var refreshReceiver: BroadcastReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesContext = arguments?.getParcelable(ARG_CONTEXT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.notes_grid_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val noteId = intent?.getStringExtra("noteId") ?: return
                adapter.refreshSingleExpiredReminder(noteId)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                refreshReceiver,
                IntentFilter("com.example.fundoonotes.REFRESH_UI"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                refreshReceiver,
                IntentFilter("com.example.fundoonotes.REFRESH_UI"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }



        recyclerView = view.findViewById(R.id.notesRecyclerView)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

        adapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note ->
                if (selectedNotes.isNotEmpty()) {
                    toggleSelection(note)
                } else {
                    handleNoteClick(note)
                }
            },
            onNoteLongClick = { note -> toggleSelection(note) },
            selectedNotes = selectedNotes
        )
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        notesContext?.let { context ->
            viewModel.fetchForContext(context)

            // Collect notes normally
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

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(refreshReceiver)
    }



    fun setNoteInteractionListener(listener: EditNoteHandler) {
        this.editNoteHandler = listener
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

    private fun handleNoteClick(note: Note) {
        when (notesContext) {
            is NotesGridContext.Bin -> {
                Toast.makeText(requireContext(), "Note cannot be edited from Bin", Toast.LENGTH_SHORT).show()
            }
            is NotesGridContext.Notes,
            is NotesGridContext.Label,
            is NotesGridContext.Reminder,
            is NotesGridContext.Archive -> {
                editNoteHandler?.onNoteEdit(note)
            }
            null -> {
                Toast.makeText(requireContext(), "Unknown context", Toast.LENGTH_SHORT).show()
            }
        }
    }

}