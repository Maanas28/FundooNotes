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
import com.example.fundoonotes.UI.util.SelectionBarListener

class SelectionBar : Fragment(){

    private var deleteHandler: DeleteActionHandler? = null

    private var _binding: ViewSelectionBarBinding? = null
    private val binding get() = _binding!!

    private var selectionListener: SelectionBarListener? = null
    private var archiveListener: ArchiveActionHandler? = null
    private var unarchiveListner : UnarchiveActionHandler? = null

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

        binding.btnCloseSelectionSelectionBar.setOnClickListener {
            selectionListener?.onSelectionCancelled()
        }

        // If this screen is Archive screen, show Unarchive button
        if (unarchiveListner != null) {
            binding.btnArchiveSelectionBar.setImageResource(R.drawable.unarchive)
            binding.btnArchiveSelectionBar.setOnClickListener {
                unarchiveListner?.onUnarchiveSelected()
            }
        } else {
            // Default behavior - Archive
            binding.btnArchiveSelectionBar.setOnClickListener {
                archiveListener?.onArchiveSelected()
            }
        }

        binding.btnMoreSelectedSelectionBar.setOnClickListener {
            deleteHandler?.onDeleteSelected()
        }


    }

    fun setSelectedCount(count: Int) {
        binding.tvSelectedCount.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
