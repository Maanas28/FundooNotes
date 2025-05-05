package com.example.fundoonotes.features.labels.ui

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
import com.example.fundoonotes.databinding.FragmentLabelBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import kotlin.getValue

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

    private val notesDisplayFragment: NotesDisplayFragment by lazy {
        NotesDisplayFragment.Companion.newInstance(NotesGridContext.Label(labelName)).apply {
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
    }

    internal val notesViewModel: NotesViewModel by viewModels()
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

    private fun setupSearchBar() {
        val searchBarFragment = SearchBarFragment.Companion.newInstance(labelName)
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerLabels.id, searchBarFragment)
            .commit()
    }

    private fun setupSelectionBar() {
        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerLabels.id, selectionBar)
            .commit()
    }

    private fun setupNotesList() {
        childFragmentManager.beginTransaction()
            .replace(binding.labelsNotesGridContainer.id, notesDisplayFragment)
            .commit()
    }

    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerLabels.visibility = View.VISIBLE
        binding.selectionBarContainerLabels.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.Companion.newInstance(note)
        binding.fullscreenFragmentContainerLabel.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerLabel.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
        notesViewModel.setSearchQuery(query)
    }

    override fun hideMainLayout() {
        binding.labelsTopBarContainer.visibility = View.GONE
        binding.labelsNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerLabel.visibility = View.VISIBLE
    }

    override fun restoreMainLayout() {
        binding.fullscreenFragmentContainerLabel.visibility = View.GONE
        binding.labelsTopBarContainer.visibility = View.VISIBLE
        binding.labelsNotesGridContainer.visibility = View.VISIBLE
    }

    override fun getLabelFragmentContainerId(): Int {
        return binding.fullscreenFragmentContainerLabel.id
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
