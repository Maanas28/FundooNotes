package com.example.fundoonotes.features.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.common.components.SearchBarFragment
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.features.labels.ui.EditLabelFragment

class SettingsFragment : Fragment(), SearchListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchBarContainer = view.findViewById<View>(R.id.searchBarContainerSettings)

        val searchBarFragment = SearchBarFragment.newInstance("Settings")
        childFragmentManager.beginTransaction()
            .replace(searchBarContainer.id, searchBarFragment)
            .commit()
    }

    override fun onSearchQueryChanged(query: String) {
        // Handle search query if needed
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
