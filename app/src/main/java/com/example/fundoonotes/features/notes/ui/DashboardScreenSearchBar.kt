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
import androidx.lifecycle.ViewModelProvider
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

    private var toggleListener: ViewToggleListener? = null
    private var drawerToggleListener: DrawerToggleListener? = null
    private var searchListener: SearchListener? = null

    private var isGrid = false

    private val accountViewModel: AccountViewModel by lazy {
        AccountViewModel(requireContext())
    }

    private val notesViewModel: NotesViewModel by lazy {
        val parent = parentFragment
        if (parent != null) ViewModelProvider(parent)[NotesViewModel::class.java]
        else ViewModelProvider(requireActivity())[NotesViewModel::class.java]
    }

    private val toggleViewIcon: ImageView by lazy { requireView().findViewById(R.id.iv_toggle_view) }
    private val profileIcon: ShapeableImageView by lazy { requireView().findViewById(R.id.profile_icon) }
    private val searchInput: EditText by lazy { requireView().findViewById(R.id.et_search) }

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
    ): View = inflater.inflate(R.layout.notes_screen_search_bar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupListeners()
        observeProfileImage()
    }

    private fun initViews() {
        view?.findViewById<ImageView>(R.id.iv_menu)?.setOnClickListener {
            drawerToggleListener?.openDrawer()
        }

        profileIcon.setOnClickListener {
            AccountDetails().show(parentFragmentManager, "Account Details")
        }
    }

    private fun setupListeners() {
        toggleViewIcon.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            toggleViewIcon.setImageResource(if (isGrid) R.drawable.list_view else R.drawable.ic_grid)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                notesViewModel.setSearchQuery(query)
                searchListener?.onSearchQueryChanged(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun observeProfileImage() {
        accountViewModel.fetchAccountDetails()

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
    }
}
