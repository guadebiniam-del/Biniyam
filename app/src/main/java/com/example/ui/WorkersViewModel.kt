package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.*
import kotlinx.coroutines.flow.StateFlow

class WorkersViewModel(
    val mainViewModel: MainViewModel
) : ViewModel() {
    val appState: StateFlow<AppState> = mainViewModel.appState

    fun recordAttendance(workerId: Int, status: String) {
        mainViewModel.markWorkerAttendance(workerId, status)
    }

    fun updateWorkerSalary(workerId: Int, newSalary: Double) {
        mainViewModel.updateWorkerSalary(workerId, newSalary)
    }

    fun addNewWorker(name: String, salary: Double) {
        mainViewModel.addNewWorker(name, salary)
    }
}
