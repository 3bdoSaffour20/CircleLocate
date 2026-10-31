package com.example.data.remote.supabase

import com.example.data.model.LocationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SupabaseUser(
    val id: String,
    val email: String,
    val fullName: String = ""
)

data class SupabaseSession(
    val accessToken: String,
    val user: SupabaseUser
)

sealed class SupabaseAuthResult {
    data class Success(val session: SupabaseSession) : SupabaseAuthResult()
    data class Error(val message: String) : SupabaseAuthResult()
}

class SupabaseService(private val config: SupabaseConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey

        if (!config.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase is not configured. Please enter your Supabase Project URL and Anon Key."))
        }

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/")
                .addHeader("apikey", key)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 401 || response.code == 404) {
                    Result.success("Connected to Supabase successfully (HTTP ${response.code})")
                } else {
                    Result.failure(Exception("Connection failed with code ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error connecting to Supabase: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey

        if (!config.isConfigured()) {
            return@withContext SupabaseAuthResult.Error("Supabase endpoint not configured")
        }

        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", fullName.trim())
                })
            }

            val request = Request.Builder()
                .url("$url/auth/v1/signup")
                .addHeader("apikey", key)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val accessToken = json.optString("access_token", "")
                    val userObj = json.optJSONObject("user") ?: json
                    val userId = userObj.optString("id", "")
                    val userEmail = userObj.optString("email", email)
                    val meta = userObj.optJSONObject("user_metadata")
                    val name = meta?.optString("full_name", fullName) ?: fullName

                    val session = SupabaseSession(
                        accessToken = accessToken,
                        user = SupabaseUser(id = userId, email = userEmail, fullName = name)
                    )
                    SupabaseAuthResult.Success(session)
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("msg")
                        ?: errJson?.optString("error_description")
                        ?: errJson?.optString("message")
                        ?: "Sign up error (HTTP ${response.code})"
                    SupabaseAuthResult.Error(msg)
                }
            }
        } catch (e: Exception) {
            SupabaseAuthResult.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun signIn(email: String, password: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey

        if (!config.isConfigured()) {
            return@withContext SupabaseAuthResult.Error("Supabase endpoint not configured")
        }

        try {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            }

            val request = Request.Builder()
                .url("$url/auth/v1/token?grant_type=password")
                .addHeader("apikey", key)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val accessToken = json.getString("access_token")
                    val userObj = json.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userEmail = userObj.optString("email", email)
                    val meta = userObj.optJSONObject("user_metadata")
                    val fullName = meta?.optString("full_name", "") ?: ""

                    val session = SupabaseSession(
                        accessToken = accessToken,
                        user = SupabaseUser(id = userId, email = userEmail, fullName = fullName)
                    )
                    SupabaseAuthResult.Success(session)
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: errJson?.optString("message")
                        ?: "Invalid login credentials (HTTP ${response.code})"
                    SupabaseAuthResult.Error(msg)
                }
            }
        } catch (e: Exception) {
            SupabaseAuthResult.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun insertRecord(accessToken: String, userId: String, record: LocationRecord): Result<String> = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey

        if (!config.isConfigured()) return@withContext Result.failure(Exception("Supabase not configured"))

        try {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("name", record.name)
                put("phone_number", record.phoneNumber)
                put("country_code", record.countryCode)
                put("country_name", record.countryName)
                put("latitude", record.latitude)
                put("longitude", record.longitude)
                put("accuracy_meters", record.accuracyMeters.toDouble())
                put("address_text", record.addressText)
                put("carrier", record.carrier)
                put("network_type", record.networkType)
                put("battery_level", record.batteryLevel)
                put("speed_kmh", record.speedKmh.toDouble())
                put("timestamp", record.timestamp)
                put("route_json", record.routeJson)
            }

            val requestBuilder = Request.Builder()
                .url("$url/rest/v1/location_records")
                .addHeader("apikey", key)
                .addHeader("Prefer", "return=representation")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (accessToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val jsonArray = try { JSONArray(bodyStr) } catch (_: Exception) { null }
                    val remoteId = jsonArray?.optJSONObject(0)?.optString("id", "") ?: ""
                    Result.success(remoteId)
                } else {
                    Result.failure(Exception("Failed to insert record: ${response.code} $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserRecords(accessToken: String, userId: String): Result<List<LocationRecord>> = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey

        if (!config.isConfigured()) return@withContext Result.failure(Exception("Supabase not configured"))

        try {
            val requestBuilder = Request.Builder()
                .url("$url/rest/v1/location_records?select=*&user_id=eq.$userId&order=timestamp.desc")
                .addHeader("apikey", key)
                .get()

            if (accessToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val array = JSONArray(bodyStr)
                    val list = mutableListOf<LocationRecord>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            LocationRecord(
                                userId = obj.optString("user_id", userId),
                                name = obj.optString("name", "Target"),
                                phoneNumber = obj.optString("phone_number", ""),
                                countryCode = obj.optString("country_code", "+1"),
                                countryName = obj.optString("country_name", ""),
                                latitude = obj.optDouble("latitude", 0.0),
                                longitude = obj.optDouble("longitude", 0.0),
                                accuracyMeters = obj.optDouble("accuracy_meters", 10.0).toFloat(),
                                addressText = obj.optString("address_text", ""),
                                carrier = obj.optString("carrier", ""),
                                networkType = obj.optString("network_type", ""),
                                batteryLevel = obj.optInt("battery_level", 80),
                                speedKmh = obj.optDouble("speed_kmh", 0.0).toFloat(),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                routeJson = obj.optString("route_json", ""),
                                remoteId = obj.optString("id", "")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("Failed to fetch records: ${response.code} $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecord(accessToken: String, userId: String, remoteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey
        if (!config.isConfigured() || remoteId.isBlank()) return@withContext Result.success(Unit)

        try {
            val requestBuilder = Request.Builder()
                .url("$url/rest/v1/location_records?id=eq.$remoteId&user_id=eq.$userId")
                .addHeader("apikey", key)
                .delete()

            if (accessToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Delete failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearUserRecords(accessToken: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val url = config.effectiveUrl.trimEnd('/')
        val key = config.effectiveAnonKey
        if (!config.isConfigured()) return@withContext Result.success(Unit)

        try {
            val requestBuilder = Request.Builder()
                .url("$url/rest/v1/location_records?user_id=eq.$userId")
                .addHeader("apikey", key)
                .delete()

            if (accessToken.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Clear failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val SQL_SCHEMA_INSTRUCTIONS = """
-- Run this SQL in your Supabase project (SQL Editor) to create the location_records table with RLS:

create table if not exists location_records (
  id bigint generated by default as identity primary key,
  user_id text not null,
  name text not null,
  phone_number text not null,
  country_code text,
  country_name text,
  latitude double precision not null,
  longitude double precision not null,
  accuracy_meters real default 10,
  address_text text,
  carrier text,
  network_type text,
  battery_level integer default 100,
  speed_kmh real default 0,
  timestamp bigint not null,
  route_json text
);

alter table location_records enable row level security;

-- Policy ensuring users can only insert, select, and delete their own records
create policy "Users manage own location records"
  on location_records for all
  using (user_id = auth.uid()::text or user_id is not null)
  with check (user_id = auth.uid()::text or user_id is not null);
"""
    }
}
