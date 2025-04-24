package com.example.fundoonotes

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.common.data.model.Label
import com.example.fundoonotes.features.archive.ArchiveFragment
import com.example.fundoonotes.features.bin.BinFragment
import com.example.fundoonotes.features.feeedback.FeedbackFragment
import com.example.fundoonotes.features.help.HelpFragment
import com.example.fundoonotes.features.labels.ui.EditLabelFragment
import com.example.fundoonotes.features.labels.ui.LabelFragment
import com.example.fundoonotes.features.labels.viewmodel.LabelsViewModel
import com.example.fundoonotes.features.notes.ui.DashboardFragment
import com.example.fundoonotes.features.reminders.ui.RemindersFragment
import com.example.fundoonotes.features.settings.SettingsFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private val labelsViewModel by viewModels<LabelsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labelsViewModel.fetchLabels()
        initializeUIComponents()
        setupNavigationDrawer()
        setupObservers()

        if (savedInstanceState == null) {
            loadDefaultFragment()
        }
    }


    private fun initializeUIComponents() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.nav_view)

        // Initially hide the labels heading until we know if there are labels
        navView.menu.findItem(R.id.label_heading)?.isVisible = false
    }

    private fun setupNavigationDrawer() {
        // Setup Drawer Toggle
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configure ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
            setDisplayShowHomeEnabled(false)
        }

        // Ensure the hamburger menu is enabled
        toggle.isDrawerIndicatorEnabled = true

        // Handle Navigation Clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true

            when (menuItem.itemId) {
                R.id.nav_notes -> replaceFragment(DashboardFragment())
                R.id.nav_reminders -> replaceFragment(RemindersFragment())
                R.id.nav_create_labels -> replaceFragment(EditLabelFragment())
                R.id.nav_archive -> replaceFragment(ArchiveFragment())
                R.id.nav_trash -> replaceFragment(BinFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                R.id.app_feedback -> replaceFragment(FeedbackFragment())
                R.id.help -> replaceFragment(HelpFragment())
                else -> {
                    // Handle dynamic labels
                    val labelName = menuItem.title.toString()
                    val fragment = LabelFragment.Companion.newInstance(labelName)
                    replaceFragment(fragment)
                }
            }

            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupObservers() {
        // Observe labels for the drawer menu
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelsViewModel.labelList.collect { labels ->
                    updateLabelsInDrawer(labels)
                }
            }
        }
    }

    private fun loadDefaultFragment() {
        replaceFragment(DashboardFragment())
        navView.setCheckedItem(R.id.nav_notes)
    }

    // Function to Replace Fragments
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateLabelsInDrawer(labels: List<Label>) {
        val menu = navView.menu
        val groupId = R.id.group_labels_dynamic

        // Clear old dynamic items in the group
        for (i in menu.size - 1 downTo 0) {
            val item = menu[i]
            if (item.groupId == groupId) {
                menu.removeItem(item.itemId)
            }
        }
        if( labels.size != 0) {
            // Re-add the heading explicitly
            menu.add(groupId, R.id.label_heading, 100, "Labels")
                .setEnabled(false)
                .setCheckable(false)
        }
        // Add new labels
        val startOrder = 101
        labels.forEachIndexed { index, label ->
            val id = label.name.hashCode()
            menu.add(groupId, id, startOrder + index, label.name)
                .setIcon(R.drawable.label)
                .setCheckable(true)
        }

        navView.invalidate()
    }
}