package com.example.fundoonotes.UI.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.presentation.activites.LoginActivity
import com.example.fundoonotes.UI.presentation.fragments.ArchiveFragment
import com.example.fundoonotes.UI.presentation.fragments.FeedbackFragment
import com.example.fundoonotes.UI.presentation.fragments.HelpFragment
import com.example.fundoonotes.UI.presentation.fragments.LabelsFragment
import com.example.fundoonotes.UI.presentation.fragments.NotesFragment
import com.example.fundoonotes.UI.presentation.fragments.RemindersFragment
import com.example.fundoonotes.UI.presentation.fragments.SettingsFragment
import com.example.fundoonotes.UI.presentation.fragments.TrashFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        // Find Views
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.nav_view)


        // Setup Drawer Toggle
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        // Ensure the hamburger menu is enabled
        toggle.isDrawerIndicatorEnabled = true  // ✅ This forces the hamburger icon

        // Load default fragment (NotesFragment)
        if (savedInstanceState == null) {
            replaceFragment(NotesFragment())
            navView.setCheckedItem(R.id.nav_notes)
        }

        // Handle Navigation Clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true

            when (menuItem.itemId) {
                R.id.nav_notes -> replaceFragment(NotesFragment())
                R.id.nav_reminders -> replaceFragment(RemindersFragment())
                R.id.nav_create_labels -> replaceFragment(LabelsFragment())
                R.id.nav_archive -> replaceFragment(ArchiveFragment())
                R.id.nav_trash -> replaceFragment(TrashFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                R.id.app_feedback -> replaceFragment(FeedbackFragment())
                R.id.help -> replaceFragment(HelpFragment())
            }

            drawerLayout.closeDrawers()
            true
        }

    }

    // Function to Replace Fragments
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
