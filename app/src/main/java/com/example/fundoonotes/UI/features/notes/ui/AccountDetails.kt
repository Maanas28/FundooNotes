package com.example.fundoonotes.UI.features.notes.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.auth.ui.LoginActivity
import com.example.fundoonotes.UI.features.notes.viewmodel.AccountViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountDetails : DialogFragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
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
        view.findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }

        accountViewModel = ViewModelProvider(requireActivity())[AccountViewModel::class.java]

        // Using repeatOnLifecycle to observe accountDetails whenever lifecycle is at least STARTED
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                accountViewModel.accountDetails.collectLatest { user ->
                    user?.let {
                        tvName.text = "${it.firstName} ${it.lastName}"
                        tvEmail.text = it.email
                    }
                }
            }
        }

        accountViewModel.fetchAccountDetails()

        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            handleLogout()
        }
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
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

}
