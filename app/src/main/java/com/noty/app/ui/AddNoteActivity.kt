package com.noty.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.noty.app.data.Note
import com.noty.app.data.NoteType
import com.google.android.material.color.DynamicColors

class AddNoteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            NotyViewModelFactory(application)
        )[NotyViewModel::class.java]

        setContent {
            NotyTheme {
                NotyAddNotePage(
                    viewModel = viewModel,
                    onFinish = { finish() },
                )
            }
        }
    }
}

@Composable
fun NotyAddNotePage(
    viewModel: NotyViewModel,
    onFinish: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NoteForm(
                onDismiss = onFinish,
                onSave = { title, description, isPinned ->
                    viewModel.insert(
                        Note(
                            title = title,
                            description = if (description.isEmpty()) null else description,
                            type = NoteType.NOTE,
                            isPinned = isPinned
                        )
                    )
                    onFinish()
                }
            )
        }
    }
}
