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

    private val drawerLayout: DrawerLayout by lazy {
        findViewById(R.id.drawerLayout)
    }

    private val navView: NavigationView by lazy {
        findViewById(R.id.nav_view)
    }

    private val toggle: ActionBarDrawerToggle by lazy {
        ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
    }

    private val labelsViewModel by viewModels<LabelsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labelsViewModel.fetchLabels()
        setupNavigationDrawer()
        setupObservers()

        if (savedInstanceState == null) {
            loadDefaultFragment()
        }
    }

    private fun setupNavigationDrawer() {
        navView.menu.findItem(R.id.label_heading)?.isVisible = false

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
            setDisplayShowHomeEnabled(false)
        }

        toggle.isDrawerIndicatorEnabled = true

        navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true

            when (menuItem.itemId) {
                R.id.nav_notes -> replaceFragment(DashboardFragment.newInstance())
                R.id.nav_reminders -> replaceFragment(RemindersFragment.newInstance())
                R.id.nav_create_labels -> replaceFragment(EditLabelFragment.newInstance())
                R.id.nav_archive -> replaceFragment(ArchiveFragment.newInstance())
                R.id.nav_trash -> replaceFragment(BinFragment.newInstance())
                R.id.nav_settings -> replaceFragment(SettingsFragment.newInstance())
                R.id.app_feedback -> replaceFragment(FeedbackFragment.newInstance())
                R.id.help -> replaceFragment(HelpFragment.newInstance())
                else -> {
                    val labelName = menuItem.title.toString()
                    replaceFragment(LabelFragment.newInstance(labelName))
                }
            }

            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupObservers() {
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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun updateLabelsInDrawer(labels: List<Label>) {
        val menu = navView.menu
        val groupId = R.id.group_labels_dynamic

        for (i in menu.size - 1 downTo 0) {
            val item = menu[i]
            if (item.groupId == groupId) {
                menu.removeItem(item.itemId)
            }
        }

        if (labels.isNotEmpty()) {
            menu.add(groupId, R.id.label_heading, 100, "Labels")
                .setEnabled(false)
                .setCheckable(false)
        }

        labels.forEachIndexed { index, label ->
            val id = label.name.hashCode()
            menu.add(groupId, id, 101 + index, label.name)
                .setIcon(R.drawable.label)
                .setCheckable(true)
        }

        navView.invalidate()
    }
}