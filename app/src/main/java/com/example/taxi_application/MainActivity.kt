package com.example.taxi_application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.taxi_application.data.local.AppDatabase
import com.example.taxi_application.data.repository.TaxiRepository
import com.example.taxi_application.ui.navigation.TaxiNavGraph
import com.example.taxi_application.ui.theme.Taxi_applicationTheme
import com.example.taxi_application.ui.viewmodel.AuthViewModel
import com.example.taxi_application.ui.viewmodel.MapViewModel
import com.example.taxi_application.ui.viewmodel.OrderViewModel
import com.example.taxi_application.ui.viewmodel.OrderViewModelFactory
import com.example.taxi_application.utils.MapKitManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = TaxiRepository(db)
        
        // Инициализация Yandex MapKit
        MapKitManager.initialize(this)

        setContent {
            Taxi_applicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val mapViewModel: MapViewModel = viewModel()
                    val orderViewModel: OrderViewModel = viewModel(
                        factory = OrderViewModelFactory(repository)
                    )

                    TaxiNavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        mapViewModel = mapViewModel,
                        orderViewModel = orderViewModel
                    )
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        MapKitManager.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        MapKitManager.onStop()
    }
}