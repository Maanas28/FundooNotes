package com.example.fundoonotes.UI.features.archive

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SearchBarFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel

class ArchiveFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    DeleteActionHandler,
    UnarchiveActionHandler,
    EditNoteHandler,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    // UI Elements
    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var fullscreenContainer: View

    // Note list fragment and ViewModel
    private lateinit var notesListFragment: NotesListFragment
    private lateinit var viewModel: NotesViewModel
    private lateinit var labelsViewModel : LabelsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        searchBar = view.findViewById(R.id.searchBarContainerArchive)
        selectionBar = view.findViewById(R.id.selectionBarContainerArchive)
        fullscreenContainer = view.findViewById(R.id.fullscreenFragmentContainerArchive)

        // Shared ViewModel
        viewModel = NotesViewModel(requireContext())

        labelsViewModel = LabelsViewModel(requireContext())


        // Add SearchBarFragment for Archive screen
        val searchBarFragment = SearchBarFragment.newInstance("Archive")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerArchive, searchBarFragment)
            .commit()

        // Initialize NotesListFragment for Archive context
        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Archive).apply {
            selectionBarListener = this@ArchiveFragment
            setNoteInteractionListener(this@ArchiveFragment)
            onSelectionChanged = { count -> updateSelectionBar(count) }
            onSelectionModeEnabled = {
                searchBar.visibility = View.GONE
                selectionBar.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                searchBar.visibility = View.VISIBLE
                selectionBar.visibility = View.GONE
            }
        }

        // Add notes list fragment to the screen
        childFragmentManager.beginTransaction()
            .replace(R.id.archiveNotesGridContainer, notesListFragment)
            .commit()

        // Add a back stack listener to restore the main layout when the label fragment is popped.
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    // When selection changes, update both count and selected IDs.
    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerArchive) as? SelectionBar
        // Update both the count and the selected note IDs.
        selectionBarFragment?.updateSelectedNoteIds(notesListFragment.selectedNotes.map { it.id })
        selectionBarFragment?.setSelectedCount(count)
    }

    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        searchBar.visibility = View.VISIBLE
        selectionBar.visibility = View.GONE
    }


    // Toggle between grid and list view in notes list
    override fun toggleView(isGrid: Boolean) {
        notesListFragment.toggleView(isGrid)
    }

     // Show confirmation dialog and move selected notes to bin
    override fun onDeleteSelected() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notes")
            .setMessage("Are you sure you want to move selected notes to Bin?")
            .setPositiveButton("Move to Bin") { _, _ ->
                notesListFragment.selectedNotes.forEach { note ->
                    val updated = note.copy(inBin = true)
                    viewModel.deleteNote(updated)
                }
                notesListFragment.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Unarchive selected notes and removes them from archive list
    override fun onUnarchiveSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val unarchived = note.copy(archived = false)
            viewModel.unarchiveNote(unarchived)
        }
        notesListFragment.clearSelection()
    }

    // Opens AddNoteFragment in full screen to edit the selected note
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.newInstance(note)
        fullscreenContainer.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(R.id.fullscreenFragmentContainerArchive, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }


     // Passes updated search query to ViewModel
    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }

    // --- MainLayoutToggler implementation ---
    // Hides the archive top bar and notes grid for full-screen mode
    override fun hideMainLayout() {
        view?.findViewById<View>(R.id.archiveTopBarContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.archiveNotesGridContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.fullscreenFragmentContainerArchive)?.apply {
            visibility = View.VISIBLE
        }
    }

    //Restores archive top bar and notes grid after full-screen mode
    override fun restoreMainLayout() {
        view?.findViewById<View>(R.id.fullscreenFragmentContainerArchive)?.visibility = View.GONE
        view?.findViewById<View>(R.id.archiveTopBarContainer)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.archiveNotesGridContainer)?.visibility = View.VISIBLE
    }

    // --- LabelFragmentHost implementation ---
    // Returns the container where the label fragment should be loaded
    override fun getLabelFragmentContainerId(): Int {
        val container = view?.findViewById<View>(R.id.fullscreenFragmentContainerArchive)
        return container?.id ?: R.id.fullscreenFragmentContainerArchive
    }

    //Provides the FragmentManager for managing label fragments
    override fun getLabelFragmentManager() = childFragmentManager


    // Implement the onLabelToggledForNotes callback:
    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        // Archive-specific label toggle - usually similar to your NotesFragment:
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

}
