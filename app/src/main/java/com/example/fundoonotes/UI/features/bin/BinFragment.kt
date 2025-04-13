package com.example.fundoonotes.UI.features.bin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.NotesListFragment
import com.example.fundoonotes.UI.components.SearchBarFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.BinActionHandler
import com.example.fundoonotes.UI.util.NotesGridContext
import com.example.fundoonotes.UI.util.SearchListener
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.UI.util.ViewToggleListener

class BinFragment : Fragment(),
    SelectionBarListener,
    ViewToggleListener,
    BinActionHandler,
    SearchListener {

    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var notesListFragment: NotesListFragment
    private lateinit var viewModel: NotesViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchBar = view.findViewById(R.id.searchBarContainerBin)
        selectionBar = view.findViewById(R.id.selectionBarContainerBin)
        viewModel = NotesViewModel(requireContext())



        val searchBarFragment = SearchBarFragment.newInstance("Bin")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerBin, searchBarFragment)
            .commit()

        notesListFragment = NotesListFragment.newInstance(NotesGridContext.Bin).apply {
            selectionBarListener = this@BinFragment
            onSelectionChanged = { count -> updateSelectionBar(count) }
            onSelectionModeEnabled = {
                searchBar.visibility = View.GONE
                selectionBar.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                searchBar.visibility = View.VISIBLE
                selectionBar.visibility = View.GONE
            }
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.binNotesGridContainer, notesListFragment)
            .commit()

        // Inject Bin Selection Bar (custom icons)
        val binSelectionBarFragment = SelectionBarBin()
        childFragmentManager.beginTransaction()
            .replace(R.id.selectionBarContainerBin, binSelectionBarFragment)
            .commit()
    }

    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerBin) as? SelectionBarBin

        selectionBarFragment?.setSelectedCount(count)
    }

    override fun onSelectionCancelled() {
        notesListFragment.selectedNotes.clear()
        notesListFragment.adapter.notifyDataSetChanged()
        searchBar.visibility = View.VISIBLE
        selectionBar.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesListFragment.toggleView(isGrid)
    }

    override fun onRestoreSelected() {
        notesListFragment.selectedNotes.forEach { note ->
            val restored = note.copy(inBin = false)
            viewModel.restoreNote(restored)
        }
        notesListFragment.clearSelection()
        notesListFragment.adapter.notifyDataSetChanged()
        viewModel.fetchBinNotes()
    }


    override fun onDeleteForeverSelected() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Forever")
            .setMessage("Are you sure you want to permanently delete the selected notes?")
            .setPositiveButton("Delete") { _, _ ->
                notesListFragment.selectedNotes.forEach { note ->
                    viewModel.permanentlyDeleteNote(note)
                }
                notesListFragment.clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
        notesListFragment.adapter.notifyDataSetChanged()
        viewModel.fetchBinNotes()
    }

    override fun onSearchQueryChanged(query: String) {
        viewModel.setSearchQuery(query)
    }
}
