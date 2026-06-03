package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

class StockViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {
    val appState: StateFlow<AppState> = mainViewModel.appState

    fun recordRawMaterialActivity(materialType: String, used: Double, added: Double) {
        mainViewModel.recordRawMaterialActivity(materialType, used, added)
    }

    fun recordMasterbatchActivity(masterbatchId: Int, used: Double, bought: Double, takenOut: Double, returned: Double) {
        mainViewModel.recordMasterbatchActivity(masterbatchId, used, bought, takenOut, returned)
    }

    fun addNewMasterbatch(color: String, initialStock: Double) {
        mainViewModel.addNewMasterbatch(color, initialStock)
    }

    fun deleteMasterbatch(masterbatch: Masterbatch) {
        mainViewModel.deleteMasterbatch(masterbatch)
    }
}
