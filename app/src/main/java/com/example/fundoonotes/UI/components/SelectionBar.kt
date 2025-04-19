package com.example.fundoonotes.UI.components

import SelectionViewModel
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.data.model.Note
import com.example.fundoonotes.UI.features.labels.ApplyLabelToNoteFragment
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.*
import com.example.fundoonotes.databinding.ViewSelectionBarBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectionBar : Fragment() {

    private var _binding: ViewSelectionBarBinding? = null
    private val binding get() = _binding!!

    private val selectionViewModel by activityViewModels<SelectionViewModel>()
    private lateinit var notesViewModel: NotesViewModel

    private var mode: SelectionBarMode = SelectionBarMode.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = arguments?.getString(ARG_MODE)?.let { SelectionBarMode.valueOf(it) } ?: SelectionBarMode.DEFAULT
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSelectionBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notesViewModel = NotesViewModel(requireContext())

        setupUIForMode()
        observeSelection()

        binding.btnCloseSelectionSelectionBar.setOnClickListener {
            selectionViewModel.clearSelection()
        }
    }

    private fun setupUIForMode() {
        when (mode) {
            SelectionBarMode.DEFAULT -> {
                binding.btnLabelSelectionBar.visibility = View.VISIBLE
                binding.btnArchiveSelectionBar.visibility = View.VISIBLE
                binding.btnMoreSelectedSelectionBar.visibility = View.VISIBLE

                binding.btnLabelSelectionBar.setOnClickListener {
                    val selectedNotes = selectionViewModel.getSelection()
                    if (selectedNotes.isNotEmpty()) {
                        val labelFragment = ApplyLabelToNoteFragment.newInstance()


                        val host = parentFragment as? LabelFragmentHost ?: activity as? LabelFragmentHost

                        if (host is MainLayoutToggler) {
                            host.hideMainLayout()
                        }

                        if (host != null) {
                            host.getLabelFragmentManager().beginTransaction()
                                .replace(host.getLabelFragmentContainerId(), labelFragment)
                                .addToBackStack("label_fragment")
                                .commit()
                        } else {
                            Log.e("SelectionBar", "Host does not implement LabelFragmentHost")
                        }
                    }
                }

                val isArchiveScreen = (parentFragment?.javaClass?.simpleName == "ArchiveFragment")

                binding.btnArchiveSelectionBar.setImageResource(
                    if (isArchiveScreen) R.drawable.unarchive else R.drawable.archive
                )

                binding.btnArchiveSelectionBar.setOnClickListener {
                    val selectedNotes = selectionViewModel.getSelection()
                    selectedNotes.forEach { note ->
                        val updated = note.copy(archived = !isArchiveScreen)
                        if (isArchiveScreen) {
                            notesViewModel.unarchiveNote(updated)
                        } else {
                            notesViewModel.archiveNote(updated)
                        }
                    }
                    selectionViewModel.clearSelection()
                }

                binding.btnMoreSelectedSelectionBar.setOnClickListener {
                    val selectedNotes = selectionViewModel.getSelection()
                    if (selectedNotes.isNotEmpty()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Notes")
                            .setMessage("Are you sure you want to move selected notes to Bin?")
                            .setPositiveButton("Move to Bin") { _, _ ->
                                selectedNotes.forEach { note ->
                                    val updated = note.copy(inBin = true)
                                    notesViewModel.deleteNote(updated)
                                }
                                selectionViewModel.clearSelection()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            SelectionBarMode.BIN -> {
                binding.btnRestoreSelected.visibility = View.VISIBLE
                binding.btnMoreSelectedSelectionBar.visibility = View.VISIBLE

                binding.btnRestoreSelected.setOnClickListener {
                    val selectedNotes = selectionViewModel.getSelection()
                    selectedNotes.forEach { note ->
                        val updated = note.copy(inBin = false)
                        notesViewModel.restoreNote(updated)
                    }
                    selectionViewModel.clearSelection()
                }

                binding.btnMoreSelectedSelectionBar.setOnClickListener {
                    val selectedNotes = selectionViewModel.getSelection()
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete Forever")
                        .setMessage("Are you sure you want to permanently delete selected notes?")
                        .setPositiveButton("Delete") { _, _ ->
                            selectedNotes.forEach { note ->
                                notesViewModel.permanentlyDeleteNote(note)
                            }
                            selectionViewModel.clearSelection()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun observeSelection() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                selectionViewModel.selectedNotes.collectLatest { notes ->
                    binding.tvSelectedCount.text = notes.size.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MODE = "mode"

        fun newInstance(mode: SelectionBarMode): SelectionBar {
            return SelectionBar().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                }
            }
        }
    }
}
