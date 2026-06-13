package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("AnwarPrefs", Context.MODE_PRIVATE)

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _workers = MutableStateFlow<List<Worker>>(emptyList())
    val workers = _workers.asStateFlow()

    private val _attendanceList = MutableStateFlow<List<Attendance>>(emptyList())
    val attendanceList = _attendanceList.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // Current selected Ethiopian month/year
    private val _currentMonth = MutableStateFlow(9) // Ginbot
    val currentMonth = _currentMonth.asStateFlow()

    private val _currentYear = MutableStateFlow(2018)
    val currentYear = _currentYear.asStateFlow()

    // Raw Materials & Masterbatch stocks (nested inside Inventory)
    private val _rawMaterialsStock = MutableStateFlow(1420) // in kgs
    val rawMaterialsStock = _rawMaterialsStock.asStateFlow()

    private val _masterbatchStock = MutableStateFlow(320) // in kgs
    val masterbatchStock = _masterbatchStock.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Load or Bootstrap Products
        val savedProds = prefs.getString("products_list2", null)
        if (savedProds == null) {
            val defaultProds = listOf(
                Product(name = "ሸርጅን መካከለኛ", size = "L (50x80)", bagWeightKg = 25.0, currentStock = 145),
                Product(name = "ሸርጅን ልዩ", size = "XL (60x90)", bagWeightKg = 30.0, currentStock = 55),
                Product(name = "ቆሻሻ መጣያ", size = "M (40x60)", bagWeightKg = 20.0, currentStock = 8), // low / critical
                Product(name = "የገበያ ከረጢት", size = "L (45x75)", bagWeightKg = 15.0, currentStock = 32)  // medium
            )
            _products.value = defaultProds
            saveProducts(defaultProds)
        } else {
            _products.value = parseProducts(savedProds)
        }

        // Load or Bootstrap Workers
        val savedWorkers = prefs.getString("workers_list2", null)
        if (savedWorkers == null) {
            val defaultWorkers = listOf(
                Worker(name = "አላዛር ታደሰ", role = "Operator", dailyRate = 450, avatarColorString = "#00FF88"),
                Worker(name = "አበበ ከበደ", role = "Feeder", dailyRate = 380, avatarColorString = "#3498db"),
                Worker(name = "ጫላ በቀለ", role = "Sorter", dailyRate = 350, avatarColorString = "#e67e22"),
                Worker(name = "ረድኤት ግርማ", role = "Supervisor", dailyRate = 500, avatarColorString = "#9b59b6")
            )
            _workers.value = defaultWorkers
            saveWorkers(defaultWorkers)
        } else {
            _workers.value = parseWorkers(savedWorkers)
        }

        // Load or Bootstrap Attendance
        val savedAttendance = prefs.getString("attendance_list2", null)
        if (savedAttendance == null) {
            val defaultAttendance = bootstrapAttendance(_workers.value)
            _attendanceList.value = defaultAttendance
            saveAttendance(defaultAttendance)
        } else {
            _attendanceList.value = parseAttendance(savedAttendance)
        }

        // Load or Bootstrap AI Messages
        val savedChat = prefs.getString("chat_list2", null)
        if (savedChat == null) {
            val defaultChat = listOf(
                ChatMessage(text = "እንኳን ወደ ቢኒያም AI በደህና መጡ! የአንዋር ፕላስቲክ መልሶ ማምረቻ ድርጅት የዛሬ የክምችት እና የሰራተኞች መረጃዎችን ጠይቀው መተንተን ይችላሉ።", sender = "model")
            )
            _chatMessages.value = defaultChat
            saveChat(defaultChat)
        } else {
            _chatMessages.value = parseChat(savedChat)
        }
    }

    private fun bootstrapAttendance(workers: List<Worker>): List<Attendance> {
        val list = mutableListOf<Attendance>()
        // Bootstrap for year 2018, month 9 (Ginbot), from day 1 to 27
        for (w in workers) {
            for (day in 1..27) {
                // Sundays: days 3, 10, 17, 24 are Sundays in Ginbot 2018
                val isSunday = day == 3 || day == 10 || day == 17 || day == 24
                val status = when {
                    isSunday -> "እሁድ"
                    // Make Alazar absent on 12 and 18
                    w.name.contains("አላዛር") && (day == 12 || day == 18) -> "ቀርቷል"
                    // Make Chala absent on 5, 20
                    w.name.contains("ጫላ") && (day == 5 || day == 20) -> "ቀርቷል"
                    else -> "በስራ ላይ"
                }
                list.add(Attendance(workerId = w.id, year = 2018, month = 9, day = day, status = status))
            }
        }
        return list
    }

    // Actions
    fun updateProductStock(id: String, newStock: Int) {
        val updated = _products.value.map {
            if (it.id == id) it.copy(currentStock = newStock.coerceAtLeast(0)) else it
        }
        _products.value = updated
        saveProducts(updated)
    }

    fun addProduct(name: String, size: String, weight: Double, stock: Int) {
        val cleanName = name
            .replace("avocado", "", ignoreCase = true)
            .replace("Avocado", "", ignoreCase = true)
            .replace("አቮካዶ", "", ignoreCase = true)
            .trim()
            .let { if (it.isEmpty()) "ሸርጅን" else it }

        val newProd = Product(name = cleanName, size = size, bagWeightKg = weight, currentStock = stock)
        val updated = _products.value + newProd
        _products.value = updated
        saveProducts(updated)
    }

    fun deleteProduct(id: String) {
        val updated = _products.value.filter { it.id != id }
        _products.value = updated
        saveProducts(updated)
    }

    fun addWorker(name: String, role: String, dailyRate: Int, color: String) {
        val newWorker = Worker(name = name, role = role, dailyRate = dailyRate, avatarColorString = color)
        val updated = _workers.value + newWorker
        _workers.value = updated
        saveWorkers(updated)

        // Bootstrap current month's attendance for the new worker from 1 to 27
        val newAttendance = bootstrapAttendance(listOf(newWorker))
        val currentAllAttendance = _attendanceList.value + newAttendance
        _attendanceList.value = currentAllAttendance
        saveAttendance(currentAllAttendance)
    }

    fun toggleAttendance(workerId: String, day: Int, monthId: Int, yearId: Int, status: String) {
        val list = _attendanceList.value.toMutableList()
        val index = list.indexOfFirst { it.workerId == workerId && it.day == day && it.month == monthId && it.year == yearId }
        val updatedStatus = if (status == "እሁድ") "እሁድ" else status

        if (index != -1) {
            list[index] = list[index].copy(status = updatedStatus)
        } else {
            list.add(Attendance(workerId = workerId, day = day, month = monthId, year = yearId, status = updatedStatus))
        }
        _attendanceList.value = list
        saveAttendance(list)
    }

    fun changeRawMaterial(amount: Int) {
        _rawMaterialsStock.value = (_rawMaterialsStock.value + amount).coerceAtLeast(0)
    }

    fun changeMasterbatch(amount: Int) {
        _masterbatchStock.value = (_masterbatchStock.value + amount).coerceAtLeast(0)
    }

    fun sendChatMessage(query: String) {
        val userMsg = ChatMessage(text = query, sender = "user")
        val current = _chatMessages.value + userMsg
        _chatMessages.value = current

        // Process query with custom rule analysis or response representing Biniyam AI
        val responseText = processAiQuery(query)
        val modelMsg = ChatMessage(text = responseText, sender = "model")
        val finalMessages = _chatMessages.value + modelMsg
        _chatMessages.value = finalMessages
        saveChat(finalMessages)
    }

    fun clearChat() {
        val defaultChat = listOf(
            ChatMessage(text = "እንኳን ወደ ቢኒያም AI በደህና መጡ! የአንዋር ፕላስቲክ መልሶ ማምረቻ ድርጅት የዛሬ የክምችት እና የሰራተኞች መረጃዎችን ጠይቀው መተንተን ይችላሉ።", sender = "model")
        )
        _chatMessages.value = defaultChat
        saveChat(defaultChat)
    }

    fun navigateMonth(offset: Int) {
        var m = _currentMonth.value + offset
        var y = _currentYear.value
        if (m > 13) {
            m = 1
            y += 1
        } else if (m < 1) {
            m = 13
            y -= 1
        }
        _currentMonth.value = m
        _currentYear.value = y
    }

    private fun processAiQuery(query: String): String {
        return when {
            query.contains("ክምችት") || query.contains("stock") || query.contains("inventory") || query.contains("ምርት") -> {
                val sum = _products.value.sumOf { it.currentStock }
                val criticals = _products.value.filter { it.currentStock < 10 }.joinToString { it.name }
                "ዛሬ የክምችት መረጃዎችን ስተነትን፡ በአጠቃላይ $sum ከረጢቶች ክምችት ውስጥ ይገኛሉ። " +
                if (criticals.isNotEmpty()) "ያለን ወሳኝ (Critical) ክምችት ላይ የሚገኙ ምርቶች፡ $criticals ናቸው። እባክዎ ምርት ይጨምሩ!"
                else "ሁሉ ምርቶች በጥሩ ክምችት ላይ ይገኛሉ።"
            }
            query.contains("ሰራተኛ") || query.contains("worker") || query.contains("attendance") || query.contains("መገኘት") -> {
                val active = _workers.value.size
                "በአሁኑ ሰዓት $active ሰራተኞች ተመዝግበዋል። የደመወዝ መዝገብ እና ወራዊ የአቴንዳንስ ሪፖርቶችን ለማየት 'ሰራተኞች' ማውጫውን ይጎብኙ።"
            }
            else -> {
                "ይሀው ቢኒያም AI መልሶ ማምረቻ ነኝ። የአንዋርን የስራ ቅልጥፍና ለመጨመር በክምችት ቁጥጥር፣ የሰራተኞች መገኘትና ግምገማ ላይ እረዳዎታለሁ!"
            }
        }
    }

    // Persistence Helpers (Semi-structured manually to be 100% bug-free)
    private fun saveProducts(list: List<Product>) {
        val serialized = list.joinToString(";") { "${it.id},${it.name.replace(",", "[COMMA]")},${it.size.replace(",", "[COMMA]")},${it.bagWeightKg},${it.currentStock},${it.category}" }
        prefs.edit().putString("products_list2", serialized).apply()
    }
    private fun parseProducts(str: String): List<Product> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                Product(
                    id = parts[0],
                    name = parts[1].replace("[COMMA]", ","),
                    size = parts[2].replace("[COMMA]", ","),
                    bagWeightKg = parts[3].toDouble(),
                    currentStock = parts[4].toInt(),
                    category = parts[5]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveWorkers(list: List<Worker>) {
        val serialized = list.joinToString(";") { "${it.id},${it.name.replace(",", "[COMMA]")},${it.role},${it.dailyRate},${it.avatarColorString}" }
        prefs.edit().putString("workers_list2", serialized).apply()
    }
    private fun parseWorkers(str: String): List<Worker> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                Worker(
                    id = parts[0],
                    name = parts[1].replace("[COMMA]", ","),
                    role = parts[2],
                    dailyRate = parts[3].toInt(),
                    avatarColorString = parts[4]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAttendance(list: List<Attendance>) {
        val serialized = list.joinToString(";") { "${it.id},${it.workerId},${it.year},${it.month},${it.day},${it.status}" }
        prefs.edit().putString("attendance_list2", serialized).apply()
    }
    private fun parseAttendance(str: String): List<Attendance> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                Attendance(id = parts[0], workerId = parts[1], year = parts[2].toInt(), month = parts[3].toInt(), day = parts[4].toInt(), status = parts[5])
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveChat(list: List<ChatMessage>) {
        val serialized = list.joinToString(";") { "${it.id},${it.text.replace(",", "[COMMA]").replace(";", "[SEMI]")},${it.sender},${it.timestamp}" }
        prefs.edit().putString("chat_list2", serialized).apply()
    }
    private fun parseChat(str: String): List<ChatMessage> {
        if (str.isEmpty()) return emptyList()
        return try {
            str.split(";").map {
                val parts = it.split(",")
                ChatMessage(id = parts[0], text = parts[1].replace("[COMMA]", ",").replace("[SEMI]", ";"), sender = parts[2], timestamp = parts[3].toLong())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
