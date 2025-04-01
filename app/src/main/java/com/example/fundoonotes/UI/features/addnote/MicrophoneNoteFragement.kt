package com.example.fundoonotes.UI.features.addnote

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.example.fundoonotes.R

class MicrophoneNoteFragement(private val label: String) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_list_note_fragement, container, false)
        view.findViewById<TextView>(R.id.tempText).text = label
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()

            parentFragment?.view?.apply {
                findViewById<View>(R.id.bottom_nav_container)?.visibility = View.GONE
                findViewById<View>(R.id.contentLayout)?.visibility = View.VISIBLE
                findViewById<View>(R.id.fab)?.visibility = View.VISIBLE
                findViewById<View>(R.id.bottomTabNavigation)?.visibility = View.VISIBLE
                findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
            }
        }
    }
}