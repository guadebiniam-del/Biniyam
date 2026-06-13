package com.example.data

import java.util.UUID

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String = "Medium",
    val bagWeightKg: Double = 25.0,
    val currentStock: Int = 0,
    val category: String = "Product" // "Product", "Raw Material", "Masterbatch"
)

data class Worker(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String = "Operator",
    val dailyRate: Int = 350,
    val avatarColorString: String = "#00FF88"
)

data class Attendance(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val year: Int = 2018,
    val month: Int = 9, // 1 to 13 (Ginbot is 9)
    val day: Int, // 1 to 30
    val status: String // "በስራ ላይ" (Duty) or "ቀርቷል" (Absent) or "እሁድ" (Sunday) or "ወደፊት" (Future)
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: String, // "user" or "model"
    val timestamp: Long = System.currentTimeMillis()
)
