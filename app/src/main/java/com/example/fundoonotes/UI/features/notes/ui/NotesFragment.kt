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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SelectionBar
import com.example.fundoonotes.UI.features.addnote.*
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.ArchiveActionHandler
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.example.fundoonotes.databinding.FragmentNotesBinding

class NotesFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    ArchiveActionHandler,
    SelectionBarListener,
    DeleteActionHandler {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel : NotesViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var notesListFragment: NotesListFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        // Add SelectionBar Fragment
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
        viewModel = ViewModelProvider(this).get(NotesViewModel::class.java)
    }

    private fun setupNotesGrid() {
        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Notes).apply {
            selectionBarListener = this@NotesFragment
            onSelectionChanged = { count -> enterSelectionMode(count) }
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
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_nav_container, AddNoteFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onArchiveSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val updatedNote = note.copy(archived = true)
            viewModel.archiveNote(updatedNote, onSuccess = {},onFailure = {})
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
        binding.searchBar.visibility = View.GONE
        binding.selectionBarContainer.visibility = View.VISIBLE
        (childFragmentManager.findFragmentById(R.id.selectionBarContainer) as? SelectionBar)
            ?.setSelectedCount(count)
    }

    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        binding.searchBar.visibility = View.VISIBLE
        binding.selectionBarContainer.visibility = View.GONE
    }

    private fun hideMainNotesLayout() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDeleteSelected() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notes")
            .setMessage("Are you sure you want to move selected notes to Bin?")
            .setPositiveButton("Move to Bin") { _, _ ->
                notesListFragment.selectedNotes.forEach { note ->
                    val updated = note.copy(inBin = true)
                    viewModel.deleteNote(updated, onSuccess = {},onFailure = {})
                }
                notesListFragment.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
