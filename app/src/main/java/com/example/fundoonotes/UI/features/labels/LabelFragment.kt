package com.example.fundoonotes.UI.features.labels

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
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.EditNoteHandler
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.LabelFragmentHost
import com.example.fundoonotes.UI.util.MainLayoutToggler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SearchListener
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener

class LabelFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    DeleteActionHandler,
    EditNoteHandler,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    private lateinit var searchBar : View
    private lateinit var selectionBar : View
    private lateinit var notesListFragment : NotesListFragment
    private lateinit var viewModel : NotesViewModel
    private lateinit var labelsViewModel : LabelsViewModel
    private lateinit var fullscreenContainer: View


    private var labelName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labelName = arguments?.getString(ARG_LABEL_NAME) ?: ""
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_label, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        searchBar = view.findViewById(R.id.searchBarContainerLabels)
        selectionBar = view.findViewById(R.id.selectionBarContainerLabels)
        fullscreenContainer = view.findViewById(R.id.fullscreenFragmentContainerLabel)
        viewModel = ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        labelsViewModel = ViewModelProvider(requireActivity())[LabelsViewModel::class.java]


        // Inject Search Bar
        val searchBarFragment = SearchBarFragment.newInstance(labelName)
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerLabels, searchBarFragment)
            .commit()

        // Inject Notes List
        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Label(labelName)).apply {
            selectionBarListener = this@LabelFragment
            setNoteInteractionListener(this@LabelFragment)
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
            .replace(R.id.labelsNotesGridContainer, notesListFragment)
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
            .findFragmentById(R.id.selectionBarContainerLabels) as? SelectionBar

        selectionBarFragment?.updateSelectedNoteIds(notesListFragment.selectedNotes.map { it.id })
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

    // NoteInteractionListener implementations
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.newInstance(note)
        fullscreenContainer.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(R.id.fullscreenFragmentContainerLabel, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }

    // --- MainLayoutToggler implementation ---
    // Hides the archive top bar and notes grid for full-screen mode
    override fun hideMainLayout() {
        view?.findViewById<View>(R.id.labelsTopBarContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.labelsTopBarContainer)?.visibility = View.GONE
        view?.findViewById<View>(R.id.fullscreenFragmentContainerLabel)?.apply {
            visibility = View.VISIBLE
        }
    }

    //Restores archive top bar and notes grid after full-screen mode
    override fun restoreMainLayout() {
        view?.findViewById<View>(R.id.fullscreenFragmentContainerLabel)?.visibility = View.GONE
        view?.findViewById<View>(R.id.labelsTopBarContainer)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.labelsNotesGridContainer)?.visibility = View.VISIBLE
    }

    // --- LabelFragmentHost implementation ---
    // Returns the container where the label fragment should be loaded
    override fun getLabelFragmentContainerId(): Int {
        val container = view?.findViewById<View>(R.id.fullscreenFragmentContainerLabel)
        return container?.id ?: R.id.fullscreenFragmentContainerLabel
    }

    //Provides the FragmentManager for managing label fragments
    override fun getLabelFragmentManager() = childFragmentManager


    // Implement the onLabelToggledForNotes callback:
    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        // Archive-specific label toggle - usually similar to your NotesFragment:
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    companion object {
        private const val ARG_LABEL_NAME = "label_name"

        fun newInstance(labelName: String): LabelFragment {
            return LabelFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LABEL_NAME, labelName)
                }
            }
        }
    }
}
