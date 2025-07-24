package com.example.qonfetty.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class NfcManager(private val activity: Activity) {
    
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(activity)
    }
    
    fun isNfcAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            val intent = Intent(activity, activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity, 0, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )
            
            val techLists = arrayOf(
                arrayOf(android.nfc.tech.Ndef::class.java.name),
                arrayOf(android.nfc.tech.NdefFormatable::class.java.name)
            )
            
            adapter.enableForegroundDispatch(activity, pendingIntent, null, techLists)
        }
    }
    
    fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
    
    suspend fun writeMemberIdToCard(tag: Tag, memberId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("NfcManager", "Writing member ID: $memberId to NFC card")
            
            // Create NDEF message with member ID
            val memberIdRecord = NdefRecord.createTextRecord("en", memberId)
            val ndefMessage = NdefMessage(arrayOf(memberIdRecord))
            
            // Try to write using Ndef
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                return@withContext writeToNdef(ndef, ndefMessage, memberId)
            }
            
            // Try to format and write using NdefFormatable
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                return@withContext formatAndWrite(ndefFormatable, ndefMessage, memberId)
            }
            
            Log.e("NfcManager", "Tag is not NDEF compatible")
            Result.failure(Exception("NFC card is not compatible with NDEF"))
            
        } catch (e: Exception) {
            Log.e("NfcManager", "Error writing to NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun writeToNdef(ndef: Ndef, ndefMessage: NdefMessage, memberId: String): Result<String> {
        return try {
            ndef.connect()
            
            if (!ndef.isWritable) {
                ndef.close()
                return Result.failure(Exception("NFC card is not writable"))
            }
            
            if (ndefMessage.byteArrayLength > ndef.maxSize) {
                ndef.close()
                return Result.failure(Exception("Message too large for NFC card"))
            }
            
            ndef.writeNdefMessage(ndefMessage)
            ndef.close()
            
            Log.d("NfcManager", "Successfully wrote member ID to NDEF card")
            Result.success(memberId)
            
        } catch (e: Exception) {
            try {
                ndef.close()
            } catch (closeException: IOException) {
                Log.e("NfcManager", "Error closing NDEF: ${closeException.message}")
            }
            Log.e("NfcManager", "Error writing to NDEF: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun formatAndWrite(ndefFormatable: NdefFormatable, ndefMessage: NdefMessage, memberId: String): Result<String> {
        return try {
            ndefFormatable.connect()
            ndefFormatable.format(ndefMessage)
            ndefFormatable.close()
            
            Log.d("NfcManager", "Successfully formatted and wrote member ID to NFC card")
            Result.success(memberId)
            
        } catch (e: Exception) {
            try {
                ndefFormatable.close()
            } catch (closeException: IOException) {
                Log.e("NfcManager", "Error closing NdefFormatable: ${closeException.message}")
            }
            Log.e("NfcManager", "Error formatting and writing: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun readMemberIdFromCard(tag: Tag): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("NfcManager", "Reading member ID from NFC card")
            
            val ndef = Ndef.get(tag)
            if (ndef == null) {
                return@withContext Result.failure(Exception("Tag is not NDEF compatible"))
            }
            
            ndef.connect()
            val ndefMessage = ndef.ndefMessage
            
            if (ndefMessage == null) {
                ndef.close()
                return@withContext Result.failure(Exception("No NDEF message found on card"))
            }
            
            val records = ndefMessage.records
            if (records.isEmpty()) {
                ndef.close()
                return@withContext Result.failure(Exception("No records found on NFC card"))
            }
            
            // Look for text record
            for (record in records) {
                if (record.toMimeType() == "text/plain") {
                    val payload = record.payload
                    val text = String(payload, 3, payload.size - 3) // Skip language code
                    ndef.close()
                    Log.d("NfcManager", "Read member ID: $text")
                    return@withContext Result.success(text)
                }
            }
            
            ndef.close()
            Result.failure(Exception("No text record found on NFC card"))
            
        } catch (e: Exception) {
            Log.e("NfcManager", "Error reading from NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
} 