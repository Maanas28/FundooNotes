package com.example.fundoonotes.features.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.common.components.NotesDisplayFragment
import com.example.fundoonotes.common.components.SearchBarFragment
import com.example.fundoonotes.common.components.SelectionBar
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.common.util.enums.SelectionBarMode
import com.example.fundoonotes.common.util.interfaces.EditNoteHandler
import com.example.fundoonotes.common.util.interfaces.LabelActionHandler
import com.example.fundoonotes.common.util.interfaces.LabelFragmentHost
import com.example.fundoonotes.common.util.interfaces.MainLayoutToggler
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.SelectionBarListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentArchiveBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel

class ArchiveFragment : Fragment(),
    SelectionBarListener,         // Handles selection bar actions (cancel, etc.)
    ViewToggleListener,           // Handles toggle between grid/list views
    EditNoteHandler,              // Handles note edit click
    SearchListener,               // Handles search query updates
    MainLayoutToggler,            // Controls visibility of fullscreen/editor views
    LabelFragmentHost,            // Hosts label-related fragments
    LabelActionHandler {          // Handles label assignment/removal on selected notes

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    // Reference to note grid fragment
    private lateinit var notesDisplayFragment: NotesDisplayFragment

    // ViewModels shared with activity
    private val notesViewModel by activityViewModels<NotesViewModel>()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false) // Inflate view using ViewBinding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSearchBar()          // Initialize search bar fragment
        setupSelectionBar()       // Initialize selection bar fragment
        setupNotesList()          // Initialize notes display fragment

        // Restore main layout when fullscreen/backstack fragments are removed
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) restoreMainLayout()
        }
    }

    private fun setupSearchBar() {
        // Inject search bar for Archive context
        val searchBarFragment = SearchBarFragment.newInstance("Archive")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerArchive.id, searchBarFragment)
            .commit()
    }

    private fun setupSelectionBar() {
        // Inject selection bar with default mode
        val selectionBar = SelectionBar.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerArchive.id, selectionBar)
            .commit()
    }

    private fun setupNotesList() {
        // Setup grid/list display for archive notes
        notesDisplayFragment = NotesDisplayFragment.newInstance(NotesGridContext.Archive).apply {
            selectionBarListener = this@ArchiveFragment
            setNoteInteractionListener(this@ArchiveFragment)

            // Toggle visibility of search and selection bars
            onSelectionModeEnabled = {
                binding.searchBarContainerArchive.visibility = View.GONE
                binding.selectionBarContainerArchive.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBarContainerArchive.visibility = View.VISIBLE
                binding.selectionBarContainerArchive.visibility = View.GONE
            }
        }

        // Attach notes fragment to container
        childFragmentManager.beginTransaction()
            .replace(binding.archiveNotesGridContainer.id, notesDisplayFragment)
            .commit()
    }

    override fun onSelectionCancelled() {
        // Reset UI when selection is cancelled
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerArchive.visibility = View.VISIBLE
        binding.selectionBarContainerArchive.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        // Toggle between grid and list display
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun onNoteEdit(note: Note) {
        // Launch AddNoteFragment in edit mode (fullscreen)
        binding.fullscreenFragmentContainerArchive.visibility = View.VISIBLE
        val addNoteFragment = AddNoteFragment.newInstance(note)
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerArchive.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
        // Pass search query to ViewModel
        notesViewModel.setSearchQuery(query)
    }

    override fun hideMainLayout() {
        // Hide main grid UI and show fullscreen container
        binding.archiveTopBarContainer.visibility = View.GONE
        binding.archiveNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerArchive.visibility = View.VISIBLE
    }

    override fun restoreMainLayout() {
        // Restore grid UI when fullscreen/editor is closed
        binding.fullscreenFragmentContainerArchive.visibility = View.GONE
        binding.archiveTopBarContainer.visibility = View.VISIBLE
        binding.archiveNotesGridContainer.visibility = View.VISIBLE
    }

    override fun getLabelFragmentContainerId(): Int {
        // Provide container ID for hosting label-related fragments
        return binding.fullscreenFragmentContainerArchive.id
    }

    override fun getLabelFragmentManager() = childFragmentManager // Provide fragment manager for label dialogs

    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        // Handle assigning/removing labels to selected notes
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
}