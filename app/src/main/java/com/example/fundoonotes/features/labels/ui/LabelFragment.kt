package com.example.fundoonotes.features.labels.ui

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
import com.example.fundoonotes.databinding.FragmentLabelBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel

class LabelFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    EditNoteHandler,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    private var _binding: FragmentLabelBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private val notesViewModel by activityViewModels<NotesViewModel>()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    private var labelName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labelName = arguments?.getString(ARG_LABEL_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabelBinding.inflate(inflater, container, false)
        setupSearchBar()
        setupSelectionBar()
        setupNotesList()
        setupBackStackListener()
        return binding.root
    }

    /** Adds a SearchBarFragment with the label name as the title */
    private fun setupSearchBar() {
        val searchBarFragment = SearchBarFragment.Companion.newInstance(labelName)
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerLabels.id, searchBarFragment)
            .commit()
    }

    /** Adds a shared SelectionBar in DEFAULT mode */
    private fun setupSelectionBar() {
        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerLabels.id, selectionBar)
            .commit()
    }

    /** Adds a NotesListFragment scoped to this label context */
    private fun setupNotesList() {
        notesDisplayFragment = NotesDisplayFragment.Companion.newInstance(NotesGridContext.Label(labelName)).apply {
            selectionBarListener = this@LabelFragment
            setNoteInteractionListener(this@LabelFragment)
            onSelectionModeEnabled = {
                binding.searchBarContainerLabels.visibility = View.GONE
                binding.selectionBarContainerLabels.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBarContainerLabels.visibility = View.VISIBLE
                binding.selectionBarContainerLabels.visibility = View.GONE
            }
        }

        childFragmentManager.beginTransaction()
            .replace(binding.labelsNotesGridContainer.id, notesDisplayFragment)
            .commit()
    }

    /** Restores layout when the label selection fragment is popped */
    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    /** Called when selection is cancelled from SelectionBar */
    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerLabels.visibility = View.VISIBLE
        binding.selectionBarContainerLabels.visibility = View.GONE
    }

    /** Switches between grid and list layout in NotesListFragment */
    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    /** Opens AddNoteFragment in full screen for editing a note */
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.Companion.newInstance(note)
        binding.fullscreenFragmentContainerLabel.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerLabel.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    /** Updates search query in NotesViewModel */
    override fun onSearchQueryChanged(query: String) {
        notesViewModel.setSearchQuery(query)
    }

    /** Hides main layout when a full-screen view is opened */
    override fun hideMainLayout() {
        binding.labelsTopBarContainer.visibility = View.GONE
        binding.labelsNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerLabel.visibility = View.VISIBLE
    }

    /** Restores main layout after full-screen view is closed */
    override fun restoreMainLayout() {
        binding.fullscreenFragmentContainerLabel.visibility = View.GONE
        binding.labelsTopBarContainer.visibility = View.VISIBLE
        binding.labelsNotesGridContainer.visibility = View.VISIBLE
    }

    /** Returns the container ID for loading label selection fragments */
    override fun getLabelFragmentContainerId(): Int {
        return binding.fullscreenFragmentContainerLabel.id
    }

    /** Provides the childFragmentManager to load label fragments */
    override fun getLabelFragmentManager() = childFragmentManager

    /** Applies label toggling to selected notes */
    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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