package com.example.fundoonotes.UI.features.auth.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.MainActivity
import com.example.fundoonotes.UI.util.AuthUtil
import com.example.fundoonotes.UI.features.auth.factory.AuthViewModelFactory
import com.example.fundoonotes.UI.features.auth.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val factory = AuthViewModelFactory(FirebaseAuth.getInstance())
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        initGoogleClient()
        setupObservers()
        setupListeners()
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
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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
            }
        }
    }

    private fun initGoogleClient() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(this, options)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(it.data).result
            account?.let { acc -> viewModel.loginWithGoogle(acc) }
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
