package com.example.myapplication

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

// Головний клас Activity — це "точка входу" в Android-додаток.
// ComponentActivity — це базовий клас, який дозволяє використовувати Jetpack Compose для створення інтерфейсу.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContent — це функція, яка вказує, що UI буде створено за допомогою Jetpack Compose.
        setContent {
            // Використання теми, яку створено при створенні проєкту
            MyApplicationTheme {
                // Surface — це контейнер, який задає фон та розміри для всього екрана
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Викликаємо головну функцію з інтерфейсом системи аварійного живлення
                    EmergencyPowerSystemScreen()
                }
            }
        }
    }
}

// --- ОСНОВНА КОМПОЗИЦІЙНА ФУНКЦІЯ (ГОЛОВНИЙ ІНТЕРФЕЙС) ---
@Composable
fun EmergencyPowerSystemScreen() {

    // ---------------- ОГОЛОШЕННЯ ЗМІННИХ ----------------
    // remember + mutableStateOf дозволяють "зберігати стан" змінної у Compose.
    // Якщо значення змінюється — інтерфейс автоматично оновлюється.
    var userName by remember { mutableStateOf("") }        // Ім’я користувача, введене у поле
    var greetingText by remember { mutableStateOf("") }     // Текст привітання, який відображається після натискання кнопки

    var inputPowerLevel by remember { mutableStateOf("") }  // Поле для введення числового значення (напруга)
    var statusMessage by remember { mutableStateOf("") }    // Повідомлення про стан живлення

    // Додаткові приклади змінних різних типів:
    val systemName: String = "Система аварійного електропостачання" // текстова змінна (String)
    var isSystemOnline: Boolean = true                              // логічна змінна (true/false)
    val nominalVoltage: Double = 220.0                              // дійсне число (Double)
    val backupBatteryCount: Int = 2                                 // ціле число (Int)

    // Column — вертикальне розміщення елементів один під одним
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // ---- 1. Привітальний текст ----
        // Відображає назву системи у верхній частині екрана
        Text(
            text = systemName,
            style = MaterialTheme.typography.headlineSmall
        )

        // Поле для введення імені користувача
        TextField(
            value = userName,
            onValueChange = { userName = it }, // зберігаємо введене значення в змінну
            label = { Text("Введіть ваше ім'я") }
        )

        // Кнопка, при натисканні якої виводиться привітання
        Button(onClick = {
            // if — умовний оператор:
            // якщо ім’я не пусте, то показуємо привітання,
            // інакше — просимо ввести ім’я
            greetingText = if (userName.isNotBlank()) {
                "Вітаємо, $userName! Система працює у нормальному режимі."
            } else {
                "Будь ласка, введіть ім’я для ідентифікації оператора."
            }
        }) {
            Text("Привітатися")
        }

        // Відображаємо текст привітання, якщо він не пустий
        if (greetingText.isNotEmpty()) {
            Text(text = greetingText)
        }

        // Divider — горизонтальна лінія для розділення частин інтерфейсу
        Divider(modifier = Modifier.padding(vertical = 8.dp))


        // ---- 2. Введення числового значення ----
        // Поле для введення рівня напруги (лише числа)
        TextField(
            value = inputPowerLevel,
            onValueChange = { inputPowerLevel = it },
            label = { Text("Введіть рівень вхідної напруги (В)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Кнопка для перевірки стану системи
        Button(onClick = {
            // Пробуємо перетворити введений текст у число
            val voltage = inputPowerLevel.toDoubleOrNull()

            // Якщо перетворення не вдалось — показуємо повідомлення про помилку
            statusMessage = if (voltage == null) {
                "Будь ласка, введіть числове значення!"
            } else {
                // Якщо число введене, використовуємо when — аналог switch-case.
                when {
                    // Напруга нижча за 180 В — аварійний режим, живлення від генератора
                    voltage < 180 -> "Напруга занадто низька! Активується аварійне живлення від генератора."

                    // Нормальна напруга — робота від основної мережі
                    voltage in 180.0..240.0 -> "Напруга в нормі. Живлення здійснюється від основної мережі."

                    // Якщо вище 240 В — перенапруга, система переходить у захисний режим
                    else -> "Перенапруга! Система переходить у режим захисту."
                }
            }
        }) {
            Text("Перевірити стан")
        }

        // Відображаємо результат перевірки напруги
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // ---- 3. Виведення типів та змінних (демонстрація оголошення) ----
        Spacer(modifier = Modifier.height(16.dp))
        Text("Інформація про систему:")
        Text("Онлайн: $isSystemOnline")
        Text("Номінальна напруга: $nominalVoltage В")
        Text("Кількість резервних батарей: $backupBatteryCount")
    }
}

// --- Попередній перегляд у Android Studio ---
// Функція @Preview дозволяє побачити, як виглядатиме екран без запуску на пристрої
@Preview(showBackground = true)
@Composable
fun PreviewEmergencyPowerSystemScreen() {
    MyApplicationTheme {
        EmergencyPowerSystemScreen()
    }
}
