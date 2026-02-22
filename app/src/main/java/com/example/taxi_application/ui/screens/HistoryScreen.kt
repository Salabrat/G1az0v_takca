package com.example.taxi_application.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxi_application.data.model.RideHistoryEntity
import com.example.taxi_application.data.model.TariffType
import com.example.taxi_application.ui.theme.*
import com.example.taxi_application.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    orderViewModel: OrderViewModel,
    onBack: () -> Unit
) {
    val rideHistory by orderViewModel.rideHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "История поездок",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TaxiYellow,
                    titleContentColor = TaxiBlack,
                    navigationIconContentColor = TaxiBlack
                )
            )
        },
        containerColor = TaxiLightGray
    ) { paddingValues ->
        if (rideHistory.isEmpty()) {
            EmptyHistoryPlaceholder(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Итоговая статистика
                    HistoryStatsCard(rides = rideHistory)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(rideHistory, key = { it.id }) { ride ->
                    RideHistoryCard(ride = ride)
                }
            }
        }
    }
}

@Composable
private fun HistoryStatsCard(rides: List<RideHistoryEntity>) {
    val totalSpent = rides.sumOf { it.price }
    val totalKm = rides.sumOf { it.distanceKm }
    val avgRating = if (rides.any { it.rating > 0 })
        rides.filter { it.rating > 0 }.map { it.rating }.average() else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Поездок", value = "${rides.size}", emoji = "🚕")
            StatDivider()
            StatItem(label = "Потрачено", value = "$totalSpent ₽", emoji = "💰")
            StatDivider()
            StatItem(label = "Километров", value = "${totalKm.format(0)} км", emoji = "📍")
            if (avgRating > 0) {
                StatDivider()
                StatItem(label = "Рейтинг", value = avgRating.format(1), emoji = "⭐")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TaxiWhite)
        Text(label, fontSize = 11.sp, color = TaxiGray)
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .background(TaxiGray.copy(alpha = 0.3f))
    )
}

@Composable
private fun RideHistoryCard(ride: RideHistoryEntity) {
    val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
    val date = dateFormat.format(Date(ride.timestamp))

    val tariffEmoji = when (ride.tariff) {
        TariffType.ECONOMY.name -> "🚗"
        TariffType.COMFORT.name -> "🚙"
        TariffType.BUSINESS.name -> "🚐"
        else -> "🚕"
    }

    val paymentEmoji = if (ride.paymentMethod == "CASH") "💵" else "📱"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Верхняя строка: дата + стоимость
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    date,
                    fontSize = 12.sp,
                    color = TaxiGray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(paymentEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${ride.price} ₽",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TaxiBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Маршрут
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.width(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TaxiGreen)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(TaxiGray.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TaxiRed)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        ride.fromAddress,
                        fontSize = 14.sp,
                        color = TaxiBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        ride.toAddress,
                        fontSize = 14.sp,
                        color = TaxiBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = TaxiLightGray)
            Spacer(modifier = Modifier.height(8.dp))

            // Нижняя строка: тариф + расстояние + рейтинг
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$tariffEmoji ${TariffType.entries.find { it.name == ride.tariff }?.displayName ?: ride.tariff}",
                        fontSize = 12.sp,
                        color = TaxiGray
                    )
                    Text(
                        "📍 ${ride.distanceKm.format(1)} км",
                        fontSize = 12.sp,
                        color = TaxiGray
                    )
                    Text(
                        "⏱ ${ride.durationMin} мин",
                        fontSize = 12.sp,
                        color = TaxiGray
                    )
                }
                if (ride.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(ride.rating) { Text("⭐", fontSize = 12.sp) }
                    }
                }
            }

            // Водитель (если есть)
            if (ride.driverName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Водитель: ${ride.driverName} · ${ride.carModel} ${ride.carPlate}",
                    fontSize = 11.sp,
                    color = TaxiGray
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚕", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Поездок пока нет",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TaxiBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Закажите первое такси!",
                fontSize = 14.sp,
                color = TaxiGray
            )
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
