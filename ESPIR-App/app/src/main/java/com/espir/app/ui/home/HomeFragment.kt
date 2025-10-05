package com.espir.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.espir.app.R
import com.espir.app.viewmodel.MainViewModel

class HomeFragment : Fragment() {

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        // Observe connection state and other UI updates here
        viewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            // Update connection status UI
        }
        
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            // Update UI based on connection state
        }
    }
}