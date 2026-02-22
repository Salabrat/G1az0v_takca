package com.example.taxi_application.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.taxi_application.data.model.LatLng
import com.example.taxi_application.data.model.NearbyDriver
import com.example.taxi_application.ui.theme.TaxiYellow
import com.example.taxi_application.utils.MapKitManager
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    zoom: Float = 14f,
    userLocation: LatLng? = null,
    nearbyDrivers: List<NearbyDriver> = emptyList(),
    onMapClick: ((LatLng) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapObjectCollection by remember { mutableStateOf<MapObjectCollection?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> MapKitManager.onStart()
                Lifecycle.Event.ON_STOP -> MapKitManager.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(center, zoom) {
        mapView?.mapWindow?.map?.move(
            CameraPosition(Point(center.lat, center.lng), zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    LaunchedEffect(userLocation, nearbyDrivers) {
        mapObjectCollection?.clear()
        
        userLocation?.let { loc ->
            mapObjectCollection?.addPlacemark()?.apply {
                geometry = Point(loc.lat, loc.lng)
            }
        }

        nearbyDrivers.forEach { driver ->
            mapObjectCollection?.addPlacemark()?.apply {
                geometry = Point(driver.lat, driver.lng)
            }
        }
    }

    if (hasError) {
        // Fallback на Canvas-карту при ошибке
        GlazovMapView(
            modifier = modifier,
            center = center,
            currentLocation = userLocation,
            nearbyDrivers = nearbyDrivers,
            onMapTap = onMapClick ?: {}
        )
        
        // Показываем предупреждение
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFFFF9800), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ Yandex MapKit: $errorMessage\nИспользуется Canvas-карта",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { ctx ->
                try {
                    MapKitManager.initialize(ctx)
                    MapView(ctx).also { mv ->
                        mapView = mv
                        mapObjectCollection = mv.mapWindow.map.mapObjects.addCollection()
                        
                        mv.mapWindow.map.move(
                            CameraPosition(Point(center.lat, center.lng), zoom, 0f, 0f)
                        )

                        onMapClick?.let { clickHandler ->
                            mv.mapWindow.map.addInputListener(object : com.yandex.mapkit.map.InputListener {
                                override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                                    clickHandler(LatLng(point.latitude, point.longitude))
                                }

                                override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {}
                            })
                        }
                    }
                } catch (e: Exception) {
                    Log.e("YandexMapView", "Failed to initialize MapKit", e)
                    hasError = true
                    errorMessage = when {
                        e.message?.contains("API key") == true -> "Не установлен API ключ"
                        else -> e.message ?: "Ошибка инициализации"
                    }
                    // Возвращаем пустой MapView для избежания краша
                    MapView(ctx)
                }
            },
            update = { mv ->
                mapView = mv
            }
        )
    }
}
