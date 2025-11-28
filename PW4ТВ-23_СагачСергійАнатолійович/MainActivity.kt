package com.example.emergencypowersupplysystem

// Імпорт стандартних Android-класів
import android.os.Bundle

// Імпорт базового класу Activity для Compose
import androidx.activity.ComponentActivity

// Імпорт функції для встановлення інтерфейсу
import androidx.activity.compose.setContent

// Імпорти для побудови інтерфейсу
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Імпорт теми застосунку
import com.example.emergencypowersupplysystem.ui.theme.EmergencyPowerSupplySystemTheme

// Імпорт для делегованих властивостей
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// ======================================================
// 1. DATA CLASS — структура даних аварійного живлення
// ======================================================
data class EmergencyPowerUnit(

    // Унікальний ідентифікатор генератора
    val id: Int,

    // Назва генератора
    val name: String,

    // Максимальна потужність у кВт
    val maxPower: Double,

    // Статус активності генератора
    val isActive: Boolean
)

// ======================================================
// 2. DELEGATED PROPERTY — делегована властивість
// ======================================================
class PowerStatusDelegate : ReadWriteProperty<Any?, Boolean> {

    // Приватна змінна для збереження стану
    private var value: Boolean = false

    // Метод отримання значення
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return value
    }

    // Метод встановлення значення
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        this.value = value
    }
}

// ======================================================
// 3. EXTENSION-ФУНКЦІЇ — розширення стандартних класів
// ======================================================

// Перевірка перевантаження генератора
fun EmergencyPowerUnit.isOverloaded(currentLoad: Double): Boolean {
    return currentLoad > maxPower
}

// Форматування потужності у текст
fun Double.formatPower(): String {
    return String.format("%.2f кВт", this)
}

// Перевірка, чи число є від’ємним
fun Double.isNegative(): Boolean {
    return this < 0
}

// Перевірка, чи навантаження нормальне
fun EmergencyPowerUnit.isLoadNormal(load: Double): Boolean {
    return load in 0.0..maxPower
}

// Перевірка, чи система активна
fun EmergencyPowerUnit.isSystemReady(): Boolean {
    return isActive
}

// Перетворення логічного стану у текст
fun Boolean.toStatusText(): String {
    return if (this) "Система увімкнена" else "Система вимкнена"
}

// ======================================================
// 4. УНІВЕРСАЛЬНІ ФУНКЦІЇ + TRY / CATCH
// ======================================================
class EmergencyPowerManager {

    // Делегована змінна стану системи
    var systemStatus: Boolean by PowerStatusDelegate()

    // Функція запуску системи
    fun startSystem(): String {
        return try {
            systemStatus = true
            "Система аварійного електропостачання запущена"
        } catch (e: Exception) {
            "Помилка запуску системи"
        }
    }

    // Функція зупинки системи
    fun stopSystem(): String {
        return try {
            systemStatus = false
            "Система аварійного електропостачання зупинена"
        } catch (e: Exception) {
            "Помилка зупинки системи"
        }
    }

    // Безпечний розрахунок навантаження
    fun calculateLoad(unit: EmergencyPowerUnit, currentLoad: Double): String {
        return try {

            // Якщо значення менше нуля
            if (currentLoad.isNegative()) {
                "Помилка: введено від’ємне значення навантаження"
            }
            // Якщо є перевантаження
            else if (unit.isOverloaded(currentLoad)) {
                "Перевантаження. Поточна потужність: ${currentLoad.formatPower()}"
            }
            // Якщо все в нормі
            else {
                "Навантаження в нормі: ${currentLoad.formatPower()}"
            }

        } catch (e: Exception) {
            "Помилка обробки навантаження"
        }
    }
}

// ======================================================
// 5. ГОЛОВНИЙ ЕКРАН ЗАСТОСУНКУ (UI)
// ======================================================
class MainActivity : ComponentActivity() {

    // Головний метод запуску застосунку
    override fun onCreate(savedInstanceState: Bundle?) {

        // Стандартний виклик батьківського класу
        super.onCreate(savedInstanceState)

        // Створення об’єкта генератора
        val generator = EmergencyPowerUnit(
            id = 1,
            name = "Резервний дизельний генератор",
            maxPower = 50.0,
            isActive = true
        )

        // Створення менеджера аварійного живлення
        val manager = EmergencyPowerManager()

        // Встановлення графічного інтерфейсу
        setContent {

            // Підключення теми застосунку
            EmergencyPowerSupplySystemTheme {

                // Змінна для відображення результату
                var resultText by remember {
                    mutableStateOf("Система не активна")
                }

                // Змінна для поля введення навантаження
                var loadInput by remember {
                    mutableStateOf("")
                }

                // Вертикальний контейнер елементів
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),

                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Заголовок екрану
                    Text(
                        text = "Система аварійного електропостачання",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Поле введення навантаження
                    OutlinedTextField(
                        value = loadInput,
                        onValueChange = { loadInput = it },
                        label = { Text("Введіть поточне навантаження (кВт)") }
                    )

                    // Кнопка запуску системи
                    Button(onClick = {
                        resultText = manager.startSystem()
                    }) {
                        Text("Запустити систему")
                    }

                    // Кнопка зупинки системи
                    Button(onClick = {
                        resultText = manager.stopSystem()
                    }) {
                        Text("Зупинити систему")
                    }

                    // Кнопка перевірки навантаження
                    Button(onClick = {

                        // Безпечне перетворення тексту у число
                        val load = loadInput.toDoubleOrNull()

                        // Якщо число коректне
                        resultText = if (load != null) {
                            manager.calculateLoad(generator, load)
                        }
                        // Якщо користувач ввів не число
                        else {
                            "Помилка: введіть коректне числове значення"
                        }

                    }) {
                        Text("Перевірити навантаження")
                    }

                    // Розділювальна лінія
                    Divider(thickness = 2.dp)

                    // Вивід результату
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
