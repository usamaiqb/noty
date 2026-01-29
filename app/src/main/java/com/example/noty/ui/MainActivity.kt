package com.example.noty.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.noty.R
import com.example.noty.data.Task
import com.example.noty.data.TaskType
import com.example.noty.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: NotyViewModel
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
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
                    101
                )
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter { task ->
            showDeleteConfirmation(task)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.allTasks.observe(this) { tasks ->
            adapter.submitList(tasks)
            binding.emptyState.visibility = if (tasks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            
            // Scroll to bottom when new task is added
            if (tasks.isNotEmpty()) {
                 binding.recyclerView.smoothScrollToPosition(tasks.size - 1)
            }
        }
    }

    private fun setupInput() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        
        val editTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTaskTitle)
        val editDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTaskDescription)

        dialogView.findViewById<android.widget.Button>(R.id.buttonCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<android.widget.Button>(R.id.buttonSave).setOnClickListener {
            val title = editTitle.text.toString().trim()
            val description = editDescription.text.toString().trim()

            if (title.isNotEmpty()) {
                viewModel.insert(Task(
                    title = title,
                    description = if (description.isEmpty()) null else description,
                    type = TaskType.TASK
                ))
                dialog.dismiss()
            } else {
                editTitle.error = "Title is required"
            }
        }

        dialog.setContentView(dialogView)
        dialog.show()
    }

    private fun showDeleteConfirmation(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirmation)
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(task)
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
