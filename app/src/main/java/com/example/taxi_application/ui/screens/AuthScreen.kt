package com.example.taxi_application.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxi_application.ui.theme.*
import com.example.taxi_application.ui.viewmodel.AuthState
import com.example.taxi_application.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TaxiBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип
            Text(
                text = "🚕",
                fontSize = 72.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Такси Глазов",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TaxiYellow,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Быстро. Удобно. Надёжно.",
                fontSize = 14.sp,
                color = TaxiGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TaxiDarkGray)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Введите номер телефона",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TaxiWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DEV: используйте +7 999 777 09 01",
                        fontSize = 13.sp,
                        color = TaxiYellow,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { if (it.length <= 11) phoneNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("+7 (___) ___-__-__", color = TaxiGray) },
                        prefix = { Text("+7 ", color = TaxiYellow, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (phoneNumber.length >= 10) viewModel.login(phoneNumber) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TaxiYellow,
                            unfocusedBorderColor = TaxiGray,
                            focusedTextColor = TaxiWhite,
                            unfocusedTextColor = TaxiWhite,
                            cursorColor = TaxiYellow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.login(phoneNumber) },
                        enabled = phoneNumber.length >= 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TaxiYellow,
                            contentColor = TaxiBlack,
                            disabledContainerColor = TaxiGray
                        )
                    ) {
                        Text(
                            "Войти",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = TaxiRed,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Нажимая «Получить код», вы соглашаетесь\nс условиями использования сервиса",
                fontSize = 11.sp,
                color = TaxiGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
