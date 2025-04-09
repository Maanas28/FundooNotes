package com.example.fundoonotes.UI.features.notes.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.notes.viewmodel.AccountViewModel
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesScreenSearchBar : Fragment() {

    private lateinit var btnToogleView: ImageView
    private lateinit var profileIcon: ShapeableImageView
    private lateinit var accountViewModel: AccountViewModel

    private var isGrid = false
    private var toggleListener: ViewToggleListener? = null
    private var drawerToggleListener: DrawerToggleListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        toggleListener = parent as? ViewToggleListener
            ?: throw ClassCastException("$parent must implement ViewToggleListener")

        drawerToggleListener = parent as? DrawerToggleListener
            ?: throw ClassCastException("$parent must implement DrawerToggleListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.notes_screen_search_bar, container, false)

        btnToogleView = view.findViewById(R.id.iv_toggle_view)
        profileIcon = view.findViewById(R.id.profile_icon)

        // Load profile image from ViewModel
        accountViewModel = ViewModelProvider(requireActivity())[AccountViewModel::class.java]

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

        accountViewModel.fetchAccountDetails()

        btnToogleView.setOnClickListener {
            isGrid = !isGrid
            toggleListener?.toggleView(isGrid)
            btnToogleView.setImageResource(
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

        return view
    }
}
