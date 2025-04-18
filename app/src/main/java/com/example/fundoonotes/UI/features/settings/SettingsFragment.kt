package com.example.fundoonotes.UI.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.components.SearchBarFragment
import com.example.fundoonotes.UI.util.SearchListener

class SettingsFragment : Fragment(), SearchListener {

    private lateinit var searchBar: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchBar = view.findViewById(R.id.searchBarContainerSettings)

        val searchBarFragment = SearchBarFragment.newInstance("Settings")
        childFragmentManager.beginTransaction()
            .replace(R.id.searchBarContainerSettings, searchBarFragment)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
    }
}
