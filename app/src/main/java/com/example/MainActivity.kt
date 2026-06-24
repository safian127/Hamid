package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.GTDDatabase
import com.example.data.repository.GTDRepository
import com.example.ui.screens.GTDAppMainScreen
import com.example.ui.viewmodel.GTDViewModel
import com.example.ui.viewmodel.GTDViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room database and repository
        val database = GTDDatabase.getDatabase(this)
        val repository = GTDRepository(
            taskDao = database.taskDao(),
            projectDao = database.projectDao(),
            tagDao = database.tagDao()
        )

        // Instantiate ViewModel
        val viewModelFactory = GTDViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[GTDViewModel::class.java]

        setContent {
            MyApplicationTheme {
                GTDAppMainScreen(viewModel = viewModel)
            }
        }
    }
}

