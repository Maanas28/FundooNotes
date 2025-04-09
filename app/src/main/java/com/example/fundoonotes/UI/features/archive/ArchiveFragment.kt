package com.example.fundoonotes.UI.features.archive

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SearchBarFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.UnarchiveActionHandler
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.EditNoteHandler

class ArchiveFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    DeleteActionHandler,
    UnarchiveActionHandler,
    EditNoteHandler {

    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var notesListFragment: NotesListFragment
    private lateinit var viewModel: NotesViewModel
    private lateinit var fullscreenContainer: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchBar = view.findViewById(R.id.searchBarContainerArchive)
        selectionBar = view.findViewById(R.id.selectionBarContainerArchive)
        fullscreenContainer = view.findViewById(R.id.fullscreenFragmentContainerArchive)
        viewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]

        val searchBarFragment = SearchBarFragment.newInstance("Archive")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerArchive, searchBarFragment)
            .commit()

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

        childFragmentManager.beginTransaction()
            .replace(R.id.archiveNotesGridContainer, notesListFragment)
            .commit()
    }

    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerArchive) as? SelectionBar

        selectionBarFragment?.setSelectedCount(count)
    }

    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        searchBar.visibility = View.VISIBLE
        selectionBar.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesListFragment.toggleView(isGrid)
    }

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

    override fun onUnarchiveSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val unarchived = note.copy(archived = false)
            viewModel.unarchiveNote(unarchived)
        }
        notesListFragment.clearSelection()
    }

    // NoteInteractionListener implementations
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.newInstance(note)
        fullscreenContainer.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(R.id.fullscreenFragmentContainerArchive, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    fun restoreMainNotesLayout() {
        view?.findViewById<View>(R.id.fullscreenFragmentContainerArchive)?.visibility = View.GONE
        view?.findViewById<View>(R.id.archiveTopBarContainer)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.archiveNotesGridContainer)?.visibility = View.VISIBLE
    }
}
