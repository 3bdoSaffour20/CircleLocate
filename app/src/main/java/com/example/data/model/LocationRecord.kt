package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_records")
data class LocationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val name: String,
    val phoneNumber: String,
    val countryCode: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val addressText: String,
    val carrier: String,
    val networkType: String,
    val batteryLevel: Int,
    val speedKmh: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val routeJson: String = "",
    val remoteId: String = ""
)

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val label: String = ""
)
