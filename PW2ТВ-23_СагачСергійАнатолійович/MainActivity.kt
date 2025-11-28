package com.example.emergencysupplypowersystem

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergencysupplypowersystem.ui.theme.EmergencySupplyPowerSystemTheme
import kotlin.math.pow

/**
 * Головний клас застосунку.
 * Точка входу в програму. Встановлює тему та головний екран із розрахунками.
 */
class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Встановлюємо контент застосунку з головним екраном
        setContent {
            EmergencySupplyPowerSystemTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) {
                    EnergyCalculatorScreen() // Запуск основного екрану
                }
            }
        }
    }
}

// ============================================================================ //
//                      РОЗРАХУНКОВІ ТА ФІЗИЧНІ ФУНКЦІЇ                         //
// ============================================================================ //

/**
 * Розрахунок спожитої енергії.
 * Використовує просту арифметичну операцію: E = P × t
 * @param powerWatts Потужність приладу у Ватах
 * @param hours Час роботи у годинах
 * @return Спожита енергія у кВт⋅год
 */
fun calculateEnergy(powerWatts: Double, hours: Double): Double {
    return (powerWatts * hours) / 1000.0
}

/**
 * Розрахунок сумарного енергоспоживання для довільної кількості приладів.
 * Демонструє використання параметра vararg (довільна кількість аргументів).
 * Кожен елемент — пара (потужність, час роботи).
 * @return Сумарне споживання енергії у кВт⋅год
 */
fun totalEnergyConsumption(vararg devices: Pair<Double, Double>): Double {
    return devices.sumOf { (power, hours) -> power * hours } / 1000.0
}

/**
 * Розрахунок часу автономної роботи від акумулятора.
 * Алгоритм: t = E_батареї / E_споживання
 * @param batteryCapacityWh Ємність батареї у Вт⋅год
 * @param totalConsumptionWh Загальне енергоспоживання у Вт⋅год
 * @return Час автономної роботи у годинах
 */
fun calculateBackupTime(batteryCapacityWh: Double, totalConsumptionWh: Double): Double {
    if (totalConsumptionWh <= 0.0) return 0.0
    return batteryCapacityWh / totalConsumptionWh
}

/**
 * Моделювання процесу розряду батареї з урахуванням:
 *  - ККД системи (efficiency)
 *  - щорічної деградації (degradation)
 *  - кількості років експлуатації (years)
 * Формула: E_залиш = E_початкова × η × (1 - деградація)^роки
 */
fun simulateBatteryDischarge(
    initialCapacityWh: Double,   // Початкова ємність батареї (Вт⋅год)
    efficiency: Double,          // ККД (0.0 - 1.0)
    degradation: Double,         // Щорічне зниження ємності (0.0 - 1.0)
    years: Int                   // Кількість років використання
): Double {
    return initialCapacityWh * efficiency * (1 - degradation).pow(years)
}

/**
 * Модель втрат енергії у проводці.
 * Формула: P_втрат = I^2 × R
 * @param current Струм (А)
 * @param resistance Опір (Ом)
 * @return Втрати енергії (Вт)
 */
fun calculateLineLoss(current: Double, resistance: Double): Double {
    return current.pow(2) * resistance
}

// ============================================================================ //
//                           ІНТЕРФЕЙС КОРИСТУВАЧА                              //
// ============================================================================ //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyCalculatorScreen() {
    // Змінні для введення даних користувачем
    var power by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var battery by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var resistance by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") } // нове поле — роки експлуатації батареї

    // Змінні для результатів обчислень
    var resultEnergy by remember { mutableStateOf(0.0) }
    var backupTime by remember { mutableStateOf(0.0) }
    var losses by remember { mutableStateOf(0.0) }
    var degradedBattery by remember { mutableStateOf(0.0) }

    // Розташування елементів у колонці
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Система аварійного електропостачання",
            fontSize = 20.sp
        )

        // Поле для введення потужності
        OutlinedTextField(
            value = power,
            onValueChange = { power = it },
            label = { Text("Потужність приладу (Вт)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Поле для введення часу роботи
        OutlinedTextField(
            value = hours,
            onValueChange = { hours = it },
            label = { Text("Час роботи (год)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Поле для введення ємності батареї
        OutlinedTextField(
            value = battery,
            onValueChange = { battery = it },
            label = { Text("Ємність батареї (Вт⋅год)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Поле для введення років використання батареї
        OutlinedTextField(
            value = years,
            onValueChange = { years = it },
            label = { Text("Роки експлуатації батареї") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Додаткові параметри для розрахунку втрат у проводах
        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Струм (А)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = resistance,
            onValueChange = { resistance = it },
            label = { Text("Опір лінії (Ом)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Кнопка для запуску розрахунків
        Button(
            onClick = {
                val p = power.toDoubleOrNull() ?: 0.0
                val h = hours.toDoubleOrNull() ?: 0.0
                val b = battery.toDoubleOrNull() ?: 0.0
                val i = current.toDoubleOrNull() ?: 0.0
                val r = resistance.toDoubleOrNull() ?: 0.0
                val y = years.toIntOrNull() ?: 0

                // Викликаємо наші функції
                resultEnergy = calculateEnergy(p, h)
                backupTime = calculateBackupTime(b, p * h)
                losses = calculateLineLoss(i, r)
                degradedBattery = simulateBatteryDischarge(b, 0.9, 0.05, y)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Обчислити")
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Відображення результатів
        Text(text = "Споживання: ${"%.3f".format(resultEnergy)} кВт⋅год")
        Text(text = "Час автономної роботи: ${"%.2f".format(backupTime)} год")
        Text(text = "Втрати у проводці: ${"%.2f".format(losses)} Вт")

        Spacer(modifier = Modifier.height(12.dp))

        // Відображення деградації батареї
        Text(text = "Ємність батареї через $years р.: ${"%.1f".format(degradedBattery)} Вт⋅год")

        Spacer(modifier = Modifier.height(12.dp))

        // Приклад використання vararg для кількох приладів
        val total = totalEnergyConsumption(100.0 to 2.0, 60.0 to 3.0, 200.0 to 1.5)
        Text(text = "Загальне споживання (3 прилади): ${"%.3f".format(total)} кВт⋅год")
    }
}