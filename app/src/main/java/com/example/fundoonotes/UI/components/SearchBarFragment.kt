package com.example.fundoonotes.UI.components

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.util.SearchListener
import com.example.fundoonotes.UI.util.ViewToggleListener

class SearchBarFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggleViewButton: ImageButton
    private var toggleListener: ViewToggleListener? = null
    private var isGrid = true

    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageButton
    private lateinit var titleView: TextView
    private var searchListener: SearchListener? = null

    private var isSearchMode = false // NEW

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search_bar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        titleView = view.findViewById(R.id.tvTitle)
        searchEditText = view.findViewById(R.id.editSearch)
        searchIcon = view.findViewById(R.id.btnSearch)
        toggleViewButton = view.findViewById(R.id.btnToggleGrid)

        val title = arguments?.getString(ARG_TITLE) ?: "Notes"
        titleView.text = title

        // Open drawer
        view.findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Toggle grid/list
        toggleViewButton.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            toggleViewButton.setImageResource(
                if (isGrid) R.drawable.list_view else R.drawable.ic_grid
            )
        }

        // Toggle search / close mode
        searchIcon.setOnClickListener {
            if (!isSearchMode) {
                // Switch to search mode
                isSearchMode = true
                titleView.visibility = View.GONE
                searchEditText.visibility = View.VISIBLE
                searchEditText.requestFocus()
                searchIcon.setImageResource(R.drawable.close)
            } else {
                // Exit search mode
                isSearchMode = false
                searchEditText.setText("")
                searchEditText.visibility = View.GONE
                titleView.visibility = View.VISIBLE
                searchIcon.setImageResource(R.drawable.search)
                searchListener?.onSearchQueryChanged("")
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchListener?.onSearchQueryChanged(s.toString())
               }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        toggleListener = parentFragment as? ViewToggleListener
        searchListener = parentFragment as? SearchListener
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
