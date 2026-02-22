package com.example.taxi_application.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.taxi_application.data.model.LatLng
import com.example.taxi_application.data.model.NearbyDriver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    center: LatLng,
    zoom: Double = 14.0,
    userLocation: LatLng? = null,
    nearbyDrivers: List<NearbyDriver> = emptyList(),
    onMapClick: ((LatLng) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Инициализация osmdroid конфигурации
    DisposableEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                
                // Установка центра и зума
                controller.setZoom(zoom)
                controller.setCenter(GeoPoint(center.lat, center.lng))
                
                // Добавление overlay для текущей позиции
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)
                
                // Обработчик кликов по карте
                onMapClick?.let { clickHandler ->
                    setOnClickListener {
                        val projection = projection
                        val geoPoint = projection.fromPixels(it.x.toInt(), it.y.toInt()) as GeoPoint
                        clickHandler(LatLng(geoPoint.latitude, geoPoint.longitude))
                        true
                    }
                }
            }
        },
        update = { mapView ->
            // Обновление центра карты
            mapView.controller.setCenter(GeoPoint(center.lat, center.lng))
            
            // Очистка старых маркеров водителей
            mapView.overlays.removeAll { it is Marker }
            
            // Добавление маркеров для водителей
            nearbyDrivers.forEach { driver ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(driver.lat, driver.lng)
                    title = driver.name
                    snippet = "${driver.carModel} (${driver.carPlate})"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            
            // Добавление маркера для пользователя (если есть)
            userLocation?.let { loc ->
                val userMarker = Marker(mapView).apply {
                    position = GeoPoint(loc.lat, loc.lng)
                    title = "Вы здесь"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                mapView.overlays.add(userMarker)
            }
            
            mapView.invalidate()
        }
    )
}
