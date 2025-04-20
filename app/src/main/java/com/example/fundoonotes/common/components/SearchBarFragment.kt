package com.example.fundoonotes.common.components

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.databinding.FragmentSearchBarBinding

class SearchBarFragment : Fragment() {

    private var _binding: FragmentSearchBarBinding? = null
    private val binding get() = _binding!!

    private lateinit var drawerLayout: DrawerLayout
    private var toggleListener: ViewToggleListener? = null
    private var searchListener: SearchListener? = null

    private var isGrid = true
    private var isSearchMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)

        val title = arguments?.getString(ARG_TITLE) ?: "Notes"
        binding.tvTitle.text = title

        binding.btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnToggleGrid.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            binding.btnToggleGrid.setImageResource(
                if (isGrid) R.drawable.list_view else R.drawable.ic_grid
            )
        }

        binding.btnSearch.setOnClickListener {
            toggleSearchMode()
        }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchListener?.onSearchQueryChanged(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun toggleSearchMode() {
        if (!isSearchMode) {
            isSearchMode = true
            binding.tvTitle.visibility = View.GONE
            binding.editSearch.visibility = View.VISIBLE
            binding.editSearch.requestFocus()
            binding.btnSearch.setImageResource(R.drawable.close)
        } else {
            isSearchMode = false
            binding.editSearch.setText("")
            binding.editSearch.visibility = View.GONE
            binding.tvTitle.visibility = View.VISIBLE
            binding.btnSearch.setImageResource(R.drawable.search)
            searchListener?.onSearchQueryChanged("")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        toggleListener = parentFragment as? ViewToggleListener
        searchListener = parentFragment as? SearchListener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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