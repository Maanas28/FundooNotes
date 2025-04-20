package com.example.fundoonotes.common.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.common.util.adapter.NotesAdapter
import com.example.fundoonotes.common.util.interfaces.EditNoteHandler
import com.example.fundoonotes.common.util.interfaces.SelectionBarListener
import com.example.fundoonotes.common.util.managers.PermissionManager
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentNotesListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesDisplayFragment : Fragment(), SelectionBarListener {

    private var _binding: FragmentNotesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<NotesViewModel>()
    private lateinit var permissionManager: PermissionManager
    private lateinit var adapter: NotesAdapter
    private lateinit var refreshReceiver: BroadcastReceiver
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

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
        binding.notesRecyclerView.layoutManager = StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL
        ).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }

        adapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note ->
                val isSelectionActive = selectionSharedViewModel.getSelection().isNotEmpty()
                if (isSelectionActive) {
                    selectionSharedViewModel.toggleSelection(note)
                } else {
                    handleNoteClick(note)
                }
            },
            onNoteLongClick = { note ->
                if (selectionSharedViewModel.getSelection().isEmpty()) {
                    onSelectionModeEnabled?.invoke()
                }
                selectionSharedViewModel.toggleSelection(note)
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
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.getFilteredNotesFlow(context).collectLatest { notes ->
                        val filtered = viewModel.filterNotesForContext(notes, context)
                        adapter.updateList(filtered)

                        val selectedIds = selectionSharedViewModel.getSelection()
                        adapter.updateSelectedNotes(selectedIds.toSet())
                    }
                }
            }
        }
    }

    private fun observeSelectionChanges() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectionSharedViewModel.selectedNotes.collectLatest { selected ->
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
            viewModel.reverseSyncNotes()

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
        selectionSharedViewModel.clearSelection()
    }
}