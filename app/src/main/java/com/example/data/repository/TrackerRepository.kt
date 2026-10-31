package com.example.data.repository

import com.example.data.dao.LocationRecordDao
import com.example.data.model.GeoPoint
import com.example.data.model.LocationRecord
import com.example.data.remote.supabase.SupabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random
import kotlin.math.cos

sealed class TrackingProgress {
    data class Processing(val stageIndex: Int, val progress: Float, val statusKey: String) : TrackingProgress()
    data class Success(val record: LocationRecord, val routePoints: List<GeoPoint>) : TrackingProgress()
    data class Error(val message: String) : TrackingProgress()
}

class TrackerRepository(
    private val dao: LocationRecordDao,
    val supabaseService: SupabaseService
) {

    fun getAllRecords(): Flow<List<LocationRecord>> = dao.getAllRecords()

    fun getRecordsForUser(userId: String): Flow<List<LocationRecord>> = dao.getRecordsForUser(userId)

    fun searchRecords(query: String, userId: String): Flow<List<LocationRecord>> = dao.searchRecords(query, userId)

    suspend fun insertRecord(record: LocationRecord): Long = dao.insertRecord(record)

    suspend fun deleteRecord(record: LocationRecord, accessToken: String = "", userId: String = "") {
        dao.deleteRecord(record)
        if (record.remoteId.isNotBlank() && userId.isNotBlank()) {
            try {
                supabaseService.deleteRecord(accessToken, userId, record.remoteId)
            } catch (_: Exception) {}
        }
    }

    suspend fun clearForUser(userId: String, accessToken: String = "") {
        if (userId.isNotBlank()) {
            dao.clearForUser(userId)
            try {
                supabaseService.clearUserRecords(accessToken, userId)
            } catch (_: Exception) {}
        } else {
            dao.clearAll()
        }
    }

    suspend fun syncUserRecordsFromCloud(accessToken: String, userId: String): Result<Int> {
        if (userId.isBlank()) return Result.success(0)
        return try {
            val cloudRecordsResult = supabaseService.fetchUserRecords(accessToken, userId)
            if (cloudRecordsResult.isSuccess) {
                val cloudRecords = cloudRecordsResult.getOrNull().orEmpty()
                if (cloudRecords.isNotEmpty()) {
                    dao.insertAll(cloudRecords)
                }
                Result.success(cloudRecords.size)
            } else {
                Result.failure(cloudRecordsResult.exceptionOrNull() ?: Exception("Failed to sync from cloud"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun simulateTracking(
        name: String,
        rawNumber: String,
        countryCode: String,
        countryName: String,
        userGpsLocation: GeoPoint?,
        userId: String = "",
        accessToken: String = "",
        onProgress: (TrackingProgress) -> Unit
    ): LocationRecord {
        val random = Random(rawNumber.filter { it.isDigit() }.hashCode().toLong() + System.currentTimeMillis() % 1000)

        // Stage 1: Connect
        onProgress(TrackingProgress.Processing(1, 0.20f, "stage_1"))
        delay(650)

        // Stage 2: Analyze MCC/MNC
        onProgress(TrackingProgress.Processing(2, 0.45f, "stage_2"))
        delay(700)

        // Stage 3: Triangulate Towers
        onProgress(TrackingProgress.Processing(3, 0.70f, "stage_3"))
        delay(750)

        // Stage 4: Acquire GPS
        onProgress(TrackingProgress.Processing(4, 0.90f, "stage_4"))
        delay(600)

        // Determine coordinates & carrier
        val carrier = resolveCarrier(countryCode, random)
        val targetCoords = generateCoordinates(countryCode, userGpsLocation, random)
        val routePoints = generateRoutePoints(targetCoords.lat, targetCoords.lng, random)
        val address = generateAddress(countryCode, random)
        val accuracy = 5.0f + random.nextFloat() * 12.0f
        val battery = 35 + random.nextInt(60)
        val speed = if (random.nextBoolean()) (12.0f + random.nextFloat() * 45.0f) else 0.0f
        val networkType = if (random.nextInt(10) > 2) "5G Ultra Wideband" else "4G LTE Advanced"

        val routeJson = JSONArray().apply {
            routePoints.forEach { pt ->
                put(JSONObject().apply {
                    put("lat", pt.lat)
                    put("lng", pt.lng)
                    put("label", pt.label)
                })
            }
        }.toString()

        var record = LocationRecord(
            userId = userId,
            name = name.trim(),
            phoneNumber = "$countryCode ${rawNumber.trim()}",
            countryCode = countryCode,
            countryName = countryName,
            latitude = targetCoords.lat,
            longitude = targetCoords.lng,
            accuracyMeters = accuracy,
            addressText = address,
            carrier = carrier,
            networkType = networkType,
            batteryLevel = battery,
            speedKmh = speed,
            timestamp = System.currentTimeMillis(),
            routeJson = routeJson
        )

        // Try syncing to Supabase Cloud if user is authenticated with Supabase
        if (userId.isNotBlank()) {
            try {
                val remoteResult = supabaseService.insertRecord(accessToken, userId, record)
                if (remoteResult.isSuccess) {
                    val remoteId = remoteResult.getOrNull().orEmpty()
                    record = record.copy(remoteId = remoteId)
                }
            } catch (_: Exception) {
                // Keep local record safe
            }
        }

        val insertedId = dao.insertRecord(record)
        val savedRecord = record.copy(id = insertedId)

        // Stage 5: Done
        onProgress(TrackingProgress.Processing(5, 1.0f, "stage_5"))
        delay(400)
        onProgress(TrackingProgress.Success(savedRecord, routePoints))

        return savedRecord
    }

    private fun resolveCarrier(countryCode: String, random: Random): String {
        val carriers = when (countryCode) {
            "+218" -> listOf(
                "Libyana (ليبيانا)",
                "Al-Madar Al-Jadeed (المدار الجديد)",
                "LTT (Libya Telecom & Technology)",
                "Hatef Libya (هاتف ليبيا)"
            )
            "+1" -> listOf("Verizon Wireless", "AT&T Mobility", "T-Mobile US")
            "+44" -> listOf("EE UK", "Vodafone UK", "O2 UK", "Three UK")
            "+966" -> listOf("STC (Saudi Telecom)", "Mobily", "Zain KSA")
            "+971" -> listOf("e& (Etisalat UAE)", "du Telecom", "Virgin Mobile UAE")
            "+20" -> listOf("Vodafone Egypt", "Orange Egypt", "WE (Telecom Egypt)", "Etisalat Misr")
            "+213" -> listOf("Mobilis (موبيليس)", "Djezzy (جيزي)", "Ooredoo Algérie (أوريدو)")
            "+216" -> listOf("Tunisie Telecom (اتصالات تونس)", "Ooredoo Tunisie", "Orange Tunisie")
            "+212" -> listOf("Maroc Telecom (اتصالات المغرب)", "inwi", "Orange Maroc")
            "+249" -> listOf("Zain Sudan (زين السودان)", "MTN Sudan", "Sudani (سوداني)")
            "+964" -> listOf("Asiacell (آسيا سيل)", "Zain Iraq (زين العراق)", "Korek Telecom (كورك)")
            "+961" -> listOf("touch Lebanon (تاتش)", "Alfa Telecom (ألفا)")
            "+970" -> listOf("Jawwal (جوال)", "Ooredoo Palestine (أوريدو فلسطين)")
            "+963" -> listOf("Syriatel (سيريتل)", "MTN Syria")
            "+962" -> listOf("Zain Jordan (زين الأردن)", "Orange Jordan", "Umniah (أمنية)")
            "+965" -> listOf("Zain Kuwait (زين الكويت)", "Ooredoo Kuwait", "stc Kuwait")
            "+974" -> listOf("Ooredoo Qatar (أوريدو قطر)", "Vodafone Qatar")
            "+968" -> listOf("Omantel (عمانتل)", "Ooredoo Oman", "Vodafone Oman")
            "+973" -> listOf("Batelco (بتلكو)", "stc Bahrain", "Zain Bahrain")
            "+967" -> listOf("Yemen Mobile (يمن موبايل)", "SabaFon (سبأفون)", "YOU Telecom (يو)")
            "+33" -> listOf("Orange France", "SFR", "Bouygues Telecom", "Free Mobile")
            "+49" -> listOf("Telekom Deutschland", "Vodafone Germany", "O2 Telefónica")
            "+90" -> listOf("Turkcell", "Vodafone Türkiye", "Türk Telekom")
            "+91" -> listOf("Jio 5G", "Airtel India", "Vi (Vodafone Idea)")
            "+61" -> listOf("Telstra", "Optus", "Vodafone Australia")
            "+81" -> listOf("NTT Docomo", "SoftBank", "au KDDI")
            else -> listOf("Global Telecom", "GSM International", "Cellular Network")
        }
        return carriers[random.nextInt(carriers.size)]
    }

    private fun generateCoordinates(
        countryCode: String,
        userGpsLocation: GeoPoint?,
        random: Random
    ): GeoPoint {
        if (userGpsLocation != null && (countryCode == "+1" || random.nextBoolean())) {
            val deltaLat = (random.nextDouble() - 0.5) * 0.05
            val deltaLng = (random.nextDouble() - 0.5) * 0.05 / cos(Math.toRadians(userGpsLocation.lat))
            return GeoPoint(userGpsLocation.lat + deltaLat, userGpsLocation.lng + deltaLng, "Target Device")
        }

        val basePoint = when (countryCode) {
            "+218" -> {
                // Major Libyan centers: Tripoli (32.8872, 13.1913), Benghazi (32.1167, 20.0667), Misrata (32.3754, 15.0925)
                val libyanCities = listOf(
                    GeoPoint(32.8872, 13.1913, "Tripoli, Libya (طرابلس)"),
                    GeoPoint(32.1167, 20.0667, "Benghazi, Libya (بنغازي)"),
                    GeoPoint(32.3754, 15.0925, "Misrata, Libya (مصراتة)"),
                    GeoPoint(32.7667, 21.7333, "Al Bayda, Libya (البيضاء)"),
                    GeoPoint(32.7936, 12.4883, "Zawiya, Libya (الزاوية)")
                )
                libyanCities[random.nextInt(libyanCities.size)]
            }
            "+1" -> GeoPoint(37.7749, -122.4194)
            "+44" -> GeoPoint(51.5074, -0.1278)
            "+966" -> GeoPoint(24.7136, 46.6753)
            "+971" -> GeoPoint(25.2048, 55.2708)
            "+20" -> GeoPoint(30.0444, 31.2357)
            "+213" -> GeoPoint(36.7538, 3.0588)
            "+216" -> GeoPoint(36.8065, 10.1815)
            "+212" -> GeoPoint(33.5731, -7.5898)
            "+249" -> GeoPoint(15.5007, 32.5599)
            "+964" -> GeoPoint(33.3152, 44.3661)
            "+961" -> GeoPoint(33.8938, 35.5018)
            "+970" -> GeoPoint(31.9038, 35.2034)
            "+963" -> GeoPoint(33.5138, 36.2765)
            "+962" -> GeoPoint(31.9454, 35.9284)
            "+965" -> GeoPoint(29.3759, 47.9774)
            "+974" -> GeoPoint(25.2854, 51.5310)
            "+968" -> GeoPoint(23.5859, 58.4059)
            "+973" -> GeoPoint(26.2285, 50.5860)
            "+967" -> GeoPoint(15.3694, 44.1910)
            "+33" -> GeoPoint(48.8566, 2.3522)
            "+49" -> GeoPoint(52.5200, 13.4050)
            "+90" -> GeoPoint(41.0082, 28.9784)
            "+91" -> GeoPoint(19.0760, 72.8777)
            "+61" -> GeoPoint(-33.8688, 151.2093)
            "+81" -> GeoPoint(35.6762, 139.6503)
            else -> GeoPoint(40.7128, -74.0060)
        }

        val jitterLat = (random.nextDouble() - 0.5) * 0.04
        val jitterLng = (random.nextDouble() - 0.5) * 0.04
        return GeoPoint(basePoint.lat + jitterLat, basePoint.lng + jitterLng, basePoint.label.ifBlank { "Target Device" })
    }

    private fun generateRoutePoints(targetLat: Double, targetLng: Double, random: Random): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var curLat = targetLat - 0.015 - (random.nextDouble() * 0.01)
        var curLng = targetLng - 0.012 - (random.nextDouble() * 0.01)

        points.add(GeoPoint(curLat, curLng, "15 min ago"))
        curLat += 0.005 + random.nextDouble() * 0.003
        curLng += 0.004 + random.nextDouble() * 0.003
        points.add(GeoPoint(curLat, curLng, "10 min ago"))

        curLat += 0.006 + random.nextDouble() * 0.003
        curLng += 0.004 + random.nextDouble() * 0.003
        points.add(GeoPoint(curLat, curLng, "5 min ago"))

        points.add(GeoPoint(targetLat, targetLng, "Live Position"))
        return points
    }

    private fun generateAddress(countryCode: String, random: Random): String {
        val addresses = when (countryCode) {
            "+218" -> listOf(
                "Omar Al-Mukhtar Street, Tripoli City Center (شارع عمر المختار، طرابلس)",
                "Al-Dahra District, Port Road, Tripoli (طريق الميناء، الظهرة، طرابلس)",
                "Dubai Street, Al-Kish District, Benghazi (شارع دبي، حي الكيش، بنغازي)",
                "Gamal Abdel Nasser St, Downtown Benghazi (شارع جمال عبد الناصر، بنغازي)",
                "Tripoli Coastal Highway, Janzour Area (طريق الساحل، جنزور، طرابلس)",
                "Ramadan Al-Sweihli St, City Center, Misrata (شارع رمضان السويحلي، مصراتة)",
                "Martyrs Square District, Central Tripoli (ميدان الشهداء، وسط العاصمة طرابلس)",
                "Al-Fwehat District, Airport Road, Benghazi (طريق المطار، الفويهات، بنغازي)"
            )
            "+1" -> listOf(
                "Market St & 4th Ave, Downtown District",
                "Grand Avenue, Metro Center, Sector 9",
                "Pine Boulevard & Westview Dr",
                "Commerce Way, Technology Park"
            )
            "+44" -> listOf(
                "Baker St & Marylebone Rd, City Center",
                "King’s Cross Way, Camden District",
                "Kensington High Street, West End",
                "Victoria Embankment, Thames Basin"
            )
            "+966" -> listOf(
                "King Fahd Road, Al Olaya District, Riyadh",
                "Tahlia Street, Al Andalus, Jeddah",
                "Prince Sultan Rd, Al Zahra",
                "Northern Ring Branch Road, Al Nakheel"
            )
            "+971" -> listOf(
                "Sheikh Zayed Road, Downtown Dubai, Near Burj Area",
                "Al Wasl Road, Jumeirah 2",
                "Corniche Road, Al Danah, Abu Dhabi",
                "Business Bay Avenue, Bay Square"
            )
            "+20" -> listOf(
                "El Tahrir St, Dokki, Giza",
                "Abbas El Akkad St, Nasr City, Cairo",
                "Corniche El Nile, Maadi",
                "Road 90, New Cairo City"
            )
            "+213" -> listOf(
                "Didouche Mourad St, Downtown Algiers (شارع ديدوش مراد، الجزائر)",
                "Boulevard Zighoud Youcef, Waterfront (نهج زيغود يوسف)",
                "Rue Larbi Ben M'hidi, City Center"
            )
            "+216" -> listOf(
                "Avenue Habib Bourguiba, Tunis (شارع الحبيب بورقيبة، تونس)",
                "Avenue Mohamed V, Lafayette District",
                "Les Berges du Lac 2, Tunis"
            )
            "+212" -> listOf(
                "Boulevard Mohammed V, Casablanca (شارع محمد الخامس، الدار البيضاء)",
                "Boulevard d'Anfa, Maarif District",
                "Avenue Hassan II, Rabat"
            )
            "+249" -> listOf(
                "Al-Qasr Avenue, Khartoum Center (شارع القصر، الخرطوم)",
                "Africa Road, Al-Riyadh District",
                "Nile Street, Blue Nile Basin"
            )
            "+964" -> listOf(
                "Al-Mansour District, 14 Ramadan St, Baghdad (حي المنصور، بغداد)",
                "Al-Karrada Street, City Center",
                "Al-Jadriya University Quarter"
            )
            "+961" -> listOf(
                "Hamra Street, Beirut (شارع الحمرا، بيروت)",
                "Mar Mikhael, Armenia Street",
                "Corniche Beirut, Raouche Promenade"
            )
            "+970" -> listOf(
                "Al-Irsal Street, Ramallah (شارع الإرسال، رام الله)",
                "Al-Manara Square, City Center",
                "Salah al-Din Street, East Jerusalem"
            )
            "+963" -> listOf(
                "Al-Shaalan District, Damascus (حي الشعلان، دمشق)",
                "Baghdad Street, City Center",
                "Mazaat Autostrad, West Damascus"
            )
            "+962" -> listOf(
                "Zahran Street, 4th Circle, Amman (شارع زهران، الدوار الرابع، عمان)",
                "Rainbow Street, Jabal Amman",
                "Wasfi Al-Tal Street, Gardens"
            )
            "+33" -> listOf(
                "Avenue des Champs-Élysées, 8th Arrondissement, Paris",
                "Boulevard Saint-Germain, Latin Quarter",
                "Rue de Rivoli, City Center"
            )
            "+49" -> listOf(
                "Friedrichstraße, Mitte District, Berlin",
                "Kurfürstendamm, Charlottenburg",
                "Leipziger Platz, City Center"
            )
            else -> listOf(
                "Central Avenue & 5th Crossing",
                "Metro Tech Park Boulevard",
                "Riverside Way, District 4"
            )
        }
        return addresses[random.nextInt(addresses.size)]
    }

    companion object {
        fun parseRouteJson(jsonStr: String): List<GeoPoint> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<GeoPoint>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GeoPoint(
                            lat = obj.getDouble("lat"),
                            lng = obj.getDouble("lng"),
                            label = obj.optString("label", "")
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
