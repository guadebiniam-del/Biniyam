package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class InventoryRepository(private val dao: InventoryDao) {

    // --- PRODUCTS ---
    val allProductsFlow: Flow<List<Product>> = dao.getAllProductsFlow()
    val allProductTransactionsFlow: Flow<List<ProductTransaction>> = dao.getAllProductTransactionsFlow()

    suspend fun getAllProducts(): List<Product> = dao.getAllProducts()

    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)

    suspend fun updateProduct(product: Product) = dao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = dao.deleteProduct(product)

    suspend fun addProductTransactionAndUpdateStock(transaction: ProductTransaction) {
        // Insert product transaction
        dao.insertProductTransaction(transaction)

        // Adjust product stock
        val product = dao.getProductById(transaction.productId)
        if (product != null) {
            val netAdjustment = transaction.fabricated - transaction.sold + transaction.adjusted
            val newStock = maxOf(0, product.currentStock + netAdjustment)
            dao.updateProduct(product.copy(currentStock = newStock))
        }
    }

    suspend fun adjustProductStockDirect(productId: Int, adjustedValue: Int, notes: String, date: String) {
        val product = dao.getProductById(productId)
        if (product != null) {
            val transaction = ProductTransaction(
                productId = productId,
                date = date,
                fabricated = 0,
                sold = 0,
                adjusted = adjustedValue,
                notes = notes
            )
            dao.insertProductTransaction(transaction)
            val newStock = maxOf(0, product.currentStock + adjustedValue)
            dao.updateProduct(product.copy(currentStock = newStock))
        }
    }


    // --- RAW MATERIALS ---
    val allRawMaterialsFlow: Flow<List<RawMaterial>> = dao.getAllRawMaterialsFlow()
    val allRawMaterialTransactionsFlow: Flow<List<RawMaterialTransaction>> = dao.getAllRawMaterialTransactionsFlow()

    suspend fun insertRawMaterial(rawMaterial: RawMaterial) = dao.insertRawMaterial(rawMaterial)

    suspend fun addRawMaterialTransactionAndUpdateStock(transaction: RawMaterialTransaction) {
        dao.insertRawMaterialTransaction(transaction)

        val material = dao.getRawMaterialByType(transaction.materialType)
        if (material != null) {
            val netChange = transaction.added - transaction.used
            val newStock = maxOf(0.0, material.currentStock + netChange)
            dao.updateRawMaterial(material.copy(currentStock = newStock))
        } else {
            // If raw material doesn't exist, create it
            val initialStock = maxOf(0.0, transaction.added - transaction.used)
            dao.insertRawMaterial(RawMaterial(type = transaction.materialType, currentStock = initialStock))
        }
    }


    // --- MASTERBATCH ---
    val allMasterbatchesFlow: Flow<List<Masterbatch>> = dao.getAllMasterbatchesFlow()
    val allMasterbatchTransactionsFlow: Flow<List<MasterbatchTransaction>> = dao.getAllMasterbatchTransactionsFlow()

    suspend fun insertMasterbatch(masterbatch: Masterbatch): Long = dao.insertMasterbatch(masterbatch)

    suspend fun updateMasterbatch(masterbatch: Masterbatch) = dao.updateMasterbatch(masterbatch)

    suspend fun deleteMasterbatch(masterbatch: Masterbatch) = dao.deleteMasterbatch(masterbatch)

    suspend fun addMasterbatchTransactionAndUpdateStock(transaction: MasterbatchTransaction) {
        dao.insertMasterbatchTransaction(transaction)

        val masterbatch = dao.getMasterbatchById(transaction.masterbatchId)
        if (masterbatch != null) {
            val netChange = transaction.bought - transaction.used
            val newStock = maxOf(0.0, masterbatch.currentStock + netChange)
            dao.updateMasterbatch(masterbatch.copy(currentStock = newStock))
        }
    }


    // --- WORKERS ---
    val allWorkersFlow: Flow<List<Worker>> = dao.getAllWorkersFlow()
    val allAttendanceFlow: Flow<List<WorkerAttendance>> = dao.getAllAttendanceFlow()

    suspend fun getAllWorkers(): List<Worker> = dao.getAllWorkers()

    suspend fun insertWorker(worker: Worker): Long = dao.insertWorker(worker)

    suspend fun updateWorker(worker: Worker) = dao.updateWorker(worker)

    suspend fun recordAttendance(attendance: WorkerAttendance) {
        // Delete existing attendance for this worker on this date to overwrite
        dao.deleteAttendanceForWorkerOnDate(attendance.workerId, attendance.date)
        // Record new attendance
        dao.insertAttendance(attendance)

        // If the worker has "Left" permanently, we also update their status in the Main Workers list
        if (attendance.status == "Left") {
            val worker = dao.getWorkerById(attendance.workerId)
            if (worker != null && worker.isActive) {
                dao.updateWorker(worker.copy(isActive = false, leaveDate = attendance.date))
            }
        } else if (attendance.status == "On Duty") {
            // Re-activate if they were inactive but are now marked on duty
            val worker = dao.getWorkerById(attendance.workerId)
            if (worker != null && !worker.isActive) {
                dao.updateWorker(worker.copy(isActive = true, leaveDate = null))
            }
        }
    }

    suspend fun getAttendanceForDate(date: String): List<WorkerAttendance> = dao.getAttendanceForDate(date)

    fun getAttendanceForDateFlow(date: String): Flow<List<WorkerAttendance>> = dao.getAttendanceForDateFlow(date)

    suspend fun getAttendanceForRange(startDate: String, endDate: String): List<WorkerAttendance> =
        dao.getAttendanceForRange(startDate, endDate)
        
    suspend fun getRawMaterialTransactionsForRange(startDate: String, endDate: String): List<RawMaterialTransaction> =
        dao.getRawMaterialTransactionsForRange(startDate, endDate)

    suspend fun getMasterbatchTransactionsForRange(startDate: String, endDate: String): List<MasterbatchTransaction> =
        dao.getMasterbatchTransactionsForRange(startDate, endDate)

    suspend fun getProductTransactionsForRange(startDate: String, endDate: String): List<ProductTransaction> =
        dao.getProductTransactionsForRange(startDate, endDate)
}
