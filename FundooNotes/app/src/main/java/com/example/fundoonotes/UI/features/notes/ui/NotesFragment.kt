package com.example.fundoonotes.UI.features.notes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.example.fundoonotes.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.fundoonotes.UI.features.addnote.*
import com.example.fundoonotes.UI.features.archive.ArchiveActionHandler
import com.example.fundoonotes.UI.features.drawer.DrawerToggleListener
import com.example.fundoonotes.UI.features.notes.util.NotesGridContext
import com.example.fundoonotes.UI.features.notes.util.ViewToggleListener

class NotesFragment : Fragment(),
    ViewToggleListener,
    DrawerToggleListener,
    ArchiveActionHandler {

    // UI Components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bottomTabNavigation: View
    private lateinit var notesLayout: View
    private lateinit var bottomNavContainer: View
    private lateinit var fab: View
    private lateinit var selectionBarContainer: FragmentContainerView
    private lateinit var searchBar: View

    // Notes View
    private lateinit var notesGridFragment: NotesGridFragment

    // ---------------------- Lifecycle ----------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        val notesContainer = NotesGridFragment.newInstance(NotesGridContext.Notes)
        childFragmentManager.beginTransaction()
            .replace(R.id.container, notesContainer)
            .commit()
        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupNotesGrid()
        setupBottomNav()
        setupBackPressHandling()
        setupListeners(view)
    }

    // ---------------------- View Bindings ----------------------

    private fun bindViews(view: View) {
        bottomNav = view.findViewById(R.id.bottomNavigationView)
        bottomTabNavigation = view.findViewById(R.id.bottomTabNavigation)
        notesLayout = view.findViewById(R.id.contentLayout)
        bottomNavContainer = view.findViewById(R.id.bottom_nav_container)
        fab = view.findViewById(R.id.fab)
        searchBar = view.findViewById(R.id.searchBar)
        selectionBarContainer = view.findViewById(R.id.selectionBarContainer)
    }

    private fun setupNotesGrid() {
        notesGridFragment = NotesGridFragment.newInstance(NotesGridContext.Notes).apply {
            onSelectionChanged = { count -> enterSelectionMode(count) }
            onSelectionModeEnabled = {
                searchBar.visibility = View.GONE
                selectionBarContainer.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                searchBar.visibility = View.VISIBLE
                selectionBarContainer.visibility = View.GONE
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.container, notesGridFragment)
            .commit()
    }


    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
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
            if (bottomNavContainer.isVisible) {
                restoreMainNotesLayout()
                childFragmentManager.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupListeners(view: View) {
        fab.setOnClickListener {
            hideMainNotesLayout()
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_nav_container, AddNoteFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageButton>(R.id.btnCloseSelectionSelectionBar)?.setOnClickListener {
            notesGridFragment.selectedNotes.clear()
            notesGridFragment.adapter.notifyDataSetChanged()
            exitSelectionMode()
        }
    }

    // ---------------------- ArchiveActionHandler ----------------------

    override fun onArchiveSelected() {

        notesGridFragment.selectedNotes.forEach { note ->
            val updatedNote = note.copy(archived = true)
            notesGridFragment.viewModel.archiveNote(updatedNote)
        }

        notesGridFragment.clearSelection()
    }

    // ---------------------- DrawerToggleListener ----------------------

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    // ---------------------- ViewToggleListener ----------------------

    override fun toggleView(isGrid: Boolean) {
        notesGridFragment.toggleView(isGrid)
    }

    // ---------------------- UI State Handlers ----------------------

    private fun hideMainNotesLayout() {
        bottomNavContainer.visibility = View.VISIBLE
        notesLayout.visibility = View.GONE
        fab.visibility = View.GONE
        bottomTabNavigation.visibility = View.GONE
        bottomNav.visibility = View.GONE
    }

    fun restoreMainNotesLayout() {
        bottomNavContainer.visibility = View.GONE
        notesLayout.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE
        bottomTabNavigation.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
    }

    fun enterSelectionMode(count: Int) {
        searchBar.visibility = View.GONE
        selectionBarContainer.visibility = View.VISIBLE // ✅ Show fragment properly
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainer) as? SelectionBar

        selectionBarFragment?.setSelectedCount(count)
    }

    fun exitSelectionMode() {
        searchBar.visibility = View.VISIBLE
        selectionBarContainer.visibility = View.GONE // ✅ Hide fragment properly
    }
}
