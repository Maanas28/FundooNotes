package com.example.fundoonotes.UI.components

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.util.ViewToggleListener

class SearchBarFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggleViewButton: ImageButton
    private var toggleListener: ViewToggleListener? = null
    private var isGrid = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search_bar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup DrawerLayout
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        // Set title from args
        val title = arguments?.getString(ARG_TITLE) ?: "Notes"
        view.findViewById<TextView>(R.id.tvTitle).text = title

        // Drawer toggle
        view.findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Grid/List toggle
        toggleViewButton = view.findViewById(R.id.btnToggleGrid)
        toggleViewButton.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            toggleViewButton.setImageResource(
                if (isGrid) R.drawable.list_view else R.drawable.ic_grid
            )
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        toggleListener = parentFragment as? ViewToggleListener // 🔒 Safe cast
    }

    companion object {
        private const val ARG_TITLE = "title"

        fun newInstance(title: String): SearchBarFragment {
            return SearchBarFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}
