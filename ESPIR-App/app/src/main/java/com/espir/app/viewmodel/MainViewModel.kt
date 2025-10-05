package com.espir.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.espir.app.ble.EspirBleManager
import com.espir.app.data.AppDatabase
import com.espir.app.data.Device
import com.espir.app.data.DeviceWithCommands
import com.espir.app.data.IRCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val deviceDao = database.deviceDao()
    private val commandDao = database.irCommandDao()
    
    private val _bleManager = MutableLiveData<EspirBleManager>()
    val bleManager: LiveData<EspirBleManager> = _bleManager
    
    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected
    
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Device data
    val allDevices: LiveData<List<Device>> = deviceDao.getAllDevices()
    val devicesWithCommands: LiveData<List<DeviceWithCommands>> = deviceDao.getDevicesWithCommands()
    
    init {
        _bleManager.value = EspirBleManager(application.applicationContext)
    }
    
    fun initializeBle() {
        _bleManager.value?.let { manager ->
            manager.connectionState.observeForever { state ->
                _isConnected.value = state == EspirBleManager.ConnectionState.CONNECTED
                _statusMessage.value = when (state) {
                    EspirBleManager.ConnectionState.DISCONNECTED -> "Disconnected"
                    EspirBleManager.ConnectionState.CONNECTING -> "Connecting..."
                    EspirBleManager.ConnectionState.CONNECTED -> "Connected"
                    EspirBleManager.ConnectionState.DISCONNECTING -> "Disconnecting..."
                }
            }
            
            manager.setResponseCallback { response ->
                handleBleResponse(response)
            }
        }
    }
    
    fun sendIRCommand(deviceName: String, commandName: String) {
        viewModelScope.launch {
            val device = deviceDao.getDeviceByName(deviceName)
            if (device != null) {
                val command = commandDao.getCommandByName(device.id, commandName)
                if (command != null) {
                    val bleCommand = createTransmitCommand(deviceName, commandName)
                    _bleManager.value?.sendCommand(bleCommand)
                    _statusMessage.value = "Sending command: $commandName"
                } else {
                    _errorMessage.value = "Command not found: $commandName"
                }
            } else {
                _errorMessage.value = "Device not found: $deviceName"
            }
        }
    }
    
    fun startLearningMode() {
        val learnCommand = createLearnCommand()
        _bleManager.value?.sendCommand(learnCommand)
        _statusMessage.value = "Starting IR learning mode..."
    }
    
    fun addDevice(name: String, type: String, manufacturer: String? = null, model: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = Device(
                    name = name,
                    type = type,
                    manufacturer = manufacturer,
                    model = model
                )
                deviceDao.insertDevice(device)
                
                // Also send to ESP32
                val addDeviceCommand = createAddDeviceCommand(device)
                _bleManager.value?.sendCommand(addDeviceCommand)
                
                viewModelScope.launch(Dispatchers.Main) {
                    _statusMessage.value = "Device added: $name"
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _errorMessage.value = "Failed to add device: ${e.message}"
                }
            }
        }
    }
    
    fun deleteDevice(device: Device) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deviceDao.deleteDevice(device)
                
                // Also send to ESP32
                val deleteDeviceCommand = createDeleteDeviceCommand(device.name)
                _bleManager.value?.sendCommand(deleteDeviceCommand)
                
                viewModelScope.launch(Dispatchers.Main) {
                    _statusMessage.value = "Device deleted: ${device.name}"
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _errorMessage.value = "Failed to delete device: ${e.message}"
                }
            }
        }
    }
    
    fun addCommand(deviceId: Int, name: String, description: String?, irCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val command = IRCommand(
                    deviceId = deviceId,
                    name = name,
                    description = description,
                    irCode = irCode
                )
                commandDao.insertCommand(command)
                
                viewModelScope.launch(Dispatchers.Main) {
                    _statusMessage.value = "Command added: $name"
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _errorMessage.value = "Failed to add command: ${e.message}"
                }
            }
        }
    }
    
    fun getSystemStatus() {
        val statusCommand = createStatusCommand()
        _bleManager.value?.sendCommand(statusCommand)
        _statusMessage.value = "Requesting system status..."
    }
    
    private fun handleBleResponse(response: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Parse JSON response and handle accordingly
                // This would include parsing the response and updating UI state
                _statusMessage.value = "Response received: ${response.take(50)}..."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse response: ${e.message}"
            }
        }
    }
    
    private fun createTransmitCommand(device: String, command: String): String {
        return """
            {
                "command": "TRANSMIT",
                "parameters": {
                    "device": "$device",
                    "command": "$command"
                },
                "requestId": "${System.currentTimeMillis()}"
            }
        """.trimIndent()
    }
    
    private fun createLearnCommand(): String {
        return """
            {
                "command": "LEARN",
                "parameters": {
                    "timeout": 15000
                },
                "requestId": "${System.currentTimeMillis()}"
            }
        """.trimIndent()
    }
    
    private fun createAddDeviceCommand(device: Device): String {
        return """
            {
                "command": "ADD_DEVICE",
                "parameters": {
                    "name": "${device.name}",
                    "type": "${device.type}",
                    "manufacturer": "${device.manufacturer ?: ""}",
                    "model": "${device.model ?: ""}"
                },
                "requestId": "${System.currentTimeMillis()}"
            }
        """.trimIndent()
    }
    
    private fun createDeleteDeviceCommand(deviceName: String): String {
        return """
            {
                "command": "DELETE_DEVICE",
                "parameters": {
                    "name": "$deviceName"
                },
                "requestId": "${System.currentTimeMillis()}"
            }
        """.trimIndent()
    }
    
    private fun createStatusCommand(): String {
        return """
            {
                "command": "GET_STATUS",
                "parameters": {},
                "requestId": "${System.currentTimeMillis()}"
            }
        """.trimIndent()
    }
    
    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
}