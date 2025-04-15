package com.example.fundoonotes.UI.features.notes.ui

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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.notes.viewmodel.AccountViewModel
import com.example.fundoonotes.UI.features.notes.viewmodel.NotesViewModel
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardScreenSearchBar : Fragment() {

    private lateinit var toggleViewIcon: ImageView
    private lateinit var profileIcon: ShapeableImageView
    private lateinit var searchInput: EditText
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var notesViewModel: NotesViewModel

    private var toggleListener: ViewToggleListener? = null
    private var drawerToggleListener: DrawerToggleListener? = null
    private var isGrid = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        toggleListener = parent as? ViewToggleListener
        drawerToggleListener = parent as? DrawerToggleListener
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
        notesViewModel =  NotesViewModel(requireContext())

        accountViewModel.fetchAccountDetails()

        // Load profile image from ViewModel
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
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
                notesViewModel.setSearchQuery(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}