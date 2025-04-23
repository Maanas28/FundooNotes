package com.example.fundoonotes.features.auth.ui

import AuthViewModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.util.CloudinaryUploader
import com.example.fundoonotes.databinding.ActivityRegisterBinding
import com.example.fundoonotes.features.auth.util.AuthUtil
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding // ViewBinding
    private val viewModel by viewModels<AuthViewModel>() // Auth ViewModel

    private lateinit var oneTapClient: SignInClient // Google One Tap Client
    private lateinit var signInRequest: BeginSignInRequest // Google One Tap Request

    private var imageUri: Uri? = null // Selected image URI
    private var uploadedImageUrl: String? = null // Uploaded profile image URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide() // Hide the action bar

        setupListeners() // Setup button listeners
        setupObservers() // Setup LiveData observers
    }

    // Google One Tap sign-in setup
    private fun setupGoogleOneTap() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    // Handle Google registration using Credential API
    private fun signUpWithGoogle() {
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@RegisterActivity)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(this@RegisterActivity, request)
                val credential = response.credential

                // Parse and handle Google credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val userInfo = User(
                        firstName = binding.editTextFirstName.text.toString(),
                        lastName = binding.editTextLastName.text.toString(),
                        email = "",
                        profileImage = uploadedImageUrl
                    )

                    viewModel.registerWithGoogle(idToken, userInfo)
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    toast(getString(R.string.unexpected_credential_type))
                }

            } catch (e: Exception) {
                toast(getString(R.string.google_signin_failed, e.localizedMessage))
            }
        }
    }

    // Setup all button click listeners
    private fun setupListeners() {
        binding.imageProfile.setOnClickListener {
            pickImageLauncher.launch("image/*") // Pick image from gallery
        }

        binding.buttonGoogleRegister.setOnClickListener {
            signUpWithGoogle() // Google sign-up
        }

        binding.buttonRegister.setOnClickListener {
            registerWithEmailPassword() // Email/password registration
        }

        binding.buttonLoginRegister.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)) // Navigate to login
            finish()
        }
    }

    // Perform validation and register with email/password
    private fun registerWithEmailPassword() {
        val firstName = binding.editTextFirstName.text.toString().trim()
        val lastName = binding.editTextLastName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirm = binding.editTextConfirmPassword.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            toast(getString(R.string.fill_all_fields))
            return
        }

        if (password != confirm) {
            toast(getString(R.string.password_mismatch))
            return
        }

        if (!AuthUtil.isValidEmail(email)) {
            toast(getString(R.string.invalid_email))
            return
        }

        if (!AuthUtil.isValidPassword(password)) {
            toast(getString(R.string.weak_password))
            return
        }

        val user = User(
            firstName = firstName,
            lastName = lastName,
            email = email,
            profileImage = uploadedImageUrl
        )

        viewModel.register(user, password)
        binding.progressBar.visibility = View.VISIBLE
    }

    // Observe authentication result from ViewModel
    private fun setupObservers() {
        viewModel.authResult.observe(this) { (success, message) ->
            binding.progressBar.visibility = View.GONE
            if (success) {
                toast(getString(R.string.registration_success))
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                toast(getString(R.string.registration_failed, message))
            }
        }
    }

    // Image picker launcher for Cloudinary upload
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = uri
            val file = File(cacheDir, "temp_profile.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }

            // Upload image to Cloudinary
            lifecycleScope.launch {
                toast(getString(R.string.uploading_image))
                uploadedImageUrl = CloudinaryUploader.uploadImage(file)
                uploadedImageUrl?.let { url ->
                    Glide.with(this@RegisterActivity)
                        .load(url)
                        .placeholder(R.drawable.account)
                        .into(binding.imageProfile)
                }
            }
        }
    }

    // Show toast messages
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
