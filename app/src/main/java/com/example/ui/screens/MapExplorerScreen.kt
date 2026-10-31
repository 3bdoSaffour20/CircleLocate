package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GeoPoint
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
