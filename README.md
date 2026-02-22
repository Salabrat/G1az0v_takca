# 🚕 Такси Глазов — Android App

Нативное Android-приложение для заказа такси в г. Глазов (Удмуртия).  
Стек: **Kotlin + Jetpack Compose + Firebase + Room**

---

## 📁 Структура проекта

```
app/src/main/java/com/example/taxi_application/
├── MainActivity.kt                      # Точка входа
│
├── data/
│   ├── model/
│   │   └── Models.kt                    # Все data-классы (TaxiOrder, UserProfile, NearbyDriver, ...)
│   ├── local/
│   │   ├── AppDatabase.kt               # Room база данных
│   │   ├── RideHistoryDao.kt            # DAO: история поездок (офлайн)
│   │   └── FavoriteAddressDao.kt        # DAO: избранные адреса (офлайн)
│   └── repository/
│       └── TaxiRepository.kt            # Репозиторий: Firebase + Room
│
└── ui/
    ├── theme/
    │   ├── Color.kt                     # Цвета (жёлто-чёрная тема такси)
    │   ├── Theme.kt                     # MaterialTheme конфигурация
    │   └── Type.kt                      # Типографика
    ├── navigation/
    │   └── NavGraph.kt                  # Навигация между экранами
    ├── viewmodel/
    │   ├── AuthViewModel.kt             # Firebase Phone Auth
    │   ├── MapViewModel.kt              # Карта, геолокация, районы Глазова
    │   ├── OrderViewModel.kt            # Логика заказа такси
    │   └── OrderViewModelFactory.kt     # Factory для OrderViewModel
    ├── screens/
    │   ├── AuthScreen.kt                # Экран авторизации по SMS
    │   ├── MainMapScreen.kt             # Главный экран с картой
    │   ├── HistoryScreen.kt             # История поездок
    │   └── ProfileScreen.kt             # Профиль пользователя
    └── components/
        ├── YandexMapView.kt             # Yandex MapKit интеграция
        ├── GlazovMapView.kt             # Кастомная карта Глазова (Canvas, deprecated)
        └── OrderBottomSheet.kt          # Нижний лист заказа такси
└── utils/
    └── MapKitManager.kt                 # Менеджер Yandex MapKit
```

---

## 🗄️ Структура базы данных

### Firebase Firestore

```
firestore/
├── users/{userId}
│   ├── uid: String
│   ├── phone: String
│   ├── name: String
│   ├── totalRides: Int
│   └── createdAt: Long
│
├── orders/{orderId}
│   ├── id: String
│   ├── userId: String
│   ├── fromAddress: String
│   ├── toAddress: String
│   ├── fromLat/fromLng: Double
│   ├── toLat/toLng: Double
│   ├── tariff: String (ECONOMY|COMFORT|BUSINESS)
│   ├── paymentMethod: String (CASH|SBP)
│   ├── estimatedPrice: Int
│   ├── distanceKm: Double
│   ├── status: String (SEARCHING|ACCEPTED|ARRIVING|IN_PROGRESS|COMPLETED|CANCELLED)
│   ├── driverId/driverName/driverPhone: String
│   ├── carModel/carPlate: String
│   ├── rating: Int (1-5)
│   ├── tip: Int
│   └── timestamp: Long
│
└── drivers/{driverId}
    ├── id: String
    ├── name: String
    ├── carModel/carPlate: String
    ├── lat/lng: Double
    ├── tariff: String
    ├── rating: Double
    └── isAvailable: Boolean
```

### Room (локальная БД — офлайн)

| Таблица              | Назначение                          |
|----------------------|-------------------------------------|
| `ride_history`       | История поездок (офлайн-доступ)     |
| `favorite_addresses` | Избранные адреса (Дом, Работа, ...) |

---

## 🚀 Настройка и запуск

### 1. Firebase

1. Создайте проект на [console.firebase.google.com](https://console.firebase.google.com)
2. Добавьте Android-приложение с package name: `com.example.taxi_application`
3. Скачайте `google-services.json` и поместите в `app/`
4. Включите **Phone Authentication** в Firebase Auth
5. Создайте базу Firestore (режим test для разработки)

### 2. Yandex MapKit API Key

**ВАЖНО:** Приложение использует Yandex MapKit для отображения карты.

1. Получите API ключ на [developer.tech.yandex.ru](https://developer.tech.yandex.ru/services/)
2. Откройте файл `app/src/main/java/com/example/taxi_application/utils/MapKitManager.kt`
3. Замените `YOUR_YANDEX_MAPKIT_API_KEY` на ваш реальный ключ:
   ```kotlin
   private const val YANDEX_MAPKIT_API_KEY = "ваш_ключ_здесь"
   ```

> **Зависимость уже добавлена:** `com.yandex.android:maps.mobile:4.9.0-full`  
> **Центр Глазова:** Point(58.1387, 52.6584)

### 3. Сборка

```bash
# Debug сборка
./gradlew assembleDebug

# Release сборка
./gradlew assembleRelease
```

---

## 🗺️ Координаты Глазова

| Объект                    | Широта    | Долгота   |
|---------------------------|-----------|-----------|
| Центр города (пр. Ленина) | 58.1387   | 52.6584   |
| Машзавод                  | 58.152    | 52.685    |
| Гончарка                  | 58.130    | 52.650    |
| Торфозавод                | 58.125    | 52.630    |
| Октябрьский               | 58.135    | 52.695    |
| Звёздный                  | 58.157    | 52.650    |
| Слобода                   | 58.117    | 52.660    |

---

## 💰 Тарифы

| Тариф       | Посадка | За км | Минимум |
|-------------|---------|-------|---------|
| Эконом      | 100 ₽   | 25 ₽  | 100 ₽   |
| Комфорт     | 150 ₽   | 35 ₽  | 150 ₽   |
| Бизнес/Минивэн | 250 ₽ | 50 ₽ | 250 ₽   |

---

## 📱 Экраны приложения

1. **AuthScreen** — авторизация по номеру телефона (Firebase SMS)
2. **MainMapScreen** — главный экран с картой, кнопкой заказа, статусом
3. **OrderBottomSheet** — выбор адресов, тарифа, способа оплаты
4. **HistoryScreen** — история поездок с офлайн-доступом (Room)
5. **ProfileScreen** — профиль, избранные адреса, поддержка

---

## 🔧 Технический стек

| Компонент       | Технология                        |
|-----------------|-----------------------------------|
| UI              | Jetpack Compose + Material3       |
| Навигация       | Navigation Compose                |
| Архитектура     | MVVM (ViewModel + StateFlow)      |
| Бэкенд          | Firebase Firestore + Firebase Auth|
| Локальная БД    | Room (офлайн история)             |
| Геолокация      | Google Play Services Location     |
| Карта           | Canvas (→ Yandex MapKit/Mapbox)   |
| Async           | Kotlin Coroutines + Flow          |

---

## 📋 TODO для продакшна

- [ ] Интегрировать Яндекс MapKit SDK (заменить GlazovMapView)
- [ ] Добавить push-уведомления (Firebase Cloud Messaging)
- [ ] Реализовать панель водителя (отдельное приложение)
- [ ] Добавить СБП-оплату через API банка
- [ ] Написать Firestore Security Rules
- [ ] Добавить аналитику (Firebase Analytics)
- [ ] iOS-версия на SwiftUI (аналогичная архитектура)
