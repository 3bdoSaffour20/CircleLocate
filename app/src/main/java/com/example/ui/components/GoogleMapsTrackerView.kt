package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.data.model.GeoPoint
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.location.DeviceLocationManager
import com.example.ui.theme.CircleGreenAccent
import com.example.ui.theme.CircleGreenPrimary

enum class GoogleMapType(val id: String, val labelEn: String, val labelAr: String) {
    ROADMAP("roadmap", "Roadmap", "طرق"),
    SATELLITE("satellite", "Satellite", "أقمار"),
    HYBRID("hybrid", "Hybrid", "هجين"),
    TERRAIN("terrain", "Terrain", "تضاريس")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapsTrackerView(
    targetLocation: GeoPoint?,
    targetName: String?,
    targetPhone: String?,
    routePoints: List<GeoPoint> = emptyList(),
    userLocation: GeoPoint? = null,
    language: AppLanguage = AppLanguage.ENGLISH,
    modifier: Modifier = Modifier,
    onSwitchToRadar: (() -> Unit)? = null,
    showControls: Boolean = true
) {
    var selectedMapType by remember { mutableStateOf(GoogleMapType.ROADMAP) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showTypePicker by remember { mutableStateOf(false) }

    // Resolve Google Maps API Key: use BuildConfig key or fallback to provided project key
    val apiKey = remember {
        val configuredKey = try {
            BuildConfig.MAPS_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (configuredKey.isNotBlank() && !configuredKey.startsWith("YOUR_")) {
            configuredKey
        } else {
            "AIzaSyDx5DwpWE6aXr3m_agwMKXREYiROuW5SDU"
        }
    }

    val defaultLat = targetLocation?.lat ?: userLocation?.lat ?: 32.8872 // Tripoli, Libya default
    val defaultLng = targetLocation?.lng ?: userLocation?.lng ?: 13.1913

    // Update map state when targetLocation or mapType changes
    LaunchedEffect(targetLocation, userLocation, selectedMapType, routePoints) {
        val wv = webViewRef ?: return@LaunchedEffect
        val targetLat = targetLocation?.lat ?: defaultLat
        val targetLng = targetLocation?.lng ?: defaultLng
        val userLatStr = userLocation?.let { "${it.lat}, ${it.lng}" } ?: "null"
        val nameEscaped = (targetName ?: "Target Device").replace("'", "\\'")
        val phoneEscaped = (targetPhone ?: "").replace("'", "\\'")

        val routePointsJs = routePoints.joinToString(prefix = "[", postfix = "]") { pt ->
            "{lat: ${pt.lat}, lng: ${pt.lng}, label: '${pt.label}'}"
        }

        val jsCall = """
            if (window.updateMapData) {
                window.updateMapData(
                    $targetLat,
                    $targetLng,
                    '$nameEscaped',
                    '$phoneEscaped',
                    '$userLatStr',
                    '${selectedMapType.id}',
                    $routePointsJs
                );
            }
        """.trimIndent()
        wv.evaluateJavascript(jsCall, null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_maps_view")
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewRef = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isMapLoaded = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isMapLoaded = true
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            // Handled gracefully
                        }
                    }

                    val htmlContent = generateGoogleMapsHtml(
                        apiKey = apiKey,
                        initialLat = defaultLat,
                        initialLng = defaultLng,
                        targetName = targetName ?: "Target Device",
                        targetPhone = targetPhone ?: "",
                        initialMapType = selectedMapType.id,
                        userLocation = userLocation,
                        routePoints = routePoints
                    )
                    loadDataWithBaseURL("https://maps.googleapis.com", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Indicator
        AnimatedVisibility(
            visible = !isMapLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CircleGreenPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "جاري تحميل خرائط جوجل..." else "Loading Google Maps...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Top Target Device Info Bar
        if (targetName != null || targetPhone != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CircleGreenAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = targetName ?: "Target Device",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (targetPhone != null) {
                            Text(
                                text = targetPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = CircleGreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Google Maps Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CircleGreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Google Maps",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CircleGreenPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Distance Indicator if user location is available
        if (userLocation != null && targetLocation != null) {
            val distKm = DeviceLocationManager.calculateDistanceKm(
                userLocation.lat, userLocation.lng,
                targetLocation.lat, targetLocation.lng
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Distance",
                        tint = CircleGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Distance: ${DeviceLocationManager.formatDistance(distKm)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Floating Control Buttons on Right Side
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Switch between Google Maps and Tactical Radar
                if (onSwitchToRadar != null) {
                    SmallFloatingActionButton(
                        onClick = onSwitchToRadar,
                        containerColor = CircleGreenPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("switch_to_radar_button")
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = "Switch to Radar")
                    }
                }

                // Map Layer Picker Button
                SmallFloatingActionButton(
                    onClick = { showTypePicker = !showTypePicker },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CircleGreenPrimary,
                    modifier = Modifier.testTag("google_map_type_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Layer")
                }

                // Layer selection panel
                AnimatedVisibility(visible = showTypePicker) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            GoogleMapType.entries.forEach { type ->
                                val isSelected = type == selectedMapType
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedMapType = type
                                            showTypePicker = false
                                            webViewRef?.evaluateJavascript(
                                                "if (window.setMapType) window.setMapType('${type.id}');",
                                                null
                                            )
                                        }
                                        .background(
                                            if (isSelected) CircleGreenPrimary.copy(alpha = 0.15f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) type.labelAr else type.labelEn,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) CircleGreenPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Zoom In
                SmallFloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if (window.zoomIn) window.zoomIn();", null)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("google_map_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if (window.zoomOut) window.zoomOut();", null)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("google_map_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                // Recenter
                SmallFloatingActionButton(
                    onClick = {
                        webViewRef?.evaluateJavascript("if (window.recenterMap) window.recenterMap();", null)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CircleGreenPrimary,
                    modifier = Modifier.testTag("google_map_recenter")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                }
            }
        }
    }
}

private fun generateGoogleMapsHtml(
    apiKey: String,
    initialLat: Double,
    initialLng: Double,
    targetName: String,
    targetPhone: String,
    initialMapType: String,
    userLocation: GeoPoint?,
    routePoints: List<GeoPoint>
): String {
    val userLatJs = userLocation?.let { "${it.lat}, ${it.lng}" } ?: "null"
    val routePointsJs = routePoints.joinToString(prefix = "[", postfix = "]") { pt ->
        "{lat: ${pt.lat}, lng: ${pt.lng}, label: '${pt.label}'}"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="initial-scale=1.0, user-scalable=no, width=device-width" />
            <meta charset="utf-8" />
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background-color: #f3f5f2;
                    overflow: hidden;
                }
                .pulse-ring {
                    width: 44px;
                    height: 44px;
                    border: 3px solid #00E676;
                    border-radius: 50%;
                    position: absolute;
                    animation: pulse 2s infinite ease-out;
                    pointer-events: none;
                }
                @keyframes pulse {
                    0% { transform: scale(0.4); opacity: 1; }
                    100% { transform: scale(1.6); opacity: 0; }
                }
            </style>
            <script src="https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initMap" async defer></script>
            <script>
                var map;
                var targetMarker;
                var userMarker;
                var routePolyline;
                var waypointMarkers = [];
                var pulseOverlay;
                var infoWindow;

                var curTargetLat = $initialLat;
                var curTargetLng = $initialLng;

                function initMap() {
                    var centerPos = { lat: curTargetLat, lng: curTargetLng };
                    map = new google.maps.Map(document.getElementById('map'), {
                        zoom: 14,
                        center: centerPos,
                        mapTypeId: '$initialMapType',
                        disableDefaultUI: true,
                        zoomControl: false,
                        mapTypeControl: false,
                        scaleControl: true,
                        streetViewControl: false,
                        rotateControl: false,
                        fullscreenControl: false,
                        gestureHandling: 'greedy',
                        styles: [
                            { featureType: 'poi', stylers: [{ visibility: 'simplified' }] }
                        ]
                    });

                    infoWindow = new google.maps.InfoWindow();

                    // Create Target Marker with Custom Green Pin
                    createTargetMarker(centerPos, '$targetName', '$targetPhone');

                    // If route points exist, draw route
                    var initialRoute = $routePointsJs;
                    drawRoute(initialRoute);

                    // If user location exists, place user pin
                    var userPos = $userLatJs;
                    if (userPos) {
                        createUserMarker(userPos);
                    }
                }

                function createTargetMarker(pos, name, phone) {
                    if (targetMarker) {
                        targetMarker.setPosition(pos);
                        return;
                    }

                    // Green teardrop marker with CircleLocate branding
                    var pinSvg = "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(`
                        <svg xmlns="http://www.w3.org/2000/svg" width="44" height="54" viewBox="0 0 44 54">
                            <defs>
                                <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
                                    <feDropShadow dx="0" dy="3" stdDeviation="3" flood-color="#000" flood-opacity="0.3"/>
                                </filter>
                            </defs>
                            <path filter="url(#shadow)" d="M22 2C11 2 2 11 2 22c0 14 20 30 20 30s20-16 20-30c0-11-9-20-20-20z" fill="#00C853"/>
                            <circle cx="22" cy="20" r="11" fill="#FFFFFF"/>
                            <circle cx="22" cy="20" r="6" fill="#00E676"/>
                        </svg>
                    `);

                    targetMarker = new google.maps.Marker({
                        position: pos,
                        map: map,
                        title: name,
                        icon: {
                            url: pinSvg,
                            scaledSize: new google.maps.Size(44, 54),
                            anchor: new google.maps.Point(22, 52)
                        },
                        animation: google.maps.Animation.DROP
                    });

                    targetMarker.addListener('click', function() {
                        var content = '<div style="padding:6px; font-family:sans-serif;">' +
                            '<h4 style="margin:0 0 4px 0; color:#00C853;">' + name + '</h4>' +
                            '<p style="margin:0; font-size:12px; color:#555;">' + phone + '</p>' +
                            '<p style="margin:4px 0 0 0; font-size:11px; color:#888;">Lat: ' + pos.lat.toFixed(4) + ', Lng: ' + pos.lng.toFixed(4) + '</p>' +
                            '</div>';
                        infoWindow.setContent(content);
                        infoWindow.open(map, targetMarker);
                    });
                }

                function createUserMarker(pos) {
                    var userSvg = "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(`
                        <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 30 30">
                            <circle cx="15" cy="15" r="13" fill="#2979FF" fill-opacity="0.25"/>
                            <circle cx="15" cy="15" r="7" fill="#2979FF" stroke="#FFFFFF" stroke-width="2"/>
                        </svg>
                    `);

                    if (userMarker) {
                        userMarker.setPosition(pos);
                    } else {
                        userMarker = new google.maps.Marker({
                            position: pos,
                            map: map,
                            title: 'Your Location',
                            icon: {
                                url: userSvg,
                                scaledSize: new google.maps.Size(30, 30),
                                anchor: new google.maps.Point(15, 15)
                            }
                        });
                    }
                }

                function drawRoute(points) {
                    if (routePolyline) {
                        routePolyline.setMap(null);
                    }
                    waypointMarkers.forEach(function(m) { m.setMap(null); });
                    waypointMarkers = [];

                    if (!points || points.length < 2) return;

                    var path = points.map(function(pt) {
                        return { lat: pt.lat, lng: pt.lng };
                    });

                    routePolyline = new google.maps.Polyline({
                        path: path,
                        geodesic: true,
                        strokeColor: '#00C853',
                        strokeOpacity: 0.9,
                        strokeWeight: 5,
                        map: map
                    });

                    // Add small waypoint dots
                    for (var i = 0; i < points.length - 1; i++) {
                        var pt = points[i];
                        var dotSvg = "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(`
                            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14">
                                <circle cx="7" cy="7" r="5" fill="#FFFFFF" stroke="#00C853" stroke-width="2"/>
                            </svg>
                        `);
                        var wm = new google.maps.Marker({
                            position: { lat: pt.lat, lng: pt.lng },
                            map: map,
                            title: pt.label,
                            icon: {
                                url: dotSvg,
                                scaledSize: new google.maps.Size(14, 14),
                                anchor: new google.maps.Point(7, 7)
                            }
                        });
                        waypointMarkers.push(wm);
                    }
                }

                window.updateMapData = function(lat, lng, name, phone, userPos, mapType, points) {
                    curTargetLat = lat;
                    curTargetLng = lng;
                    var pos = { lat: lat, lng: lng };
                    if (map) {
                        map.panTo(pos);
                        if (mapType) map.setMapTypeId(mapType);
                        createTargetMarker(pos, name, phone);
                        if (userPos && userPos.lat) {
                            createUserMarker(userPos);
                        }
                        if (points) {
                            drawRoute(points);
                        }
                    }
                };

                window.setMapType = function(type) {
                    if (map) map.setMapTypeId(type);
                };

                window.zoomIn = function() {
                    if (map) map.setZoom(map.getZoom() + 1);
                };

                window.zoomOut = function() {
                    if (map) map.setZoom(map.getZoom() - 1);
                };

                window.recenterMap = function() {
                    if (map) {
                        map.panTo({ lat: curTargetLat, lng: curTargetLng });
                        map.setZoom(15);
                    }
                };
            </script>
        </head>
        <body>
            <div id="map"></div>
        </body>
        </html>
    """.trimIndent()
}
