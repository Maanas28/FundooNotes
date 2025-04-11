package com.example.fundoonotes.UI.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.archive.ArchiveFragment
import com.example.fundoonotes.UI.features.labels.ApplyLabelToNoteFragment
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.databinding.ViewSelectionBarBinding
import com.example.fundoonotes.UI.util.*

class SelectionBar : Fragment() {

    private var _binding: ViewSelectionBarBinding? = null
    private val binding get() = _binding!!

    // Internal list of selected note IDs.
    private var selectedNoteIds: List<String> = emptyList()

    private var selectionListener: SelectionBarListener? = null
    private var archiveListener: ArchiveActionHandler? = null
    private var unarchiveListener: UnarchiveActionHandler? = null
    private var deleteHandler: DeleteActionHandler? = null
    private var labelHandler: LabelActionHandler? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSelectionBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get interface implementations from the host (either fragment or activity)
        selectionListener = parentFragment as? SelectionBarListener
        archiveListener = parentFragment as? ArchiveActionHandler
        deleteHandler = parentFragment as? DeleteActionHandler
        unarchiveListener = parentFragment as? UnarchiveActionHandler
        labelHandler = parentFragment as? LabelActionHandler

        // Back/Close button – if the host is ArchiveFragment, call its onSelectionCancelled.
        binding.btnCloseSelectionSelectionBar.setOnClickListener {
            if (parentFragment is ArchiveFragment) {
                (parentFragment as ArchiveFragment).onSelectionCancelled()
            } else {
                selectionListener?.onSelectionCancelled()
            }
        }

        // Label button – use the internal list of selected IDs.
        binding.btnLabelSelectionBar.setOnClickListener {
            if (selectedNoteIds.isNotEmpty()) {
                Log.d("SelectionBar", "Attempting to open label fragment with ${selectedNoteIds.size} notes")
                // Hide the main layout using the host's implementation.
                (parentFragment as? MainLayoutToggler ?: activity as? MainLayoutToggler)?.hideMainLayout()

                // Get the host that provides the label fragment details.
                val host = parentFragment as? LabelFragmentHost ?: activity as? LabelFragmentHost
                Log.d("SelectionBar", "Using host: ${host?.javaClass?.simpleName}")
                if (host != null) {
                    val labelFragment = ApplyLabelToNoteFragment.newInstance(selectedNoteIds)
                    host.getLabelFragmentManager().beginTransaction()
                        .replace(host.getLabelFragmentContainerId(), labelFragment)
                        .addToBackStack("label_fragment")
                        .commit()
                } else {
                    Log.e("SelectionBar", "Host does not implement LabelFragmentHost")
                }
            } else {
                Log.e("SelectionBar", "No notes selected")
            }
        }

        if (unarchiveListener != null) {
            binding.btnArchiveSelectionBar.setImageResource(R.drawable.unarchive)
            binding.btnArchiveSelectionBar.setOnClickListener {
                unarchiveListener?.onUnarchiveSelected()
            }
        } else {
            binding.btnArchiveSelectionBar.setOnClickListener {
                archiveListener?.onArchiveSelected()
            }
        }

        binding.btnMoreSelectedSelectionBar.setOnClickListener {
            deleteHandler?.onDeleteSelected()
        }
    }

    /**
     * Called by the host fragment each time the selection changes.
     * This method updates the internal list and the displayed count.
     */
    fun updateSelectedNoteIds(ids: List<String>) {
        selectedNoteIds = ids
        setSelectedCount(ids.size)
    }

    fun setSelectedCount(count: Int) {
        binding.tvSelectedCount.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
