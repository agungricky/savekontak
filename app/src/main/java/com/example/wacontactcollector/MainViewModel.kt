package com.example.wacontactcollector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wacontactcollector.data.CaptureHistoryEntity
import com.example.wacontactcollector.data.CaptureSessionStore
import com.example.wacontactcollector.data.ContactRepository
import com.example.wacontactcollector.data.CustomerEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WaContactApplication
    private val repository: ContactRepository = app.container.repository

    val total: StateFlow<Int> = repository.total.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )
    val customers: StateFlow<List<CustomerEntity>> = repository.customers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val history: StateFlow<List<CaptureHistoryEntity>> = repository.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val monitoring: StateFlow<Boolean> = CaptureSessionStore.monitoring
    val pendingPhone = CaptureSessionStore.pendingPhone
    val scanStatus = CaptureSessionStore.scanStatus

    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun startMonitoring() {
        CaptureSessionStore.startMonitoring()
        _message.value = "Pantauan lokal diaktifkan"
    }

    fun stopMonitoring() {
        CaptureSessionStore.stopMonitoring()
        _message.value = "Pantauan dihentikan"
    }

    fun approvePendingPhone() {
        val pending = CaptureSessionStore.pendingPhone.value ?: return
        viewModelScope.launch {
            val result = repository.recordApprovedCandidate(pending.normalizedPhone)
            CaptureSessionStore.clearCandidate()
            _message.value = result.message
        }
    }

    fun dismissPendingPhone() {
        CaptureSessionStore.clearCandidate()
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _message.value = "Semua data lokal dihapus"
        }
    }

    fun clearMessage() { _message.value = null }

    fun launchWhatsApp(business: Boolean) = repository.launchWhatsApp(business)
}
