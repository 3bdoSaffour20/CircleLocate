package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeoPoint
import com.example.location.DeviceLocationManager
import com.example.ui.theme.CircleGreenAccent
import com.example.ui.theme.CircleGreenPrimary
import com.example.ui.theme.CircleGreenPrimaryDark
import kotlin.math.cos
import kotlin.math.sin

enum class MapStyle {
    STREET,
    RADAR,
    SATELLITE,
    TERRAIN
}

@Composable
fun InteractiveMapView(
    targetLocation: GeoPoint?,
    targetName: String?,
    targetPhone: String?,
    routePoints: List<GeoPoint> = emptyList(),
    userLocation: GeoPoint? = null,
    modifier: Modifier = Modifier,
    initialStyle: MapStyle = MapStyle.STREET,
    showControls: Boolean = true,
    onSwitchToGoogleMaps: (() -> Unit)? = null
) {
    var mapStyle by remember { mutableStateOf(initialStyle) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing animation for radar target ring
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseProgress"
    )

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_map_view")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.6f, 3.5f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f + panOffsetX
        val centerY = height / 2f + panOffsetY

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMapBackground(mapStyle)

            // Draw map grid and stylistic features
            drawMapGridAndFeatures(
                width = width,
                height = height,
                centerX = centerX,
                centerY = centerY,
                zoom = zoomScale,
                style = mapStyle,
                radarAngle = radarAngle
            )

            // Coordinate conversion base
            val baseLat = targetLocation?.lat ?: userLocation?.lat ?: 37.7749
            val baseLng = targetLocation?.lng ?: userLocation?.lng ?: -122.4194

            fun toScreenOffset(lat: Double, lng: Double): Offset {
                // Approximate local Cartesian projection around base
                val deltaLng = (lng - baseLng) * 8000.0 * zoomScale
                val deltaLat = (baseLat - lat) * 9500.0 * zoomScale
                return Offset(
                    x = centerX + deltaLng.toFloat(),
                    y = centerY + deltaLat.toFloat()
                )
            }

            // Draw Route Trail if available
            if (routePoints.isNotEmpty()) {
                drawRoutePolyline(
                    points = routePoints,
                    toScreen = ::toScreenOffset,
                    mapStyle = mapStyle
                )
            }

            // Draw Line between User and Target if both are present
            if (userLocation != null && targetLocation != null) {
                val userPt = toScreenOffset(userLocation.lat, userLocation.lng)
                val targetPt = toScreenOffset(targetLocation.lat, targetLocation.lng)

                drawLine(
                    color = CircleGreenAccent.copy(alpha = 0.6f),
                    start = userPt,
                    end = targetPt,
                    strokeWidth = 3f * zoomScale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            }

            // Draw User Location Marker (Blue GPS beacon)
            if (userLocation != null) {
                val userPt = toScreenOffset(userLocation.lat, userLocation.lng)
                drawCircle(
                    color = Color(0x332979FF),
                    radius = 28f * zoomScale,
                    center = userPt
                )
                drawCircle(
                    color = Color(0xFF2979FF),
                    radius = 9f * zoomScale,
                    center = userPt
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * zoomScale,
                    center = userPt
                )
            }

            // Draw Tracked Target Marker
            val targetPt = if (targetLocation != null) {
                toScreenOffset(targetLocation.lat, targetLocation.lng)
            } else {
                Offset(centerX, centerY)
            }

            // Pulsing Concentric Rings
            val maxPulseRadius = 80f * zoomScale
            drawCircle(
                color = CircleGreenAccent.copy(alpha = (1f - pulseProgress) * 0.45f),
                radius = maxPulseRadius * pulseProgress,
                center = targetPt,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = CircleGreenPrimary.copy(alpha = (1f - (pulseProgress + 0.3f) % 1f) * 0.35f),
                radius = maxPulseRadius * ((pulseProgress + 0.3f) % 1f),
                center = targetPt,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = CircleGreenAccent.copy(alpha = 0.2f),
                radius = 24f * zoomScale,
                center = targetPt
            )

            // Pin teardrop icon
            drawTargetPin(
                center = targetPt,
                zoom = zoomScale,
                primaryColor = CircleGreenPrimary,
                accentColor = CircleGreenAccent
            )
        }

        // Target Callout Overlay Label
        if (targetName != null || targetPhone != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp)
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Distance Indicator if user and target exist
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
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

        // Floating Map Controls (Zoom in/out, Recenter, Style toggle)
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onSwitchToGoogleMaps != null) {
                    SmallFloatingActionButton(
                        onClick = onSwitchToGoogleMaps,
                        containerColor = CircleGreenPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("switch_to_google_maps_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Switch to Google Maps")
                    }
                }

                SmallFloatingActionButton(
                    onClick = {
                        mapStyle = when (mapStyle) {
                            MapStyle.STREET -> MapStyle.RADAR
                            MapStyle.RADAR -> MapStyle.SATELLITE
                            MapStyle.SATELLITE -> MapStyle.TERRAIN
                            MapStyle.TERRAIN -> MapStyle.STREET
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CircleGreenPrimary,
                    modifier = Modifier.testTag("map_style_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Style")
                }

                SmallFloatingActionButton(
                    onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(3.5f) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("map_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                SmallFloatingActionButton(
                    onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("map_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                SmallFloatingActionButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomScale = 1.0f
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = CircleGreenPrimary,
                    modifier = Modifier.testTag("map_recenter")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                }
            }
        }
    }
}

