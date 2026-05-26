package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.MainView
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VideoViewModel
import com.example.viewmodel.VideoViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel: VideoViewModel by viewModels {
      VideoViewModelFactory(applicationContext)
    }

    setContent {
      MyApplicationTheme {
        MainView(viewModel = viewModel)
      }
    }
  }
}
