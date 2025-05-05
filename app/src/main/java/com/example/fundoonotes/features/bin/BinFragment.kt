package com.example.fundoonotes.features.bin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.fundoonotes.common.components.NotesDisplayFragment
import com.example.fundoonotes.common.components.SearchBarFragment
import com.example.fundoonotes.common.components.SelectionBar
import com.example.fundoonotes.common.util.NotesGridContext
import com.example.fundoonotes.common.util.enums.SelectionBarMode
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.SelectionBarListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.FragmentBinBinding
import kotlin.getValue

class BinFragment : Fragment(), SelectionBarListener, ViewToggleListener, SearchListener {

    private var _binding: FragmentBinBinding? = null
    private val binding get() = _binding!!

    private val notesDisplayFragment: NotesDisplayFragment by lazy {
        NotesDisplayFragment.Companion.newInstance(NotesGridContext.Bin).apply {
            selectionBarListener = this@BinFragment
            onSelectionModeEnabled = {
                binding.searchBarContainerBin.visibility = View.GONE
                binding.selectionBarContainerBin.visibility = View.VISIBLE
            }
            onSelectionModeDisabled = {
                binding.searchBarContainerBin.visibility = View.VISIBLE
                binding.selectionBarContainerBin.visibility = View.GONE
            }
        }
    }

    internal val notesViewModel: NotesViewModel by viewModels()
    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val searchBarFragment = SearchBarFragment.Companion.newInstance("Bin")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerBin.id, searchBarFragment)
            .commit()

        childFragmentManager.beginTransaction()
            .replace(binding.binNotesGridContainer.id, notesDisplayFragment)
            .commit()

        val selectionBar = SelectionBar.Companion.newInstance(SelectionBarMode.BIN)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerBin.id, selectionBar)
            .commit()
    }

    override fun onSelectionCancelled() {
        selectionSharedViewModel.clearSelection()
        binding.searchBarContainerBin.visibility = View.VISIBLE
        binding.selectionBarContainerBin.visibility = View.GONE
    }

    override fun toggleView(isGrid: Boolean) {
        notesDisplayFragment.toggleView(isGrid)
    }

    override fun onSearchQueryChanged(query: String) {
        notesViewModel.setSearchQuery(query)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
