package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.AuthScreen
import com.example.ui.AuthViewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Initialize local persistence database, DAO, and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ChatRepository(database.chatDao())

        // Create ChatViewModel using constructor injection factory
        val viewModel: ChatViewModel by viewModels {
            ChatViewModelFactory(repository)
        }

        val authViewModel: AuthViewModel by viewModels()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                    if (currentUser == null) {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = {}
                        )
                    } else {
                        ChatScreen(
                            viewModel = viewModel,
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}
