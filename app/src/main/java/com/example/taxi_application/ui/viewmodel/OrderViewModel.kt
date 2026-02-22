package com.example.taxi_application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxi_application.data.model.LatLng
import com.example.taxi_application.data.model.NearbyDriver
import com.example.taxi_application.data.model.OrderStatus
import com.example.taxi_application.data.model.PaymentMethod
import com.example.taxi_application.data.model.RideHistoryEntity
import com.example.taxi_application.data.model.TariffType
import com.example.taxi_application.data.model.TaxiOrder
import com.example.taxi_application.data.repository.TaxiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class OrderUiState {
    object Idle : OrderUiState()
    object Loading : OrderUiState()
    data class Searching(val orderId: String) : OrderUiState()
    data class DriverFound(val order: TaxiOrder, val driver: NearbyDriver?) : OrderUiState()
    data class InProgress(val order: TaxiOrder) : OrderUiState()
    data class Completed(val order: TaxiOrder) : OrderUiState()
    data class Error(val message: String) : OrderUiState()
}

class OrderViewModel(private val repository: TaxiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderUiState>(OrderUiState.Idle)
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val _fromAddress = MutableStateFlow("")
    val fromAddress: StateFlow<String> = _fromAddress.asStateFlow()

    private val _toAddress = MutableStateFlow("")
    val toAddress: StateFlow<String> = _toAddress.asStateFlow()

    private val _fromLatLng = MutableStateFlow<LatLng?>(null)
    val fromLatLng: StateFlow<LatLng?> = _fromLatLng.asStateFlow()

    private val _toLatLng = MutableStateFlow<LatLng?>(null)
    val toLatLng: StateFlow<LatLng?> = _toLatLng.asStateFlow()

    private val _selectedTariff = MutableStateFlow(TariffType.ECONOMY)
    val selectedTariff: StateFlow<TariffType> = _selectedTariff.asStateFlow()

    private val _selectedPayment = MutableStateFlow(PaymentMethod.CASH)
    val selectedPayment: StateFlow<PaymentMethod> = _selectedPayment.asStateFlow()

    private val _estimatedPrice = MutableStateFlow(0)
    val estimatedPrice: StateFlow<Int> = _estimatedPrice.asStateFlow()

    private val _estimatedDistance = MutableStateFlow(0.0)
    val estimatedDistance: StateFlow<Double> = _estimatedDistance.asStateFlow()

    private val _estimatedDuration = MutableStateFlow(0)
    val estimatedDuration: StateFlow<Int> = _estimatedDuration.asStateFlow()

    private val _rideHistory = MutableStateFlow<List<RideHistoryEntity>>(emptyList())
    val rideHistory: StateFlow<List<RideHistoryEntity>> = _rideHistory.asStateFlow()

    private val mapViewModel = MapViewModel()

    init {
        loadRideHistory()
    }

    fun setFromAddress(address: String, latLng: LatLng? = null) {
        _fromAddress.value = address
        latLng?.let { _fromLatLng.value = it }
        recalculatePrice()
    }

    fun setToAddress(address: String, latLng: LatLng? = null) {
        _toAddress.value = address
        latLng?.let { _toLatLng.value = it }
        recalculatePrice()
    }

    fun setTariff(tariff: TariffType) {
        _selectedTariff.value = tariff
        recalculatePrice()
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPayment.value = method
    }

    private fun recalculatePrice() {
        val from = _fromLatLng.value ?: return
        val to = _toLatLng.value ?: return
        val distance = mapViewModel.calculateDistance(from, to)
        val duration = (distance / 0.5).toInt().coerceAtLeast(5)
        _estimatedDistance.value = distance
        _estimatedDuration.value = duration
        _estimatedPrice.value = mapViewModel.calculatePrice(distance, _selectedTariff.value)
    }

    fun createOrder(userId: String) {
        val from = _fromLatLng.value
        val to = _toLatLng.value
        if (_fromAddress.value.isBlank() || _toAddress.value.isBlank()) {
            _uiState.value = OrderUiState.Error("Укажите адреса отправления и назначения")
            return
        }

        _uiState.value = OrderUiState.Loading

        val order = TaxiOrder(
            id = UUID.randomUUID().toString(),
            userId = userId,
            fromAddress = _fromAddress.value,
            toAddress = _toAddress.value,
            fromLat = from?.lat ?: GLAZOV_CENTER.lat,
            fromLng = from?.lng ?: GLAZOV_CENTER.lng,
            toLat = to?.lat ?: GLAZOV_CENTER.lat,
            toLng = to?.lng ?: GLAZOV_CENTER.lng,
            tariff = _selectedTariff.value.name,
            paymentMethod = _selectedPayment.value.name,
            estimatedPrice = _estimatedPrice.value,
            distanceKm = _estimatedDistance.value,
            status = OrderStatus.SEARCHING.name,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.createOrder(order)
                .onSuccess { orderId ->
                    _uiState.value = OrderUiState.Searching(orderId)
                    listenToOrderUpdates(orderId)
                }
                .onFailure { e ->
                    _uiState.value = OrderUiState.Error("Ошибка создания заказа: ${e.localizedMessage}")
                }
        }
    }

    private fun listenToOrderUpdates(orderId: String) {
        viewModelScope.launch {
            repository.listenToOrder(orderId).collect { order ->
                order ?: return@collect
                when (order.status) {
                    OrderStatus.ACCEPTED.name, OrderStatus.ARRIVING.name ->
                        _uiState.value = OrderUiState.DriverFound(order, null)
                    OrderStatus.IN_PROGRESS.name ->
                        _uiState.value = OrderUiState.InProgress(order)
                    OrderStatus.COMPLETED.name -> {
                        _uiState.value = OrderUiState.Completed(order)
                        saveToLocalHistory(order)
                    }
                    OrderStatus.CANCELLED.name ->
                        _uiState.value = OrderUiState.Idle
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrder(orderId)
            _uiState.value = OrderUiState.Idle
        }
    }

    fun rateOrder(orderId: String, rating: Int) {
        viewModelScope.launch {
            repository.rateOrder(orderId, rating)
            _uiState.value = OrderUiState.Idle
            resetOrder()
        }
    }

    fun resetOrder() {
        _uiState.value = OrderUiState.Idle
        _fromAddress.value = ""
        _toAddress.value = ""
        _fromLatLng.value = null
        _toLatLng.value = null
        _estimatedPrice.value = 0
        _estimatedDistance.value = 0.0
        _estimatedDuration.value = 0
    }

    private fun saveToLocalHistory(order: TaxiOrder) {
        viewModelScope.launch {
            repository.saveRideToHistory(
                RideHistoryEntity(
                    id = order.id,
                    fromAddress = order.fromAddress,
                    toAddress = order.toAddress,
                    fromLat = order.fromLat,
                    fromLng = order.fromLng,
                    toLat = order.toLat,
                    toLng = order.toLng,
                    tariff = order.tariff,
                    paymentMethod = order.paymentMethod,
                    price = order.estimatedPrice,
                    distanceKm = order.distanceKm,
                    durationMin = _estimatedDuration.value,
                    status = order.status,
                    rating = order.rating,
                    timestamp = order.timestamp,
                    driverName = order.driverName,
                    driverPhone = order.driverPhone,
                    carModel = order.carModel,
                    carPlate = order.carPlate
                )
            )
        }
    }

    private fun loadRideHistory() {
        viewModelScope.launch {
            repository.getRideHistory().collect { history ->
                _rideHistory.value = history
            }
        }
    }
}
