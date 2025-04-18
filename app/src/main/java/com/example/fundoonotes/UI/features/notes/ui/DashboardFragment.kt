package com.example.fundoonotes.UI.features.notes.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.addnote.CanvasNoteFragment
import com.example.fundoonotes.UI.features.addnote.ImageNoteFragment
import com.example.fundoonotes.UI.features.addnote.ListNoteFragement
import com.example.fundoonotes.UI.features.addnote.MicrophoneNoteFragement
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.AddNoteListener
import com.example.fundoonotes.UI.util.ArchiveActionHandler
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.EditNoteHandler
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.LabelFragmentHost
import com.example.fundoonotes.UI.util.LabelSelectionListener
import com.example.fundoonotes.UI.util.MainLayoutToggler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.example.fundoonotes.databinding.FragmentNotesBinding

class DashboardFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    ArchiveActionHandler,
    SelectionBarListener,
    DeleteActionHandler,
    LabelActionHandler,
    LabelSelectionListener,
    LabelFragmentHost,
    EditNoteHandler,
    AddNoteListener,
    MainLayoutToggler {

    // ViewBinding for this fragment
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var labelsViewModel: LabelsViewModel

    // Fragment and UI elements
    internal lateinit var notesListFragment: NotesListFragment
    private lateinit var drawerLayout: DrawerLayout


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)

        Log.d("DashboardFragment", "onViewCreated called again")

        // Initialize drawer layout
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        // Load selection bar fragment inside selectionBarContainer
        childFragmentManager.beginTransaction()
            .replace(R.id.selectionBarContainer, SelectionBar())
            .commit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize ViewModels
        notesViewModel = NotesViewModel(requireContext())
        labelsViewModel = LabelsViewModel(requireContext())

        // Setup UI components
        setupNotesGrid()
        setupBottomNav()
        setupBackPressHandling()
        setupFab()

        // Add a back stack listener to restore the main layout when the label fragment is popped.
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    // Initializes the main notes grid and its interaction listeners
    private fun setupNotesGrid() {
        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Notes).apply {
            selectionBarListener = this@DashboardFragment
            setNoteInteractionListener(this@DashboardFragment)
            onSelectionChanged = { count -> enterSelectionMode(count) }
            onSelectionModeEnabled = {
                binding.searchBar.visibility = View.GONE
                binding.selectionBarContainer.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBar.visibility = View.VISIBLE
                binding.selectionBarContainer.visibility = View.GONE
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.container, notesListFragment)
            .commit()
    }

    // Bottom Nav click - launches full-screen fragments using parentFragmentManager
    private fun setupBottomNav() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_lists -> ListNoteFragement("List Notes")
                R.id.nav_audio -> MicrophoneNoteFragement("Microphone Notes")
                R.id.nav_canvas -> CanvasNoteFragment("Canvas Notes")
                R.id.nav_gallery -> ImageNoteFragment("Image Notes")
                else -> null
            }

            fragment?.let {
                hideMainLayout()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fullScreenContainer, it)
                    .addToBackStack(null)
                    .commit()
            }

            true
        }
    }

    // Handle system back press inside the fragment
    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                binding.fullScreenContainer.isVisible -> {
                    // First pop fragment from back stack
                    parentFragmentManager.popBackStack()
                    // Then restore main layout
                    restoreMainLayout()
                    // Don't call the default back press
                }
                else -> {
                    // Normal back behavior (like closing the app)
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    // Setup Floating Action Button to add new notes
    private fun setupFab() {
        binding.fab.setOnClickListener {
            hideMainLayout()
            val addNoteFragment = AddNoteFragment.newInstance(null).apply {
                setAddNoteListener(this@DashboardFragment)
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fullScreenContainer, addNoteFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // Triggered when selection count changes in NotesListFragment
    fun enterSelectionMode(count: Int) {
        val noteIds = notesListFragment.selectedNotes.map { it.id }

        if (count == 0) {
            onSelectionCancelled()
            return
        }

        (childFragmentManager.findFragmentById(R.id.selectionBarContainer) as? SelectionBar)?.apply {
            updateSelectedNoteIds(noteIds)
            setSelectedCount(count)
        }

        binding.searchBar.visibility = View.GONE
        binding.selectionBarContainer.visibility = View.VISIBLE
    }

    // Cancels selection mode
    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        binding.searchBar.visibility = View.VISIBLE
        binding.selectionBarContainer.visibility = View.GONE
    }

    // Archive selected notes
    override fun onArchiveSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val updatedNote = note.copy(archived = true)
            notesViewModel.archiveNote(updatedNote, onSuccess = {}, onFailure = {})
        }
        notesListFragment.clearSelection()
    }

    // Prompt delete confirmation for selected notes
    override fun onDeleteSelected() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notes")
            .setMessage("Are you sure you want to move selected notes to Bin?")
            .setPositiveButton("Move to Bin") { _, _ ->
                notesListFragment.selectedNotes.forEach { note ->
                    val updated = note.copy(inBin = true)
                    notesViewModel.deleteNote(updated, onSuccess = {}, onFailure = {})
                }
                notesListFragment.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Toggles between grid and list layout
    override fun toggleView(isGrid: Boolean) {
        notesListFragment.toggleView(isGrid)
    }

    // Opens the navigation drawer
    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    // When label dialog is updated with selected labels
    override fun onLabelListUpdated(selectedLabels: List<Label>) {
        Log.d("LabelSelection", "User selected: $selectedLabels")
    }

    override fun getLabelFragmentManager(): FragmentManager = childFragmentManager

    override fun getLabelFragmentContainerId(): Int = R.id.fullScreenContainer

    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    // When a note is being edited
    override fun onNoteEdit(note: Note) {
        openAddOrEditNote(note)
    }

    override fun onNoteAdded(note: Note) {
        Log.d("NotesFragment", "Note added: ${note.title}")
        notesViewModel.refreshAndLoad(NotesGridContext.Notes)

    }

    override fun onNoteUpdated(note: Note) {
        Log.d("NotesFragment", "Note updated: ${note.title}")
        notesViewModel.refreshAndLoad(NotesGridContext.Notes)

    }

    override fun onAddNoteCancelled() {
        Log.d("NotesFragment", "Note addition cancelled")
    }

    private fun openAddOrEditNote(note: Note?) {
        hideMainLayout()
        val addNoteFragment = AddNoteFragment.newInstance(note).apply {
            setAddNoteListener(this@DashboardFragment)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fullScreenContainer, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    // Hide the main layout and show container layout (Add/Edit, Canvas, etc.)
    override fun hideMainLayout() {
        binding.fullScreenContainer.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.fab.visibility = View.GONE
        binding.bottomTabNavigation.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
    }

    // Restore main layout and hide bottom container
    override fun restoreMainLayout() {
        binding.fullScreenContainer.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        binding.bottomTabNavigation.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE

        // Clear the fullScreenContainer to avoid view leaks
        if (binding.fullScreenContainer.childCount > 0) {
            binding.fullScreenContainer.removeAllViews()
        }
    }

    override fun onResume(){
        super.onResume()
        Log.d("DashboardFragment", "onResume called");
        notesViewModel.fetchNotes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}