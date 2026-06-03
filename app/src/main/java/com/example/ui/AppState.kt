package com.example.ui

import com.example.data.*

data class AppState(
    val products: List<Product> = emptyList(),
    val rawMaterials: List<RawMaterial> = emptyList(),
    val masterbatches: List<Masterbatch> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val selectedDate: String = "",
    val currentAttendance: List<WorkerAttendance> = emptyList(),
    val allWorkerAttendance: List<WorkerAttendance> = emptyList(),
    val allProductTransactions: List<ProductTransaction> = emptyList(),
    val allRawMaterialTransactions: List<RawMaterialTransaction> = emptyList(),
    val allMasterbatchTransactions: List<MasterbatchTransaction> = emptyList(),
    val activityLogs: List<ActivityLog> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val appVersionInfo: AppVersion? = null,
    val stats: AggregatedStats = AggregatedStats(),
    val workerStats: List<WorkerAggStats> = emptyList(),
    val factoryStatus: String = "ACTIVE" // "ACTIVE" or "IDLE"
)
