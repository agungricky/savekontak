package com.example.wacontactcollector.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory session state. Monitoring is opt-in and resets when the app process restarts. */
object CaptureSessionStore {
    private val _monitoring = MutableStateFlow(false)
    private val _detectedPhones = MutableStateFlow<List<PendingPhone>>(emptyList())
    private val _scanStatus = MutableStateFlow(ScanStatus())

    val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()
    val detectedPhones: StateFlow<List<PendingPhone>> = _detectedPhones.asStateFlow()
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    fun startMonitoring() {
        _monitoring.value = true
        _scanStatus.value = ScanStatus()
        _detectedPhones.value = emptyList()
    }
    fun stopMonitoring() { _monitoring.value = false }
    fun publishCandidate(phone: String) {
        if (!_monitoring.value) return
        if (_detectedPhones.value.any { it.normalizedPhone == phone }) return
        _detectedPhones.value = (listOf(PendingPhone(phone)) + _detectedPhones.value).take(MAX_DETECTED_PHONES)
    }
    fun removeCandidate(phone: String) {
        _detectedPhones.value = _detectedPhones.value.filterNot { it.normalizedPhone == phone }
    }
    fun recordScroll(packageName: String) {
        val current = _scanStatus.value
        _scanStatus.value = current.copy(
            packageName = packageName,
            scrollEventCount = current.scrollEventCount + 1,
            lastMonitoringAt = System.currentTimeMillis()
        )
    }

    private const val MAX_DETECTED_PHONES = 50
}
