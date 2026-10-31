package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.GeoPoint
import com.example.data.model.LocationRecord
import com.example.data.model.UserAccount
import com.example.data.remote.supabase.SupabaseAuthResult
import com.example.data.remote.supabase.SupabaseConfig
import com.example.data.remote.supabase.SupabaseService
import com.example.data.repository.TrackerRepository
import com.example.data.repository.TrackingProgress
import com.example.localization.AppLanguage
import com.example.location.DeviceLocationManager
import com.example.ui.components.CountryInfo
import com.example.ui.components.SupportedCountries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TrackingUiState {
    object Idle : TrackingUiState()
    data class Processing(val stageIndex: Int, val progress: Float, val statusKey: String) : TrackingUiState()
    data class Success(val record: LocationRecord, val routePoints: List<GeoPoint>) : TrackingUiState()
    data class Error(val message: String) : TrackingUiState()
}

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val supabaseConfig = SupabaseConfig(application)
    val supabaseService = SupabaseService(supabaseConfig)
    private val repository = TrackerRepository(database.locationRecordDao(), supabaseService)
    private val userDao = database.userAccountDao()
    val locationManager = DeviceLocationManager(application)
    private val authPrefs = application.getSharedPreferences("circlelocate_auth_session", Context.MODE_PRIVATE)

    // User Account & Instant Login State
    val savedAccounts: StateFlow<List<UserAccount>> = userDao.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _isAuthModalVisible = MutableStateFlow(false)
    val isAuthModalVisible: StateFlow<Boolean> = _isAuthModalVisible.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    // Current Navigation Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Inputs
    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _phoneInput = MutableStateFlow("")
    val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

    private val _selectedCountry = MutableStateFlow(SupportedCountries[0]) // Default +1 US
    val selectedCountry: StateFlow<CountryInfo> = _selectedCountry.asStateFlow()

    // Tracking state
    private val _trackingUiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Idle)
    val trackingUiState: StateFlow<TrackingUiState> = _trackingUiState.asStateFlow()

    private val _currentRecord = MutableStateFlow<LocationRecord?>(null)
    val currentRecord: StateFlow<LocationRecord?> = _currentRecord.asStateFlow()

    private val _currentRoutePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val currentRoutePoints: StateFlow<List<GeoPoint>> = _currentRoutePoints.asStateFlow()

    private val _showResultSheet = MutableStateFlow(false)
    val showResultSheet: StateFlow<Boolean> = _showResultSheet.asStateFlow()

    // History Scoped to Logged In User
    private val _historyQuery = MutableStateFlow("")
    val historyQuery: StateFlow<String> = _historyQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyRecords: StateFlow<List<LocationRecord>> = _currentUser
        .flatMapLatest { user ->
            val userId = user?.supabaseUid?.ifBlank { user.emailOrPhone } ?: ""
            repository.getRecordsForUser(userId)
        }
        .combine(_historyQuery) { records, query ->
            if (query.isBlank()) {
                records
            } else {
                val q = query.trim().lowercase()
                records.filter {
                    it.name.lowercase().contains(q) || it.phoneNumber.contains(q) || it.countryName.lowercase().contains(q)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language & Theme Settings
    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Dialogs
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showSupabaseDialog = MutableStateFlow(false)
    val showSupabaseDialog: StateFlow<Boolean> = _showSupabaseDialog.asStateFlow()

    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    private var trackingJob: Job? = null

    init {
        try {
            // Check initial device GPS permission
            locationManager.checkPermission()
            if (locationManager.hasPermission.value) {
                locationManager.requestCurrentLocation()
            }
        } catch (_: Exception) {
            // Handled gracefully
        }

        // Check if user account exists on this app and restore persistent session
        viewModelScope.launch {
            try {
                val savedUserId = authPrefs.getLong("active_user_id", -1L)
                val savedEmail = authPrefs.getString("active_email", null)

                var restoredUser: UserAccount? = null
                if (savedUserId != -1L) {
                    restoredUser = userDao.getUserById(savedUserId)
                }
                if (restoredUser == null && !savedEmail.isNullOrBlank()) {
                    restoredUser = userDao.getUserByEmailOrPhone(savedEmail)
                }
                if (restoredUser == null) {
                    restoredUser = userDao.getLastActiveUser()
                }

                if (restoredUser != null) {
                    // User already logged in on this device: keep login data unchanged!
                    _currentUser.value = restoredUser
                    _isAuthModalVisible.value = false
                    authPrefs.edit()
                        .putLong("active_user_id", restoredUser.id)
                        .putString("active_email", restoredUser.emailOrPhone)
                        .putBoolean("is_logged_in", true)
                        .apply()
                    userDao.updateUser(restoredUser.copy(lastLoginAt = System.currentTimeMillis(), isAutoLoginEnabled = true))
                    syncUserRecordsFromSupabase(restoredUser)
                } else {
                    // No account created yet; do not show intrusive popup on launch.
                    // User can explore the app immediately and login anytime.
                    _isAuthModalVisible.value = false
                }
            } catch (_: Exception) {
                // Handled gracefully
            }
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setName(name: String) {
        _nameInput.value = name
        _inputError.value = null
    }

    fun setPhone(phone: String) {
        _phoneInput.value = phone
        _inputError.value = null
    }

    fun setCountry(country: CountryInfo) {
        _selectedCountry.value = country
    }

    fun setHistoryQuery(query: String) {
        _historyQuery.value = query
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun setShowUpdateDialog(show: Boolean) {
        _showUpdateDialog.value = show
    }

    fun setShowSupabaseDialog(show: Boolean) {
        _showSupabaseDialog.value = show
    }

    fun setShowResultSheet(show: Boolean) {
        _showResultSheet.value = show
    }

    fun startTracking() {
        val name = _nameInput.value.trim()
        val phone = _phoneInput.value.trim()

        if (name.isBlank() || phone.isBlank() || phone.length < 3) {
            _inputError.value = "validation_empty"
            return
        }

        _inputError.value = null
        trackingJob?.cancel()

        val userId = _currentUser.value?.supabaseUid?.ifBlank { _currentUser.value?.emailOrPhone } ?: ""
        val accessToken = _currentUser.value?.supabaseAccessToken ?: ""

        trackingJob = viewModelScope.launch {
            _trackingUiState.value = TrackingUiState.Processing(1, 0.15f, "stage_1")

            repository.simulateTracking(
                name = name,
                rawNumber = phone,
                countryCode = _selectedCountry.value.code,
                countryName = if (_language.value == AppLanguage.ARABIC) _selectedCountry.value.nameAr else _selectedCountry.value.nameEn,
                userGpsLocation = locationManager.currentLocation.value,
                userId = userId,
                accessToken = accessToken,
                onProgress = { progress ->
                    when (progress) {
                        is TrackingProgress.Processing -> {
                            _trackingUiState.value = TrackingUiState.Processing(
                                progress.stageIndex,
                                progress.progress,
                                progress.statusKey
                            )
                        }
                        is TrackingProgress.Success -> {
                            _currentRecord.value = progress.record
                            _currentRoutePoints.value = progress.routePoints
                            _trackingUiState.value = TrackingUiState.Success(progress.record, progress.routePoints)
                            _showResultSheet.value = true
                        }
                        is TrackingProgress.Error -> {
                            _trackingUiState.value = TrackingUiState.Error(progress.message)
                        }
                    }
                }
            )
        }
    }

    fun cancelTracking() {
        trackingJob?.cancel()
        _trackingUiState.value = TrackingUiState.Idle
    }

    fun viewRecordOnMap(record: LocationRecord) {
        _currentRecord.value = record
        _currentRoutePoints.value = TrackerRepository.parseRouteJson(record.routeJson)
        _selectedTab.value = 1
        _showResultSheet.value = true
    }

    fun deleteRecord(record: LocationRecord) {
        viewModelScope.launch {
            val userId = _currentUser.value?.supabaseUid?.ifBlank { _currentUser.value?.emailOrPhone } ?: ""
            val accessToken = _currentUser.value?.supabaseAccessToken ?: ""
            repository.deleteRecord(record, accessToken, userId)
            if (_currentRecord.value?.id == record.id) {
                _currentRecord.value = null
                _currentRoutePoints.value = emptyList()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            val userId = _currentUser.value?.supabaseUid?.ifBlank { _currentUser.value?.emailOrPhone } ?: ""
            val accessToken = _currentUser.value?.supabaseAccessToken ?: ""
            repository.clearForUser(userId, accessToken)
            _currentRecord.value = null
            _currentRoutePoints.value = emptyList()
        }
    }

    fun requestLocationPermissionGranted() {
        locationManager.checkPermission()
        locationManager.requestCurrentLocation()
    }

    fun showAuthModal(show: Boolean) {
        _isAuthModalVisible.value = show
        _authError.value = null
    }

    fun loginInstantly(account: UserAccount) {
        viewModelScope.launch {
            try {
                // If Supabase is configured, also attempt remote token renewal
                var updated = account.copy(lastLoginAt = System.currentTimeMillis(), isAutoLoginEnabled = true)
                if (supabaseConfig.isConfigured() && account.emailOrPhone.contains("@")) {
                    val authResult = supabaseService.signIn(account.emailOrPhone, account.pinCode)
                    if (authResult is SupabaseAuthResult.Success) {
                        updated = updated.copy(
                            supabaseUid = authResult.session.user.id,
                            supabaseAccessToken = authResult.session.accessToken
                        )
                    }
                }

                userDao.updateUser(updated)
                _currentUser.value = updated
                _isAuthModalVisible.value = false
                _authError.value = null

                // Persist session to SharedPreferences so it survives app restarts
                authPrefs.edit()
                    .putLong("active_user_id", updated.id)
                    .putString("active_email", updated.emailOrPhone)
                    .putBoolean("is_logged_in", true)
                    .apply()

                // Sync records from Supabase
                syncUserRecordsFromSupabase(updated)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            }
        }
    }

    fun createAccount(fullName: String, emailOrPhone: String, pin: String, autoLogin: Boolean) {
        if (fullName.isBlank() || emailOrPhone.isBlank()) {
            _authError.value = "Please enter both full name and email or phone"
            return
        }

        viewModelScope.launch {
            try {
                var supabaseUid = ""
                var supabaseAccessToken = ""

                // Connect to Supabase Cloud Authentication if configured and email format
                if (supabaseConfig.isConfigured() && emailOrPhone.contains("@")) {
                    val supaResult = supabaseService.signUp(emailOrPhone.trim(), pin, fullName.trim())
                    when (supaResult) {
                        is SupabaseAuthResult.Success -> {
                            supabaseUid = supaResult.session.user.id
                            supabaseAccessToken = supaResult.session.accessToken
                        }
                        is SupabaseAuthResult.Error -> {
                            // Try signing in if user already exists on Supabase backend
                            val signinResult = supabaseService.signIn(emailOrPhone.trim(), pin)
                            if (signinResult is SupabaseAuthResult.Success) {
                                supabaseUid = signinResult.session.user.id
                                supabaseAccessToken = signinResult.session.accessToken
                            }
                        }
                    }
                }

                val colors = listOf("#00C853", "#00B4D8", "#7C4DFF", "#FF6D00", "#00BFA5")
                val newAccount = UserAccount(
                    fullName = fullName.trim(),
                    emailOrPhone = emailOrPhone.trim(),
                    pinCode = if (pin.isBlank()) "1234" else pin.trim(),
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis(),
                    isAutoLoginEnabled = true,
                    avatarColorHex = colors.random(),
                    supabaseUid = supabaseUid,
                    supabaseAccessToken = supabaseAccessToken
                )

                val id = userDao.insertUser(newAccount)
                val saved = newAccount.copy(id = id)
                _currentUser.value = saved
                _isAuthModalVisible.value = false
                _authError.value = null

                // Persist session to SharedPreferences so it survives app restarts
                authPrefs.edit()
                    .putLong("active_user_id", id)
                    .putString("active_email", saved.emailOrPhone)
                    .putBoolean("is_logged_in", true)
                    .apply()

                syncUserRecordsFromSupabase(saved)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            }
        }
    }

    private fun syncUserRecordsFromSupabase(account: UserAccount) {
        val uid = account.supabaseUid
        val token = account.supabaseAccessToken
        if (uid.isNotBlank() && token.isNotBlank()) {
            viewModelScope.launch {
                _isSyncingCloud.value = true
                repository.syncUserRecordsFromCloud(token, uid)
                _isSyncingCloud.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authPrefs.edit().clear().apply()
                _currentUser.value?.let {
                    userDao.updateUser(it.copy(isAutoLoginEnabled = false))
                }
                _currentUser.value = null
                _isAuthModalVisible.value = true
            } catch (_: Exception) {}
        }
    }

    fun testAndSaveSupabaseConfig(
        url: String,
        anonKey: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            supabaseConfig.customUrl = url.trim()
            supabaseConfig.customAnonKey = anonKey.trim()

            val testRes = supabaseService.testConnection()
            if (testRes.isSuccess) {
                onResult(true, testRes.getOrNull() ?: "Connected to Supabase")
            } else {
                onResult(false, testRes.exceptionOrNull()?.message ?: "Failed to connect to Supabase")
            }
        }
    }
}
