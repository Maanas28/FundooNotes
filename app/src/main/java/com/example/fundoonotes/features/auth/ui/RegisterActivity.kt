package com.example.fundoonotes.features.auth.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.util.CloudinaryUploader
import com.example.fundoonotes.databinding.ActivityRegisterBinding
import com.example.fundoonotes.features.auth.util.AuthUtil
import com.example.fundoonotes.features.auth.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel by viewModels<AuthViewModel>()

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(this)
    }

    private var imageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.buttonGoogleRegister.setOnClickListener {
            lifecycleScope.launch {
                signUpWithGoogle()
            }
        }

        binding.buttonRegister.setOnClickListener {
            registerWithEmailPassword()
        }

        binding.buttonLoginRegister.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.imageProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

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

    private suspend fun signUpWithGoogle() {
        try {
            binding.progressBar.visibility = View.VISIBLE

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = this@RegisterActivity,
                request = request
            )
            handleCredential(result)

        } catch (e: GetCredentialException) {
            binding.progressBar.visibility = View.GONE
            Log.e("RegisterActivity", "Google sign in failed", e)
            toast("Google sign-in failed: ${e.message}")
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e("RegisterActivity", "Unknown error in Google sign-in", e)
            toast("Sign-in error: ${e.message}")
        }
    }

    private fun handleCredential(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        handleIdToken(idToken)
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        Log.e("RegisterActivity", "Error parsing Google ID token", e)
                        toast("Failed to process Google sign-in: ${e.message}")
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    toast("Unsupported credential type")
                }
            }

            else -> {
                binding.progressBar.visibility = View.GONE
                toast("Unsupported credential")
            }
        }
    }

    private fun handleIdToken(idToken: String) {
        val parts = idToken.split(".")
        if (parts.size != 3) {
            binding.progressBar.visibility = View.GONE
            Log.e("RegisterActivity", "Invalid ID token format")
            toast("Invalid ID token")
            return
        }

        val seg = parts[1]
        val padLen = (4 - seg.length % 4) % 4
        val padded = seg + "=".repeat(padLen)

        try {
            val decoded = Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
            val payloadJson = String(decoded, StandardCharsets.UTF_8)
            val payload = JSONObject(payloadJson)

            val user = User(
                firstName = payload.optString("given_name", ""),
                lastName = payload.optString("family_name", ""),
                email = payload.optString("email", ""),
                profileImage = payload.optString("picture", null)
            )

            viewModel.registerWithGoogle(idToken, user)

        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Log.e("RegisterActivity", "Token payload decode failed", e)
            toast("Failed to parse user info")
        }
    }

    private fun registerWithEmailPassword() {
        val firstName = binding.editTextFirstName.text.toString().trim()
        val lastName = binding.editTextLastName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirm = binding.editTextConfirmPassword.text.toString().trim()

        when {
            firstName.isEmpty() -> { toast("Please enter your first name"); return }
            lastName.isEmpty() -> { toast("Please enter your last name"); return }
            email.isEmpty() -> { toast("Please enter your email address"); return }
            password.isEmpty() -> { toast("Please enter a password"); return }
            confirm.isEmpty() -> { toast("Please confirm your password"); return }
            password != confirm -> { toast(getString(R.string.password_mismatch)); return }
            !AuthUtil.isValidEmail(email) -> { toast(getString(R.string.invalid_email)); return }
            !AuthUtil.isValidPassword(password) -> { toast(getString(R.string.weak_password)); return }
            else -> {
                val user = User(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    profileImage = uploadedImageUrl
                )
                binding.progressBar.visibility = View.VISIBLE
                viewModel.register(user, password)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = uri
            val file = File(cacheDir, "temp_profile.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            lifecycleScope.launch(Dispatchers.IO) {
                runOnUiThread { toast(getString(R.string.uploading_image)) }
                uploadedImageUrl = CloudinaryUploader.uploadImage(file)
                runOnUiThread {
                    uploadedImageUrl?.let { url ->
                        Glide.with(this@RegisterActivity)
                            .load(url)
                            .placeholder(R.drawable.account)
                            .into(binding.imageProfile)
                    }
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
