package com.example.fundoonotes.UI.features.notes.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.archive.ArchiveActionHandler

class SelectionBar : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.view_selection_bar, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isHandlerAttached = parentFragment is ArchiveActionHandler
        Log.d("SelectionBar", "Is ArchiveActionHandler attached? $isHandlerAttached")

        view.findViewById<ImageButton>(R.id.btnArchiveSelectionBar).setOnClickListener {
            (parentFragment as? ArchiveActionHandler)?.onArchiveSelected()
        }
    }

    fun setSelectedCount(count: Int) {
        view?.findViewById<TextView>(R.id.tvSelectedCount)?.text = count.toString()
    }


}