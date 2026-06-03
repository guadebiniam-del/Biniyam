package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

class InventoryRepository {

    private val db: FirebaseFirestore

    companion object {
        @Volatile
        private var INSTANCE: InventoryRepository? = null

        fun getInstance(): InventoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = InventoryRepository()
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        // Explicitly enable offline state holding capabilities for premium offline resilience
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = settings
        db = firestore
    }

    // Task awaiting helper for standard play-services Task
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Firestore transaction task failed"))
            }
        }
    }

    // Helper to generate a unique positive 31-bit random integer ID to avoid collisions
    private fun generateUniqueId(): Int {
        return java.util.UUID.randomUUID().hashCode() and 0x7FFFFFFF
    }

    // Generic snapshot listener converted to a Flow for real-time sync across all devices
    private fun <T : Any> getCollectionFlow(collectionPath: String, clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = db.collection(collectionPath)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(clazz)
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    // On init, check and seed default materials/colors if empty, and delete default products/workers actively
    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Active Clean-up of default fake products if they exist in Firestore
                val defaultProductNames = listOf("heavy carrier bag", "industrial waste bag", "shopping bag", "mobile")
                val productsQuery = db.collection("products").get().awaitTask()
                for (doc in productsQuery.documents) {
                    val name = doc.getString("name")?.lowercase()?.trim() ?: ""
                    if (defaultProductNames.any { name == it || name.contains(it) }) {
                        doc.reference.delete().awaitTask()
                        Log.d("InventoryRepository", "Deleted default product: $name")
                    }
                }

                // Active Clean-up of default fake workers if they exist in Firestore
                val defaultWorkerNames = listOf("abebe kebede", "anwar adem", "chala gerba", "soliana yared")
                val workersQuery = db.collection("workers").get().awaitTask()
                for (doc in workersQuery.documents) {
                    val name = doc.getString("name")?.lowercase()?.trim() ?: ""
                    if (defaultWorkerNames.any { name == it || name.contains(it) }) {
                        doc.reference.delete().awaitTask()
                        Log.d("InventoryRepository", "Deleted default worker: $name")
                    }
                }

                // Seed Raw Materials if empty
                val rmSnapshot = db.collection("raw_materials").limit(1).get().awaitTask()
                if (rmSnapshot.isEmpty) {
                    Log.d("InventoryRepository", "Raw materials collections are empty. Seeding defaults...")
                    val rawMaterials = listOf(
                        RawMaterial("LD", 1200.0),
                        RawMaterial("HD", 850.0),
                        RawMaterial("Waste", 2400.0)
                    )
                    rawMaterials.forEach { insertRawMaterial(it) }
                }

                // Seed Masterbatches if empty
                val mbSnapshot = db.collection("masterbatches").limit(1).get().awaitTask()
                if (mbSnapshot.isEmpty) {
                    Log.d("InventoryRepository", "Masterbatches collections are empty. Seeding defaults...")
                    val masterbatches = listOf(
                        Masterbatch(id = 1, color = "Black", currentStock = 150.0),
                        Masterbatch(id = 2, color = "White", currentStock = 120.0),
                        Masterbatch(id = 3, color = "Red", currentStock = 50.0),
                        Masterbatch(id = 4, color = "Blue", currentStock = 60.0),
                        Masterbatch(id = 5, color = "Green", currentStock = 45.0),
                        Masterbatch(id = 6, color = "Yellow", currentStock = 30.0)
                    )
                    masterbatches.forEach { insertMasterbatch(it) }
                }

                Log.d("InventoryRepository", "Database check and cleanup completed successfully.")
            } catch (e: Exception) {
                Log.e("InventoryRepository", "Error running database setup/cleanup: ${e.message}", e)
            }
        }
    }

    // --- ANNOUNCEMENTS ---
    val allAnnouncementsFlow: Flow<List<Announcement>> = getCollectionFlow("announcements", Announcement::class.java)
        .map { list -> list.sortedByDescending { it.timestamp } }

    suspend fun insertAnnouncement(announcement: Announcement) {
        val idToUse = announcement.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        val updated = announcement.copy(
            id = idToUse,
            timestamp = if (announcement.timestamp == 0L) System.currentTimeMillis() else announcement.timestamp
        )
        db.collection("announcements").document(idToUse).set(updated).awaitTask()
    }

    suspend fun deleteAnnouncement(id: String) {
        db.collection("announcements").document(id).delete().awaitTask()
    }

    // --- APP VERSION MANAGEMENT ---
    val appVersionFlow: Flow<AppVersion?> = getCollectionFlow("appVersion", AppVersion::class.java)
        .map { list -> list.firstOrNull { it.id == "latest" } ?: list.firstOrNull() }

    suspend fun updateAppVersion(appVersion: AppVersion) {
        val docId = "latest"
        val updated = appVersion.copy(
            id = docId,
            timestamp = System.currentTimeMillis()
        )
        db.collection("appVersion").document(docId).set(updated).awaitTask()
    }

    // --- PRODUCTS ---
    val allActivityLogsFlow: Flow<List<ActivityLog>> = getCollectionFlow("activity_logs", ActivityLog::class.java)
        .map { list -> list.sortedByDescending { it.timestamp } }

    suspend fun insertActivityLog(log: ActivityLog) {
        val idToUse = if (log.id.isEmpty()) java.util.UUID.randomUUID().toString() else log.id
        val updatedMessage = log.copy(
            id = idToUse,
            timestamp = if (log.timestamp == 0L) System.currentTimeMillis() else log.timestamp
        )
        db.collection("activity_logs").document(idToUse).set(updatedMessage).awaitTask()
    }

    val allProductsFlow: Flow<List<Product>> = getCollectionFlow("products", Product::class.java)
        .map { list -> list.sortedByDescending { it.id } }

    val allProductTransactionsFlow: Flow<List<ProductTransaction>> = getCollectionFlow("product_transactions", ProductTransaction::class.java)
        .map { list -> list.sortedWith(compareByDescending<ProductTransaction> { it.date }.thenByDescending { it.id }) }

    suspend fun getAllProducts(): List<Product> {
        return try {
            db.collection("products").get().awaitTask().documents.mapNotNull { it.toObject(Product::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertProduct(product: Product): Long {
        val idToUse = if (product.id == 0) generateUniqueId() else product.id
        val updatedProduct = product.copy(id = idToUse)
        db.collection("products").document(idToUse.toString()).set(updatedProduct).awaitTask()
        return idToUse.toLong()
    }

    suspend fun updateProduct(product: Product) {
        db.collection("products").document(product.id.toString()).set(product).awaitTask()
    }

    suspend fun deleteProduct(product: Product) {
        db.collection("products").document(product.id.toString()).delete().awaitTask()
    }

    suspend fun getProductById(id: Int): Product? {
        return try {
            db.collection("products").document(id.toString()).get().awaitTask().toObject(Product::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductTransactionsForDate(date: String): List<ProductTransaction> {
        return try {
            db.collection("product_transactions")
                .whereEqualTo("date", date)
                .get()
                .awaitTask()
                .documents
                .mapNotNull { doc -> doc.toObject(ProductTransaction::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertProductTransaction(transaction: ProductTransaction): Long {
        val idToUse = if (transaction.id == 0) generateUniqueId() else transaction.id
        val updated = transaction.copy(id = idToUse)
        db.collection("product_transactions").document(idToUse.toString()).set(updated).awaitTask()
        return idToUse.toLong()
    }

    suspend fun addProductTransactionAndUpdateStock(transaction: ProductTransaction) {
        val existingList = getProductTransactionsForDate(transaction.date)
        val existing = existingList.find { it.productId == transaction.productId }

        if (existing != null) {
            val product = getProductById(transaction.productId)
            if (product != null) {
                val oldNet = existing.fabricated - existing.sold + existing.adjusted
                val newNet = transaction.fabricated - transaction.sold + transaction.adjusted
                val diff = newNet - oldNet
                val newStock = maxOf(0, product.currentStock + diff)
                updateProduct(product.copy(currentStock = newStock))
            }
            insertProductTransaction(transaction.copy(id = existing.id))
        } else {
            insertProductTransaction(transaction)
            val product = getProductById(transaction.productId)
            if (product != null) {
                val netAdjustment = transaction.fabricated - transaction.sold + transaction.adjusted
                val newStock = maxOf(0, product.currentStock + netAdjustment)
                updateProduct(product.copy(currentStock = newStock))
            }
        }
    }

    suspend fun adjustProductStockDirect(productId: Int, adjustedValue: Int, notes: String, date: String) {
        val product = getProductById(productId)
        if (product != null) {
            val transaction = ProductTransaction(
                productId = productId,
                date = date,
                fabricated = 0,
                sold = 0,
                adjusted = adjustedValue,
                notes = notes
            )
            insertProductTransaction(transaction)
            val newStock = maxOf(0, product.currentStock + adjustedValue)
            updateProduct(product.copy(currentStock = newStock))
        }
    }

    // --- RAW MATERIALS ---
    val allRawMaterialsFlow: Flow<List<RawMaterial>> = getCollectionFlow("raw_materials", RawMaterial::class.java)

    val allRawMaterialTransactionsFlow: Flow<List<RawMaterialTransaction>> = getCollectionFlow("raw_material_transactions", RawMaterialTransaction::class.java)
        .map { list -> list.sortedWith(compareByDescending<RawMaterialTransaction> { it.date }.thenByDescending { it.id }) }

    suspend fun insertRawMaterial(rawMaterial: RawMaterial) {
        db.collection("raw_materials").document(rawMaterial.type).set(rawMaterial).awaitTask()
    }

    suspend fun updateRawMaterial(rawMaterial: RawMaterial) {
        db.collection("raw_materials").document(rawMaterial.type).set(rawMaterial).awaitTask()
    }

    suspend fun getRawMaterialByType(type: String): RawMaterial? {
        return try {
            db.collection("raw_materials").document(type).get().awaitTask().toObject(RawMaterial::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRawMaterialTransactionsForDate(date: String): List<RawMaterialTransaction> {
        return try {
            db.collection("raw_material_transactions")
                .whereEqualTo("date", date)
                .get()
                .awaitTask()
                .documents
                .mapNotNull { it.toObject(RawMaterialTransaction::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertRawMaterialTransaction(transaction: RawMaterialTransaction): Long {
        val idToUse = if (transaction.id == 0) generateUniqueId() else transaction.id
        val updated = transaction.copy(id = idToUse)
        db.collection("raw_material_transactions").document(idToUse.toString()).set(updated).awaitTask()
        return idToUse.toLong()
    }

    suspend fun addRawMaterialTransactionAndUpdateStock(transaction: RawMaterialTransaction) {
        val existingList = getRawMaterialTransactionsForDate(transaction.date)
        val existing = existingList.find { it.materialType == transaction.materialType }

        if (existing != null) {
            val material = getRawMaterialByType(transaction.materialType)
            if (material != null) {
                val oldNet = existing.added - existing.used
                val newNet = transaction.added - transaction.used
                val diff = newNet - oldNet
                val newStock = maxOf(0.0, material.currentStock + diff)
                updateRawMaterial(material.copy(currentStock = newStock))
            }
            insertRawMaterialTransaction(transaction.copy(id = existing.id))
        } else {
            insertRawMaterialTransaction(transaction)
            val material = getRawMaterialByType(transaction.materialType)
            if (material != null) {
                val netChange = transaction.added - transaction.used
                val newStock = maxOf(0.0, material.currentStock + netChange)
                updateRawMaterial(material.copy(currentStock = newStock))
            } else {
                val initialStock = maxOf(0.0, transaction.added - transaction.used)
                insertRawMaterial(RawMaterial(type = transaction.materialType, currentStock = initialStock))
            }
        }
    }

    // --- MASTERBATCH ---
    val allMasterbatchesFlow: Flow<List<Masterbatch>> = getCollectionFlow("masterbatches", Masterbatch::class.java)
        .map { list -> list.sortedByDescending { it.id } }

    val allMasterbatchTransactionsFlow: Flow<List<MasterbatchTransaction>> = getCollectionFlow("masterbatch_transactions", MasterbatchTransaction::class.java)
        .map { list -> list.sortedWith(compareByDescending<MasterbatchTransaction> { it.date }.thenByDescending { it.id }) }

    suspend fun insertMasterbatch(masterbatch: Masterbatch): Long {
        val idToUse = if (masterbatch.id == 0) generateUniqueId() else masterbatch.id
        val updated = masterbatch.copy(id = idToUse)
        db.collection("masterbatches").document(idToUse.toString()).set(updated).awaitTask()
        return idToUse.toLong()
    }

    suspend fun updateMasterbatch(masterbatch: Masterbatch) {
        db.collection("masterbatches").document(masterbatch.id.toString()).set(masterbatch).awaitTask()
    }

    suspend fun deleteMasterbatch(masterbatch: Masterbatch) {
        db.collection("masterbatches").document(masterbatch.id.toString()).delete().awaitTask()
    }

    suspend fun getMasterbatchById(id: Int): Masterbatch? {
        return try {
            db.collection("masterbatches").document(id.toString()).get().awaitTask().toObject(Masterbatch::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMasterbatchTransactionsForDate(date: String): List<MasterbatchTransaction> {
        return try {
            db.collection("masterbatch_transactions")
                .whereEqualTo("date", date)
                .get()
                .awaitTask()
                .documents
                .mapNotNull { it.toObject(MasterbatchTransaction::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertMasterbatchTransaction(transaction: MasterbatchTransaction): Long {
        val idToUse = if (transaction.id == 0) generateUniqueId() else transaction.id
        val updated = transaction.copy(id = idToUse)
        db.collection("masterbatch_transactions").document(idToUse.toString()).set(updated).awaitTask()
        return idToUse.toLong()
    }

    suspend fun addMasterbatchTransactionAndUpdateStock(transaction: MasterbatchTransaction) {
        val existingList = getMasterbatchTransactionsForDate(transaction.date)
        val existing = existingList.find { it.masterbatchId == transaction.masterbatchId }

        if (existing != null) {
            val mb = getMasterbatchById(transaction.masterbatchId)
            if (mb != null) {
                val oldNet = existing.bought - existing.used
                val newNet = transaction.bought - transaction.used
                val diff = newNet - oldNet
                val newStock = maxOf(0.0, mb.currentStock + diff)
                updateMasterbatch(mb.copy(currentStock = newStock))
            }
            insertMasterbatchTransaction(transaction.copy(id = existing.id))
        } else {
            insertMasterbatchTransaction(transaction)
            val masterbatch = getMasterbatchById(transaction.masterbatchId)
            if (masterbatch != null) {
                val netChange = transaction.bought - transaction.used
                val newStock = maxOf(0.0, masterbatch.currentStock + netChange)
                updateMasterbatch(masterbatch.copy(currentStock = newStock))
            }
        }
    }

    // --- WORKERS ---
    val allWorkersFlow: Flow<List<Worker>> = getCollectionFlow("workers", Worker::class.java)
        .map { list -> list.sortedBy { it.name } }

    val allAttendanceFlow: Flow<List<WorkerAttendance>> = getCollectionFlow("worker_attendance", WorkerAttendance::class.java)
        .map { list -> list.sortedWith(compareByDescending<WorkerAttendance> { it.date }.thenByDescending { it.id }) }

    suspend fun getAllWorkers(): List<Worker> {
        return try {
            db.collection("workers").get().awaitTask().documents.mapNotNull { it.toObject(Worker::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun insertWorker(worker: Worker): Long {
        val idToUse = if (worker.id == 0) generateUniqueId() else worker.id
        val updated = worker.copy(id = idToUse)
        db.collection("workers").document(idToUse.toString()).set(updated).awaitTask()
        return idToUse.toLong()
    }

    suspend fun updateWorker(worker: Worker) {
        db.collection("workers").document(worker.id.toString()).set(worker).awaitTask()
    }

    suspend fun getWorkerById(id: Int): Worker? {
        return try {
            db.collection("workers").document(id.toString()).get().awaitTask().toObject(Worker::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteAttendanceForWorkerOnDate(workerId: Int, date: String) {
        try {
            val querySnap = db.collection("worker_attendance")
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("date", date)
                .get()
                .awaitTask()
            for (doc in querySnap.documents) {
                doc.reference.delete().awaitTask()
            }
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error deleting attendance record: ${e.message}")
        }
    }

    suspend fun getAttendanceForDate(date: String): List<WorkerAttendance> {
        return try {
            db.collection("worker_attendance")
                .whereEqualTo("date", date)
                .get()
                .awaitTask()
                .documents
                .mapNotNull { it.toObject(WorkerAttendance::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAttendanceForDateFlow(date: String): Flow<List<WorkerAttendance>> = callbackFlow {
        val listener = db.collection("worker_attendance")
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WorkerAttendance::class.java)
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun recordAttendance(attendance: WorkerAttendance) {
        deleteAttendanceForWorkerOnDate(attendance.workerId, attendance.date)

        val idToUse = if (attendance.id == 0) generateUniqueId() else attendance.id
        val updated = attendance.copy(id = idToUse)
        db.collection("worker_attendance").document(idToUse.toString()).set(updated).awaitTask()

        if (attendance.status == "Left") {
            val worker = getWorkerById(attendance.workerId)
            if (worker != null && worker.isActive) {
                updateWorker(worker.copy(isActive = false, leaveDate = attendance.date))
            }
        } else if (attendance.status == "On Duty") {
            val worker = getWorkerById(attendance.workerId)
            if (worker != null && !worker.isActive) {
                updateWorker(worker.copy(isActive = true, leaveDate = null))
            }
        }
    }

    // --- RANGE & ANALYTICAL QUERIES (Filtered custom-sorted in-memory for offline performance and zero complex indexes) ---
    suspend fun getAttendanceForRange(startDate: String, endDate: String): List<WorkerAttendance> {
        return try {
            db.collection("worker_attendance").get().awaitTask().documents
                .mapNotNull { it.toObject(WorkerAttendance::class.java) }
                .filter { it.date in startDate..endDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRawMaterialTransactionsForRange(startDate: String, endDate: String): List<RawMaterialTransaction> {
        return try {
            db.collection("raw_material_transactions").get().awaitTask().documents
                .mapNotNull { it.toObject(RawMaterialTransaction::class.java) }
                .filter { it.date in startDate..endDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMasterbatchTransactionsForRange(startDate: String, endDate: String): List<MasterbatchTransaction> {
        return try {
            db.collection("masterbatch_transactions").get().awaitTask().documents
                .mapNotNull { it.toObject(MasterbatchTransaction::class.java) }
                .filter { it.date in startDate..endDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductTransactionsForRange(startDate: String, endDate: String): List<ProductTransaction> {
        return try {
            db.collection("product_transactions").get().awaitTask().documents
                .mapNotNull { it.toObject(ProductTransaction::class.java) }
                .filter { it.date in startDate..endDate }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
