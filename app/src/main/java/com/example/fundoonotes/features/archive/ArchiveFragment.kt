package com.example.fundoonotes.features.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.example.fundoonotes.features.labels.ui.EditLabelFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import kotlin.getValue

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

    // ViewModels shared with activity
    internal val notesViewModel: NotesViewModel by viewModels()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    // Reference to note grid fragment using lazy initialization
    private val notesDisplayFragment: NotesDisplayFragment by lazy {
        NotesDisplayFragment.newInstance(NotesGridContext.Archive).apply {
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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        childFragmentManager.beginTransaction()
            .replace(binding.archiveNotesGridContainer.id, notesDisplayFragment)
            .commit()
    }

    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
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

    companion object {
        fun newInstance(): ArchiveFragment {
            return ArchiveFragment()
        }
    }
}
