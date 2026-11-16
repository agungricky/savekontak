package com.example.wacontactcollector

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wacontactcollector.data.AppSettings
import com.example.wacontactcollector.data.ContactRepository
import com.example.wacontactcollector.data.CustomerEntity
import com.example.wacontactcollector.data.SyncHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val total: Int = 0,
    val customers: List<CustomerEntity> = emptyList(),
    val history: List<SyncHistoryEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isSyncing: Boolean = false,
    val lastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository =
        (application as WaContactApplication).container.repository
    private val settings = (application as WaContactApplication).container.preferences.settings

    val total: StateFlow<Int> = repository.total.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )
    val customers: StateFlow<List<CustomerEntity>> = repository.customers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val history: StateFlow<List<SyncHistoryEntity>> = repository.history.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val appSettings: StateFlow<AppSettings> = settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )

    private val _syncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()
    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun syncNow() {
        viewModelScope.launch {
            _syncing.value = true
            _message.value = null
            _message.value = repository.sync().message
            _syncing.value = false
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            (getApplication<WaContactApplication>().container.preferences).save(newSettings)
            repository.scheduleSync(newSettings.syncEnabled, newSettings.intervalHours)
            _message.value = "Pengaturan disimpan"
        }
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
