package com.example.taxi_application.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxi_application.data.model.GlazovDistrict
import com.example.taxi_application.data.model.LatLng
import com.example.taxi_application.data.model.NearbyDriver
import com.example.taxi_application.data.model.TariffType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

// Центр Глазова (пр. Ленина, центральная площадь)
val GLAZOV_CENTER = LatLng(58.1387, 52.6584)

class MapViewModel : ViewModel() {

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _mapCenter = MutableStateFlow(GLAZOV_CENTER)
    val mapCenter: StateFlow<LatLng> = _mapCenter.asStateFlow()

    private val _nearbyDrivers = MutableStateFlow<List<NearbyDriver>>(emptyList())
    val nearbyDrivers: StateFlow<List<NearbyDriver>> = _nearbyDrivers.asStateFlow()

    private val _currentDistrict = MutableStateFlow<GlazovDistrict>(GlazovDistrict.CENTER)
    val currentDistrict: StateFlow<GlazovDistrict> = _currentDistrict.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    init {
        loadMockDrivers()
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            _currentLocation.value = latLng
                            _mapCenter.value = latLng
                            _currentDistrict.value = detectDistrict(latLng)
                        }
                    }
            } catch (e: Exception) {
                // Если нет разрешения — используем центр Глазова
                _currentLocation.value = GLAZOV_CENTER
            }
        }
    }

    fun centerOnCurrentLocation() {
        _currentLocation.value?.let { _mapCenter.value = it }
            ?: run { _mapCenter.value = GLAZOV_CENTER }
    }

    fun updateMapCenter(latLng: LatLng) {
        _mapCenter.value = latLng
        _currentDistrict.value = detectDistrict(latLng)
    }

    fun reverseGeocode(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale("ru"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.lat, latLng.lng, 1)
                val address = addresses?.firstOrNull()
                val result = buildString {
                    address?.thoroughfare?.let { append(it) }
                    address?.subThoroughfare?.let { append(", $it") }
                    if (isEmpty()) append(GLAZOV_STREETS.random())
                }
                onResult(result)
            } catch (e: Exception) {
                onResult("${latLng.lat.format(4)}, ${latLng.lng.format(4)}")
            }
        }
    }

    // ─── Определение района Глазова по координатам ────────────────────────────
    fun detectDistrict(latLng: LatLng): GlazovDistrict {
        val (lat, lng) = latLng
        return when {
            lat in 58.135..58.145 && lng in 52.650..52.670 -> GlazovDistrict.CENTER
            lat in 58.125..58.135 && lng in 52.640..52.660 -> GlazovDistrict.GONCHARKA
            lat in 58.145..58.160 && lng in 52.670..52.700 -> GlazovDistrict.MASHZAVOD
            lat in 58.120..58.130 && lng in 52.620..52.640 -> GlazovDistrict.TORFOZAVOD
            lat in 58.130..58.140 && lng in 52.680..52.710 -> GlazovDistrict.OKTYABRSKY
            lat in 58.150..58.165 && lng in 52.640..52.660 -> GlazovDistrict.ZVEZDNY
            lat in 58.110..58.125 && lng in 52.650..52.670 -> GlazovDistrict.SLOBODA
            else -> GlazovDistrict.OTHER
        }
    }

    // ─── Расчёт расстояния (формула Haversine) ────────────────────────────────
    fun calculateDistance(from: LatLng, to: LatLng): Double {
        val r = 6371.0
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) *
                sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun calculatePrice(distanceKm: Double, tariff: TariffType): Int {
        val base = tariff.basePrice
        val perKm = tariff.pricePerKm
        val minPrice = base
        val calculated = base + (distanceKm * perKm).toInt()
        return maxOf(minPrice, calculated)
    }

    private fun loadMockDrivers() {
        _nearbyDrivers.value = listOf(
            NearbyDriver("d1", "Иван К.", "Lada Vesta", "А123ВС18", 58.1400, 52.6600, TariffType.ECONOMY.name, 4.8),
            NearbyDriver("d2", "Сергей М.", "Kia Rio", "В456УД18", 58.1370, 52.6550, TariffType.COMFORT.name, 4.9),
            NearbyDriver("d3", "Алексей П.", "Toyota Camry", "С789ЕА18", 58.1420, 52.6520, TariffType.BUSINESS.name, 5.0),
            NearbyDriver("d4", "Николай Р.", "Lada Granta", "Е321КМ18", 58.1360, 52.6610, TariffType.ECONOMY.name, 4.7),
        )
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}

val GLAZOV_STREETS = listOf(
    "пр. Ленина", "ул. Кирова", "ул. Луначарского", "ул. Короленко",
    "ул. Драгунова", "ул. Сибирская", "ул. Революции", "ул. Пряженникова",
    "ул. Калинина", "ул. Энгельса", "ул. Советская", "ул. Победы",
    "ул. Молодёжная", "ул. Школьная", "ул. Садовая", "ул. Лесная",
    "ул. Мира", "ул. Строителей", "ул. Гагарина", "ул. Чапаева"
)
