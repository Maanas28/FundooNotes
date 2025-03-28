package com.example.fundoonotes.UI.features.archive

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.notes.ui.NotesGridFragment
import com.example.fundoonotes.UI.features.notes.ui.SearchBarFragment
import com.example.fundoonotes.UI.features.notes.ui.SelectionBar
import com.example.fundoonotes.UI.features.notes.util.NotesGridContext

class ArchiveFragment : Fragment() {

    private lateinit var searchBar: View
    private lateinit var selectionBar: View
    private lateinit var notesGridFragment: NotesGridFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchBar = view.findViewById(R.id.searchBarContainerArchive)
        selectionBar = view.findViewById(R.id.selectionBarContainerArchive)

        val searchBarFragment = SearchBarFragment.newInstance("Archive")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerArchive, searchBarFragment)
            .commit()

        notesGridFragment = NotesGridFragment.newInstance(NotesGridContext.Archive).apply {
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
            .replace(R.id.archiveNotesGridContainer, notesGridFragment)
            .commit()
    }



    private fun updateSelectionBar(count: Int) {
        val selectionBarFragment = childFragmentManager
            .findFragmentById(R.id.selectionBarContainerArchive) as? SelectionBar

        selectionBarFragment?.setSelectedCount(count)
    }
}
