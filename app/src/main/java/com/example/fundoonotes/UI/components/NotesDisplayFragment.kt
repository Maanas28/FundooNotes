package com.example.fundoonotes.UI.components

import SelectionViewModel
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.*
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.labels.ApplyLabelToNoteFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.databinding.FragmentNotesListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesDisplayFragment : Fragment(), SelectionBarListener {

    private var _binding: FragmentNotesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotesViewModel
    private lateinit var permissionManager: PermissionManager
    private lateinit var adapter: NotesAdapter
    private lateinit var refreshReceiver: BroadcastReceiver
    private val selectionViewModel by activityViewModels<SelectionViewModel>()

    private var notesContext: NotesGridContext? = null

    var selectionBarListener: SelectionBarListener? = null
    var editNoteHandler: EditNoteHandler? = null
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onSelectionModeEnabled: (() -> Unit)? = null
    var onSelectionModeDisabled: (() -> Unit)? = null

    companion object {
        private const val ARG_CONTEXT = "notes_context"

        fun newInstance(context: NotesGridContext): NotesDisplayFragment {
            return NotesDisplayFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTEXT, context)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesContext = BundleCompat.getParcelable(arguments, ARG_CONTEXT, NotesGridContext::class.java)
        permissionManager = PermissionManager(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = NotesViewModel(requireContext())

        setupRecyclerView()
        setupSwipeToRefresh()
        observeNotes()
        observeSelectionChanges()
        registerRefreshReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(refreshReceiver)
        _binding = null
    }

    override fun onSelectionCancelled() {
        clearSelection()
    }

    private fun setupRecyclerView() {
        binding.notesRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

        adapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note ->
                val isSelectionActive = selectionViewModel.getSelection().isNotEmpty()
                if (isSelectionActive) {
                    selectionViewModel.toggleSelection(note)
                } else {
                    handleNoteClick(note)
                }
            },
            onNoteLongClick = { note ->
                if (selectionViewModel.getSelection().isEmpty()) {
                    onSelectionModeEnabled?.invoke()
                }
                selectionViewModel.toggleSelection(note)
            }
        )


        binding.notesRecyclerView.adapter = adapter
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            performReverseSync()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.black,
            R.color.blue
        )
    }

    private fun observeNotes() {
        notesContext?.let { context ->
            viewModel.fetchForContext(context)

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                    viewModel.getFilteredNotesFlow(context).collectLatest { notes ->
                        val filtered = viewModel.filterNotesForContext(notes, context)
                        adapter.updateList(filtered)

                        val selectedIds = selectionViewModel.getSelection()
                        adapter.updateSelectedNotes(selectedIds.toSet())
                    }
                }
            }
        }
    }

    private fun observeSelectionChanges() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                selectionViewModel.selectedNotes.collectLatest { selected ->
                    val allNotes = adapter.getCurrentNotes()
                    val selectedSet = selected.toSet()
                    adapter.updateSelectedNotes(selectedSet)

                    onSelectionChanged?.invoke(selected.size)

                    if (selected.isEmpty()) onSelectionModeDisabled?.invoke()
                    else if (selected.size == 1) onSelectionModeEnabled?.invoke()
                }
            }
        }
    }

    private fun registerRefreshReceiver() {
        refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val noteId = intent?.getStringExtra("noteId") ?: return
                adapter.refreshSingleExpiredReminder(noteId)
            }
        }

        val filter = IntentFilter("com.example.fundoonotes.REFRESH_UI")
        permissionManager.registerReceiver(refreshReceiver, filter)
    }

    private fun performReverseSync() {
        if (viewModel.checkInternetConnection()) {
            viewModel.reverseSyncNotes(requireContext())

            binding.swipeRefreshLayout.postDelayed({
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), "Sync completed", Toast.LENGTH_SHORT).show()
                }
            }, 3000)

        } else {
            binding.swipeRefreshLayout.isRefreshing = false

            AlertDialog.Builder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Please connect to the internet to sync your notes")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun handleNoteClick(note: Note) {
        when (notesContext) {
            is NotesGridContext.Bin -> Toast.makeText(requireContext(), "Note cannot be edited from Bin", Toast.LENGTH_SHORT).show()
            else -> editNoteHandler?.onNoteEdit(note)
        }
    }

    private fun getSelectedNotes(selectedIds: List<String>): Set<Note> {
        return viewModel.getNotesByIds(selectedIds).toSet()
    }

    fun setNoteInteractionListener(listener: EditNoteHandler) {
        this.editNoteHandler = listener
    }

    fun toggleView(isGrid: Boolean) {
        binding.notesRecyclerView.layoutManager = if (isGrid)
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        else
            LinearLayoutManager(requireContext())
    }

    fun clearSelection() {
        selectionViewModel.clearSelection()
    }
}