package com.example.taxi_application.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Координаты ───────────────────────────────────────────────────────────────
data class LatLng(val lat: Double, val lng: Double)

// ─── Тарифы ───────────────────────────────────────────────────────────────────
enum class TariffType(val displayName: String, val basePrice: Int, val pricePerKm: Int) {
    ECONOMY("Эконом", 100, 25),
    COMFORT("Комфорт", 150, 35),
    BUSINESS("Бизнес/Минивэн", 250, 50)
}

// ─── Способы оплаты ───────────────────────────────────────────────────────────
enum class PaymentMethod(val displayName: String) {
    CASH("Наличными"),
    SBP("Перевод (СБП)")
}

// ─── Статус заказа ────────────────────────────────────────────────────────────
enum class OrderStatus(val displayName: String) {
    SEARCHING("Поиск водителя"),
    ACCEPTED("Водитель принял"),
    ARRIVING("Водитель едет"),
    IN_PROGRESS("В пути"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён")
}

// ─── Районы Глазова ───────────────────────────────────────────────────────────
enum class GlazovDistrict(val displayName: String) {
    CENTER("Центр"),
    GONCHARKA("Гончарка"),
    MASHZAVOD("Машзавод"),
    TORFOZAVOD("Торфозавод"),
    OKTYABRSKY("Октябрьский"),
    ZVEZDNY("Звёздный"),
    SLOBODA("Слобода"),
    OTHER("Другой район")
}

// ─── Room: История поездок ────────────────────────────────────────────────────
@Entity(tableName = "ride_history")
data class RideHistoryEntity(
    @PrimaryKey val id: String,
    val fromAddress: String,
    val toAddress: String,
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val tariff: String,
    val paymentMethod: String,
    val price: Int,
    val distanceKm: Double,
    val durationMin: Int,
    val status: String,
    val rating: Int,
    val timestamp: Long,
    val driverName: String = "",
    val driverPhone: String = "",
    val carModel: String = "",
    val carPlate: String = ""
)

// ─── Room: Избранные адреса ───────────────────────────────────────────────────
@Entity(tableName = "favorite_addresses")
data class FavoriteAddressEntity(
    @PrimaryKey val id: String,
    val label: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val icon: String = "place"
)

// ─── Firestore: Заказ такси ───────────────────────────────────────────────────
data class TaxiOrder(
    val id: String = "",
    val userId: String = "",
    val fromAddress: String = "",
    val toAddress: String = "",
    val fromLat: Double = 0.0,
    val fromLng: Double = 0.0,
    val toLat: Double = 0.0,
    val toLng: Double = 0.0,
    val tariff: String = TariffType.ECONOMY.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val estimatedPrice: Int = 0,
    val distanceKm: Double = 0.0,
    val status: String = OrderStatus.SEARCHING.name,
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val carModel: String = "",
    val carPlate: String = "",
    val rating: Int = 0,
    val tip: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

// ─── Firestore: Профиль пользователя ─────────────────────────────────────────
data class UserProfile(
    val uid: String = "",
    val phone: String = "",
    val name: String = "",
    val totalRides: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Firestore: Водитель (для отображения на карте) ───────────────────────────
data class NearbyDriver(
    val id: String = "",
    val name: String = "",
    val carModel: String = "",
    val carPlate: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val tariff: String = TariffType.ECONOMY.name,
    val rating: Double = 5.0,
    val isAvailable: Boolean = true
)
