package com.example.fundoonotes.UI.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.databinding.ViewSelectionBarBinding
import com.example.fundoonotes.UI.util.ArchiveActionHandler
import com.example.fundoonotes.UI.util.UnarchiveActionHandler
import com.example.fundoonotes.UI.util.DeleteActionHandler
import com.example.fundoonotes.UI.util.LabelActionHandler
import com.example.fundoonotes.UI.util.SelectionBarListener

class SelectionBar : Fragment() {

    private var _binding: ViewSelectionBarBinding? = null
    private val binding get() = _binding!!

    private var selectionListener: SelectionBarListener? = null
    private var archiveListener: ArchiveActionHandler? = null
    private var unarchiveListner: UnarchiveActionHandler? = null
    private var deleteHandler: DeleteActionHandler? = null
    private var labelHandler: LabelActionHandler? = null

    // Local list to hold selected note IDs updated from parent
    private var selectedNoteIds: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSelectionBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectionListener = parentFragment as? SelectionBarListener
        archiveListener = parentFragment as? ArchiveActionHandler
        deleteHandler = parentFragment as? DeleteActionHandler
        unarchiveListner = parentFragment as? UnarchiveActionHandler
        labelHandler = parentFragment as? LabelActionHandler

        binding.btnCloseSelectionSelectionBar.setOnClickListener {
            selectionListener?.onSelectionCancelled()
        }

        binding.btnLabelSelectionBar.setOnClickListener {
            if (selectedNoteIds.isNotEmpty()) {
                labelHandler?.onLabelSelected(selectedNoteIds, true, selectedNoteIds)
            }
        }

        // Archive vs. Unarchive logic
        if (unarchiveListner != null) {
            binding.btnArchiveSelectionBar.setImageResource(R.drawable.unarchive)
            binding.btnArchiveSelectionBar.setOnClickListener {
                unarchiveListner?.onUnarchiveSelected()
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

    // Public method to update the list of selected note IDs from the parent
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
