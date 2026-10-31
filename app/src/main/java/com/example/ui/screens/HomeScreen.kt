package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GeoPoint
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.CountrySelectorButton
import com.example.ui.components.GoogleMapsTrackerView
import com.example.ui.components.InteractiveMapView
import com.example.ui.theme.CircleGreenAccent
import com.example.ui.theme.CircleGreenPrimary
import com.example.viewmodel.TrackerViewModel

@Composable
fun HomeScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val nameInput by viewModel.nameInput.collectAsState()
    val phoneInput by viewModel.phoneInput.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val inputError by viewModel.inputError.collectAsState()
    val currentRecord by viewModel.currentRecord.collectAsState()
    val currentRoutePoints by viewModel.currentRoutePoints.collectAsState()
    val userLocation by viewModel.locationManager.currentLocation.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var useGoogleMaps by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize()) {
        // Background interactive Google Maps or Tactical Radar map view
        if (useGoogleMaps) {
            GoogleMapsTrackerView(
                targetLocation = currentRecord?.let { GeoPoint(it.latitude, it.longitude) },
                targetName = currentRecord?.name,
                targetPhone = currentRecord?.phoneNumber,
                routePoints = currentRoutePoints,
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
                routePoints = currentRoutePoints,
                userLocation = userLocation,
                modifier = Modifier.fillMaxSize(),
                showControls = true,
                onSwitchToGoogleMaps = { useGoogleMaps = true }
            )
        }

        // Floating Search & Input Panel at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .testTag("search_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Top Action Bar inside Card: User Account Chip at Top-End
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { viewModel.showAuthModal(true) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentUser != null) CircleGreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .testTag("account_chip_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentUser != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CircleGreenAccent)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentUser!!.fullName.split(" ").firstOrNull() ?: currentUser!!.fullName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CircleGreenPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Login",
                                        tint = CircleGreenPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = AppStrings.get("instant_login", language),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CircleGreenPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Prominent Centered Hero Header: Large Logo + Centered Labels
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Centered Logo Image
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .shadow(6.dp, CircleShape),
                            shape = CircleShape,
                            color = Color(0xFFF1F6EF)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.circle_locate_symbol),
                                    contentDescription = "CircleLocate Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // App Title - Aligned to Center Below the Logo
                        Text(
                            text = AppStrings.get("app_name", language),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // App Subtitle / Tagline - Aligned to Center Below the Title
                        Text(
                            text = AppStrings.get("tagline", language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input 1: Enter Name
                    Text(
                        text = AppStrings.get("enter_name", language),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { viewModel.setName(it) },
                        placeholder = {
                            Text(
                                text = AppStrings.get("name_placeholder", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name",
                                tint = CircleGreenPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CircleGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input 2: Type a Number (with country code dropdown selector)
                    Text(
                        text = AppStrings.get("type_number", language),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountrySelectorButton(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { viewModel.setCountry(it) },
                            language = language,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { viewModel.setPhone(it) },
                            placeholder = {
                                Text(
                                    text = selectedCountry.exampleNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = CircleGreenPrimary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.startTracking()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("phone_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CircleGreenPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    // Input Error notice if validation failed
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = AppStrings.get(inputError!!, language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent bright green "GET LOCATION" button with arrow icon
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.startTracking()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .testTag("get_location_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CircleGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = AppStrings.get("get_location", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Arrow",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Terms of Use notice text at bottom
                    Text(
                        text = AppStrings.get("terms_notice", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
