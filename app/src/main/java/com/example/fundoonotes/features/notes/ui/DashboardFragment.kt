package com.example.fundoonotes.features.notes.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.fundoonotes.R
import com.example.fundoonotes.common.components.NotesDisplayFragment
import com.example.fundoonotes.common.components.SelectionBar
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.common.util.enums.SelectionBarMode
import com.example.fundoonotes.common.util.interfaces.*
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentNotesBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel

class DashboardFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    SelectionBarListener,
    LabelActionHandler,
    LabelSelectionListener,
    LabelFragmentHost,
    EditNoteHandler,
    AddNoteListener,
    MainLayoutToggler,
    SearchListener {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    internal val notesViewModel: NotesViewModel by viewModels()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    private val drawerLayout: DrawerLayout by lazy {
        requireActivity().findViewById(R.id.drawerLayout)
    }

    private val searchBarFragment: DashboardScreenSearchBar by lazy {
        DashboardScreenSearchBar()
    }

    private val notesDisplayFragment: NotesDisplayFragment by lazy {
        NotesDisplayFragment.newInstance(
            NotesGridContext.Notes,
            animateOnLoad = true
        ).apply {
            selectionBarListener = this@DashboardFragment
            setNoteInteractionListener(this@DashboardFragment)
            onSelectionModeEnabled = {
                binding.searchBar.visibility = View.GONE
                binding.selectionBarContainer.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBar.visibility = View.VISIBLE
                binding.selectionBarContainer.visibility = View.GONE
            }
        }
    }

    private val selectionBar: SelectionBar by lazy {
        SelectionBar.newInstance(SelectionBarMode.DEFAULT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        setupSelectionBar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        childFragmentManager.beginTransaction()
            .replace(binding.searchBar.id, searchBarFragment)
            .commit()

        setupNotesGrid()
        setupBackPressHandling()
        setupFab()
        setupBackStackListener()
    }

    private fun setupSelectionBar() {
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainer.id, selectionBar)
            .commit()
    }

    private fun setupNotesGrid() {
        childFragmentManager.beginTransaction()
            .replace(binding.container.id, notesDisplayFragment)
            .commit()
    }

    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.fullScreenContainer.isVisible) {
                parentFragmentManager.popBackStack()
                restoreMainLayout()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            openAddOrEditNote(null)
        }
    }

    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBar.visibility = View.VISIBLE
        binding.selectionBarContainer.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun getLabelFragmentContainerId(): Int = binding.fullScreenContainer.id

    override fun getLabelFragmentManager(): FragmentManager = childFragmentManager

    override fun onLabelListUpdated(selectedLabels: List<Label>) {
        Log.d("LabelSelection", "User selected: $selectedLabels")
    }

    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onNoteEdit(note: Note) {
        openAddOrEditNote(note)
    }

    override fun onNoteAdded(note: Note) {
        notesViewModel.fetchNotes()
    }

    override fun onNoteUpdated(note: Note) {
        notesViewModel.fetchNotes()
    }

    override fun onAddNoteCancelled() {}

    private fun openAddOrEditNote(note: Note?) {
        hideMainLayout()
        val addNoteFragment = AddNoteFragment.newInstance(note).apply {
            setAddNoteListener(this@DashboardFragment)
        }
        parentFragmentManager.beginTransaction()
            .replace(binding.fullScreenContainer.id, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun hideMainLayout() {
        binding.fullScreenContainer.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.fab.visibility = View.GONE
    }

    override fun restoreMainLayout() {
        binding.fullScreenContainer.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE

        if (binding.fullScreenContainer.isNotEmpty()) {
            binding.fullScreenContainer.removeAllViews()
        }
    }

    override fun onSearchQueryChanged(query: String) {
        notesViewModel.setSearchQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
