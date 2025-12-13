package com.example.emergencypowersystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


// Дані моніторингу
data class EnergyData(
    val deviceName: String, // Назва пристрою (UPS)
    val timestamp: Long,    // Мітка часу в мілісекундах
    val voltage: Double,    // Напруга пристрою
    val current: Double,    // Струм пристрою
    val status: String      // Статус пристрою ("Normal", "Warning", "Critical")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Встановлюємо Compose UI як основний контент активності
        setContent {
            EmergencyPowerApp() // Виклик основного composable для UI
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyPowerApp() {
    val devices = listOf("UPS1", "UPS2", "UPS3")
    // Список пристроїв для моніторингу
    var selectedUpsTab by remember { mutableStateOf(0) }
    // Поточна вкладка UPS
    var selectedMainTab by remember { mutableStateOf(0) }
    // Поточна основна вкладка (0 = Моніторинг, 1 = Журнал)

    val energyChannel = remember { Channel<EnergyData>(Channel.UNLIMITED) }
    // Канал для передачі даних енергоспоживання
    var energyList by remember { mutableStateOf(listOf<EnergyData>()) }
    // Список останніх даних енергоспоживання
    var eventLog by remember { mutableStateOf(listOf<String>()) }
    // Журнал подій (з попередженнями та критичними станами)

    var simulationRunning by remember { mutableStateOf(false) }
    // Флаг, чи запущена симуляція
    var generatorJob by remember { mutableStateOf<Job?>(null) }
    // Змінна для збереження корутини генератора
    val scope = rememberCoroutineScope()
    // CoroutineScope для запуску корутин у Compose

    // Корутина для обробки даних з каналу
    LaunchedEffect(Unit) {
        scope.launch {
            for (data in energyChannel) {
                // Додаємо нові дані в список, обмежуючи його до 100 записів
                energyList = listOf(data) + energyList.take(99)
                if (data.status != "Normal") {
                    // Форматуємо час події
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(data.timestamp))
                    // Додаємо запис у журнал подій, обмежуючи його до 100 записів
                    eventLog = listOf("${data.deviceName} - ${data.status} at $timeStr") + eventLog.take(99)
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Система аварійного електропостачання") }) }
        // Верхня панель з назвою програми
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Основна колонка з урахуванням padding Scaffold

            // Основні вкладки
            TabRow(selectedTabIndex = selectedMainTab) {
                Tab(selected = selectedMainTab == 0, onClick = { selectedMainTab = 0 }) { Text("Моніторинг") }
                Tab(selected = selectedMainTab == 1, onClick = { selectedMainTab = 1 }) { Text("Журнал подій") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Відступ між вкладками і вмістом

            when (selectedMainTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Вкладки для окремих UPS
                        TabRow(selectedTabIndex = selectedUpsTab) {
                            devices.forEachIndexed { index, device ->
                                Tab(selected = selectedUpsTab == index, onClick = { selectedUpsTab = index }) {
                                    Text(device)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Відступ між вкладками UPS і кнопками

                        // Кнопки керування симуляцією
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                // Запуск симуляції, якщо вона ще не запущена
                                if (generatorJob == null || generatorJob?.isCancelled == true) {
                                    simulationRunning = true
                                    generatorJob = scope.launch {
                                        generateEnergyData(energyChannel, devices) { simulationRunning }
                                    }
                                }
                            }) { Text("Почати симуляцію") }

                            Button(onClick = {
                                // Зупинка симуляції
                                simulationRunning = false
                                generatorJob?.cancel()
                                generatorJob = null
                            }) { Text("Зупинити симуляцію") }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Відступ перед списком даних

                        // Вивід даних для обраного UPS
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(energyList.filter { it.deviceName == devices[selectedUpsTab] }) { data ->
                                // Форматуємо час для відображення
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                    .format(Date(data.timestamp))
                                // Колір картки залежно від статусу
                                val cardColor = when (data.status) {
                                    "Normal" -> Color(0xFFDFFFE0)    // Зелений для нормального стану
                                    "Warning" -> Color(0xFFFFF8DC)   // Жовтий для попередження
                                    "Critical" -> Color(0xFFFFC0C0)  // Червоний для критичного стану
                                    else -> Color.White
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(cardColor),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        // Відображення даних у картці
                                        Text("Пристрій: ${data.deviceName}")
                                        Text("Час: $timeStr")
                                        Text("Напруга: ${"%.2f".format(data.voltage)} В")
                                        Text("Струм: ${"%.2f".format(data.current)} A")
                                        Text("Статус: ${data.status}")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Вкладка "Журнал подій"
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(eventLog) { event ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Text(event, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Генератор даних для симуляції UPS
suspend fun generateEnergyData(
    channel: Channel<EnergyData>,          // Канал для відправки даних
    devices: List<String>,                 // Список пристроїв
    simulationFlag: () -> Boolean          // Флаг для перевірки, чи триває симуляція
) {
    while (simulationFlag()) {              // Поки симуляція активна
        devices.forEach { device ->        // Для кожного пристрою
            val voltage = Random.nextDouble(210.0, 250.0)
            // Генерація випадкової напруги
            val current = Random.nextDouble(0.5, 15.0)
            // Генерація випадкового струму

            val status = when {
                voltage < 220 || voltage > 240 -> "Warning"  // Попередження, якщо напруга виходить за межі
                current > 12 -> "Critical"                   // Критично, якщо струм перевищує 12А
                else -> "Normal"                             // Нормальний стан
            }

            val data = EnergyData(device, System.currentTimeMillis(), voltage, current, status)
            // Створюємо об'єкт даних енергоспоживання
            channel.send(data)
            // Відправляємо дані у канал для обробки
        }
        delay(1000L)  // Затримка 1 секунда між генераціями
    }
}
