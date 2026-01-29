package com.example.noty.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.noty.R
import com.example.noty.data.Note
import com.example.noty.data.NoteType
import com.example.noty.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.color.DynamicColors


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 101
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NotyViewModel
    private lateinit var adapter: NoteAdapter
    private var previousNoteCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        viewModel = ViewModelProvider(
            this,
            NotyViewModelFactory(application)
        )[NotyViewModel::class.java]

        checkNotificationPermission()

        setupRecyclerView()
        setupInput()
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter { note ->
            showDeleteConfirmation(note)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allNotes.observe(this) { notes ->
            adapter.submitList(notes)
            binding.emptyState.visibility = if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            // Scroll to bottom only when a new note is added (not on delete)
            if (notes.size > previousNoteCount && notes.isNotEmpty()) {
                 binding.recyclerView.smoothScrollToPosition(notes.size - 1)
            }
            previousNoteCount = notes.size
        }
    }

    private fun setupInput() {
        binding.fabAddNote.setOnClickListener {
            showAddNoteDialog()
        }
    }

    private fun showAddNoteDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note, null)

        val editTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editNoteTitle)
        val editDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editNoteDescription)

        dialogView.findViewById<android.widget.Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.buttonSave).setOnClickListener {
            val title = editTitle.text.toString().trim()
            val description = editDescription.text.toString().trim()

            if (title.isNotEmpty()) {
                viewModel.insert(Note(
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    type = NoteType.NOTE
                ))
                dialog.dismiss()
            } else {
                editTitle.error = "Title is required"
            }
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun showDeleteConfirmation(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirmation)
            .setMessage("Are you sure you want to delete '${note.title}'?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(note)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                showThemeSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showThemeSelectionDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_theme_selection, null)

        val radioGroup = view.findViewById<android.widget.RadioGroup>(R.id.radioGroupTheme)

        lifecycleScope.launch {
            val currentTheme = viewModel.themeFlow.first()
            when(currentTheme) {
                com.example.noty.utils.ThemeManager.ThemeMode.SYSTEM -> radioGroup.check(R.id.radioSystem)
                com.example.noty.utils.ThemeManager.ThemeMode.LIGHT -> radioGroup.check(R.id.radioLight)
                com.example.noty.utils.ThemeManager.ThemeMode.DARK -> radioGroup.check(R.id.radioDark)
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val newMode = when (checkedId) {
                R.id.radioSystem -> com.example.noty.utils.ThemeManager.ThemeMode.SYSTEM
                R.id.radioLight -> com.example.noty.utils.ThemeManager.ThemeMode.LIGHT
                R.id.radioDark -> com.example.noty.utils.ThemeManager.ThemeMode.DARK
                else -> com.example.noty.utils.ThemeManager.ThemeMode.SYSTEM
            }
            viewModel.setTheme(newMode)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }
}
