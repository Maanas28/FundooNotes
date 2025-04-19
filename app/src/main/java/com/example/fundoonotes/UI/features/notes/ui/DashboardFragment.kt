package com.example.fundoonotes.UI.features.notes.ui

import SelectionViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.*
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.*
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.features.notes.util.DashboardBottomNavHandler
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.databinding.FragmentNotesBinding

class DashboardFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    SelectionBarListener,
    LabelActionHandler,
    LabelSelectionListener,
    LabelFragmentHost,
    EditNoteHandler,
    AddNoteListener,
    MainLayoutToggler {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var labelsViewModel: LabelsViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private val selectionViewModel by activityViewModels<SelectionViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        setupViewModels()
        setupDrawerLayout()
        setupSelectionBar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupNotesGrid()
        setupBottomNav()
        setupBackPressHandling()
        setupFab()
        setupBackStackListener()
    }

    /** Initializes NotesViewModel and LabelsViewModel */
    private fun setupViewModels() {
        notesViewModel = NotesViewModel(requireContext())
        labelsViewModel = LabelsViewModel(requireContext())
    }

    /** Accesses the drawer layout from the activity */
    private fun setupDrawerLayout() {
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)
    }

    /** Adds the reusable SelectionBar in DEFAULT mode */
    private fun setupSelectionBar() {
        val selectionBar = SelectionBar.newInstance(SelectionBarMode.DEFAULT)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainer.id, selectionBar)
            .commit()
    }

    /** Initializes the notes grid and listeners */
    private fun setupNotesGrid() {
        notesDisplayFragment = NotesDisplayFragment.newInstance(NotesGridContext.Notes).apply {
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
        selectionViewModel.clearSelection()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}