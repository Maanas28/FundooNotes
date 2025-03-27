package com.example.fundoonotes.UI.presentation.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import com.example.fundoonotes.R

class SelectionBar : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnDownloadSelected).setOnClickListener {
            (parentFragment as? ArchiveActionHandler)?.onArchiveSelected()
        }
    }

}