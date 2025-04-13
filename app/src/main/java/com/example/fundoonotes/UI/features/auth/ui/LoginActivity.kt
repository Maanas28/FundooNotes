package com.example.fundoonotes.UI.features.auth.ui

import AuthViewModel
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.MainActivity
import com.example.fundoonotes.UI.util.AuthUtil
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private val oneTapLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Identity.getSignInClient(this)
                .getSignInCredentialFromIntent(result.data)
                .googleIdToken?.let { idToken ->
                    viewModel.loginWithGoogle(idToken)
                } ?: showToast("Google Sign-In failed: ID Token null")
        } else {
            showToast("Google Sign-In cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = AuthViewModel(this)

        setupGoogleOneTap()
        setupObservers()
        setupListeners()
    }

    private fun setupGoogleOneTap() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.editTextEmail).text.toString()
            val password = findViewById<EditText>(R.id.editTextPassword).text.toString()

            if (!AuthUtil.isValidEmail(email)) showToast("Invalid Email")
            else if (!AuthUtil.isValidPassword(password)) showToast("Weak Password")
            else viewModel.login(email, password)
        }

        findViewById<ImageView>(R.id.buttonGoogleLogin).setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                }
                .addOnFailureListener {
                    showToast("Google Sign-In failed: ${it.message}")
                }
        }

        findViewById<TextView>(R.id.buttonRegisterLogin).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.authResult.observe(this) { (success, message) ->
            if (success) {
                showToast("Login Successful")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                showToast("Login Failed: $message")
                Log.e("LoginActivity", "Login error: $message")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
