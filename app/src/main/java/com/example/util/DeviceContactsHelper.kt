package com.example.util

import android.content.Context
import android.provider.ContactsContract
import com.example.data.models.Contact

object DeviceContactsHelper {
    fun loadDeviceContacts(context: Context): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val seenNumbers = mutableSetOf<String>()
        val colors = listOf(
            0xFF00E5FF, 0xFF7C4DFF, 0xFF10B981, 0xFFFF2A85, 0xFFF59E0B,
            0xFF3B82F6, 0xFFEC4899, 0xFF14B8A6, 0xFF8B5CF6, 0xFFF97316
        )
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                var colorIdx = 0
                while (it.moveToNext()) {
                    val rawNum = it.getString(numIdx) ?: continue
                    val cleanNum = rawNum.replace("[^0-9+]".toRegex(), "")
                    if (cleanNum.isEmpty() || seenNumbers.contains(cleanNum)) continue
                    seenNumbers.add(cleanNum)
                    val name = it.getString(nameIdx) ?: cleanNum
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) ?: "" else ""
                    val contactId = if (idIdx >= 0) it.getString(idIdx) ?: "c_${System.currentTimeMillis()}" else "c_${System.currentTimeMillis()}"
                    val chatinTag = "@" + name.lowercase().replace("[^a-z0-9]".toRegex(), ".")
                    contactsList.add(
                        Contact(
                            id = "device_$contactId",
                            name = name,
                            phoneNumber = rawNum,
                            chatinId = chatinTag,
                            avatarUrl = photoUri,
                            avatarColorHex = colors[colorIdx % colors.size],
                            statusMessage = "Kontak Buku Telepon Perangkat",
                            isOnline = true,
                            lastSeenText = "Buku Telepon",
                            isFavorite = false,
                            hasRcs = true,
                            allowsVoip = true
                        )
                    )
                    colorIdx++
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactsList
    }
}
