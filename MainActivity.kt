package com.example.emergencypowersystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

// -------------------------------------------------------------
// Модель стану енергоблоку
// -------------------------------------------------------------
data class PowerUnitStatus( // Оголошення data class для збереження даних блоку
    val unitId: String, // Ідентифікатор блоку
    val voltage: Double, // Напруга у вольтах
    val current: Double, // Струм у амперах
    val temperature: Double, // Температура у градусах Цельсія
    val timestamp: Long // Час у мілісекундах
)

// -------------------------------------------------------------
// Генератор даних
// -------------------------------------------------------------
class EmergencyPowerDataGenerator { // Клас для генерації випадкових даних
    private val unitIds = listOf("BACKUP_GENERATOR_A", "BACKUP_GENERATOR_B", "UPS_MODULE_1", "UPS_MODULE_2") // Список блоків

    fun generateSingle(unitId: String): PowerUnitStatus { // Генерація даних для одного блоку
        return PowerUnitStatus(
            unitId = unitId,
            voltage = (380..400).random() + Math.random(), // Випадкова напруга
            current = (10..40).random() + Math.random(), // Випадковий струм
            temperature = (20..55).random() + Math.random(), // Випадкова температура
            timestamp = System.currentTimeMillis() // Поточний час
        )
    }

    fun generateBurst(): List<PowerUnitStatus> = unitIds.map { generateSingle(it) } // Генерація даних для всіх блоків
}

// -------------------------------------------------------------
// Менеджер потоків
// -------------------------------------------------------------
class EmergencyPowerFlowManager { // Клас для керування потоками генерації
    private val generator = EmergencyPowerDataGenerator() // Створення генератора
    private val jobs = mutableListOf<Job>() // Список активних потоків

    fun startStreams(onData: (PowerUnitStatus) -> Unit) { // Запуск потоків
        stopStreams() // Зупинка попередніх потоків
        generator.generateBurst().forEach { unit -> // Для кожного блоку
            val job = CoroutineScope(Dispatchers.Default).launch { // Запуск корутини
                while (isActive) { // Поки активна
                    delay((500L..2000L).random()) // Затримка
                    onData(generator.generateSingle(unit.unitId)) // Генерація нових даних
                }
            }
            jobs.add(job) // Додавання потоку у список
        }
    }

    fun stopStreams() { // Зупинка всіх потоків
        jobs.forEach { it.cancel() } // Скасування кожного
        jobs.clear() // Очищення списку
    }

    fun generateOnce(): List<PowerUnitStatus> = generator.generateBurst() // Генерація даних один раз
}

// -------------------------------------------------------------
// Основна Activity з навігацією
// -------------------------------------------------------------
class MainActivity : ComponentActivity() { // Головна Activity
    override fun onCreate(savedInstanceState: Bundle?) { // Метод створення
        super.onCreate(savedInstanceState) // Виклик базового методу
        enableEdgeToEdge() // Повноекранний режим
        setContent { // Встановлення UI
            val navController = rememberNavController() // Контролер навігації
            NavHost(navController = navController, startDestination = "main") { // Навігаційний хост
                composable("main") { EmergencyPowerScreen(navController) } // Головний екран
                composable("log") { EventLogScreen(navController) } // Екран журналу
            }
        }
    }
}

// -------------------------------------------------------------
// Головний екран моніторингу
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyPowerScreen(navController: NavController) { // Компонент головного екрану
    val flowManager = remember { EmergencyPowerFlowManager() } // Менеджер потоків
    val dataList = remember { mutableStateListOf<PowerUnitStatus>() } // Список даних
    var streaming by remember { mutableStateOf(false) } // Статус потоків

    LaunchedEffect(streaming) { // Ефект при зміні статусу
        if (streaming) {
            flowManager.startStreams { newData -> // Запуск потоків
                dataList.add(0, newData) // Додавання даних
                if (dataList.size > 100) dataList.removeLast() // Обмеження списку
            }
        } else flowManager.stopStreams() // Зупинка потоків
    }

    Scaffold( // Шаблон інтерфейсу
        topBar = {
            TopAppBar(title = { Text("Система аварійного електропостачання") }) // Верхня панель
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) { // Основна колонка

            Column(Modifier.fillMaxWidth().padding(8.dp)) { // Колонка кнопок
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { streaming = true }) { Text("Старт") } // Кнопка старт
                    Button(onClick = { streaming = false }) { Text("Стоп") } // Кнопка стоп
                }
                Spacer(Modifier.height(8.dp)) // Відступ
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { dataList.clear() }) { Text("Очистити") } // Кнопка очистити
                    Button(
                        onClick = {
                            dataList.clear()
                            dataList.addAll(flowManager.generateOnce()) // Оновлення даних
                        },
                        modifier = Modifier.widthIn(min = 120.dp) // Мінімальна ширина
                    ) {
                        Text("Оновити всі") // Кнопка оновити всі
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) { // Прокручуваний список
                items(dataList) { item ->
                    PowerUnitCard(item) // Картка блоку
                }
            }

            Button( // Кнопка переходу до журналу
                onClick = { navController.navigate("log") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("Перейти до журналу подій") // Текст кнопки
            }
        }
    }
}

// -------------------------------------------------------------
// Екран журналу подій
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLogScreen(navController: NavController) { // Компонент журналу
    val generator = remember { EmergencyPowerDataGenerator() } // Генератор
    val allData = remember { generator.generateBurst() } // Дані для журналу

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Журнал подій") }, // Заголовок
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Кнопка назад
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад") // Іконка назад
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) { // Список карток
            items(allData) { item ->
                PowerUnitCard(item) // Картка блоку
            }
        }
    }
}

// -------------------------------------------------------------
// Картка енергоблоку
// -------------------------------------------------------------
@Composable
fun PowerUnitCard(data: PowerUnitStatus) { // Компонент картки
    val formattedTime = remember(data.timestamp) { // Форматування часу
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) // Формат дати
        sdf.format(Date(data.timestamp)) // Перетворення часу
    }

    Card( // Картка
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Тінь картки
    ) {
        Column(Modifier.padding(12.dp)) { // Вміст картки
            Text("Блок: ${data.unitId}", style = MaterialTheme.typography.titleMedium) // Назва блоку
            Text("Напруга: %.2f В".format(data.voltage)) // Напруга
            Text("Струм: %.2f А".format(data.current)) // Струм
            Text("Температура: %.2f °C".format(data.temperature)) // Температура
            Text("Час: $formattedTime") // Час
        }
    }
}
