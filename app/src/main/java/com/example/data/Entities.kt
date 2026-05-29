package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- PRODUCTS ---
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val size: String,
    val color: String,
    val counter: Int = 500,
    val piecesPerBag: Int = 100,
    val bagWeightKg: Double = 0.5,
    val currentStock: Int = 0 // Number of bags in stock
)

@Entity(tableName = "product_transactions")
data class ProductTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val date: String, // "yyyy-MM-dd"
    val fabricated: Int, // daily fabricated bags
    val sold: Int, // bags sold
    val adjusted: Int, // stock adjustments (offset)
    val notes: String = ""
)

// --- RAW MATERIALS ---
@Entity(tableName = "raw_materials")
data class RawMaterial(
    @PrimaryKey val type: String, // "LD", "HD", "Waste"
    val currentStock: Double = 0.0 // in kg
)

@Entity(tableName = "raw_material_transactions")
data class RawMaterialTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val materialType: String, // "LD", "HD", "Waste"
    val date: String, // "yyyy-MM-dd"
    val used: Double, // kg used daily
    val added: Double // kg added/purchased daily
)

// --- MASTERBATCH ---
@Entity(tableName = "masterbatches")
data class Masterbatch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val color: String, // Color identifying the masterbatch
    val currentStock: Double = 0.0 // in kg
)

@Entity(tableName = "masterbatch_transactions")
data class MasterbatchTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val masterbatchId: Int,
    val date: String, // "yyyy-MM-dd"
    val used: Double, // kg used daily
    val bought: Double // kg bought daily
)

// --- WORKERS ---
@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val joinDate: String, // "yyyy-MM-dd"
    val leaveDate: String? = null, // "yyyy-MM-dd" if resigned, null if active
    val isActive: Boolean = true
)

@Entity(tableName = "worker_attendance")
data class WorkerAttendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerId: Int,
    val date: String, // "yyyy-MM-dd"
    val status: String // "On Duty", "Absent", "Left" (resigned), "Sunday Off"
)
