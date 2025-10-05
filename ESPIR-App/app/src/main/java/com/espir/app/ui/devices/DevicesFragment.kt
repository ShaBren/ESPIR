package com.espir.app.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.espir.app.R
import com.espir.app.viewmodel.MainViewModel

class DevicesFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        recyclerView = view.findViewById(R.id.devices_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Observe device list and update adapter
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            // Update RecyclerView adapter with device list
        }
    }
}