package com.example.qonfetty.nfc

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NfcWriteManager {
    private var currentWriteCallback: ((android.nfc.Tag) -> Unit)? = null
    private var currentMemberId: String? = null
    private var currentNfcManager: NfcManager? = null
    private var currentOnSuccess: ((String) -> Unit)? = null
    private var currentOnError: ((String) -> Unit)? = null
    
    fun startWrite(
        memberId: String,
        nfcManager: NfcManager,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        currentMemberId = memberId
        currentNfcManager = nfcManager
        currentOnSuccess = onSuccess
        currentOnError = onError
        
        currentWriteCallback = { tag ->
            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                try {
                    Log.d("NfcWriteManager", "Writing member ID: $memberId to NFC card")
                    
                    val writeResult = nfcManager.writeMemberIdToCard(tag, memberId)
                    
                    writeResult.fold(
                        onSuccess = { writtenMemberId ->
                            Log.d("NfcWriteManager", "Successfully wrote member ID: $writtenMemberId")
                            currentOnSuccess?.invoke(writtenMemberId)
                            clearCurrentWrite()
                        },
                        onFailure = { exception ->
                            Log.e("NfcWriteManager", "Failed to write member ID: ${exception.message}")
                            currentOnError?.invoke(exception.message ?: "Unknown error")
                            clearCurrentWrite()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("NfcWriteManager", "Error during NFC write: ${e.message}")
                    currentOnError?.invoke(e.message ?: "Unknown error")
                    clearCurrentWrite()
                }
            }
        }
        
        Log.d("NfcWriteManager", "NFC write callback set up for member ID: $memberId")
    }
    
    fun getCurrentWriteCallback(): ((android.nfc.Tag) -> Unit)? {
        return currentWriteCallback
    }
    
    fun clearCurrentWrite() {
        currentWriteCallback = null
        currentMemberId = null
        currentNfcManager = null
        currentOnSuccess = null
        currentOnError = null
        Log.d("NfcWriteManager", "Cleared current NFC write callback")
    }
    
    fun isWriting(): Boolean {
        return currentWriteCallback != null
    }
} 