package com.example.emergencysupplypowersystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

// ---------------------------------------------------------------------------
//  ІНТЕРФЕЙС ДЖЕРЕЛА ЖИВЛЕННЯ
// ---------------------------------------------------------------------------
// PowerSource описує базову поведінку будь-якого джерела електроенергії
// (основне живлення, акумулятор, генератор тощо)
interface PowerSource {
    val name: String            // Назва джерела
    var powerOutput: Double     // Поточна потужність у Вт
    fun supplyPower(load: Double): Boolean  // Постачання енергії під навантаження
    fun status(): String        // Текстовий опис поточного стану
}

// ---------------------------------------------------------------------------
//  АБСТРАКТНИЙ КЛАС
// ---------------------------------------------------------------------------
// AbstractPowerSource реалізує базову логіку спільну для всіх типів живлення.
abstract class AbstractPowerSource(
    override val name: String,
    override var powerOutput: Double
) : PowerSource {
    override fun status(): String = "$name: потужність = $powerOutput Вт"
}

// ---------------------------------------------------------------------------
//  КЛАС ОСНОВНОГО ЖИВЛЕННЯ
// ---------------------------------------------------------------------------
class MainPower : AbstractPowerSource("Основне живлення", 1000.0) {
    var isAvailable = true  // Чи доступне основне живлення

    // Постачаємо енергію тільки якщо живлення доступне і потужності вистачає
    override fun supplyPower(load: Double): Boolean {
        return if (isAvailable && powerOutput >= load) {
            powerOutput -= load * 0.01  // Споживання потужності
            true
        } else false
    }

    override fun status(): String =
        if (isAvailable) "Основне живлення активне" else "Основне живлення недоступне"
}

// ---------------------------------------------------------------------------
//  КЛАС АКУМУЛЯТОРА
// ---------------------------------------------------------------------------
// Містить заряд (chargeLevel), який зменшується під навантаженням
class Battery(var capacity: Double, var chargeLevel: Double) :
    AbstractPowerSource("Акумулятор", 500.0) {

    override fun supplyPower(load: Double): Boolean {
        return if (chargeLevel > 0 && powerOutput >= load) {
            chargeLevel = max(0.0, chargeLevel - load * 0.005) // Втрата заряду
            true
        } else false
    }

    // Заряджаємо акумулятор на певний відсоток
    fun recharge(amount: Double) {
        chargeLevel = max(0.0, minOf(100.0, chargeLevel + amount))
    }

    override fun status(): String =
        "Батарея: заряд = ${"%.1f".format(chargeLevel)}%"
}

// ---------------------------------------------------------------------------
//  КЛАС ГЕНЕРАТОРА
// ---------------------------------------------------------------------------
// Працює, поки є паливо. Можна поповнювати запас палива.
class Generator : AbstractPowerSource("Генератор", 1500.0) {
    var fuelLevel = 100.0  // Рівень палива (%)

    override fun supplyPower(load: Double): Boolean {
        return if (fuelLevel > 0 && powerOutput >= load) {
            fuelLevel = max(0.0, fuelLevel - load * 0.01) // Витрата палива
            true
        } else false
    }

    // Доливання палива
    fun refuel(amount: Double) {
        fuelLevel = max(0.0, minOf(100.0, fuelLevel + amount))
    }

    override fun status(): String =
        "Генератор: паливо = ${"%.1f".format(fuelLevel)}%"
}

// ---------------------------------------------------------------------------
//  СИСТЕМА АВАРІЙНОГО ЕЛЕКТРОПОСТАЧАННЯ
// ---------------------------------------------------------------------------
// Моделює автоматичне перемикання між джерелами залежно від доступності.
class EmergencySupplySystem(
    private val mainPower: MainPower,
    private val battery: Battery,
    private val generator: Generator
) {
    var activeSource: PowerSource = mainPower  // Поточне активне джерело

    // Оновлення системи: вибираємо доступне джерело
    fun updateSystem(load: Double) {
        when {
            mainPower.isAvailable && mainPower.supplyPower(load) -> activeSource = mainPower
            battery.supplyPower(load) -> activeSource = battery
            generator.supplyPower(load) -> activeSource = generator
            else -> activeSource = object : PowerSource {
                override val name = "Немає живлення"
                override var powerOutput = 0.0
                override fun supplyPower(load: Double) = false
                override fun status() = "Система знеструмлена!"
            }
        }
    }

    fun systemStatus(): String = activeSource.status()
}

// ---------------------------------------------------------------------------
//  ГОЛОВНА АКТИВНІСТЬ
// ---------------------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Встановлюємо головний екран Jetpack Compose
        setContent {
            EmergencySupplyApp()
        }
    }
}

