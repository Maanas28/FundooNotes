package com.example.fundoonotes.UI.features.reminders

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SearchBarFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.labels.LabelFragment
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.EditNoteHandler
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.LabelFragmentHost
import com.example.fundoonotes.UI.util.MainLayoutToggler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SearchListener
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener

class RemindersFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    EditNoteHandler ,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var notesListFragment: NotesListFragment
    private lateinit var fullscreenContainer: View
    private lateinit var viewModel : NotesViewModel
    private lateinit var labelsViewModel : LabelsViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_reminders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchBar = view.findViewById(R.id.searchBarContainerReminder)
        selectionBar = view.findViewById(R.id.selectionBarContainerReminder)
        fullscreenContainer = view.findViewById(R.id.fullscreenFragmentContainerReminder)
        viewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        labelsViewModel = ViewModelProvider(requireActivity())[LabelsViewModel::class.java]


        val searchBarFragment = SearchBarFragment.newInstance("Reminder")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerReminder, searchBarFragment)
            .commit()

        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Reminder).apply {
            selectionBarListener = this@RemindersFragment
            setNoteInteractionListener(this@RemindersFragment)
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
            .replace(R.id.reminderNotesGridContainer, notesListFragment)
            .commit()

        // Add a back stack listener to restore the main layout when the label fragment is popped.
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }



    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerReminder) as? SelectionBar

        selectionBarFragment?.updateSelectedNoteIds(notesListFragment.selectedNotes.map { it.id })
        selectionBarFragment?.setSelectedCount(count)
    }

    // NoteInteractionListener implementations
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.newInstance(note)
        fullscreenContainer.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(R.id.fullscreenFragmentContainerReminder, addNoteFragment)
            .addToBackStack(null)
            .commit()
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

    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }

    // --- MainLayoutToggler implementation ---
    // Hides the archive top bar and notes grid for full-screen mode
    override fun hideMainLayout() {
        view?.findViewById<View>(R.id.reminderTopBarContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.reminderNotesGridContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.fullscreenFragmentContainerReminder)?.apply {
            visibility = View.VISIBLE
        }
    }

    //Restores archive top bar and notes grid after full-screen mode
    override fun restoreMainLayout() {
        view?.findViewById<View>(R.id.fullscreenFragmentContainerReminder)?.visibility = View.GONE
        view?.findViewById<View>(R.id.reminderTopBarContainer)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.reminderNotesGridContainer)?.visibility = View.VISIBLE
    }

    // --- LabelFragmentHost implementation ---
    // Returns the container where the label fragment should be loaded
    override fun getLabelFragmentContainerId(): Int {
        val container = view?.findViewById<View>(R.id.fullscreenFragmentContainerReminder)
        return container?.id ?: R.id.fullscreenFragmentContainerReminder
    }

    //Provides the FragmentManager for managing label fragments
    override fun getLabelFragmentManager() = childFragmentManager


    // Implement the onLabelToggledForNotes callback:
    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        // Archive-specific label toggle - usually similar to your NotesFragment:
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }
}
