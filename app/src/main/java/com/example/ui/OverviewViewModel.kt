package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

class OverviewViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {
    val appState: StateFlow<AppState> = mainViewModel.appState

    fun updateSelectedDate(dateStr: String) {
        mainViewModel.updateSelectedDate(dateStr)
    }

    fun setReportPeriod(period: MainViewModel.ReportPeriod) {
        mainViewModel.setReportPeriod(period)
    }

    fun addNewProduct(name: String, size: String, color: String, counter: Int, piecesPerBag: Int, weight: Double, initialStock: Int) {
        mainViewModel.addNewProduct(name, size, color, counter, piecesPerBag, weight, initialStock)
    }

    fun addNewMasterbatch(color: String, initialStock: Double) {
        mainViewModel.addNewMasterbatch(color, initialStock)
    }

    fun recordRawMaterialActivity(materialType: String, used: Double, added: Double) {
        mainViewModel.recordRawMaterialActivity(materialType, used, added)
    }
}
