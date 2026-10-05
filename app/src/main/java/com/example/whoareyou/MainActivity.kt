package com.example.whoareyou


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.whoareyou.ui.SettingsScreen
import com.example.whoareyou.ui.theme.WhoAreYouTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhoAreYouTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhoAreYouTheme {
        Greeting("Android")
    }
}