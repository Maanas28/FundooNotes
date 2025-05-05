package com.example.fundoonotes.common.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
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
import com.example.fundoonotes.common.viewmodel.NotesViewModel.SyncState
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentNotesListBinding
import com.example.fundoonotes.features.notes.ui.DashboardFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesDisplayFragment : Fragment(), SelectionBarListener {

    private var _binding: FragmentNotesListBinding? = null
    private val binding get() = _binding!!

    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    private val viewModel: NotesViewModel by lazy {
        val parent = parentFragment
        if (parent != null) {
            ViewModelProvider(parent)[NotesViewModel::class.java]
        } else {
            ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        }
    }

    private val permissionManager: PermissionManager by lazy {
        PermissionManager(requireContext())
    }

    private val adapter: NotesAdapter by lazy {
        NotesAdapter(
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
    }

    private val refreshReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val noteId = intent?.getStringExtra("noteId") ?: return
                adapter.refreshSingleExpiredReminder(noteId)
            }
        }
    }

    private var notesContext: NotesGridContext? = null
    private var animateOnLoad: Boolean = false

    var selectionBarListener: SelectionBarListener? = null
    var editNoteHandler: EditNoteHandler? = null
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onSelectionModeEnabled: (() -> Unit)? = null
    var onSelectionModeDisabled: (() -> Unit)? = null

    companion object {
        private const val ARG_CONTEXT = "notes_context"
        private const val ARG_ANIMATE_ON_LOAD = "animate_on_load"

        fun newInstance(context: NotesGridContext, animateOnLoad: Boolean = false): NotesDisplayFragment {
            return NotesDisplayFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTEXT, context)
                    putBoolean(ARG_ANIMATE_ON_LOAD, animateOnLoad)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesContext = BundleCompat.getParcelable(arguments, ARG_CONTEXT, NotesGridContext::class.java)
        animateOnLoad = arguments?.getBoolean(ARG_ANIMATE_ON_LOAD) ?: false
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
        observeSyncState()
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

        binding.notesRecyclerView.adapter = adapter
        binding.notesRecyclerView.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down)
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

                        if (animateOnLoad) {
                            binding.notesRecyclerView.scheduleLayoutAnimation()
                        }

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
                    val selectedSet = selected.toSet()
                    adapter.updateSelectedNotes(selectedSet)

                    onSelectionChanged?.invoke(selected.size)

                    if (selected.isEmpty()) onSelectionModeDisabled?.invoke()
                    else if (selected.size == 1) onSelectionModeEnabled?.invoke()
                }
            }
        }
    }

    private fun observeSyncState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncState.collectLatest { state ->
                    when (state) {
                        is SyncState.Syncing -> {
                            binding.blockerView.visibility = View.VISIBLE
                            binding.progressBar.visibility = View.VISIBLE
                            binding.swipeRefreshLayout.isEnabled = false
                        }
                        is SyncState.Success, is SyncState.Idle -> {
                            binding.blockerView.visibility = View.GONE
                            binding.progressBar.visibility = View.GONE
                            binding.swipeRefreshLayout.isEnabled = true
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                        is SyncState.Failed -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            binding.blockerView.visibility = View.GONE
                            binding.progressBar.visibility = View.GONE
                            binding.swipeRefreshLayout.isEnabled = true
                            binding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun registerRefreshReceiver() {
        val filter = IntentFilter("com.example.fundoonotes.REFRESH_UI")
        permissionManager.registerReceiver(refreshReceiver, filter)
    }

    private fun performReverseSync() {
        if (viewModel.checkInternetConnection()) {
            viewModel.reverseSyncNotes()
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.no_internet_title))
                .setMessage(getString(R.string.no_internet_message))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun handleNoteClick(note: Note) {
        when (notesContext) {
            is NotesGridContext.Bin -> Toast.makeText(requireContext(), getString(R.string.cannot_edit_from_bin), Toast.LENGTH_SHORT).show()
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