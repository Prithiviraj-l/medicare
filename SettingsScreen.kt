package com.example.medicare.ui.theme.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.medicare.data.PreferenceStore
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferenceStore(context) }

    val ringtoneUri by prefs.ringtoneUri.collectAsState(
        initial = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    )
    val isVibrationEnabled by prefs.vibrationEnabled.collectAsState(initial = true)

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                scope.launch {
                    prefs.saveRingtoneUri(it.toString())
                    Toast.makeText(context, "Ringtone updated!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🔙 Back Button
        Button(onClick = onBackClick) {
            Text("⬅️ Back")
        }

        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // 🔔 Ringtone Picker
        Button(onClick = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri.toUri())
            }
            ringtoneLauncher.launch(intent)
        }) {
            Text("Choose Ringtone")
        }

        // 💥 Vibration toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Vibration")
            Switch(
                checked = isVibrationEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        prefs.setVibrationEnabled(checked)
                    }
                }
            )
        }

        // 🧪 Test Ringtone
        Button(onClick = {
            try {
                val ringtone = RingtoneManager.getRingtone(context, ringtoneUri.toUri())
                ringtone?.play()
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to play ringtone", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Test Alarm Sound")
        }

        // ⚙️ Request Alarm Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Button(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }) {
                Text("Request Alarm Permission")
            }
        }
    }
}
