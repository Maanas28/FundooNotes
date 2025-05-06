package com.example.fundoonotes.features.reminders.ui

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
import com.example.fundoonotes.databinding.FragmentRemindersBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import com.example.fundoonotes.features.notes.ui.DashboardFragment
import kotlin.getValue

class RemindersFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    EditNoteHandler,
    SearchListener,
    MainLayoutToggler,
    LabelFragmentHost,
    LabelActionHandler {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private val notesDisplayFragment: NotesDisplayFragment by lazy {
        NotesDisplayFragment.Companion.newInstance(NotesGridContext.Reminder).apply {
            selectionBarListener = this@RemindersFragment
            setNoteInteractionListener(this@RemindersFragment)
            onSelectionModeEnabled = {
                binding.searchBarContainerReminder.visibility = View.GONE
                binding.selectionBarContainerReminder.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBarContainerReminder.visibility = View.VISIBLE
                binding.selectionBarContainerReminder.visibility = View.GONE
            }
        }
    }

    internal val viewModel: NotesViewModel by viewModels()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchBar()
        setupNotesList()
        setupBackStackListener()
    }

    private fun setupSearchBar() {
        val searchBarFragment = SearchBarFragment.Companion.newInstance("Reminder")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerReminder.id, searchBarFragment)
            .commit()
    }

    private fun setupNotesList() {
        childFragmentManager.beginTransaction()
            .replace(binding.reminderNotesGridContainer.id, notesDisplayFragment)
            .commit()

        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerReminder.id, selectionBar)
            .commit()
    }

    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.Companion.newInstance(note)
        binding.fullscreenFragmentContainerReminder.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerReminder.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerReminder.visibility = View.VISIBLE
        binding.selectionBarContainerReminder.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }

    override fun hideMainLayout() {
        binding.reminderTopBarContainer.visibility = View.GONE
        binding.reminderNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerReminder.visibility = View.VISIBLE
    }

    override fun restoreMainLayout() {
        binding.fullscreenFragmentContainerReminder.visibility = View.GONE
        binding.reminderTopBarContainer.visibility = View.VISIBLE
        binding.reminderNotesGridContainer.visibility = View.VISIBLE
    }

    override fun getLabelFragmentContainerId(): Int {
        return binding.fullscreenFragmentContainerReminder.id
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
        fun newInstance(): RemindersFragment {
            return RemindersFragment()
        }
    }
}
