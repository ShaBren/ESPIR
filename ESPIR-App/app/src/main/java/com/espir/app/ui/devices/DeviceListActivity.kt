package com.espir.app.ui.devices

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.espir.app.R

class DeviceListActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.device_list_title)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}