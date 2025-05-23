package com.example.fundoonotes.common.components

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.R
import com.example.fundoonotes.common.util.enums.SelectionBarMode
import com.example.fundoonotes.common.util.enums.SelectionBarMode.BIN
import com.example.fundoonotes.common.util.enums.SelectionBarMode.DEFAULT
import com.example.fundoonotes.common.util.interfaces.LabelFragmentHost
import com.example.fundoonotes.common.util.interfaces.MainLayoutToggler
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.common.viewmodel.SelectionSharedViewModel
import com.example.fundoonotes.databinding.ViewSelectionBarBinding
import com.example.fundoonotes.features.labels.ui.ApplyLabelToNoteFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectionBar : Fragment() {

    private var _binding: ViewSelectionBarBinding? = null
    private val binding get() = _binding!!

    private val selectionSharedViewModel by activityViewModels<SelectionSharedViewModel>()

    private val notesViewModel: NotesViewModel by lazy {
        val parent = parentFragment
        if (parent != null) {
            ViewModelProvider(parent)[NotesViewModel::class.java]
        } else {
            ViewModelProvider(requireActivity())[NotesViewModel::class.java]
        }
    }

    private var mode: String = SelectionBarMode.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = arguments?.getString(ARG_MODE) ?: SelectionBarMode.DEFAULT
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

        setupUIForMode()
        observeSelection()

        binding.btnCloseSelectionSelectionBar.setOnClickListener {
            selectionSharedViewModel.clearSelection()
        }
    }

    private fun setupUIForMode() {
        when (mode) {
            DEFAULT -> {
                binding.btnLabelSelectionBar.visibility = View.VISIBLE
                binding.btnArchiveSelectionBar.visibility = View.VISIBLE
                binding.btnMoreSelectedSelectionBar.visibility = View.VISIBLE

                binding.btnLabelSelectionBar.setOnClickListener {
                    val selectedNotes = selectionSharedViewModel.getSelection()
                    if (selectedNotes.isNotEmpty()) {
                        val labelFragment = ApplyLabelToNoteFragment.Companion.newInstance()

                        val host = parentFragment as? LabelFragmentHost
                            ?: activity as? LabelFragmentHost

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
                    val selectedNotes = selectionSharedViewModel.getSelection()
                    selectedNotes.forEach { note ->
                        val updated = note.copy(archived = !isArchiveScreen)
                        if (isArchiveScreen) {
                            notesViewModel.unarchiveNote(updated)
                        } else {
                            notesViewModel.archiveNote(updated)
                        }
                    }
                    selectionSharedViewModel.clearSelection()
                }

                binding.btnMoreSelectedSelectionBar.setOnClickListener {
                    val selectedNotes = selectionSharedViewModel.getSelection()
                    if (selectedNotes.isNotEmpty()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.delete_notes_title))
                            .setMessage(getString(R.string.delete_notes_message))
                            .setPositiveButton(getString(R.string.move_to_bin)) { _, _ ->
                                selectedNotes.forEach { note ->
                                    val updated = note.copy(inBin = true)
                                    notesViewModel.deleteNote(updated)
                                }
                                selectionSharedViewModel.clearSelection()
                            }
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show()
                    }
                }
            }

            BIN -> {
                binding.btnRestoreSelected.visibility = View.VISIBLE
                binding.btnMoreSelectedSelectionBar.visibility = View.VISIBLE

                binding.btnRestoreSelected.setOnClickListener {
                    val selectedNotes = selectionSharedViewModel.getSelection()
                    selectedNotes.forEach { note ->
                        val updated = note.copy(inBin = false)
                        notesViewModel.restoreNote(updated)
                    }
                    selectionSharedViewModel.clearSelection()
                }

                binding.btnMoreSelectedSelectionBar.setOnClickListener {
                    val selectedNotes = selectionSharedViewModel.getSelection()
                    AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.delete_forever_title))
                        .setMessage(getString(R.string.delete_forever_message))
                        .setPositiveButton(getString(R.string.delete)) { _, _ ->
                            selectedNotes.forEach { note ->
                                notesViewModel.permanentlyDeleteNote(note)
                            }
                            selectionSharedViewModel.clearSelection()
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show()
                }
            }
        }
    }

    private fun observeSelection() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectionSharedViewModel.selectedNotes.collectLatest { notes ->
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

        fun newInstance(mode: String): SelectionBar {
            return SelectionBar().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                }
            }
        }
    }
}
