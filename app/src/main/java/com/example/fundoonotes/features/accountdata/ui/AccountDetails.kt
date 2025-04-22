package com.example.fundoonotes.features.accountdata.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.features.accountdata.viewmodel.AccountViewModel
import com.example.fundoonotes.features.auth.ui.LoginActivity
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountDetails : DialogFragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var imageView: ShapeableImageView
    private lateinit var accountViewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_account_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        imageView = view.findViewById(R.id.ivProfile)

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            handleLogout()
        }

        accountViewModel = AccountViewModel(requireContext())

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.accountDetails.collectLatest { user ->
                    tvName.text = "${user?.firstName} ${user?.lastName}"
                    tvEmail.text = user?.email
                    user?.profileImage?.let { imageUrl ->
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.account)
                            .into(imageView)
                    }
                }
            }
        }

        accountViewModel.fetchAccountDetails()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setGravity(Gravity.CENTER)
    }

    private fun handleLogout() {
        accountViewModel.logout {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}