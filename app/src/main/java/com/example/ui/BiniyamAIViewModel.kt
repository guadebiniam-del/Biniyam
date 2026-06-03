package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

class BiniyamAIViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {
    val appState: StateFlow<AppState> = mainViewModel.appState

    fun recordActivityLog(category: String, actionType: String, description: String) {
        mainViewModel.logAction(category, actionType, description)
    }
}
