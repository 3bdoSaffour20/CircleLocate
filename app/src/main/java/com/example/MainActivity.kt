package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.AccountAuthDialog
import com.example.ui.components.RadarScannerOverlay
import com.example.ui.components.SupabaseSyncDialog
import com.example.ui.components.TrackingResultBottomSheet
import com.example.ui.components.UpdateCheckDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapExplorerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CircleGreenAccent
import com.example.ui.theme.CircleGreenPrimary
import com.example.ui.theme.CircleGreenPrimaryDark
import com.example.ui.theme.CircleLocateTheme
import com.example.viewmodel.TrackerViewModel
import com.example.viewmodel.TrackingUiState

class MainActivity : ComponentActivity() {

    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val language by viewModel.language.collectAsState()

            // RTL / LTR layout provider depending on selected language (Arabic vs English)
            val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CircleLocateTheme(darkTheme = isDarkMode) {
                    CircleLocateApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CircleLocateApp(viewModel: TrackerViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val language by viewModel.language.collectAsState()
    val trackingState by viewModel.trackingUiState.collectAsState()
    val currentRecord by viewModel.currentRecord.collectAsState()
    val showResultSheet by viewModel.showResultSheet.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val showSupabaseDialog by viewModel.showSupabaseDialog.collectAsState()
    val isAuthModalVisible by viewModel.isAuthModalVisible.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val nameInput by viewModel.nameInput.collectAsState()
    val phoneInput by viewModel.phoneInput.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.requestLocationPermissionGranted()
        }
    }

    // Auto-request location permissions on first launch
    LaunchedEffect(Unit) {
        if (!viewModel.locationManager.hasPermission.value) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Back handling: If on secondary tab, return to Home
    BackHandler(enabled = selectedTab != 0) {
        viewModel.setTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            VibrantGreenBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.setTab(it) },
                language = language
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(viewModel = viewModel)
                    1 -> MapExplorerScreen(viewModel = viewModel)
                    2 -> HistoryScreen(viewModel = viewModel)
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // Radar Scanning HUD Dialog
    if (trackingState is TrackingUiState.Processing) {
        val proc = trackingState as TrackingUiState.Processing
        RadarScannerOverlay(
            targetName = nameInput.ifBlank { "Target" },
            targetPhone = "${selectedCountry.code} $phoneInput",
            stageIndex = proc.stageIndex,
            progress = proc.progress,
            statusKey = proc.statusKey,
            language = language,
            onCancel = { viewModel.cancelTracking() }
        )
    }

    // Tracking Result Bottom Sheet
    if (showResultSheet && currentRecord != null) {
        TrackingResultBottomSheet(
            record = currentRecord!!,
            language = language,
            onDismiss = { viewModel.setShowResultSheet(false) },
            onTrackAgain = {
                viewModel.setShowResultSheet(false)
                viewModel.startTracking()
            }
        )
    }

    // Update Check Modal Dialog
    if (showUpdateDialog) {
        UpdateCheckDialog(
            language = language,
            onDismiss = { viewModel.setShowUpdateDialog(false) }
        )
    }

    // Supabase Cloud Sync Setup Guide Modal Dialog
    if (showSupabaseDialog) {
        SupabaseSyncDialog(
            viewModel = viewModel,
            language = language,
            onDismiss = { viewModel.setShowSupabaseDialog(false) }
        )
    }

    // Account Authentication Modal (Create Account if not exist, or Instant Login if exists)
    if (isAuthModalVisible) {
        AccountAuthDialog(
            existingAccounts = savedAccounts,
            currentAccount = currentUser,
            language = language,
            errorMessage = authError,
            onInstantLogin = { viewModel.loginInstantly(it) },
            onCreateAccount = { name, email, pin, auto ->
                viewModel.createAccount(name, email, pin, auto)
            },
            onDismiss = { viewModel.showAuthModal(false) }
        )
    }
}

@Composable
fun VibrantGreenBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    language: AppLanguage
) {
    // Requirements: "Clean, minimalist UI featuring a vibrant green navigation bar"
    // Proper navigation bar insets handling
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .testTag("vibrant_green_nav_bar"),
        color = CircleGreenPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CircleGreenPrimary,
                            CircleGreenPrimaryDark
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    icon = Icons.Default.Search,
                    label = AppStrings.get("nav_search", language),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    testTag = "nav_item_search"
                )

                NavItem(
                    icon = Icons.Default.Map,
                    label = AppStrings.get("nav_map", language),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    testTag = "nav_item_map"
                )

                NavItem(
                    icon = Icons.Default.History,
                    label = AppStrings.get("nav_history", language),
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    testTag = "nav_item_history"
                )

                NavItem(
                    icon = Icons.Default.Settings,
                    label = AppStrings.get("nav_settings", language),
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    testTag = "nav_item_settings"
                )
            }
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) CircleGreenPrimaryDark else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
        )
    }
}
