package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast

object TelephonyHelper {
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka dialer telepon: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSmsFallback(context: Context, phoneNumber: String, messageText: String = "") {
        try {
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanNumber")
                putExtra("sms_body", messageText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka SMS seluler: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendDirectSms(context: Context, phoneNumber: String, messageText: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanNumber, null, messageText, null, null)
            }
            Toast.makeText(context, "SMS Terkirim via Jaringan Seluler", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            sendSmsFallback(context, phoneNumber, messageText)
            false
        }
    }
}
