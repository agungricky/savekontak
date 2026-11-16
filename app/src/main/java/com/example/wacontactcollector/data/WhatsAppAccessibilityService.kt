package com.example.wacontactcollector.data

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Opt-in, local-only observer for WhatsApp UI events.
 * It does not retrieve the window tree and never stores message text, media, or credentials.
 */
class WhatsAppAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = WHATSAPP_PACKAGES.toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS.inv()
            notificationTimeout = 500
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!CaptureSessionStore.monitoring.value) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in WHATSAPP_PACKAGES) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        CaptureSessionStore.recordScan(packageName)

        // Only inspect event-provided fragments for a phone pattern. No node-tree traversal.
        val candidate = event.text.asSequence()
            .mapNotNull { PhoneCandidateExtractor.find(it.toString()) }
            .firstOrNull()
            ?: event.contentDescription?.toString()?.let(PhoneCandidateExtractor::find)

        if (candidate != null) CaptureSessionStore.publishCandidate(candidate)
    }

    override fun onInterrupt() = Unit

    companion object {
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
    }
}

object PhoneCandidateExtractor {
    private val candidatePattern = Regex("(?<!\\d)(?:\\+?62|0|8)(?:[\\s().-]*\\d){8,14}(?!\\d)")

    fun find(text: String): String? = candidatePattern.find(text)?.value?.let(PhoneNormalizer::normalize)
}
