package com.example.fundoonotes.features.accountdata.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.databinding.FragmentAccountDetailsBinding
import com.example.fundoonotes.features.accountdata.viewmodel.AccountViewModel
import com.example.fundoonotes.features.auth.ui.LoginActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountDetails : DialogFragment() {

    private var _binding: FragmentAccountDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var accountViewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout using ViewBinding
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Set up logout button
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        // Initialize ViewModel
        accountViewModel = AccountViewModel(requireContext())

        // Observe user details from ViewModel and populate UI
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.accountDetails.collectLatest { user ->
                    binding.tvName.text = "${user?.firstName} ${user?.lastName}"
                    binding.tvEmail.text = user?.email

                    // Load profile image using Glide
                    user?.profileImage?.let { imageUrl ->
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.account)
                            .into(binding.ivProfile)
                    }
                }
            }
        }

        // Trigger fetch for account details
        accountViewModel.fetchAccountDetails()
    }

    override fun onStart() {
        super.onStart()
        // Set custom dialog size and position
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setGravity(Gravity.CENTER)
    }

    private fun handleLogout() {
        // Log out and redirect to login screen
        accountViewModel.logout {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
