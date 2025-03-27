package com.example.fundoonotes.UI.features.notes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.isVisible
import com.example.fundoonotes.UI.features.addnote.AddNoteFragment
import com.example.fundoonotes.UI.features.archive.ArchiveActionHandler
import com.example.fundoonotes.UI.features.addnote.CanvasNoteFragment
import com.example.fundoonotes.UI.features.drawer.DrawerToggleListener
import com.example.fundoonotes.UI.features.addnote.ImageNoteFragment
import com.example.fundoonotes.UI.features.addnote.ListNoteFragement
import com.example.fundoonotes.UI.features.addnote.MicrophoneNoteFragement
import com.example.fundoonotes.UI.features.notes.util.ViewToggleListener

class NotesFragment : Fragment(), ViewToggleListener, DrawerToggleListener, ArchiveActionHandler {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bottomTabNavigation: View
    private lateinit var notesLayout: View
    private lateinit var bottomNavContainer: View
    private lateinit var fab: View
    private lateinit var selectionBar: View
    private lateinit var searchBar: View
    private lateinit var selectedCountText: TextView
    private lateinit var notesGridFragment: NotesGridFragment



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)

        // Get Drawer from MainActivity
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)


        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views once
        bottomNav = view.findViewById(R.id.bottomNavigationView)
        bottomTabNavigation = view.findViewById(R.id.bottomTabNavigation)
        notesLayout = view.findViewById(R.id.contentLayout)
        bottomNavContainer = view.findViewById(R.id.bottom_nav_container)
        fab = view.findViewById(R.id.fab)
        searchBar = view.findViewById(R.id.searchBar)
        selectionBar = view.findViewById(R.id.selectionBar)
        selectedCountText = view.findViewById(R.id.tvSelectedCount)



        notesGridFragment = NotesGridFragment()

        notesGridFragment.onSelectionChanged = { count ->
            enterSelectionMode(count)
        }

        notesGridFragment.onSelectionModeEnabled = {
            searchBar.visibility = View.GONE
            selectionBar.visibility = View.VISIBLE
        }

        notesGridFragment.onSelectionModeDisabled = {
            searchBar.visibility = View.VISIBLE
            selectionBar.visibility = View.GONE
            selectedCountText.text = "0"
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.container, notesGridFragment)
            .commit()



        // Handle Bottom Nav clicks
        bottomNav.setOnItemSelectedListener{ item ->
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

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (bottomNavContainer.isVisible) {
                restoreMainNotesLayout()
                childFragmentManager.popBackStack()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        fab.setOnClickListener {
            hideMainNotesLayout()

            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_nav_container, AddNoteFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<ImageButton>(R.id.btnCloseSelection).setOnClickListener {
            notesGridFragment.selectedNotes.clear()
            notesGridFragment.adapter.notifyDataSetChanged()
            exitSelectionMode()
        }

    }

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun toggleView(isGrid: Boolean) {
        notesGridFragment.toggleView(isGrid)
    }

    override fun onArchiveSelected() {
        notesGridFragment.selectedNotes.forEach { note ->
            val updatedNote = note.copy(archived = true)
            notesGridFragment.viewModel.archiveNote(updatedNote)
        }
        notesGridFragment.clearSelection()
    }

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
        selectionBar.visibility = View.VISIBLE
        selectedCountText.text = count.toString()
    }

    fun exitSelectionMode() {
        searchBar.visibility = View.VISIBLE
        selectionBar.visibility = View.GONE
        selectedCountText.text = "0"

    }
}