package com.example.fundoonotes.features.auth.ui

import AuthViewModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.example.fundoonotes.common.data.model.User
import com.example.fundoonotes.common.util.CloudinaryUploader
import com.example.fundoonotes.features.auth.util.AuthUtil
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.launch
import java.io.File
import kotlin.getValue

class RegisterActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthViewModel>()
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private var imageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = uri
            val file = File(cacheDir, "temp_profile.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }

            lifecycleScope.launch {
                toast("Uploading Image...")
                uploadedImageUrl = CloudinaryUploader.uploadImage(file)
                uploadedImageUrl?.let { url ->
                    findViewById<ImageView>(R.id.imageProfile)?.let { imageView ->
                        Glide.with(this@RegisterActivity)
                            .load(url)
                            .placeholder(R.drawable.account)
                            .into(imageView)
                    }
                }
            }
        }
    }

    private val oneTapLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Identity.getSignInClient(this)
                .getSignInCredentialFromIntent(result.data)
                .googleIdToken?.let { idToken ->
                    val firstName = findViewById<EditText>(R.id.editTextFirstName).text.toString()
                    val lastName = findViewById<EditText>(R.id.editTextLastName).text.toString()
                    val uploadedUrl = uploadedImageUrl // set from Cloudinary after image upload

                    val userInfo = User(
                        firstName = firstName,
                        lastName = lastName,
                        email = "", // FirebaseAuth will overwrite this
                        profileImage = uploadedUrl
                    )

                    viewModel.registerWithGoogle(idToken, userInfo)
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
                } ?: toast("Google Sign-In failed: ID Token null")
        } else {
            Log.d("RegisterActivity", "Google Sign-In failed: ${result} with code ${result.resultCode}")
            toast("Google Sign-In cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.hide()

        setupGoogleOneTap()
        setupListeners()
        setupObservers()
    }

    private fun setupGoogleOneTap() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.imageProfile)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<ImageView>(R.id.buttonGoogleRegister)?.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                }
                .addOnFailureListener {
                    toast("Google Sign-In failed: ${it.message}")
                }
        }

        findViewById<Button>(R.id.buttonRegister)?.setOnClickListener {
            val firstName = findViewById<EditText>(R.id.editTextFirstName).text.toString().trim()
            val lastName = findViewById<EditText>(R.id.editTextLastName).text.toString().trim()
            val email = findViewById<EditText>(R.id.editTextEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.editTextPassword).text.toString().trim()
            val confirm = findViewById<EditText>(R.id.editTextConfirmPassword).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                toast("Please fill all fields")
                return@setOnClickListener
            }

            if (password != confirm) {
                toast("Passwords do not match")
                return@setOnClickListener
            }

            if (!AuthUtil.isValidEmail(email)) {
                toast("Invalid Email")
                return@setOnClickListener
            }

            if (!AuthUtil.isValidPassword(password)) {
                toast("Weak Password")
                return@setOnClickListener
            }

            viewModel.register(
                User(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    profileImage = uploadedImageUrl
                ), password
            )

            findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        }

        findViewById<TextView>(R.id.buttonLoginRegister).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.authResult.observe(this) { (success, message) ->
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            if (success) {
                toast("Registration Successful")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                toast("Registration Failed: $message")
                Log.e("RegisterActivity", "Registration error: $message")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
