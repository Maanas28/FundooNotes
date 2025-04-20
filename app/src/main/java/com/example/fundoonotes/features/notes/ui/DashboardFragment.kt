package com.example.fundoonotes.features.notes.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.R
import com.example.fundoonotes.common.components.NotesDisplayFragment
import com.example.fundoonotes.common.components.SelectionBar
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.common.data.model.Note
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.common.util.enums.SelectionBarMode
import com.example.fundoonotes.common.util.interfaces.AddNoteListener
import com.example.fundoonotes.common.util.interfaces.DrawerToggleListener
import com.example.fundoonotes.common.util.interfaces.EditNoteHandler
import com.example.fundoonotes.common.util.interfaces.LabelActionHandler
import com.example.fundoonotes.common.util.interfaces.LabelFragmentHost
import com.example.fundoonotes.common.util.interfaces.LabelSelectionListener
import com.example.fundoonotes.common.util.interfaces.MainLayoutToggler
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.SelectionBarListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentNotesBinding
import com.example.fundoonotes.features.addnote.AddNoteFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import com.example.fundoonotes.features.notes.util.DashboardBottomNavHandler

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

    private val notesViewModel by activityViewModels<NotesViewModel>()
    private val labelsViewModel by activityViewModels<LabelsViewModel>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        setupDrawerLayout()
        setupSelectionBar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val searchBarFragment = DashboardScreenSearchBar()
        childFragmentManager.beginTransaction()
            .replace(binding.searchBar.id, searchBarFragment)
            .commit()

        setupNotesGrid()
        setupBottomNav()
        setupBackPressHandling()
        setupFab()
        setupBackStackListener()
    }


    /** Accesses the drawer layout from the activity */
    private fun setupDrawerLayout() {
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)
    }

    /** Adds the reusable SelectionBar in DEFAULT mode */
    private fun setupSelectionBar() {
        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainer.id, selectionBar)
            .commit()
    }

    /** Initializes the notes grid and listeners */
    private fun setupNotesGrid() {
        notesDisplayFragment = NotesDisplayFragment.Companion.newInstance(NotesGridContext.Notes).apply {
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

        childFragmentManager.beginTransaction()
            .replace(binding.container.id, notesDisplayFragment)
            .commit()
    }

    /** Handles bottom navigation for different note types */
    private fun setupBottomNav() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = DashboardBottomNavHandler.getFragmentForItem(item.itemId)
            fragment?.let {
                hideMainLayout()
                parentFragmentManager.beginTransaction()
                    .replace(binding.fullScreenContainer.id, it)
                    .addToBackStack(null)
                    .commit()
            }
            true
        }
    }

    /** Restores layout when full-screen fragment is popped */
    private fun setupBackStackListener() {
        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                restoreMainLayout()
            }
        }
    }

    /** Handles system back press (especially for full screen views) */
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

    /** FloatingActionButton to launch AddNoteFragment */
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

    override fun onAddNoteCancelled() {
        Log.d("DashboardFragment", "Note addition cancelled")
    }

    /** Opens AddNoteFragment for adding or editing a note */
    private fun openAddOrEditNote(note: Note?) {
        hideMainLayout()
        val addNoteFragment = AddNoteFragment.Companion.newInstance(note).apply {
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
        binding.bottomTabNavigation.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
    }

    override fun restoreMainLayout() {
        binding.fullScreenContainer.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        binding.bottomTabNavigation.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE

        // Clear previous views to avoid memory leaks
        if (binding.fullScreenContainer.childCount > 0) {
            binding.fullScreenContainer.removeAllViews()
        }
    }

    override fun onSearchQueryChanged(query: String) {
        // Forward the search query to the ViewModel
        notesViewModel.setSearchQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}