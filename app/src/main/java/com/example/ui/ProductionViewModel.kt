package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

class ProductionViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {
    val appState: StateFlow<AppState> = mainViewModel.appState

    fun recordProductDailyActivity(productId: Int, fabricated: Int, sold: Int, adjusted: Int, notes: String) {
        mainViewModel.recordProductDailyActivity(productId, fabricated, sold, adjusted, notes)
    }

    fun adjustProductStock(productId: Int, adjustedValue: Int, notes: String) {
        mainViewModel.adjustProductStock(productId, adjustedValue, notes)
    }

    fun deleteProduct(product: Product) {
        mainViewModel.deleteProduct(product)
    }
}
