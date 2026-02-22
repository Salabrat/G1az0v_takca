package com.example.taxi_application.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxi_application.ui.theme.*
import com.example.taxi_application.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val phone = authViewModel.currentPhone ?: "+7 (---) ---"
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Профиль", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Аватар и номер телефона
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TaxiBlack)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(TaxiYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Пассажир",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TaxiWhite
                        )
                        Text(
                            phone,
                            fontSize = 14.sp,
                            color = TaxiGray
                        )
                    }
                }
            }

            // Настройки
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TaxiWhite)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.Favorite,
                        iconColor = TaxiRed,
                        title = "Избранные адреса",
                        subtitle = "Дом, работа, дача..."
                    )
                    HorizontalDivider(color = TaxiLightGray)
                    ProfileMenuItem(
                        icon = Icons.Default.Notifications,
                        iconColor = TaxiOrange,
                        title = "Уведомления",
                        subtitle = "SMS и push-уведомления"
                    )
                    HorizontalDivider(color = TaxiLightGray)
                    ProfileMenuItem(
                        icon = Icons.Default.Phone,
                        iconColor = TaxiGreen,
                        title = "Поддержка",
                        subtitle = "8-800-XXX-XX-XX (бесплатно)"
                    )
                    HorizontalDivider(color = TaxiLightGray)
                    ProfileMenuItem(
                        icon = Icons.Default.Info,
                        iconColor = TaxiBlue,
                        title = "О приложении",
                        subtitle = "Такси Глазов v1.0"
                    )
                }
            }

            // Кнопка выхода
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TaxiRed.copy(alpha = 0.1f),
                    contentColor = TaxiRed
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти из аккаунта", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти?", fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TaxiRed)
                ) {
                    Text("Выйти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TaxiBlack)
            Text(subtitle, fontSize = 12.sp, color = TaxiGray)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TaxiGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