// ---------------------------------------------------------------------------
//  ОСНОВНИЙ COMPOSE ІНТЕРФЕЙС
// ---------------------------------------------------------------------------
@Composable
fun EmergencySupplyApp() {
    // Ініціалізація об'єктів системи
    val mainPower = remember { MainPower() }
    val battery = remember { Battery(100.0, 70.0) }
    val generator = remember { Generator() }
    val system = remember { EmergencySupplySystem(mainPower, battery, generator) }

    var selectedTab by remember { mutableStateOf(0) } // Для навігації між вкладками

    Scaffold(
        // Нижня панель навігації
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Monitor, contentDescription = null) },
                    label = { Text("Моніторинг") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Основне живлення") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BatteryFull, contentDescription = null) },
                    label = { Text("Акумулятор") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.EnergySavingsLeaf, contentDescription = null) },
                    label = { Text("Генератор") }
                )
            }
        }
    ) { padding ->
        // Перемикання між вкладками
        when (selectedTab) {
            0 -> MonitoringScreen(system, battery, generator, mainPower, Modifier.padding(padding))
            1 -> MainPowerScreen(system, mainPower, Modifier.padding(padding))
            2 -> BatteryScreen(system, battery, Modifier.padding(padding))
            3 -> GeneratorScreen(system, generator, Modifier.padding(padding))
        }
    }
}

// ---------------------------------------------------------------------------
//  ВКЛАДКА 1: АКТИВНИЙ МОНІТОРИНГ
// ---------------------------------------------------------------------------
// Автоматично оновлює дані про навантаження та стан системи
@Composable
fun MonitoringScreen(system: EmergencySupplySystem, battery: Battery, generator: Generator, mainPower: MainPower, modifier: Modifier) {
    var load by remember { mutableStateOf(300.0) }
    var status by remember { mutableStateOf(system.systemStatus()) }

    // Симуляція реального процесу — навантаження змінюється щосекунди
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            load = Random.nextDouble(200.0, 800.0)
            mainPower.isAvailable = Random.nextInt(0, 10) > 2
            system.updateSystem(load)
            status = system.systemStatus()
        }
    }

    // Відображення інтерфейсу
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Активний моніторинг", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        PowerCard("Поточне джерело", system.activeSource.name, Color(0xFF03DAC5))
        PowerCard("Статус системи", status, Color(0xFFBB86FC))
        PowerCard("Навантаження", "${"%.1f".format(load)} Вт", Color(0xFFCF6679))
        Text("Батарея: ${"%.1f".format(battery.chargeLevel)}% | Паливо: ${"%.1f".format(generator.fuelLevel)}%", color = Color.LightGray)
    }
}

// ---------------------------------------------------------------------------
//  ВКЛАДКА 2: ОСНОВНЕ ЖИВЛЕННЯ
// ---------------------------------------------------------------------------
@Composable
fun MainPowerScreen(system: EmergencySupplySystem, mainPower: MainPower, modifier: Modifier) {
    var currentStatus by remember { mutableStateOf(mainPower.status()) }

    // Автоматичне оновлення стану
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            mainPower.isAvailable = Random.nextInt(0, 10) > 2
            currentStatus = mainPower.status()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Основне живлення", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        PowerCard("Статус", currentStatus, Color(0xFF03DAC5))
    }
}

// ---------------------------------------------------------------------------
//  ВКЛАДКА 3: АКУМУЛЯТОР
// ---------------------------------------------------------------------------
@Composable
fun BatteryScreen(system: EmergencySupplySystem, battery: Battery, modifier: Modifier) {
    var rechargeAmount by remember { mutableStateOf(10.0) }
    var currentStatus by remember { mutableStateOf(battery.status()) }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Акумулятор", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        PowerCard("Заряд", currentStatus, Color(0xFFBB86FC))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            battery.recharge(rechargeAmount)
            system.updateSystem(0.0)
            currentStatus = battery.status()
        }) {
            Text("Поповнити заряд на ${rechargeAmount}%")
        }
    }
}

// ---------------------------------------------------------------------------
//  ВКЛАДКА 4: ГЕНЕРАТОР
// ---------------------------------------------------------------------------
@Composable
fun GeneratorScreen(system: EmergencySupplySystem, generator: Generator, modifier: Modifier) {
    var refuelAmount by remember { mutableStateOf(10.0) }
    var currentStatus by remember { mutableStateOf(generator.status()) }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Генератор", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        PowerCard("Паливо", currentStatus, Color(0xFF03DAC5))
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            generator.refuel(refuelAmount)
            system.updateSystem(0.0)
            currentStatus = generator.status()
        }) {
            Text("Долити паливо на ${refuelAmount}%")
        }
    }
}

// ---------------------------------------------------------------------------
//  КОМПОНЕНТ КАРТКИ ВІДОБРАЖЕННЯ ІНФОРМАЦІЇ
// ---------------------------------------------------------------------------
@Composable
fun PowerCard(title: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 18.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // Область значення
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(value, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
