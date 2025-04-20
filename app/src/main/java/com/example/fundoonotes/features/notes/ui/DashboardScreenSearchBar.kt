package com.example.fundoonotes.features.notes.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.common.util.interfaces.DrawerToggleListener
import com.example.fundoonotes.common.util.interfaces.SearchListener
import com.example.fundoonotes.common.util.interfaces.ViewToggleListener
import com.example.fundoonotes.common.viewmodel.NotesViewModel
import com.example.fundoonotes.features.accountdata.ui.AccountDetails
import com.example.fundoonotes.features.accountdata.viewmodel.AccountViewModel
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardScreenSearchBar : Fragment() {

    private lateinit var toggleViewIcon: ImageView
    private lateinit var profileIcon: ShapeableImageView
    private lateinit var searchInput: EditText
    private lateinit var accountViewModel: AccountViewModel
    private var searchListener: SearchListener? = null
    private val notesViewModel by activityViewModels<NotesViewModel>()


    private var toggleListener: ViewToggleListener? = null
    private var drawerToggleListener: DrawerToggleListener? = null
    private var isGrid = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        toggleListener = parent as? ViewToggleListener
        drawerToggleListener = parent as? DrawerToggleListener
        searchListener = parent as? SearchListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.notes_screen_search_bar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleViewIcon = view.findViewById(R.id.iv_toggle_view)
        profileIcon = view.findViewById(R.id.profile_icon)
        searchInput = view.findViewById(R.id.et_search)

        accountViewModel = AccountViewModel(requireContext())

        accountViewModel.fetchAccountDetails()

        // Load profile image from ViewModel
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.accountDetails.collectLatest { user ->
                    user?.profileImage?.let { imageUrl ->
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.account)
                            .into(profileIcon)
                    }
                }
            }
        }

        toggleViewIcon.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            toggleViewIcon.setImageResource(
                if (isGrid) R.drawable.list_view else R.drawable.ic_grid
            )
        }

        view.findViewById<ImageView>(R.id.iv_menu).setOnClickListener {
            drawerToggleListener?.openDrawer()
        }

        profileIcon.setOnClickListener {
            val dialog = AccountDetails()
            dialog.show(parentFragmentManager, "Account Details")
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                notesViewModel.setSearchQuery(query)
                searchListener?.onSearchQueryChanged(query)  // Add this line
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}