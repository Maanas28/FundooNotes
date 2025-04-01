package com.example.fundoonotes.UI.features.bin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fundoonotes.UI.util.BinActionHandler
import com.example.fundoonotes.UI.util.SelectionBarListener
import com.example.fundoonotes.databinding.FragmentSelectionBarBinBinding

class SelectionBarBin : Fragment() {
    private var _binding: FragmentSelectionBarBinBinding? = null
    private val binding get() = _binding!!

    private var selectionListener: SelectionBarListener? = null
    private var binActionHandler: BinActionHandler? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelectionBarBinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectionListener = parentFragment as? SelectionBarListener
        binActionHandler = parentFragment as? BinActionHandler

        binding.btnCloseSelectionSelectionBarBin.setOnClickListener {
            selectionListener?.onSelectionCancelled()
        }

        binding.btnRestoreSelectedSelectionBarBin.setOnClickListener {
            binActionHandler?.onRestoreSelected()
        }

        binding.btnMoreSelectedSelectionBarBin.setOnClickListener {
            binActionHandler?.onDeleteForeverSelected()
        }
    }

    fun setSelectedCount(count: Int) {
        binding.tvSelectedCountBin.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}