private fun DrawScope.drawMapBackground(style: MapStyle) {
    val bgColor = when (style) {
        MapStyle.STREET -> Color(0xFFF3F5F2)
        MapStyle.RADAR -> Color(0xFF0A110D)
        MapStyle.SATELLITE -> Color(0xFF14212D)
        MapStyle.TERRAIN -> Color(0xFFE8E5DD)
    }
    drawRect(color = bgColor)
}

private fun DrawScope.drawMapGridAndFeatures(
    width: Float,
    height: Float,
    centerX: Float,
    centerY: Float,
    zoom: Float,
    style: MapStyle,
    radarAngle: Float
) {
    val gridColor = when (style) {
        MapStyle.STREET -> Color(0xFFE0E5E1)
        MapStyle.RADAR -> Color(0x3300E676)
        MapStyle.SATELLITE -> Color(0x22FFFFFF)
        MapStyle.TERRAIN -> Color(0xFFD6D0C2)
    }

    val roadColor = when (style) {
        MapStyle.STREET -> Color.White
        MapStyle.RADAR -> Color(0x1F00E676)
        MapStyle.SATELLITE -> Color(0x44D4AF37)
        MapStyle.TERRAIN -> Color(0x88FFFFFF)
    }

    val waterColor = when (style) {
        MapStyle.STREET -> Color(0xFFBCE0FD)
        MapStyle.RADAR -> Color(0xFF041E15)
        MapStyle.SATELLITE -> Color(0xFF0B1924)
        MapStyle.TERRAIN -> Color(0xFFA5C9EB)
    }

    // Stylized Water River
    val riverPath = Path().apply {
        moveTo(0f, centerY + 180f * zoom)
        cubicTo(
            centerX - 100f * zoom, centerY + 120f * zoom,
            centerX + 80f * zoom, centerY + 240f * zoom,
            width, centerY + 160f * zoom
        )
        lineTo(width, centerY + 240f * zoom)
        cubicTo(
            centerX + 80f * zoom, centerY + 320f * zoom,
            centerX - 100f * zoom, centerY + 200f * zoom,
            0f, centerY + 260f * zoom
        )
        close()
    }
    drawPath(riverPath, waterColor)

    // Major Arterials & Roads
    val roadWidth = 8f * zoom
    drawLine(
        color = roadColor,
        start = Offset(0f, centerY - 60f * zoom),
        end = Offset(width, centerY - 60f * zoom),
        strokeWidth = roadWidth
    )
    drawLine(
        color = roadColor,
        start = Offset(centerX - 80f * zoom, 0f),
        end = Offset(centerX - 80f * zoom, height),
        strokeWidth = roadWidth
    )
    drawLine(
        color = roadColor,
        start = Offset(centerX + 120f * zoom, 0f),
        end = Offset(centerX + 120f * zoom, height),
        strokeWidth = roadWidth * 0.7f
    )
    drawLine(
        color = roadColor,
        start = Offset(0f, centerY + 80f * zoom),
        end = Offset(width, centerY + 80f * zoom),
        strokeWidth = roadWidth * 0.7f
    )

    // Grid Coordinates / Latitude-Longitude lines
    val step = 70f * zoom
    var curX = (centerX % step)
    while (curX < width) {
        drawLine(
            color = gridColor,
            start = Offset(curX, 0f),
            end = Offset(curX, height),
            strokeWidth = 1f
        )
        curX += step
    }

    var curY = (centerY % step)
    while (curY < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, curY),
            end = Offset(width, curY),
            strokeWidth = 1f
        )
        curY += step
    }

    // Radar HUD specifics in RADAR style
    if (style == MapStyle.RADAR) {
        val radX = centerX
        val radY = centerY
        val r = 260f * zoom

        drawCircle(
            color = Color(0x3300E676),
            radius = r,
            center = Offset(radX, radY),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
        drawCircle(
            color = Color(0x2200E676),
            radius = r * 0.5f,
            center = Offset(radX, radY),
            style = Stroke(width = 1.5f)
        )

        // Sweeping beam
        val angleRad = Math.toRadians(radarAngle.toDouble())
        val beamEnd = Offset(
            (radX + r * cos(angleRad)).toFloat(),
            (radY + r * sin(angleRad)).toFloat()
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xBB00E676), Color(0x0000E676)),
                start = Offset(radX, radY),
                end = beamEnd
            ),
            start = Offset(radX, radY),
            end = beamEnd,
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawRoutePolyline(
    points: List<GeoPoint>,
    toScreen: (Double, Double) -> Offset,
    mapStyle: MapStyle
) {
    if (points.size < 2) return

    val screenPoints = points.map { toScreen(it.lat, it.lng) }

    val path = Path().apply {
        moveTo(screenPoints.first().x, screenPoints.first().y)
        for (i in 1 until screenPoints.size) {
            lineTo(screenPoints[i].x, screenPoints[i].y)
        }
    }

    // Glow line
    drawPath(
        path = path,
        color = CircleGreenAccent.copy(alpha = 0.35f),
        style = Stroke(width = 10f)
    )

    // Sharp core line
    drawPath(
        path = path,
        color = CircleGreenPrimary,
        style = Stroke(width = 4f)
    )

    // Intermediate waypoint dots
    for (i in 0 until screenPoints.size - 1) {
        val pt = screenPoints[i]
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = pt
        )
        drawCircle(
            color = CircleGreenPrimaryDark,
            radius = 3f,
            center = pt
        )
    }
}

private fun DrawScope.drawTargetPin(
    center: Offset,
    zoom: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val pinHeight = 36f * zoom
    val pinRadius = 14f * zoom

    val pinPath = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            center.x - pinRadius * 1.1f, center.y - pinHeight * 0.6f,
            center.x - pinRadius, center.y - pinHeight,
            center.x, center.y - pinHeight
        )
        cubicTo(
            center.x + pinRadius, center.y - pinHeight,
            center.x + pinRadius * 1.1f, center.y - pinHeight * 0.6f,
            center.x, center.y
        )
        close()
    }

    // Shadow
    drawOval(
        color = Color(0x44000000),
        topLeft = Offset(center.x - 10f * zoom, center.y - 4f * zoom),
        size = androidx.compose.ui.geometry.Size(20f * zoom, 8f * zoom)
    )

    // Pin Body
    drawPath(pinPath, color = primaryColor)

    // Inner White Circle with green dot
    val innerCenter = Offset(center.x, center.y - pinHeight + pinRadius)
    drawCircle(color = Color.White, radius = 6f * zoom, center = innerCenter)
    drawCircle(color = accentColor, radius = 3f * zoom, center = innerCenter)
}
