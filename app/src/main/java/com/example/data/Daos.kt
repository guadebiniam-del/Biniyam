package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // --- PRODUCT TRANSACTIONS ---
    @Query("SELECT * FROM product_transactions ORDER BY date DESC, id DESC")
    fun getAllProductTransactionsFlow(): Flow<List<ProductTransaction>>

    @Query("SELECT * FROM product_transactions WHERE date = :date")
    suspend fun getProductTransactionsForDate(date: String): List<ProductTransaction>

    @Query("SELECT * FROM product_transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getProductTransactionsForRange(startDate: String, endDate: String): List<ProductTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductTransaction(transaction: ProductTransaction): Long

    // --- RAW MATERIALS ---
    @Query("SELECT * FROM raw_materials")
    fun getAllRawMaterialsFlow(): Flow<List<RawMaterial>>

    @Query("SELECT * FROM raw_materials")
    suspend fun getAllRawMaterials(): List<RawMaterial>

    @Query("SELECT * FROM raw_materials WHERE type = :type")
    suspend fun getRawMaterialByType(type: String): RawMaterial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawMaterial(rawMaterial: RawMaterial)

    @Update
    suspend fun updateRawMaterial(rawMaterial: RawMaterial)

    // --- RAW MATERIAL TRANSACTIONS ---
    @Query("SELECT * FROM raw_material_transactions ORDER BY date DESC, id DESC")
    fun getAllRawMaterialTransactionsFlow(): Flow<List<RawMaterialTransaction>>

    @Query("SELECT * FROM raw_material_transactions WHERE date = :date")
    suspend fun getRawMaterialTransactionsForDate(date: String): List<RawMaterialTransaction>

    @Query("SELECT * FROM raw_material_transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRawMaterialTransactionsForRange(startDate: String, endDate: String): List<RawMaterialTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawMaterialTransaction(transaction: RawMaterialTransaction): Long


    // --- MASTERBATCH ---
    @Query("SELECT * FROM masterbatches ORDER BY id DESC")
    fun getAllMasterbatchesFlow(): Flow<List<Masterbatch>>

    @Query("SELECT * FROM masterbatches")
    suspend fun getAllMasterbatches(): List<Masterbatch>

    @Query("SELECT * FROM masterbatches WHERE id = :id")
    suspend fun getMasterbatchById(id: Int): Masterbatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterbatch(masterbatch: Masterbatch): Long

    @Update
    suspend fun updateMasterbatch(masterbatch: Masterbatch)

    @Delete
    suspend fun deleteMasterbatch(masterbatch: Masterbatch)

    // --- MASTERBATCH TRANSACTIONS ---
    @Query("SELECT * FROM masterbatch_transactions ORDER BY date DESC, id DESC")
    fun getAllMasterbatchTransactionsFlow(): Flow<List<MasterbatchTransaction>>

    @Query("SELECT * FROM masterbatch_transactions WHERE date = :date")
    suspend fun getMasterbatchTransactionsForDate(date: String): List<MasterbatchTransaction>

    @Query("SELECT * FROM masterbatch_transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMasterbatchTransactionsForRange(startDate: String, endDate: String): List<MasterbatchTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterbatchTransaction(transaction: MasterbatchTransaction): Long


    // --- WORKERS ---
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkersFlow(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    suspend fun getAllWorkers(): List<Worker>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: Int): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    // --- WORKER ATTENDANCE ---
    @Query("SELECT * FROM worker_attendance ORDER BY date DESC, id DESC")
    fun getAllAttendanceFlow(): Flow<List<WorkerAttendance>>

    @Query("SELECT * FROM worker_attendance WHERE date = :date")
    suspend fun getAttendanceForDate(date: String): List<WorkerAttendance>

    @Query("SELECT * FROM worker_attendance WHERE date = :date")
    fun getAttendanceForDateFlow(date: String): Flow<List<WorkerAttendance>>

    @Query("SELECT * FROM worker_attendance WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAttendanceForRange(startDate: String, endDate: String): List<WorkerAttendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: WorkerAttendance): Long

    @Query("DELETE FROM worker_attendance WHERE date = :date AND workerId = :workerId")
    suspend fun deleteAttendanceForWorkerOnDate(workerId: Int, date: String)
}
