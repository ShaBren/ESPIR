package com.espir.app.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import java.util.*

class EspirBleManager(context: Context) : BleManager(context) {
    
    companion object {
        private const val TAG = "EspirBleManager"
        private const val SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
        private const val CHARACTERISTIC_UUID = "87654321-4321-4321-4321-cba987654321"
    }
    
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    private val _receivedData = MutableLiveData<String>()
    val receivedData: LiveData<String> = _receivedData
    
    private var responseCallback: ((String) -> Unit)? = null
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }
    
    override fun getGattCallback(): BleManagerGattCallback {
        return EspirGattCallback()
    }
    
    fun setResponseCallback(callback: (String) -> Unit) {
        responseCallback = callback
    }
    
    fun sendCommand(command: String): Boolean {
        return commandCharacteristic?.let { characteristic ->
            writeCharacteristic(characteristic, command.toByteArray())
                .with { device, data ->
                    Log.d(TAG, "Command sent: $command")
                }
                .enqueue()
            true
        } ?: false
    }
    
    private inner class EspirGattCallback : BleManagerGattCallback() {
        
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(UUID.fromString(SERVICE_UUID))
            if (service != null) {
                commandCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                return commandCharacteristic != null
            }
            return false
        }
        
        override fun onServicesInvalidated() {
            commandCharacteristic = null
        }
        
        override fun initialize() {
            commandCharacteristic?.let { characteristic ->
                // Enable notifications
                enableNotifications(characteristic)
                    .with { device, data ->
                        val response = String(data.value ?: byteArrayOf())
                        Log.d(TAG, "Received response: $response")
                        _receivedData.postValue(response)
                        responseCallback?.invoke(response)
                    }
                    .enqueue()
            }
        }
        
        override fun onDeviceConnecting(device: BluetoothDevice) {
            _connectionState.postValue(ConnectionState.CONNECTING)
        }
        
        override fun onDeviceConnected(device: BluetoothDevice) {
            _connectionState.postValue(ConnectionState.CONNECTED)
        }
        
        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            _connectionState.postValue(ConnectionState.DISCONNECTING)
        }
        
        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            _connectionState.postValue(ConnectionState.DISCONNECTED)
        }
    }
}