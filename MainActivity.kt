// File: com/example/medicare/MainActivity.kt

package com.example.medicare

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medicare.receiver.ReminderScheduler
import com.example.medicare.data.Medicine
import com.example.medicare.data.MedicineDatabase
import com.example.medicare.data.MedicineRepository
import com.example.medicare.ui.theme.MedicareTheme
import com.example.medicare.ui.theme.settings.SettingsScreen
import com.example.medicare.viewmodel.MedicineViewModel
import com.example.medicare.viewmodel.MedicineViewModelFactory
import java.util.*

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            MedicareTheme {
                val context = LocalContext.current
                val db = MedicineDatabase.getDatabase(context)
                val repository = MedicineRepository(db.medicineDao())
                val viewModel: MedicineViewModel = viewModel(
                    factory = MedicineViewModelFactory(repository)
                )

                var showSettings by remember { mutableStateOf(false) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showSettings) {
                        SettingsScreen(onBackClick = { showSettings = false })
                    } else {
                        MedicineScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "medicine_channel",
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medicine reminder notifications"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun MedicineScreen(
    viewModel: MedicineViewModel,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var medicineName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val medicines by viewModel.allMedicines.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onSettingsClick) {
                Text("⚙️ Settings")
            }
        }

        Text(text = "Medicine Reminder", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = medicineName,
            onValueChange = { medicineName = it },
            label = { Text("Medicine Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text("Dosage (e.g. 1 tablet)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                TimePickerDialog(
                    context,
                    { _, selectedHour: Int, selectedMinute: Int ->
                        val isPM = selectedHour >= 12
                        val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                        val ampm = if (isPM) "PM" else "AM"
                        time = String.format("%02d:%02d %s", hour12, selectedMinute, ampm)
                    },
                    hour,
                    minute,
                    false
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (time.isNotBlank()) "Time: $time" else "Pick Reminder Time")
        }

        Button(
            onClick = {
                if (medicineName.isNotBlank() && time.isNotBlank() && dosage.isNotBlank()) {
                    viewModel.addMedicine(
                        Medicine(name = medicineName, time = time, dosage = dosage)
                    )

                    val now = Calendar.getInstance()
                    val timeParts = time.split(" ", ":")
                    if (timeParts.size == 3) {
                        var hour = timeParts[0].toInt()
                        val minute = timeParts[1].toInt()
                        val ampm = timeParts[2]

                        if (ampm == "PM" && hour < 12) hour += 12
                        if (ampm == "AM" && hour == 12) hour = 0

                        now.set(Calendar.HOUR_OF_DAY, hour)
                        now.set(Calendar.MINUTE, minute)
                        now.set(Calendar.SECOND, 0)
                        now.set(Calendar.MILLISECOND, 0)

                        if (now.before(Calendar.getInstance())) {
                            now.add(Calendar.DATE, 1)
                        }

                        ReminderScheduler.scheduleMedicineReminder(context, now, medicineName)
                    }

                    Toast.makeText(context, "Reminder Saved", Toast.LENGTH_SHORT).show()
                    medicineName = ""
                    time = ""
                    dosage = ""
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Reminder")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Saved Medicines", style = MaterialTheme.typography.headlineSmall)

        LazyColumn {
            items(medicines) { medicine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(medicine.name, style = MaterialTheme.typography.titleMedium)
                            Text("Time: ${medicine.time}")
                            Text("Dosage: ${medicine.dosage}")
                        }
                        Button(onClick = { viewModel.deleteMedicine(medicine) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
