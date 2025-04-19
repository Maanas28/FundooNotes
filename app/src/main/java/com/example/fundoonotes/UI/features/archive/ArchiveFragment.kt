package com.example.fundoonotes.UI.features.archive

import SelectionViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.UI.components.*
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.databinding.FragmentArchiveBinding

class ArchiveFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    EditNoteHandler,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var labelsViewModel: LabelsViewModel
    private val selectionViewModel by activityViewModels<SelectionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notesViewModel = NotesViewModel(requireContext())
        labelsViewModel = LabelsViewModel(requireContext())

        setupSearchBar()
        setupSelectionBar()
        setupNotesList()

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) restoreMainLayout()
        }
    }

    private fun setupSearchBar() {
        val searchBarFragment = SearchBarFragment.newInstance("Archive")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerArchive.id, searchBarFragment)
            .commit()
    }

    private fun setupSelectionBar() {
        val selectionBar = SelectionBar.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerArchive.id, selectionBar)
            .commit()
    }

    private fun setupNotesList() {
        notesDisplayFragment = NotesDisplayFragment.newInstance(NotesGridContext.Archive).apply {
            selectionBarListener = this@ArchiveFragment
            setNoteInteractionListener(this@ArchiveFragment)
            onSelectionModeEnabled = {
                binding.searchBarContainerArchive.visibility = View.GONE
                binding.selectionBarContainerArchive.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBarContainerArchive.visibility = View.VISIBLE
                binding.selectionBarContainerArchive.visibility = View.GONE
            }
        }

        childFragmentManager.beginTransaction()
            .replace(binding.archiveNotesGridContainer.id, notesDisplayFragment)
            .commit()
    }


    override fun onSelectionCancelled() {
        selectionViewModel.clearSelection()
        binding.searchBarContainerArchive.visibility = View.VISIBLE
        binding.selectionBarContainerArchive.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun onNoteEdit(note: Note) {
        binding.fullscreenFragmentContainerArchive.visibility = View.VISIBLE
        val addNoteFragment = AddNoteFragment.newInstance(note)
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerArchive.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
        notesViewModel.setSearchQuery(query)
    }

    override fun hideMainLayout() {
        binding.archiveTopBarContainer.visibility = View.GONE
        binding.archiveNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerArchive.visibility = View.VISIBLE
    }

    override fun restoreMainLayout() {
        binding.fullscreenFragmentContainerArchive.visibility = View.GONE
        binding.archiveTopBarContainer.visibility = View.VISIBLE
        binding.archiveNotesGridContainer.visibility = View.VISIBLE
    }

    override fun getLabelFragmentContainerId(): Int {
        return binding.fullscreenFragmentContainerArchive.id
    }

    override fun getLabelFragmentManager() = childFragmentManager

    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
