package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage

data class CountryInfo(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val flag: String,
    val exampleNumber: String
)

val SupportedCountries = listOf(
    CountryInfo("+218", "Libya", "ليبيا", "🇱🇾", "91 123 4567"),
    CountryInfo("+1", "United States / Canada", "الولايات المتحدة / كندا", "🇺🇸", "415 555 2671"),
    CountryInfo("+44", "United Kingdom", "المملكة المتحدة", "🇬🇧", "7911 123456"),
    CountryInfo("+966", "Saudi Arabia", "المملكة العربية السعودية", "🇸🇦", "50 123 4567"),
    CountryInfo("+971", "United Arab Emirates", "الإمارات العربية المتحدة", "🇦🇪", "50 123 4567"),
    CountryInfo("+20", "Egypt", "مصر", "🇪🇬", "10 1234 5678"),
    CountryInfo("+213", "Algeria", "الجزائر", "🇩🇿", "551 23 45 67"),
    CountryInfo("+216", "Tunisia", "تونس", "🇹🇳", "20 123 456"),
    CountryInfo("+212", "Morocco", "المغرب", "🇲🇦", "6 12 34 56 78"),
    CountryInfo("+249", "Sudan", "السودان", "🇸🇩", "91 234 5678"),
    CountryInfo("+964", "Iraq", "العراق", "🇮🇶", "770 123 4567"),
    CountryInfo("+962", "Jordan", "الأردن", "🇯🇴", "7 9123 4567"),
    CountryInfo("+961", "Lebanon", "لبنان", "🇱🇧", "70 123 456"),
    CountryInfo("+970", "Palestine", "فلسطين", "🇵🇸", "59 123 4567"),
    CountryInfo("+963", "Syria", "سوريا", "🇸🇾", "944 123 456"),
    CountryInfo("+965", "Kuwait", "الكويت", "🇰🇼", "9123 4567"),
    CountryInfo("+974", "Qatar", "قطر", "🇶🇦", "5512 3456"),
    CountryInfo("+968", "Oman", "عُمان", "🇴🇲", "9123 4567"),
    CountryInfo("+973", "Bahrain", "البحرين", "🇧🇭", "3912 3456"),
    CountryInfo("+967", "Yemen", "اليمن", "🇾🇪", "771 234 567"),
    CountryInfo("+33", "France", "فرنسا", "🇫🇷", "6 12 34 56 78"),
    CountryInfo("+49", "Germany", "ألمانيا", "🇩🇪", "151 23456789"),
    CountryInfo("+34", "Spain", "إسبانيا", "🇪🇸", "612 345 678"),
    CountryInfo("+39", "Italy", "إيطاليا", "🇮🇹", "312 345 6789"),
    CountryInfo("+90", "Turkey", "تركيا", "🇹🇷", "532 123 4567"),
    CountryInfo("+91", "India", "الهند", "🇮🇳", "98765 43210"),
    CountryInfo("+61", "Australia", "أستراليا", "🇦🇺", "412 345 678"),
    CountryInfo("+81", "Japan", "اليابان", "🇯🇵", "90 1234 5678"),
    CountryInfo("+55", "Brazil", "البرازيل", "🇧🇷", "11 91234 5678"),
    CountryInfo("+52", "Mexico", "المكسيك", "🇲🇽", "55 1234 5678")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectorButton(
    selectedCountry: CountryInfo,
    onCountrySelected: (CountryInfo) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showSheet = true },
        modifier = modifier
            .testTag("country_selector_button")
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = selectedCountry.flag, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = selectedCountry.code,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Country",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CountryPickerContent(
                selected = selectedCountry,
                onSelect = {
                    onCountrySelected(it)
                    showSheet = false
                },
                onClose = { showSheet = false },
                language = language
            )
        }
    }
}

@Composable
fun CountryPickerContent(
    selected: CountryInfo,
    onSelect: (CountryInfo) -> Unit,
    onClose: () -> Unit,
    language: AppLanguage
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, language) {
        if (searchQuery.isBlank()) {
            SupportedCountries
        } else {
            val q = searchQuery.trim().lowercase()
            SupportedCountries.filter {
                it.code.contains(q) ||
                it.nameEn.lowercase().contains(q) ||
                it.nameAr.contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) "اختر الدولة ورمز الاتصال" else "Select Country Code",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(if (language == AppLanguage.ARABIC) "بحث بالاسم أو الرمز..." else "Search country or dial code...")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("country_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            items(filtered, key = { it.code + it.nameEn }) { country ->
                val isSelected = country.code == selected.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(country) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else androidx.compose.ui.graphics.Color.Transparent
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = country.flag, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) country.nameAr else country.nameEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = country.code,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
