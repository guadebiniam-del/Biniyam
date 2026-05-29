package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = InventoryRepository(database.inventoryDao())
    }

    // --- SENSITIVE DATE MANAGEMENT ---
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDate = MutableStateFlow(dateFormatter.format(Date()))

    // --- STATE FLOWS FROM REPOSITORY ---
    val allProducts: StateFlow<List<Product>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRawMaterials: StateFlow<List<RawMaterial>> = repository.allRawMaterialsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMasterbatches: StateFlow<List<Masterbatch>> = repository.allMasterbatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkers: StateFlow<List<Worker>> = repository.allWorkersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RECTIVE ATTENDANCE FOR ACTIVE DATE ---
    val currentAttendance: StateFlow<List<WorkerAttendance>> = selectedDate
        .flatMapLatest { date -> repository.getAttendanceForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions flows
    val allProductTransactions: StateFlow<List<ProductTransaction>> = repository.allProductTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRawMaterialTransactions: StateFlow<List<RawMaterialTransaction>> = repository.allRawMaterialTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMasterbatchTransactions: StateFlow<List<MasterbatchTransaction>> = repository.allMasterbatchTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SYSTEM TIME & CALENDAR PERIOD STATES ---
    sealed class ReportPeriod {
        object Daily : ReportPeriod()
        object Weekly : ReportPeriod()
        object Monthly : ReportPeriod()
        object Yearly : ReportPeriod()
    }

    val reportPeriod = MutableStateFlow<ReportPeriod>(ReportPeriod.Daily)

    // A calculated state for stats based on reportPeriod and selectedDate
    val statsState = combine(
        selectedDate,
        reportPeriod,
        allProductTransactions,
        allRawMaterialTransactions,
        allMasterbatchTransactions
    ) { date, period, pTrans, mTrans, mbTrans ->
        calculateStats(date, period, pTrans, mTrans, mbTrans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AggregatedStats())

    // Calculated worker attendances based on report period
    val workerAttendanceStats = combine(
        selectedDate,
        reportPeriod,
        allWorkers,
        allAttendanceFlow()
    ) { date, period, workers, attendances ->
        calculateWorkerStats(date, period, workers, attendances)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun allAttendanceFlow(): Flow<List<WorkerAttendance>> = repository.allAttendanceFlow


    // --- INVENTORY BUSINESS ACTIONS ---

    fun updateSelectedDate(dateStr: String) {
        selectedDate.value = dateStr
    }

    fun setReportPeriod(period: ReportPeriod) {
        reportPeriod.value = period
    }

    // Products
    fun addNewProduct(name: String, size: String, color: String, counter: Int, piecesPerBag: Int, weight: Double, initialStock: Int) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                size = size,
                color = color,
                counter = counter,
                piecesPerBag = piecesPerBag,
                bagWeightKg = weight,
                currentStock = initialStock
            )
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun recordProductDailyActivity(productId: Int, fabricated: Int, sold: Int, adjusted: Int, notes: String) {
        viewModelScope.launch {
            val transaction = ProductTransaction(
                productId = productId,
                date = selectedDate.value,
                fabricated = fabricated,
                sold = sold,
                adjusted = adjusted,
                notes = notes
            )
            repository.addProductTransactionAndUpdateStock(transaction)
        }
    }

    fun adjustProductStock(productId: Int, adjustedValue: Int, notes: String) {
        viewModelScope.launch {
            repository.adjustProductStockDirect(productId, adjustedValue, notes, selectedDate.value)
        }
    }

    // Raw Materials
    fun recordRawMaterialActivity(materialType: String, used: Double, added: Double) {
        viewModelScope.launch {
            val transaction = RawMaterialTransaction(
                materialType = materialType,
                date = selectedDate.value,
                used = used,
                added = added
            )
            repository.addRawMaterialTransactionAndUpdateStock(transaction)
        }
    }

    // Masterbatch
    fun recordMasterbatchActivity(masterbatchId: Int, used: Double, bought: Double) {
        viewModelScope.launch {
            val transaction = MasterbatchTransaction(
                masterbatchId = masterbatchId,
                date = selectedDate.value,
                used = used,
                bought = bought
            )
            repository.addMasterbatchTransactionAndUpdateStock(transaction)
        }
    }

    fun addNewMasterbatch(color: String, initialStock: Double) {
        viewModelScope.launch {
            val mb = Masterbatch(color = color, currentStock = initialStock)
            repository.insertMasterbatch(mb)
        }
    }

    fun deleteMasterbatch(masterbatch: Masterbatch) {
        viewModelScope.launch {
            repository.deleteMasterbatch(masterbatch)
        }
    }


    // --- WORKERS BUSINESS ACTIONS ---

    fun addNewWorker(name: String) {
        viewModelScope.launch {
            val worker = Worker(
                name = name,
                joinDate = selectedDate.value
            )
            repository.insertWorker(worker)
        }
    }

    fun markWorkerAttendance(workerId: Int, status: String) {
        viewModelScope.launch {
            val attendance = WorkerAttendance(
                workerId = workerId,
                date = selectedDate.value,
                status = status
            )
            repository.recordAttendance(attendance)
        }
    }


    // --- ADVANCED METRIC CALCULATION STATS ---

    private fun calculateStats(
        referenceDateStr: String,
        period: ReportPeriod,
        productTrans: List<ProductTransaction>,
        rawMaterialTrans: List<RawMaterialTransaction>,
        masterbatchTrans: List<MasterbatchTransaction>
    ): AggregatedStats {
        val (startDateStr, endDateStr) = getRangeForPeriod(referenceDateStr, period)

        val filteredP = productTrans.filter { it.date in startDateStr..endDateStr }
        val filteredMat = rawMaterialTrans.filter { it.date in startDateStr..endDateStr }
        val filteredMb = masterbatchTrans.filter { it.date in startDateStr..endDateStr }

        // Totals
        val totalFabricated = filteredP.sumOf { it.fabricated }
        val totalSold = filteredP.sumOf { it.sold }
        val totalAdjusted = filteredP.sumOf { it.adjusted }

        // Group product totals
        val productSummary = filteredP.groupBy { it.productId }.mapValues { entry ->
            val pTrans = entry.value
            ProductAggStats(
                productId = entry.key,
                fabricated = pTrans.sumOf { it.fabricated },
                sold = pTrans.sumOf { it.sold },
                adjusted = pTrans.sumOf { it.adjusted }
            )
        }

        // Group Raw Material Totals
        var ldUsed = 0.0
        var ldAdded = 0.0
        var hdUsed = 0.0
        var hdAdded = 0.0
        var wasteUsed = 0.0
        var wasteAdded = 0.0

        filteredMat.forEach {
            when (it.materialType.uppercase()) {
                "LD" -> {
                    ldUsed += it.used
                    ldAdded += it.added
                }
                "HD" -> {
                    hdUsed += it.used
                    hdAdded += it.added
                }
                "WASTE", "WEST" -> {
                    wasteUsed += it.used
                    wasteAdded += it.added
                }
            }
        }

        // Masterbatch totals
        val masterbatchSummary = filteredMb.groupBy { it.masterbatchId }.mapValues { entry ->
            val mbt = entry.value
            MasterbatchAggStats(
                masterbatchId = entry.key,
                used = mbt.sumOf { it.used },
                bought = mbt.sumOf { it.bought }
            )
        }

        return AggregatedStats(
            startDate = startDateStr,
            endDate = endDateStr,
            totalFabricated = totalFabricated,
            totalSold = totalSold,
            totalAdjusted = totalAdjusted,
            ldUsed = ldUsed,
            ldAdded = ldAdded,
            hdUsed = hdUsed,
            hdAdded = hdAdded,
            wasteUsed = wasteUsed,
            wasteAdded = wasteAdded,
            productSummary = productSummary,
            masterbatchSummary = masterbatchSummary
        )
    }

    private fun calculateWorkerStats(
        referenceDateStr: String,
        period: ReportPeriod,
        workers: List<Worker>,
        attendances: List<WorkerAttendance>
    ): List<WorkerAggStats> {
        val (startDateStr, endDateStr) = getRangeForPeriod(referenceDateStr, period)
        val filteredAtt = attendances.filter { it.date in startDateStr..endDateStr }

        return workers.map { worker ->
            val workerAttLines = filteredAtt.filter { it.workerId == worker.id }
            val daysOnDuty = workerAttLines.count { it.status == "On Duty" }
            val daysAbsent = workerAttLines.count { it.status == "Absent" }
            val daysLeft = workerAttLines.count { it.status == "Left" }
            val daysSundayOff = workerAttLines.count { it.status == "Sunday Off" }

            // Current status is attendance status on the selectedDate if recorded, or active/inactive
            val currentOnDate = attendances.find { it.workerId == worker.id && it.date == selectedDate.value }?.status
                ?: if (worker.isActive) "Unknown / Not Set" else "Left"

            WorkerAggStats(
                workerId = worker.id,
                workerName = worker.name,
                daysOnDuty = daysOnDuty,
                daysAbsent = daysAbsent,
                daysLeft = daysLeft,
                daysSundayOff = daysSundayOff,
                currentStatus = currentOnDate,
                isActiveInSystem = worker.isActive
            )
        }
    }

    // Common Date Utilities inside App
    private fun getRangeForPeriod(referenceDateStr: String, period: ReportPeriod): Pair<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            val refDate = sdf.parse(referenceDateStr) ?: Date()
            calendar.time = refDate
        } catch (e: Exception) {
            // fallback
        }

        return when (period) {
            ReportPeriod.Daily -> {
                Pair(referenceDateStr, referenceDateStr)
            }
            ReportPeriod.Weekly -> {
                // Set first day of week. Sunday is index 1.
                // Go to start of week (Monday or Sunday as preferred. Let's make Monday start of week and Sunday end)
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = sdf.format(calendar.time)
                // Go to next Sunday
                calendar.add(Calendar.DATE, 6)
                val end = sdf.format(calendar.time)
                Pair(start, end)
            }
            ReportPeriod.Monthly -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(calendar.time)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = sdf.format(calendar.time)
                Pair(start, end)
            }
            ReportPeriod.Yearly -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(calendar.time)
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                val end = sdf.format(calendar.time)
                Pair(start, end)
            }
        }
    }
}

// Data structures for aggregations
data class AggregatedStats(
    val startDate: String = "",
    val endDate: String = "",
    val totalFabricated: Int = 0,
    val totalSold: Int = 0,
    val totalAdjusted: Int = 0,
    val ldUsed: Double = 0.0,
    val ldAdded: Double = 0.0,
    val hdUsed: Double = 0.0,
    val hdAdded: Double = 0.0,
    val wasteUsed: Double = 0.0,
    val wasteAdded: Double = 0.0,
    val productSummary: Map<Int, ProductAggStats> = emptyMap(),
    val masterbatchSummary: Map<Int, MasterbatchAggStats> = emptyMap()
)

data class ProductAggStats(
    val productId: Int,
    val fabricated: Int,
    val sold: Int,
    val adjusted: Int
)

data class MasterbatchAggStats(
    val masterbatchId: Int,
    val used: Double,
    val bought: Double
)

data class WorkerAggStats(
    val workerId: Int,
    val workerName: String,
    val daysOnDuty: Int,
    val daysAbsent: Int,
    val daysLeft: Int,
    val daysSundayOff: Int,
    val currentStatus: String,
    val isActiveInSystem: Boolean
)
