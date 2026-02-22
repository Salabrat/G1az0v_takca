package com.example.taxi_application.utils

import android.content.Context
import com.yandex.mapkit.MapKitFactory

object MapKitManager {
    private var initialized = false
    
    // ВАЖНО: Замените на ваш реальный API ключ Yandex MapKit
    // Получить можно здесь: https://developer.tech.yandex.ru/services/
    private const val YANDEX_MAPKIT_API_KEY = "16c1e9ba-e81c-41e0-8296-05bd2edbceb8"
    
    fun initialize(context: Context) {
        if (!initialized) {
            MapKitFactory.setApiKey(YANDEX_MAPKIT_API_KEY)
            MapKitFactory.initialize(context)
            initialized = true
        }
    }
    
    fun onStart() {
        if (initialized) {
            MapKitFactory.getInstance().onStart()
        }
    }
    
    fun onStop() {
        if (initialized) {
            MapKitFactory.getInstance().onStop()
        }
    }
}
