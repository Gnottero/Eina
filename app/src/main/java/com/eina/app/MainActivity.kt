package com.eina.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eina.app.ui.theme.EinaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EinaApp()
        }
    }
}

@Composable
private fun EinaApp() {
    EinaTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EinaNavHost()
        }
    }
}
