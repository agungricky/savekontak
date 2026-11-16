package com.example.wacontactcollector.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory session state. Monitoring is opt-in and resets when the app process restarts. */
object CaptureSessionStore {
    private val _monitoring = MutableStateFlow(false)
    private val _pendingPhone = MutableStateFlow<PendingPhone?>(null)
    private val _scanStatus = MutableStateFlow(ScanStatus())

    val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()
    val pendingPhone: StateFlow<PendingPhone?> = _pendingPhone.asStateFlow()
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    fun startMonitoring() {
        _monitoring.value = true
        _scanStatus.value = ScanStatus()
    }
    fun stopMonitoring() { _monitoring.value = false }
    fun publishCandidate(phone: String) {
        if (_monitoring.value) _pendingPhone.value = PendingPhone(phone)
    }
    fun clearCandidate() { _pendingPhone.value = null }
    fun recordScroll(packageName: String) {
        val current = _scanStatus.value
        _scanStatus.value = current.copy(
            packageName = packageName,
            scrollEventCount = current.scrollEventCount + 1,
            lastMonitoringAt = System.currentTimeMillis()
        )
    }
}
