package com.example.fundoonotes.UI

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.R
import com.example.fundoonotes.UI.features.archive.ArchiveFragment
import com.example.fundoonotes.UI.features.auth.ui.LoginActivity
import com.example.fundoonotes.UI.features.feeedback.FeedbackFragment
import com.example.fundoonotes.UI.features.help.HelpFragment
import com.example.fundoonotes.UI.features.labels.LabelsFragment
import com.example.fundoonotes.UI.features.notes.ui.NotesFragment
import com.example.fundoonotes.UI.features.reminders.RemindersFragment
import com.example.fundoonotes.UI.features.settings.SettingsFragment
import com.example.fundoonotes.UI.features.bin.BinFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

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
                R.id.nav_trash -> replaceFragment(BinFragment())
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