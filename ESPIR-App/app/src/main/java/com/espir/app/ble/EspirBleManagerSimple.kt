package com.espir.app.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager

class EspirBleManager(context: Context) : BleManager(context) {
    
    companion object {
        private const val TAG = "EspirBleManager"
    }
    
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }
    
    override fun getGattCallback(): BleManagerGattCallback {
        return EspirGattCallback()
    }
    
    private inner class EspirGattCallback : BleManagerGattCallback() {
        
        override fun isRequiredServiceSupported(gatt: android.bluetooth.BluetoothGatt): Boolean {
            return true
        }
        
        override fun onServicesInvalidated() {
            // Empty implementation
        }
        
        override fun initialize() {
            // Empty implementation
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