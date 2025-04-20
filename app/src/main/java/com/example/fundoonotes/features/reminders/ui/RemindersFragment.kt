package com.example.fundoonotes.features.reminders.ui

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
import com.example.fundoonotes.databinding.FragmentRemindersBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel

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

    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private val viewModel by activityViewModels<NotesViewModel>()
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

    /** Sets up the SearchBarFragment */
    private fun setupSearchBar() {
        val searchBarFragment = SearchBarFragment.Companion.newInstance("Reminder")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerReminder.id, searchBarFragment)
            .commit()
    }

    /** Sets up NotesListFragment and its callbacks */
    private fun setupNotesList() {
        notesDisplayFragment = NotesDisplayFragment.Companion.newInstance(NotesGridContext.Reminder).apply {
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

        childFragmentManager.beginTransaction()
            .replace(binding.reminderNotesGridContainer.id, notesDisplayFragment)
            .commit()

        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerReminder.id, selectionBar)
            .commit()
    }

    /** Restore main layout when label fragment is popped */
    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    /** Callback from NotesListFragment when editing a note */
    override fun onNoteEdit(note: Note) {
        val addNoteFragment = AddNoteFragment.Companion.newInstance(note)
        binding.fullscreenFragmentContainerReminder.visibility = View.VISIBLE
        childFragmentManager.beginTransaction()
            .replace(binding.fullscreenFragmentContainerReminder.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    /** Clears selection and resets layout visibility */
    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerReminder.visibility = View.VISIBLE
        binding.selectionBarContainerReminder.visibility = View.GONE
    }

    /** Toggles the layout between grid and list */
    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    /** Handles search query update */
    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }

    /** Hides main layout when a full-screen fragment is displayed */
    override fun hideMainLayout() {
        binding.reminderTopBarContainer.visibility = View.GONE
        binding.reminderNotesGridContainer.visibility = View.GONE
        binding.fullscreenFragmentContainerReminder.visibility = View.VISIBLE
    }

    /** Restores main layout after full-screen fragment is popped */
    override fun restoreMainLayout() {
        binding.fullscreenFragmentContainerReminder.visibility = View.GONE
        binding.reminderTopBarContainer.visibility = View.VISIBLE
        binding.reminderNotesGridContainer.visibility = View.VISIBLE
    }

    /** Returns container ID for full-screen fragment transactions */
    override fun getLabelFragmentContainerId(): Int {
        return binding.fullscreenFragmentContainerReminder.id
    }

    /** Returns fragment manager for label operations */
    override fun getLabelFragmentManager() = childFragmentManager

    /** Updates labels for selected notes */
    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}