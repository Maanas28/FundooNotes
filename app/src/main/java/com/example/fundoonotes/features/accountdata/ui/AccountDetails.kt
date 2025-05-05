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

    private val accountViewModel: AccountViewModel by lazy {
        AccountViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeAccountDetails()
        accountViewModel.fetchAccountDetails()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnLogout.setOnClickListener { handleLogout() }
    }

    private fun observeAccountDetails() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.accountDetails.collectLatest { user ->
                    binding.tvName.text = "${user?.firstName.orEmpty()} ${user?.lastName.orEmpty()}"
                    binding.tvEmail.text = user?.email.orEmpty()

                    user?.profileImage?.let { imageUrl ->
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.account)
                            .into(binding.ivProfile)
                    }
                }
            }
        }
    }

    private fun handleLogout() {
        accountViewModel.logout {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
