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
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.labels.LabelFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.EditNoteHandler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SearchListener
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener

class RemindersFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    EditNoteHandler ,
    SearchListener{

    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var notesListFragment: NotesListFragment
    private lateinit var fullscreenContainer: View
    private lateinit var viewModel : NotesViewModel



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
    }



    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerReminder) as? SelectionBar

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

    fun restoreMainNotesLayout() {
        view?.findViewById<View>(R.id.fullscreenFragmentContainerReminder)?.visibility = View.GONE
        view?.findViewById<View>(R.id.archiveTopBarContainer)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.archiveNotesGridContainer)?.visibility = View.VISIBLE
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

}
