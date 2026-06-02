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
        repository = InventoryRepository()
    }

    // --- SENSITIVE DATE MANAGEMENT ---
    val selectedDate = MutableStateFlow(EthiopianCalendarHelper.getTodayEthiopianString())

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

    val allActivityLogs: StateFlow<List<ActivityLog>> = repository.allActivityLogsFlow
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

    val allWorkerAttendance: StateFlow<List<WorkerAttendance>> = repository.allAttendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun allAttendanceFlow(): Flow<List<WorkerAttendance>> = repository.allAttendanceFlow


    // --- LOGGER UTILITY ---
    fun logAction(category: String, actionType: String, description: String) {
        viewModelScope.launch {
            try {
                val manufacturer = android.os.Build.MANUFACTURER ?: "Generic"
                val model = android.os.Build.MODEL ?: "Android Device"
                val deviceName = if (manufacturer.equals("unknown", ignoreCase = true)) model else "$manufacturer $model"
                val etDateTime = EthiopianCalendarHelper.getTodayEthiopianDateTimeString()
                val log = ActivityLog(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    ethiopianDateTime = etDateTime,
                    category = category,
                    actionType = actionType,
                    description = description,
                    deviceName = deviceName
                )
                repository.insertActivityLog(log)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error recording log: ${e.message}", e)
            }
        }
    }

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
            val generatedId = repository.insertProduct(product)
            logAction(
                category = "Product",
                actionType = "Add",
                description = "Registered new product: '$name' (ID: $generatedId, Size: $size, Color: $color, Weight: ${weight}kg, Initial Stock: $initialStock bags)"
            )
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            logAction(
                category = "Product",
                actionType = "Delete",
                description = "Removed product: '${product.name}' (Size: ${product.size}, Color: ${product.color}, Final Stock: ${product.currentStock} bags)"
            )
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
            
            val product = allProducts.value.find { it.id == productId }
            val formattedNotes = if (notes.isBlank()) "None" else "'$notes'"
            logAction(
                category = "Product",
                actionType = "Edit",
                description = "Updated Daily sheet for '${product?.name ?: "Product #$productId"}': Fabricated: +$fabricated, Sold: -$sold, Adjusted: ${if (adjusted >= 0) "+" else ""}$adjusted bags. Notes: $formattedNotes (Date: ${selectedDate.value})"
            )
        }
    }

    fun adjustProductStock(productId: Int, adjustedValue: Int, notes: String) {
        viewModelScope.launch {
            repository.adjustProductStockDirect(productId, adjustedValue, notes, selectedDate.value)
            
            val product = allProducts.value.find { it.id == productId }
            val formattedNotes = if (notes.isBlank()) "None" else "'$notes'"
            logAction(
                category = "Product",
                actionType = "Edit",
                description = "Recorded manual stock adjustment for '${product?.name ?: "Product #$productId"}': Adjusted by ${if (adjustedValue >= 0) "+" else ""}$adjustedValue bags. Notes: $formattedNotes (Date: ${selectedDate.value})"
            )
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
            logAction(
                category = "Raw Material",
                actionType = "Edit",
                description = "Updated Raw Material '$materialType' levels on ${selectedDate.value}: Used: -${used}kg, Added: +${added}kg"
            )
        }
    }

    // Masterbatch
    fun recordMasterbatchActivity(masterbatchId: Int, used: Double, bought: Double, takenOut: Double = 0.0, returned: Double = 0.0) {
        viewModelScope.launch {
            val transaction = MasterbatchTransaction(
                masterbatchId = masterbatchId,
                date = selectedDate.value,
                used = used,
                bought = bought,
                takenOut = takenOut,
                returned = returned
            )
            repository.addMasterbatchTransactionAndUpdateStock(transaction)
            
            val mb = allMasterbatches.value.find { it.id == masterbatchId }
            logAction(
                category = "Masterbatch",
                actionType = "Edit",
                description = "Updated Masterbatch '${mb?.color ?: "ID #$masterbatchId"}' Pigment: Taken Out: ${takenOut}kg, Returned: ${returned}kg, Net Used: ${used}kg, Bought: +${bought}kg (Date: ${selectedDate.value})"
            )
        }
    }

    fun addNewMasterbatch(color: String, initialStock: Double) {
        viewModelScope.launch {
            val mb = Masterbatch(color = color, currentStock = initialStock)
            val generatedId = repository.insertMasterbatch(mb)
            logAction(
                category = "Masterbatch",
                actionType = "Add",
                description = "Added new Masterbatch option Color: '$color' (ID: $generatedId, Initial Stock: ${initialStock}kg)"
            )
        }
    }

    fun deleteMasterbatch(masterbatch: Masterbatch) {
        viewModelScope.launch {
            repository.deleteMasterbatch(masterbatch)
            logAction(
                category = "Masterbatch",
                actionType = "Delete",
                description = "Removed Masterbatch color option: '${masterbatch.color}' (Final Stock was: ${masterbatch.currentStock}kg)"
            )
        }
    }


    // --- WORKERS BUSINESS ACTIONS ---

    fun addNewWorker(name: String, monthlySalary: Double = 10000.0) {
        viewModelScope.launch {
            val worker = Worker(
                name = name,
                joinDate = selectedDate.value,
                monthlySalary = monthlySalary
            )
            val generatedId = repository.insertWorker(worker)
            logAction(
                category = "Worker",
                actionType = "Add",
                description = "Registered new Worker: '$name' (ID: $generatedId, Active starting: ${selectedDate.value})"
            )
        }
    }

    fun updateWorkerSalary(workerId: Int, newSalary: Double) {
        viewModelScope.launch {
            val worker = allWorkers.value.find { it.id == workerId }
            if (worker != null) {
                val updated = worker.copy(monthlySalary = newSalary)
                repository.updateWorker(updated)
                logAction(
                    category = "Worker",
                    actionType = "Edit",
                    description = "Updated salary for '${worker.name}' to $newSalary Birr"
                )
            }
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
            
            val worker = allWorkers.value.find { it.id == workerId }
            logAction(
                category = "Worker",
                actionType = "Edit",
                description = "Recorded Worker Attendance for '${worker?.name ?: "Worker #$workerId"}': System status changed/set to '$status' (Date: ${selectedDate.value})"
            )
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
                bought = mbt.sumOf { it.bought },
                takenOut = mbt.sumOf { it.takenOut },
                returned = mbt.sumOf { it.returned }
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
        val periodTypeStr = when (period) {
            ReportPeriod.Daily -> "DAILY"
            ReportPeriod.Weekly -> "WEEKLY"
            ReportPeriod.Monthly -> "MONTHLY"
            ReportPeriod.Yearly -> "YEARLY"
        }
        return EthiopianCalendarHelper.getRangeForEthiopianPeriod(referenceDateStr, periodTypeStr)
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
    val bought: Double,
    val takenOut: Double = 0.0,
    val returned: Double = 0.0
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
