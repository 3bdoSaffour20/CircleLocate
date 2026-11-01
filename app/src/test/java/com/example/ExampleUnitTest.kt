package com.example

import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.location.DeviceLocationManager
import com.example.ui.components.SupportedCountries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun test_localization_strings() {
    val enAppName = AppStrings.get("app_name", AppLanguage.ENGLISH)
    val arAppName = AppStrings.get("app_name", AppLanguage.ARABIC)
    assertEquals("CircleLocate", enAppName)
    assertEquals("سيركل لوكيت", arAppName)
  }

  @Test
  fun test_distance_calculation() {
    val dist = DeviceLocationManager.calculateDistanceKm(37.7749, -122.4194, 37.8044, -122.2711)
    assertTrue(dist in 10.0..18.0)
  }

  @Test
  fun test_libya_country_support() {
    val libya = SupportedCountries.find { it.code == "+218" }
    assertNotNull(libya)
    assertEquals("Libya", libya?.nameEn)
    assertEquals("ليبيا", libya?.nameAr)
    assertEquals("🇱🇾", libya?.flag)
  }

  @Test
  fun test_maps_api_key_configured() {
    val key = BuildConfig.MAPS_API_KEY
    assertNotNull(key)
    assertTrue(key.isNotEmpty())
  }
}
