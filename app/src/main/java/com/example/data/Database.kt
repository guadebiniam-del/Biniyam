package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        ProductTransaction::class,
        RawMaterial::class,
        RawMaterialTransaction::class,
        Masterbatch::class,
        MasterbatchTransaction::class,
        Worker::class,
        WorkerAttendance::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anwar_recycle_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.inventoryDao()

                    // Pre-populate Raw Materials
                    val rawMaterials = listOf(
                        RawMaterial("LD", 1200.0),
                        RawMaterial("HD", 850.0),
                        RawMaterial("Waste", 2400.0)
                    )
                    rawMaterials.forEach { dao.insertRawMaterial(it) }

                    // Pre-populate Masterbatches
                    val masterbatches = listOf(
                        Masterbatch(id = 1, color = "Black", currentStock = 150.0),
                        Masterbatch(id = 2, color = "White", currentStock = 120.0),
                        Masterbatch(id = 3, color = "Red", currentStock = 50.0),
                        Masterbatch(id = 4, color = "Blue", currentStock = 60.0),
                        Masterbatch(id = 5, color = "Green", currentStock = 45.0),
                        Masterbatch(id = 6, color = "Yellow", currentStock = 30.0)
                    )
                    masterbatches.forEach { dao.insertMasterbatch(it) }

                    // Pre-populate Products (items)
                    val products = listOf(
                        Product(id = 1, name = "Shopping Bag (Standard)", size = "30x40", color = "Red", counter = 500, piecesPerBag = 100, bagWeightKg = 0.25, currentStock = 120),
                        Product(id = 2, name = "Industrial Waste Bag", size = "40x50", color = "Black", counter = 600, piecesPerBag = 120, bagWeightKg = 0.35, currentStock = 85),
                        Product(id = 3, name = "Heavy Carrier Bag", size = "50x60", color = "Blue", counter = 800, piecesPerBag = 150, bagWeightKg = 0.5, currentStock = 40)
                    )
                    products.forEach { dao.insertProduct(it) }

                    // Pre-populate Workers
                    val workers = listOf(
                        Worker(id = 1, name = "Abebe Kebede", joinDate = "2026-01-10"),
                        Worker(id = 2, name = "Chala Gerba", joinDate = "2026-02-15"),
                        Worker(id = 3, name = "Soliana Yared", joinDate = "2026-03-01"),
                        Worker(id = 4, name = "Anwar Adem", joinDate = "2026-04-05")
                    )
                    workers.forEach { dao.insertWorker(it) }
                }
            }
        }
    }
}
