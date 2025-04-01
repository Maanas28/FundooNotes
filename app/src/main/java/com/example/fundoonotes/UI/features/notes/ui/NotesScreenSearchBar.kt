package com.example.fundoonotes.UI.features.notes.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.util.DrawerToggleListener
import com.example.fundoonotes.UI.util.ViewToggleListener
import com.google.android.material.imageview.ShapeableImageView

class NotesScreenSearchBar : Fragment() {


    private lateinit var btnToogleView: ImageView
    private var isGrid = false
    private  lateinit var profileIcon : ShapeableImageView
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
    ): View? {
        val view = inflater.inflate(R.layout.notes_screen_search_bar, container, false)

        btnToogleView = view.findViewById(R.id.iv_toggle_view)
        profileIcon = view.findViewById(R.id.profile_icon)

        btnToogleView.setOnClickListener {
            isGrid = !isGrid

            // Notify parent fragment
            toggleListener?.toggleView(isGrid)

            // Update icon
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