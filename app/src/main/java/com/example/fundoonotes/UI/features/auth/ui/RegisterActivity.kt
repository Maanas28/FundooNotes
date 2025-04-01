package com.example.fundoonotes.UI.features.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.util.AuthUtil
import com.example.fundoonotes.UI.features.auth.factory.AuthViewModelFactory
import com.example.fundoonotes.UI.features.auth.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        supportActionBar?.hide()

        val factory = AuthViewModelFactory(FirebaseAuth.getInstance())
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        initGoogleClient()
        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.buttonGoogleRegister).setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
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

            viewModel.register(email, password)
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

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
