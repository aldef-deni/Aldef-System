package com.aldef.system.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * AldefAccessibilityService:
 * Memberikan izin kepada Aldef System untuk mengotomatisasi interaksi layar (seperti auto-click tombol Kirim di WhatsApp).
 */
class AldefAccessibilityService : AccessibilityService() {

    companion object {
        var isAutoSendWhatsAppEnabled = true
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = arrayOf("com.whatsapp", "com.whatsapp.w4b")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Auto-click tombol kirim WhatsApp setelah pesan terisi oleh Intent
        if (isAutoSendWhatsAppEnabled && event.packageName?.toString()?.contains("whatsapp") == true) {
            val rootNode = rootInActiveWindow ?: return
            findAndClickSendButton(rootNode)
        }
    }

    private fun findAndClickSendButton(rootNode: AccessibilityNodeInfo) {
        // Cari tombol send berdasarkan Resource ID WhatsApp atau ContentDescription ("Kirim" / "Send")
        val sendById = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendById.isNotEmpty()) {
            for (node in sendById) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }

        // Fallback: cari berdasarkan text / content description
        val nodesByText = rootNode.findAccessibilityNodeInfosByText("Kirim") + rootNode.findAccessibilityNodeInfosByText("Send")
        for (node in nodesByText) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    override fun onInterrupt() {}
}