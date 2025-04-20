package com.example.fundoonotes.features.feeedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.common.components.SearchBarFragment
import com.example.fundoonotes.common.util.interfaces.SearchListener

class FeedbackFragment : Fragment(), SearchListener {

    private lateinit var searchBar: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchBar = view.findViewById(R.id.searchBarContainerFeedback)

        val searchBarFragment = SearchBarFragment.Companion.newInstance("Feedback")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerFeedback, searchBarFragment)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {

    }
}
