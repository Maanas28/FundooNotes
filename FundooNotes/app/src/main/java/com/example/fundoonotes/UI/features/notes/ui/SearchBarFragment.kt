package com.example.fundoonotes.UI.features.notes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R

class SearchBarFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search_bar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        // Set passed title from arguments
        val title = arguments?.getString(ARG_TITLE) ?: "Notes"
        view.findViewById<TextView>(R.id.tvTitle).text = title

        view.findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        view.findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            // TODO: Add search functionality
        }

        view.findViewById<ImageButton>(R.id.btnToggleGrid).setOnClickListener {
            // TODO: Toggle grid/list layout
        }
    }

    companion object {
        private const val ARG_TITLE = "title"

        fun newInstance(title: String): SearchBarFragment {
            val fragment = SearchBarFragment()
            val bundle = Bundle()
            bundle.putString(ARG_TITLE, title)
            fragment.arguments = bundle
            return fragment
        }
    }
}
