package com.example.whoareyou.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.whoareyou.settings.getAnnounceTextFlow
import com.example.whoareyou.settings.setAnnounceText
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier){
    val context = LocalContext.current
    val announceText by getAnnounceTextFlow(context).collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text("Озвучивать текст сообщений")
        Switch(
            checked = announceText,
            onCheckedChange = {
                    newValue -> scope.launch {
                setAnnounceText(context, newValue)
            }
            }
        )
    }
}