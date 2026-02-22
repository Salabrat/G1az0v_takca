package com.example.taxi_application.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxi_application.data.model.PaymentMethod
import com.example.taxi_application.data.model.TariffType
import com.example.taxi_application.ui.theme.*
import com.example.taxi_application.ui.viewmodel.GLAZOV_STREETS
import com.example.taxi_application.ui.viewmodel.MapViewModel
import com.example.taxi_application.ui.viewmodel.OrderViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBottomSheet(
    mapViewModel: MapViewModel,
    orderViewModel: OrderViewModel,
    onDismiss: () -> Unit,
    onOrderCreated: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val toFocusRequester = remember { FocusRequester() }

    val fromAddress by orderViewModel.fromAddress.collectAsState()
    val toAddress by orderViewModel.toAddress.collectAsState()
    val selectedTariff by orderViewModel.selectedTariff.collectAsState()
    val selectedPayment by orderViewModel.selectedPayment.collectAsState()
    val estimatedPrice by orderViewModel.estimatedPrice.collectAsState()
    val estimatedDistance by orderViewModel.estimatedDistance.collectAsState()
    val estimatedDuration by orderViewModel.estimatedDuration.collectAsState()
    val currentLocation by mapViewModel.currentLocation.collectAsState()

    var fromText by remember { mutableStateOf(fromAddress) }
    var toText by remember { mutableStateOf(toAddress) }
    var showFromSuggestions by remember { mutableStateOf(false) }
    var showToSuggestions by remember { mutableStateOf(false) }

    val fromSuggestions = remember(fromText) {
        if (fromText.length >= 2)
            GLAZOV_STREETS.filter { it.contains(fromText, ignoreCase = true) }.take(5)
        else emptyList()
    }
    val toSuggestions = remember(toText) {
        if (toText.length >= 2)
            GLAZOV_STREETS.filter { it.contains(toText, ignoreCase = true) }.take(5)
        else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = TaxiWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Куда едем?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaxiBlack
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TaxiGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Поле "Откуда" ────────────────────────────────────────────────
            AddressField(
                value = fromText,
                onValueChange = {
                    fromText = it
                    showFromSuggestions = true
                    showToSuggestions = false
                },
                label = "Откуда",
                icon = Icons.Default.RadioButtonChecked,
                iconColor = TaxiGreen,
                onClear = { fromText = ""; orderViewModel.setFromAddress("") },
                onUseCurrentLocation = {
                    currentLocation?.let { loc ->
                        mapViewModel.reverseGeocode(context, loc) { address ->
                            fromText = address
                            orderViewModel.setFromAddress(address, loc)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { toFocusRequester.requestFocus() })
            )

            // Подсказки для "Откуда"
            if (showFromSuggestions && fromSuggestions.isNotEmpty()) {
                SuggestionsList(
                    suggestions = fromSuggestions,
                    onSelect = { street ->
                        fromText = street
                        orderViewModel.setFromAddress(street)
                        showFromSuggestions = false
                        toFocusRequester.requestFocus()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Разделитель с иконкой обмена
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        val tmp = fromText
                        fromText = toText
                        toText = tmp
                        orderViewModel.setFromAddress(fromText)
                        orderViewModel.setToAddress(toText)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Поменять местами",
                        tint = TaxiGray
                    )
                }
            }

            // ─── Поле "Куда" ──────────────────────────────────────────────────
            AddressField(
                value = toText,
                onValueChange = {
                    toText = it
                    showToSuggestions = true
                    showFromSuggestions = false
                },
                label = "Куда",
                icon = Icons.Default.LocationOn,
                iconColor = TaxiRed,
                onClear = { toText = ""; orderViewModel.setToAddress("") },
                onUseCurrentLocation = null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    orderViewModel.setToAddress(toText)
                    focusManager.clearFocus()
                    showToSuggestions = false
                }),
                modifier = Modifier.focusRequester(toFocusRequester)
            )

            // Подсказки для "Куда"
            if (showToSuggestions && toSuggestions.isNotEmpty()) {
                SuggestionsList(
                    suggestions = toSuggestions,
                    onSelect = { street ->
                        toText = street
                        orderViewModel.setToAddress(street)
                        showToSuggestions = false
                        focusManager.clearFocus()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Избранные адреса ─────────────────────────────────────────────
            FavoriteAddressesRow(
                onSelect = { label, address ->
                    toText = address
                    orderViewModel.setToAddress(address)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Выбор тарифа ─────────────────────────────────────────────────
            Text(
                "Тариф",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaxiBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TariffType.entries.forEach { tariff ->
                    TariffCard(
                        tariff = tariff,
                        isSelected = selectedTariff == tariff,
                        estimatedPrice = if (selectedTariff == tariff) estimatedPrice else 0,
                        modifier = Modifier.weight(1f),
                        onClick = { orderViewModel.setTariff(tariff) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Способ оплаты ────────────────────────────────────────────────
            Text(
                "Оплата",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaxiBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    PaymentCard(
                        method = method,
                        isSelected = selectedPayment == method,
                        modifier = Modifier.weight(1f),
                        onClick = { orderViewModel.setPaymentMethod(method) }
                    )
                }
            }

            // ─── Итог стоимости ───────────────────────────────────────────────
            if (estimatedPrice > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TaxiLightGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Стоимость поездки",
                                fontSize = 13.sp,
                                color = TaxiGray
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "~${estimatedDistance.format(1)} км",
                                    fontSize = 12.sp,
                                    color = TaxiGray
                                )
                                Text(
                                    "~$estimatedDuration мин",
                                    fontSize = 12.sp,
                                    color = TaxiGray
                                )
                            }
                        }
                        Text(
                            "$estimatedPrice ₽",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TaxiBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── Кнопка заказа ────────────────────────────────────────────────
            Button(
                onClick = {
                    orderViewModel.setFromAddress(fromText)
                    orderViewModel.setToAddress(toText)
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                    orderViewModel.createOrder(userId)
                    onOrderCreated()
                },
                enabled = fromText.isNotBlank() && toText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaxiYellow,
                    contentColor = TaxiBlack,
                    disabledContainerColor = Color(0xFFDDDDDD),
                    disabledContentColor = TaxiGray
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.LocalTaxi, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (estimatedPrice > 0) "Заказать за $estimatedPrice ₽"
                    else "Заказать такси",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Поле адреса ──────────────────────────────────────────────────────────────

@Composable
private fun AddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClear: () -> Unit,
    onUseCurrentLocation: (() -> Unit)?,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TaxiGray) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Clear, null, tint = TaxiGray, modifier = Modifier.size(18.dp))
                    }
                }
                if (onUseCurrentLocation != null) {
                    IconButton(onClick = onUseCurrentLocation, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MyLocation, null, tint = TaxiBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TaxiYellow,
            unfocusedBorderColor = Color(0xFFDDDDDD),
            focusedTextColor = TaxiBlack,
            unfocusedTextColor = TaxiBlack,
            cursorColor = TaxiYellow
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// ─── Список подсказок адресов ─────────────────────────────────────────────────

@Composable
private fun SuggestionsList(
    suggestions: List<String>,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = TaxiGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(suggestion, fontSize = 14.sp, color = TaxiBlack)
                }
                if (index < suggestions.size - 1) {
                    HorizontalDivider(color = TaxiLightGray, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─── Избранные адреса ─────────────────────────────────────────────────────────

@Composable
private fun FavoriteAddressesRow(
    onSelect: (String, String) -> Unit
) {
    val favorites = listOf(
        Triple("🏠", "Дом", "ул. Кирова, 15"),
        Triple("💼", "Работа", "пр. Ленина, 8"),
        Triple("🌿", "Дача", "ул. Садовая, 42"),
        Triple("🏥", "Больница", "ул. Короленко, 6"),
        Triple("🛒", "Магазин", "пр. Ленина, 52"),
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(favorites) { (emoji, label, address) ->
            FavoriteChip(
                emoji = emoji,
                label = label,
                onClick = { onSelect(label, address) }
            )
        }
    }
}

@Composable
private fun FavoriteChip(emoji: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TaxiLightGray),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 13.sp, color = TaxiBlack, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Карточка тарифа ──────────────────────────────────────────────────────────

@Composable
private fun TariffCard(
    tariff: TariffType,
    isSelected: Boolean,
    estimatedPrice: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (emoji, description) = when (tariff) {
        TariffType.ECONOMY -> "🚗" to "Эконом"
        TariffType.COMFORT -> "🚙" to "Комфорт"
        TariffType.BUSINESS -> "🚐" to "Минивэн"
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) TaxiYellow else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TaxiYellow.copy(alpha = 0.15f) else TaxiLightGray
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = TaxiBlack
            )
            if (isSelected && estimatedPrice > 0) {
                Text(
                    "$estimatedPrice ₽",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaxiBlack
                )
            } else {
                Text(
                    "от ${tariff.basePrice} ₽",
                    fontSize = 11.sp,
                    color = TaxiGray
                )
            }
        }
    }
}

// ─── Карточка оплаты ──────────────────────────────────────────────────────────

@Composable
private fun PaymentCard(
    method: PaymentMethod,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (emoji, label) = when (method) {
        PaymentMethod.CASH -> "💵" to "Наличными"
        PaymentMethod.SBP -> "📱" to "СБП"
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) TaxiYellow else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TaxiYellow.copy(alpha = 0.15f) else TaxiLightGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = TaxiBlack
            )
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
