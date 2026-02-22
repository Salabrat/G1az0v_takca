package com.example.taxi_application.data.repository

import com.example.taxi_application.data.local.AppDatabase
import com.example.taxi_application.data.model.FavoriteAddressEntity
import com.example.taxi_application.data.model.NearbyDriver
import com.example.taxi_application.data.model.RideHistoryEntity
import com.example.taxi_application.data.model.TaxiOrder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaxiRepository(private val db: AppDatabase) {

    private val firestore = FirebaseFirestore.getInstance()

    // ─── Заказы ───────────────────────────────────────────────────────────────

    suspend fun createOrder(order: TaxiOrder): Result<String> = runCatching {
        val docRef = firestore.collection("orders").document()
        val orderWithId = order.copy(id = docRef.id)
        docRef.set(orderWithId).await()
        docRef.id
    }

    fun listenToOrder(orderId: String): Flow<TaxiOrder?> = callbackFlow {
        val listener = firestore.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(TaxiOrder::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> = runCatching {
        firestore.collection("orders").document(orderId)
            .update("status", "CANCELLED").await()
    }

    suspend fun rateOrder(orderId: String, rating: Int): Result<Unit> = runCatching {
        firestore.collection("orders").document(orderId)
            .update("rating", rating).await()
    }

    // ─── Ближайшие водители ───────────────────────────────────────────────────

    fun listenToNearbyDrivers(): Flow<List<NearbyDriver>> = callbackFlow {
        val listener = firestore.collection("drivers")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val drivers = snapshot?.documents?.mapNotNull {
                    it.toObject(NearbyDriver::class.java)
                } ?: emptyList()
                trySend(drivers)
            }
        awaitClose { listener.remove() }
    }

    // ─── История поездок (Room — офлайн) ──────────────────────────────────────

    fun getRideHistory(): Flow<List<RideHistoryEntity>> =
        db.rideHistoryDao().getAllRides()

    suspend fun saveRideToHistory(ride: RideHistoryEntity) =
        db.rideHistoryDao().insert(ride)

    // ─── Избранные адреса (Room — офлайн) ────────────────────────────────────

    fun getFavoriteAddresses(): Flow<List<FavoriteAddressEntity>> =
        db.favoriteAddressDao().getAll()

    suspend fun saveFavoriteAddress(address: FavoriteAddressEntity) =
        db.favoriteAddressDao().insert(address)

    suspend fun deleteFavoriteAddress(id: String) =
        db.favoriteAddressDao().deleteById(id)
}
