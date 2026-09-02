package com.sucharu.sucharupro.ui.features.auth.components

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sucharu.sucharupro.core.validation.CustomerValidation

/**
 * Encapsulates a selectable phone number candidate extracted from a device contact.
 */
data class ContactPhoneOption(
    val label: String,
    val rawNumber: String,
    val normalizedNumber: String
)

/**
 * Utility functions for securely querying user-selected device contacts without broad contact scraping.
 */
object ContactPickerHelper {

    /**
     * Extracts phone numbers from a user-selected contact URI with minimum necessary query scope.
     * Does NOT import, cache, or transmit unrelated contacts.
     */
    fun extractPhoneNumbers(context: Context, contactUri: Uri): List<ContactPhoneOption> {
        val options = mutableListOf<ContactPhoneOption>()

        try {
            var contactId: String? = null
            var hasPhoneNumber = false

            context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    if (idIdx >= 0) contactId = cursor.getString(idIdx)
                    if (hasPhoneIdx >= 0) hasPhoneNumber = cursor.getInt(hasPhoneIdx) > 0
                }
            }

            if (hasPhoneNumber && contactId != null) {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.LABEL
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    val numberIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                    while (phoneCursor.moveToNext()) {
                        val rawNumber = if (numberIdx >= 0) phoneCursor.getString(numberIdx) ?: "" else ""
                        if (rawNumber.isNotBlank()) {
                            val type = if (typeIdx >= 0) phoneCursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                            val customLabel = if (labelIdx >= 0) phoneCursor.getString(labelIdx) else null

                            val typeLabel = when (type) {
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                                ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
                                ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel ?: "Custom"
                                else -> "Other"
                            }

                            val normalized = CustomerValidation.normalizePhoneNumber(rawNumber)
                            val entry = ContactPhoneOption(
                                label = typeLabel,
                                rawNumber = rawNumber.trim(),
                                normalizedNumber = normalized
                            )
                            if (!options.any { it.normalizedNumber == entry.normalizedNumber }) {
                                options.add(entry)
                            }
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or transient error
        } catch (_: Exception) {
            // Graceful fallback on schema variation
        }

        return options
    }
}

/**
 * Modal dialog presented when a picked contact contains multiple phone numbers.
 */
@Composable
fun SelectContactPhoneDialog(
    options: List<ContactPhoneOption>,
    onSelectOption: (ContactPhoneOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFF0061A4), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Phone Number",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9ECAFF)
                )

                Text(
                    text = "This contact has multiple numbers. Choose which one to use:",
                    fontSize = 12.sp,
                    color = Color(0xFFB7C8D8),
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF4F6070), RoundedCornerShape(8.dp))
                                .clickable { onSelectOption(option) },
                            color = Color(0xFF0B132B)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF9ECAFF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = option.normalizedNumber,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = option.rawNumber,
                                            color = Color(0xFFB7C8D8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF0061A4).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = option.label,
                                        color = Color(0xFF9ECAFF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = Color(0xFF9ECAFF))
                }
            }
        }
    }
}
