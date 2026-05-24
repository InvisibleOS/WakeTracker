package com.shivansh.waketracker

import android.app.Application
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NfcProvisioningState {
    SCANNING, SUCCESS, FAILURE, NFC_OFF
}

class NfcProvisioningViewModel(application: Application) : AndroidViewModel(application) {
    var uiState by mutableStateOf(NfcProvisioningState.SCANNING)
        private set

    fun reset() {
        uiState = NfcProvisioningState.SCANNING
        setSetupFlag(true)
    }

    fun retry() {
        uiState = NfcProvisioningState.SCANNING
        setSetupFlag(true)
    }

    fun onDismissed() {
        setSetupFlag(false)
    }

    private fun setSetupFlag(isSettingUp: Boolean) {
        getApplication<Application>().getSharedPreferences("WakeTrackerPrefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_setting_up_tag", isSettingUp).apply()
    }
    
    fun onTagDiscovered(tag: Tag) {
        if (uiState != NfcProvisioningState.SCANNING) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val uriString = "waketracker://scan"
            
            val success = writeUriToTag(tag, uriString)
            
            withContext(Dispatchers.Main) {
                if (success) {
                    uiState = NfcProvisioningState.SUCCESS
                } else {
                    uiState = NfcProvisioningState.FAILURE
                }
            }
        }
    }

    fun onNfcDisabled() {
        uiState = NfcProvisioningState.NFC_OFF
    }

    fun onNfcEnabled() {
        if (uiState == NfcProvisioningState.NFC_OFF) {
            uiState = NfcProvisioningState.SCANNING
        }
    }

    private fun writeUriToTag(tag: Tag, uriString: String): Boolean {
        return try {
            val uriRecord = NdefRecord.createUri(uriString)
            val ndefMessage = NdefMessage(arrayOf(uriRecord))
            
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return false
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                true
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
