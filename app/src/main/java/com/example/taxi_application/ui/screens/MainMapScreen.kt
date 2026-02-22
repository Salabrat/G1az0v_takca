package com.example.taxi_application.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxi_application.data.model.GlazovDistrict
import com.example.taxi_application.data.model.TaxiOrder
import com.example.taxi_application.ui.components.OsmMapView
import com.example.taxi_application.ui.components.OrderBottomSheet
import com.example.taxi_application.ui.theme.*
import com.example.taxi_application.ui.viewmodel.MapViewModel
import com.example.taxi_application.ui.viewmodel.OrderUiState
import com.example.taxi_application.ui.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapScreen(
    mapViewModel: MapViewModel,
    orderViewModel: OrderViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by mapViewModel.currentLocation.collectAsState()
    val mapCenter by mapViewModel.mapCenter.collectAsState()
    val nearbyDrivers by mapViewModel.nearbyDrivers.collectAsState()
    val currentDistrict by mapViewModel.currentDistrict.collectAsState()
    val locationPermissionGranted by mapViewModel.locationPermissionGranted.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    var showOrderSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        mapViewModel.setLocationPermissionGranted(granted)
        if (granted) mapViewModel.fetchCurrentLocation(context)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ─── OpenStreetMap Карта Глазова ──────────────────────────────────────
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            center = mapCenter,
            zoom = 14.0,
            userLocation = currentLocation,
            nearbyDrivers = nearbyDrivers,
            onMapClick = { latLng ->
                mapViewModel.updateMapCenter(latLng)
                if (showOrderSheet) {
                    mapViewModel.reverseGeocode(context, latLng) { address ->
                        if (orderViewModel.fromAddress.value.isBlank()) {
                            orderViewModel.setFromAddress(address, latLng)
                        } else {
                            orderViewModel.setToAddress(address, latLng)
                        }
                    }
                }
            }
        )

        // ─── Верхняя панель ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Логотип / название
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TaxiYellow),
                    modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚕", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Такси Глазов",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TaxiBlack
                        )
                    }
                }

                // Кнопки профиль / история
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapIconButton(
                        icon = Icons.Default.History,
                        contentDescription = "История",
                        onClick = onNavigateToHistory
                    )
                    MapIconButton(
                        icon = Icons.Default.Person,
                        contentDescription = "Профиль",
                        onClick = onNavigateToProfile
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Текущий район
            AnimatedVisibility(visible = currentDistrict != GlazovDistrict.OTHER) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TaxiBlack.copy(alpha = 0.75f)
                    )
                ) {
                    Text(
                        text = "📍 ${currentDistrict.displayName}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = TaxiWhite
                    )
                }
            }
        }

        // ─── Правая панель управления картой ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка "моё местоположение"
            FloatingActionButton(
                onClick = {
                    if (locationPermissionGranted) {
                        mapViewModel.fetchCurrentLocation(context)
                        mapViewModel.centerOnCurrentLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        )
                    }
                },
                modifier = Modifier.size(52.dp),
                containerColor = TaxiWhite,
                contentColor = TaxiBlack,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Моё местоположение",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ─── Нижняя панель: кнопка заказа / статус ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            when (val state = orderState) {
                is OrderUiState.Idle -> {
                    // Кнопка "Заказать такси"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Счётчик машин поблизости
                        if (nearbyDrivers.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = TaxiGreen.copy(alpha = 0.9f)
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "✓ ${nearbyDrivers.size} машины рядом",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    color = TaxiWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = { showOrderSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TaxiYellow,
                                contentColor = TaxiBlack
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalTaxi,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Заказать такси",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is OrderUiState.Searching -> {
                    SearchingDriverCard(
                        orderId = state.orderId,
                        onCancel = { orderViewModel.cancelOrder(state.orderId) }
                    )
                }

                is OrderUiState.DriverFound -> {
                    DriverFoundCard(
                        order = state.order,
                        onCancel = { orderViewModel.cancelOrder(state.order.id) }
                    )
                }

                is OrderUiState.InProgress -> {
                    InProgressCard(order = state.order)
                }

                is OrderUiState.Completed -> {
                    RatingCard(
                        order = state.order,
                        onRate = { rating -> orderViewModel.rateOrder(state.order.id, rating) }
                    )
                }

                is OrderUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TaxiRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = TaxiWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.message, color = TaxiWhite, fontSize = 14.sp)
                        }
                    }
                }

                else -> {}
            }
        }

        // ─── Нижний лист заказа ───────────────────────────────────────────────
        if (showOrderSheet) {
            OrderBottomSheet(
                mapViewModel = mapViewModel,
                orderViewModel = orderViewModel,
                onDismiss = { showOrderSheet = false },
                onOrderCreated = { showOrderSheet = false }
            )
        }
    }
}

// ─── Вспомогательные компоненты ───────────────────────────────────────────────

@Composable
private fun MapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(TaxiWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TaxiBlack,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SearchingDriverCard(orderId: String, onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiWhite),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = TaxiYellow, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Ищем водителя...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TaxiBlack
            )
            Text(
                "Обычно это занимает 1-3 минуты",
                fontSize = 13.sp,
                color = TaxiGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TaxiRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(TaxiRed)
                )
            ) {
                Text("Отменить заказ", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DriverFoundCard(
    order: TaxiOrder,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiWhite),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TaxiYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚗", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        order.driverName.ifEmpty { "Водитель найден" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        order.carModel.ifEmpty { "Автомобиль" } + " · " +
                                order.carPlate.ifEmpty { "---" },
                        fontSize = 13.sp,
                        color = TaxiGray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "⭐ 4.9",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "~3 мин",
                        fontSize = 12.sp,
                        color = TaxiGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = TaxiLightGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Стоимость:", color = TaxiGray, fontSize = 13.sp)
                    Text(
                        "${order.estimatedPrice} ₽",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TaxiBlack
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TaxiRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = SolidColor(TaxiRed)
                )
            ) {
                Text("Отменить", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InProgressCard(order: TaxiOrder) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiBlack),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🚕", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Вы в пути",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaxiYellow
                )
                Text(
                    "Куда: ${order.toAddress}",
                    fontSize = 13.sp,
                    color = TaxiGray,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${order.estimatedPrice} ₽",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TaxiWhite
            )
        }
    }
}

@Composable
private fun RatingCard(
    order: TaxiOrder,
    onRate: (Int) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiWhite),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Поездка завершена!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${order.toAddress}  •  ${order.estimatedPrice} ₽",
                fontSize = 13.sp,
                color = TaxiGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Оцените поездку", fontSize = 15.sp, color = TaxiGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    Text(
                        text = if (star <= selectedRating) "⭐" else "☆",
                        fontSize = 36.sp,
                        modifier = Modifier.clickable { selectedRating = star }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onRate(selectedRating) },
                enabled = selectedRating > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaxiYellow,
                    contentColor = TaxiBlack
                )
            ) {
                Text("Готово", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
