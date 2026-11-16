package com.example.wacontactcollector.data

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import java.util.ArrayDeque

/**
 * Opt-in, local-only observer for WhatsApp UI events.
 * It inspects only visible nodes to extract a possible phone pattern and never stores
 * surrounding message text, media, or credentials.
 */
class WhatsAppAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED
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
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        CaptureSessionStore.recordScroll(packageName)

        // Inspect only the currently visible WhatsApp window. We extract a phone pattern,
        // never persist the surrounding message text, and never scroll the UI ourselves.
        val candidate = findVisiblePhone(rootInActiveWindow)
            ?: event.text.asSequence()
                .mapNotNull { PhoneCandidateExtractor.find(it.toString()) }
                .firstOrNull()
            ?: event.contentDescription?.toString()?.let(PhoneCandidateExtractor::find)

        if (candidate != null) CaptureSessionStore.publishCandidate(candidate)
    }

    private fun findVisiblePhone(root: AccessibilityNodeInfo?): String? {
        root ?: return null
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.isVisibleToUser && !node.isPassword) {
                PhoneCandidateExtractor.find(node.text?.toString() ?: "")?.let { return it }
                PhoneCandidateExtractor.find(node.contentDescription?.toString() ?: "")?.let { return it }
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
        }
        return null
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
