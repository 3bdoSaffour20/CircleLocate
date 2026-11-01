package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GeoPoint
import com.example.localization.AppStrings
import com.example.ui.components.GoogleMapsTrackerView
import com.example.ui.components.InteractiveMapView
import com.example.ui.theme.CircleGreenPrimary
import com.example.viewmodel.TrackerViewModel

@Composable
fun MapExplorerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val currentRecord by viewModel.currentRecord.collectAsState()
    val routePoints by viewModel.currentRoutePoints.collectAsState()
    val userLocation by viewModel.locationManager.currentLocation.collectAsState()

    var useGoogleMaps by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        if (useGoogleMaps) {
            GoogleMapsTrackerView(
                targetLocation = currentRecord?.let { GeoPoint(it.latitude, it.longitude) },
                targetName = currentRecord?.name,
                targetPhone = currentRecord?.phoneNumber,
                routePoints = routePoints,
                userLocation = userLocation,
                language = language,
                modifier = Modifier.fillMaxSize(),
                onSwitchToRadar = { useGoogleMaps = false },
                showControls = true
            )
        } else {
            InteractiveMapView(
                targetLocation = currentRecord?.let { GeoPoint(it.latitude, it.longitude) },
                targetName = currentRecord?.name,
                targetPhone = currentRecord?.phoneNumber,
                routePoints = routePoints,
                userLocation = userLocation,
                modifier = Modifier.fillMaxSize(),
                showControls = true,
                onSwitchToGoogleMaps = { useGoogleMaps = true }
            )
        }

        // Centered Floating Brand Header on the Map View Section
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .shadow(8.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color(0xFFF1F6EF)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.circle_locate_symbol),
                            contentDescription = "CircleLocate Logo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.get("app_name", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = AppStrings.get("tagline", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // If there's an active record, show a button to inspect details sheet
        if (currentRecord != null) {
            FloatingActionButton(
                onClick = { viewModel.setShowResultSheet(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .testTag("inspect_target_fab"),
                containerColor = CircleGreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Info, contentDescription = "Device Details")
            }
        }
    }
}
