package com.example.wacontactcollector.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

fun isWhatsAppAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    val serviceName = WhatsAppAccessibilityService::class.java.name
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
            serviceInfo.packageName == context.packageName && serviceInfo.name == serviceName
        }
}
