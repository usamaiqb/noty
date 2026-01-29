package com.example.noty.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.noty.R
import com.example.noty.databinding.ActivitySettingsBinding
import com.example.noty.utils.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: NotyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(
            this,
            NotyViewModelFactory(application)
        )[NotyViewModel::class.java]

        setupSettingsUI()
    }

    private fun setupSettingsUI() {
        // Since we are using a custom layout and not PreferenceFragment for full control
        // We will manually add the theme setting view into the container
        
        val themeSettingView = layoutInflater.inflate(R.layout.item_setting_theme, null)
        binding.settingsContainer.addView(themeSettingView)

        themeSettingView.setOnClickListener {
            showThemeSelectionDialog()
        }
        
        lifecycleScope.launch {
             val currentTheme = viewModel.themeFlow.first()
             updateThemeSubtitle(themeSettingView, currentTheme)
        }
    }

    private fun updateThemeSubtitle(view: View, mode: ThemeManager.ThemeMode) {
        val subtitle = view.findViewById<android.widget.TextView>(R.id.setting_subtitle)
        subtitle.text = when(mode) {
             ThemeManager.ThemeMode.SYSTEM -> getString(R.string.pref_theme_system)
             ThemeManager.ThemeMode.LIGHT -> getString(R.string.pref_theme_light)
             ThemeManager.ThemeMode.DARK -> getString(R.string.pref_theme_dark)
        }
    }

    private fun showThemeSelectionDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_theme_selection, null)
        
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        
        lifecycleScope.launch {
            val currentTheme = viewModel.themeFlow.first()
            when(currentTheme) {
                ThemeManager.ThemeMode.SYSTEM -> radioGroup.check(R.id.radioSystem)
                ThemeManager.ThemeMode.LIGHT -> radioGroup.check(R.id.radioLight)
                ThemeManager.ThemeMode.DARK -> radioGroup.check(R.id.radioDark)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newMode = when (checkedId) {
                R.id.radioSystem -> ThemeManager.ThemeMode.SYSTEM
                R.id.radioLight -> ThemeManager.ThemeMode.LIGHT
                R.id.radioDark -> ThemeManager.ThemeMode.DARK
                else -> ThemeManager.ThemeMode.SYSTEM
            }
            viewModel.setTheme(newMode)
            updateThemeSubtitle(binding.settingsContainer.getChildAt(0), newMode)
            dialog.dismiss()
            // Activity recreates automatically when theme changes via AppCompatDelegate in ViewModel/App
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
