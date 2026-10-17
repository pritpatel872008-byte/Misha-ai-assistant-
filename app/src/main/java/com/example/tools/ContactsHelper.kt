package com.example.tools

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactEntry(
    val id: String,
    val name: String,
    val phoneNumber: String
)

sealed class ContactResolutionResult {
    data class SingleMatch(val contact: ContactEntry) : ContactResolutionResult()
    data class AmbiguousMatches(val candidates: List<ContactEntry>) : ContactResolutionResult()
    object NotFound : ContactResolutionResult()
    object PermissionMissing : ContactResolutionResult()
}

class ContactsHelper(private val context: Context) {

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun findContact(query: String): ContactResolutionResult {
        if (!hasPermission()) {
            return ContactResolutionResult.PermissionMissing
        }

        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) {
            return ContactResolutionResult.NotFound
        }

        val results = mutableListOf<ContactEntry>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""

                    if (name.lowercase().contains(cleanQuery) || number.replace(" ", "").contains(cleanQuery)) {
                        results.add(ContactEntry(id, name, number))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        // Deduplicate contacts with same name & number
        val distinctResults = results.distinctBy { "${it.name}_${it.phoneNumber}" }

        return when {
            distinctResults.isEmpty() -> ContactResolutionResult.NotFound
            distinctResults.size == 1 -> ContactResolutionResult.SingleMatch(distinctResults.first())
            else -> {
                // Check if there is an exact case-insensitive match
                val exactMatches = distinctResults.filter { it.name.equals(query, ignoreCase = true) }
                if (exactMatches.size == 1) {
                    ContactResolutionResult.SingleMatch(exactMatches.first())
                } else {
                    ContactResolutionResult.AmbiguousMatches(distinctResults.take(4))
                }
            }
        }
    }
}
