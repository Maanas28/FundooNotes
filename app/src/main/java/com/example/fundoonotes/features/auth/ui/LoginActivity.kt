package com.example.fundoonotes.features.auth.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.databinding.ActivityLoginBinding
import com.example.fundoonotes.features.auth.util.AuthUtil
import com.example.fundoonotes.features.auth.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<AuthViewModel>()
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Email/password login
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            when {
                !AuthUtil.isValidEmail(email) ->
                    showToast(getString(R.string.invalid_email))
                !AuthUtil.isValidPassword(password) ->
                    showToast(getString(R.string.wrong_password))
                else -> {
                    binding.progressBar.visibility = View.VISIBLE
                    viewModel.login(email, password)
                }
            }
        }

        // Google login with Credential Manager
        binding.buttonGoogleLogin.setOnClickListener {
            lifecycleScope.launch {
                signInWithGoogle()
            }
        }

        // Navigate to register
        binding.buttonRegisterLogin.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.authResult.observe(this) { (success, message) ->
            binding.progressBar.visibility = View.GONE
            if (success) {
                showToast(getString(R.string.login_success))
                viewModel.getLoggedInUser(
                    onSuccess = { firebaseUser ->
                        viewModel.saveUserLocally(firebaseUser) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    },
                    onFailure = {
                        Log.e("LoginActivity", "Failed to fetch user", it)
                    }
                )
            } else {
                showToast(getString(R.string.login_failed, message))
                Log.e("LoginActivity", "Login error: $message")
            }
        }
    }

    // Sign in using Google credentials with Credential Manager
    private suspend fun signInWithGoogle() {
        try {
            binding.progressBar.visibility = View.VISIBLE

            // Configure Google ID token request
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)  // Try to use accounts user has already signed in with
                .setServerClientId(getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Show sign-in UI and get credential
            val result = credentialManager.getCredential(
                request = request,
                context = this,
            )

            // Process the returned credential
            handleCredential(result)

        } catch (e: GetCredentialException) {
            binding.progressBar.visibility = View.GONE
            Log.e("LoginActivity", "Google sign in failed", e)
            showToast("Google sign-in failed: ${e.message}")
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e("LoginActivity", "Unknown error in Google sign-in", e)
            showToast("Sign-in error: ${e.message}")
        }
    }

    private fun handleCredential(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Parse the credential
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        // Get the token
                        val idToken = googleIdTokenCredential.idToken

                        // Handle the token
                        handleGoogleIdToken(idToken)
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        Log.e("LoginActivity", "Error parsing Google ID token", e)
                        showToast("Failed to process Google sign-in: ${e.message}")
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    showToast("Unsupported credential type")
                }
            }
            else -> {
                binding.progressBar.visibility = View.GONE
                showToast("Unsupported credential")
            }
        }
    }

    // Decode and handle the ID token payload
    private fun handleGoogleIdToken(idToken: String) {
        val parts = idToken.split(".")
        if (parts.size != 3) {
            binding.progressBar.visibility = View.GONE
            showToast("Invalid ID token")
            return
        }

        // Pad and Base64-decode the payload
        val seg = parts[1]
        val padLen = (4 - seg.length % 4) % 4
        val padded = seg + "=".repeat(padLen)
        try {
            val decoded = Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
            val payloadJson = String(decoded, StandardCharsets.UTF_8)
            val payload = JSONObject(payloadJson)

            // Build user from token
            val user = com.example.fundoonotes.common.data.model.User(
                firstName   = payload.optString("given_name", ""),
                lastName    = payload.optString("family_name", ""),
                email       = payload.optString("email", ""),
                profileImage= payload.optString("picture", null)
            )

            // Fire your loginWithGoogle
            viewModel.loginWithGoogle(idToken)

        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e("LoginActivity", "Failed to parse ID token payload", e)
            showToast("Failed to retrieve Google user info")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}