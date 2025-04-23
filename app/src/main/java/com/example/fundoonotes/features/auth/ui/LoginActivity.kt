package com.example.fundoonotes.features.auth.ui

import AuthViewModel
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.databinding.ActivityLoginBinding
import com.example.fundoonotes.features.auth.util.AuthUtil
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class LoginActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthViewModel>() // ViewModel for aut

    private lateinit var binding: ActivityLoginBinding     // ViewBinding instance

    // Google Sign-In result launcher using One Tap API
    private val oneTapLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Extract Google ID Token from result
            val credential = Identity.getSignInClient(this)
                .getSignInCredentialFromIntent(result.data)

            credential.googleIdToken?.let { idToken ->
                viewModel.loginWithGoogle(idToken)
            } ?: showToast(getString(R.string.google_id_token_null))
        } else {
            showToast(getString(R.string.google_signin_cancelled))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Initialize ViewBinding
        setContentView(binding.root)

        setupObservers()     // Observe login result and user info
        setupListeners()     // Hook up button click actions
    }

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@LoginActivity)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )

                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken

                    viewModel.loginWithGoogle(idToken)
                } else {
                    showToast(getString(R.string.unexpected_credential_type))
                }

            } catch (e: Exception) {
                showToast(getString(R.string.google_signin_failed, e.localizedMessage))
            }
        }
    }


    private fun setupListeners() {
        // Email/password login button
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            // Basic validation
            if (!AuthUtil.isValidEmail(email)) {
                showToast(getString(R.string.invalid_email))
            } else if (!AuthUtil.isValidPassword(password)) {
                showToast(getString(R.string.wrong_password))
            } else {
                viewModel.login(email, password)
            }
        }

        binding.buttonGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        // Navigate to register screen
        binding.buttonRegisterLogin.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.authResult.observe(this) { (success, message) ->
            if (success) {
                showToast(getString(R.string.login_success))

                // Fetch user details from Firebase and save locally
                viewModel.getLoggedInUser(
                    onSuccess = { firebaseUser ->
                        Log.d("LoginActivity", "Firebase user: $firebaseUser")

                        viewModel.saveUserLocally(firebaseUser) {
                            // Start main screen after saving user locally
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    },
                    onFailure = {
                        Log.e("LoginActivity", "Failed to fetch user from Firestore", it)
                    }
                )
            } else {
                showToast(getString(R.string.login_failed, message))
                Log.e("LoginActivity", "Login error: $message")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
