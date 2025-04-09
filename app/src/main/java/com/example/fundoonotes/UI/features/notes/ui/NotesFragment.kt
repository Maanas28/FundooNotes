package com.example.fundoonotes.UI.features.notes.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.data.model.Label
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.addnote.CanvasNoteFragment
import com.example.fundoonotes.UI.features.addnote.ImageNoteFragment
import com.example.fundoonotes.UI.features.addnote.ListNoteFragement
import com.example.fundoonotes.UI.features.addnote.MicrophoneNoteFragement
import com.example.fundoonotes.UI.features.labels.ApplyLabelToNoteFragment
import com.example.fundoonotes.UI.features.labels.LabelsViewModel
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.AddNoteListener
import com.example.fundoonotes.UI.util.ArchiveActionHandler
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.EditNoteHandler
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.LabelSelectionListener
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.example.fundoonotes.databinding.FragmentNotesBinding

class NotesFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    ArchiveActionHandler,
    SelectionBarListener,
    DeleteActionHandler,
    LabelActionHandler,
    LabelSelectionListener,
    EditNoteHandler,
    AddNoteListener {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var labelsViewModel: LabelsViewModel
    internal lateinit var notesListFragment: NotesListFragment
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        // Add the SelectionBar which will remain in place
        childFragmentManager.beginTransaction()
            .replace(R.id.selectionBarContainer, SelectionBar())
            .commit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupNotesGrid()
        setupBottomNav()
        setupBackPressHandling()
        setupFab()
        notesViewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
        labelsViewModel = ViewModelProvider(this).get(LabelsViewModel::class.java)
    }

    private fun setupNotesGrid() {
        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Notes).apply {
            selectionBarListener = this@NotesFragment
            setNoteInteractionListener(this@NotesFragment)
            onSelectionChanged = { count ->
                enterSelectionMode(count)
            }
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
            .replace(R.id.container, notesListFragment)
            .commit()
    }

    private fun setupBottomNav() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_lists -> ListNoteFragement("List Notes")
                R.id.nav_audio -> MicrophoneNoteFragement("Microphone Notes")
                R.id.nav_canvas -> CanvasNoteFragment("Canvas Notes")
                R.id.nav_gallery -> ImageNoteFragment("Image Notes")
                else -> null
            }
            fragment?.let {
                hideMainNotesLayout()
                childFragmentManager.beginTransaction()
                    .replace(R.id.bottom_nav_container, it)
                    .addToBackStack(null)
                    .commit()
            }
            true
        }
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (binding.bottomNavContainer.isVisible) {
                restoreMainNotesLayout()
                childFragmentManager.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            hideMainNotesLayout()

            val addNoteFragment = AddNoteFragment.newInstance(null)
            addNoteFragment.setAddNoteListener(this)
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_nav_container, addNoteFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onArchiveSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val updatedNote = note.copy(archived = true)
            notesViewModel.archiveNote(updatedNote, onSuccess = {}, onFailure = {})
        }
        notesListFragment.clearSelection()
    }

    override fun toggleView(isGrid: Boolean) {
        notesListFragment.toggleView(isGrid)
    }

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun enterSelectionMode(count: Int) {
        val noteIds = notesListFragment.selectedNotes.map { it.id }
        // Update the selection bar with the current selected note IDs and count.
        (childFragmentManager.findFragmentById(R.id.selectionBarContainer) as? SelectionBar)
            ?.apply {
                updateSelectedNoteIds(noteIds)
                setSelectedCount(count)
            }
        binding.searchBar.visibility = View.GONE
        binding.selectionBarContainer.visibility = View.VISIBLE
    }

    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        binding.searchBar.visibility = View.VISIBLE
        binding.selectionBarContainer.visibility = View.GONE
    }

    override fun onLabelSelected(
        noteIds: List<String>,
        isChecked: Boolean,
        selectedNoteIds: List<String>
    ) {
        // Open ApplyLabelToNoteFragment with the selected note IDs
        val fragment = ApplyLabelToNoteFragment.newInstance(noteIds)
        hideMainNotesLayout()
        childFragmentManager.beginTransaction()
            .replace(R.id.bottom_nav_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onLabelListUpdated(selectedLabels: List<Label>) {
        Log.d("LabelSelection", "User selected: $selectedLabels")
    }

    override fun onLabelToggledForNotes(label: Label, isChecked: Boolean, noteIds: List<String>) {
        labelsViewModel.toggleLabelForNotes(label, isChecked, noteIds)
    }

    override fun onDeleteSelected() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notes")
            .setMessage("Are you sure you want to move selected notes to Bin?")
            .setPositiveButton("Move to Bin") { _, _ ->
                notesListFragment.selectedNotes.forEach { note ->
                    val updated = note.copy(inBin = true)
                    notesViewModel.deleteNote(updated, onSuccess = {}, onFailure = {})
                }
                notesListFragment.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    internal fun hideMainNotesLayout() {
        binding.bottomNavContainer.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        binding.fab.visibility = View.GONE
        binding.bottomTabNavigation.visibility = View.GONE
        binding.bottomNavigationView.visibility = View.GONE
    }

    fun restoreMainNotesLayout() {
        binding.bottomNavContainer.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        binding.fab.visibility = View.VISIBLE
        binding.bottomTabNavigation.visibility = View.VISIBLE
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    // NoteInteractionListener implementations
    override fun onNoteEdit(note: Note) {
        openAddOrEditNote(note)
    }

    // AddNoteListener implementations
    override fun onNoteAdded(note: Note) {
        Log.d("NotesFragment", "Note added: ${note.title}")
    }

    override fun onNoteUpdated(note: Note) {
        Log.d("NotesFragment", "Note updated: ${note.title}")
    }

    override fun onAddNoteCancelled() {
        Log.d("NotesFragment", "Note addition cancelled")
    }

    private fun openAddOrEditNote(note: Note?) {
        hideMainNotesLayout()

        val addNoteFragment = AddNoteFragment.newInstance(note)
        addNoteFragment.setAddNoteListener(this)
        childFragmentManager.beginTransaction()
            .replace(R.id.bottom_nav_container, addNoteFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}