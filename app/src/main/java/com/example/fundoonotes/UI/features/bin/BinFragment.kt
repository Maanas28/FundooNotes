package com.example.fundoonotes.UI.features.bin

import SelectionViewModel
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fundoonotes.UI.components.*
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.databinding.FragmentBinBinding

class BinFragment : Fragment(), SelectionBarListener, ViewToggleListener, SearchListener {

    private var _binding: FragmentBinBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesDisplayFragment: NotesDisplayFragment
    private lateinit var notesViewModel: NotesViewModel
    private val selectionViewModel by activityViewModels<SelectionViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notesViewModel = NotesViewModel(requireContext())

        val searchBarFragment = SearchBarFragment.newInstance("Bin")
        childFragmentManager.beginTransaction()
            .replace(binding.searchBarContainerBin.id, searchBarFragment)
            .commit()

        notesDisplayFragment = NotesDisplayFragment.newInstance(NotesGridContext.Bin).apply {
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

        childFragmentManager.beginTransaction()
            .replace(binding.binNotesGridContainer.id, notesDisplayFragment)
            .commit()

        val selectionBar = SelectionBar.newInstance(SelectionBarMode.BIN)
        childFragmentManager.beginTransaction()
            .replace(binding.selectionBarContainerBin.id, selectionBar)
            .commit()
    }

    override fun onSelectionCancelled() {
        selectionViewModel.clearSelection()
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
