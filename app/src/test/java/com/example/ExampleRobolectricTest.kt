package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.LocationRecord
import com.example.data.model.UserAccount
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.location.DeviceLocationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CircleLocate", appName)
  }

  @Test
  fun `test localization strings`() {
    val enAppName = AppStrings.get("app_name", AppLanguage.ENGLISH)
    val arAppName = AppStrings.get("app_name", AppLanguage.ARABIC)
    assertEquals("CircleLocate", enAppName)
    assertEquals("سيركل لوكيت", arAppName)
  }

  @Test
  fun `test distance calculation`() {
    val dist = DeviceLocationManager.calculateDistanceKm(37.7749, -122.4194, 37.8044, -122.2711)
    assertTrue(dist in 10.0..18.0)
  }

  @Test
  fun `test user account creation and instant login retrieval`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.userAccountDao()

    val testUser = UserAccount(
      fullName = "Alex Morgan",
      emailOrPhone = "alex@circlelocate.com",
      pinCode = "9988",
      isAutoLoginEnabled = true
    )

    val id = dao.insertUser(testUser)
    assertTrue(id > 0)

    val retrieved = dao.getUserByEmailOrPhone("alex@circlelocate.com")
    assertNotNull(retrieved)
    assertEquals("Alex Morgan", retrieved?.fullName)

    val lastActive = dao.getLastActiveUser()
    assertNotNull(lastActive)
    assertEquals("Alex Morgan", lastActive?.fullName)
    assertTrue(lastActive?.isAutoLoginEnabled == true)
  }

  @Test
  fun `test user-scoped records isolation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.locationRecordDao()

    val user1Record = LocationRecord(
      userId = "user_123",
      name = "Target Device 1",
      phoneNumber = "+1 555-0101",
      countryCode = "+1",
      countryName = "United States",
      latitude = 37.7749,
      longitude = -122.4194,
      accuracyMeters = 8f,
      addressText = "Market St, San Francisco",
      carrier = "Verizon",
      networkType = "5G",
      batteryLevel = 85,
      speedKmh = 0f
    )

    val user2Record = LocationRecord(
      userId = "user_456",
      name = "Target Device 2",
      phoneNumber = "+44 7911 123456",
      countryCode = "+44",
      countryName = "United Kingdom",
      latitude = 51.5074,
      longitude = -0.1278,
      accuracyMeters = 10f,
      addressText = "Baker St, London",
      carrier = "EE UK",
      networkType = "4G",
      batteryLevel = 60,
      speedKmh = 15f
    )

    dao.insertRecord(user1Record)
    dao.insertRecord(user2Record)

    // User 1 only accesses their own records
    val user1Records = dao.getRecordsForUser("user_123").first()
    assertTrue(user1Records.any { it.name == "Target Device 1" && it.userId == "user_123" })
    assertTrue(user1Records.none { it.userId == "user_456" })

    // User 2 only accesses their own records
    val user2Records = dao.getRecordsForUser("user_456").first()
    assertTrue(user2Records.any { it.name == "Target Device 2" && it.userId == "user_456" })
    assertTrue(user2Records.none { it.userId == "user_123" })
  }

  @Test
  fun `test libya country support and phone tracking simulation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val supabaseConfig = com.example.data.remote.supabase.SupabaseConfig(context)
    val supabaseService = com.example.data.remote.supabase.SupabaseService(supabaseConfig)
    val repository = com.example.data.repository.TrackerRepository(db.locationRecordDao(), supabaseService)

    // 1. Verify Libya (+218) exists in supported countries
    val libya = com.example.ui.components.SupportedCountries.find { it.code == "+218" }
    assertNotNull(libya)
    assertEquals("Libya", libya?.nameEn)
    assertEquals("ليبيا", libya?.nameAr)
    assertEquals("🇱🇾", libya?.flag)

    // 2. Simulate tracking a Libyan number (+218)
    val record = repository.simulateTracking(
      name = "Omar Al-Mukhtar",
      rawNumber = "91 123 4567",
      countryCode = "+218",
      countryName = "Libya",
      userGpsLocation = null,
      userId = "libya_user_001"
    ) {}

    // Verify carrier is an authentic Libyan provider
    val libyanCarriers = listOf(
      "Libyana (ليبيانا)",
      "Al-Madar Al-Jadeed (المدار الجديد)",
      "LTT (Libya Telecom & Technology)",
      "Hatef Libya (هاتف ليبيا)"
    )
    assertTrue("Carrier should be authentic Libyan telecom, was ${record.carrier}", record.carrier in libyanCarriers)

    // Verify coordinates fall within Libya's geographic bounds (Lat ~ 31-34, Lng ~ 11-24)
    assertTrue("Latitude ${record.latitude} should be in Libya bounds", record.latitude in 31.0..34.5)
    assertTrue("Longitude ${record.longitude} should be in Libya bounds", record.longitude in 11.0..24.5)

    // Verify address is localized to Libya
    val isLibyanAddress = record.addressText.contains("Tripoli") ||
                          record.addressText.contains("Benghazi") ||
                          record.addressText.contains("Misrata") ||
                          record.addressText.contains("طرابلس") ||
                          record.addressText.contains("بنغازي") ||
                          record.addressText.contains("مصراتة")
    assertTrue("Address text should reflect Libyan territory: ${record.addressText}", isLibyanAddress)
  }

  @Test
  fun `test google maps platform api key presence`() {
    val mapsKey = BuildConfig.MAPS_API_KEY
    assertNotNull(mapsKey)
    assertTrue(mapsKey.isNotBlank())
  }
}
