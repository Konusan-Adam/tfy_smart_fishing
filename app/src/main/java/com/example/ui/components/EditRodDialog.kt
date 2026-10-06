package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.RodProfile
import com.example.engine.CalculatedSensorCalibration
import com.example.engine.RodAction
import com.example.engine.RodCategory
import com.example.engine.RodMaterial
import com.example.engine.RodPhysicsEngine
import com.example.engine.RodPhysicsInput
import com.example.engine.WaterEnvironment
import com.example.model.FishingRod
import com.example.ui.theme.NeonGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditRodDialog(
    rod: FishingRod,
    availableProfiles: List<RodProfile> = emptyList(),
    onDismiss: () -> Unit,
    onSaveCustomProfile: (RodProfile) -> Unit = {},
    onSave: (newName: String, newBait: String, newProfile: RodProfile) -> Unit
) {
    var nameText by remember { mutableStateOf(rod.name) }
    var baitText by remember { mutableStateOf(rod.baitNote) }

    // Tüm hazır + kullanıcının kaydettiği profiller
    val allProfiles = remember(availableProfiles) {
        val list = if (availableProfiles.isNotEmpty()) availableProfiles else AppDatabase.DEFAULT_PRESETS
        list.distinctBy { it.name }
    }

    var selectedProfile by remember {
        mutableStateOf(
            allProfiles.find { it.name == rod.assignedRodProfileName } ?: allProfiles.firstOrNull() ?: RodProfile(
                name = "Standart Sazan Kamışı (3.60m - 3.00 lb)",
                lengthMeters = 3.60,
                testCurveLbs = 3.00,
                isPreset = true
            )
        )
    }

    // Tab Seçimi (0: Hazır Kütüphane, 1: Kendi Kamışını Ekle)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Kütüphane Arama ve Marka Filtresi
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrandFilter by remember { mutableStateOf("Tümü") }

    val brandFilters = listOf(
        "Tümü",
        "Shimano",
        "Daiwa",
        "Fox",
        "Okuma",
        "Prologic",
        "Sonik",
        "Albastar",
        "Lineaeffe",
        "Deniz Surf",
        "Göl / Teleskopik",
        "Özel Kamışlarım"
    )

    // Filtrelenmiş Kamış Listesi
    val filteredProfiles = remember(searchQuery, selectedBrandFilter, allProfiles) {
        allProfiles.filter { profile ->
            val matchesSearch = searchQuery.isBlank() || profile.name.contains(searchQuery, ignoreCase = true)
            val matchesBrand = when (selectedBrandFilter) {
                "Tümü" -> true
                "Özel Kamışlarım" -> !profile.isPreset
                "Deniz Surf" -> profile.categoryType == RodCategory.SURF.name || profile.name.contains("Surf", ignoreCase = true)
                "Göl / Teleskopik" -> profile.categoryType == RodCategory.POLE_LAKE.name || profile.name.contains("Pole", ignoreCase = true)
                else -> profile.name.contains(selectedBrandFilter, ignoreCase = true)
            }
            matchesSearch && matchesBrand
        }
    }

    // ==================== ➕ ÖZEL KAMIŞ OLUŞTURUCU STATE'LERİ ====================
    var customName by remember { mutableStateOf("") }
    var customLengthMeters by remember { mutableDoubleStateOf(3.60) }
    var customTestCurveLbs by remember { mutableDoubleStateOf(3.50) }
    var customAction by remember { mutableStateOf(RodAction.FAST_ACTION) }
    var customMaterial by remember { mutableStateOf(RodMaterial.HIGH_MODULUS_CARBON) }
    var customWater by remember { mutableStateOf(WaterEnvironment.CALM_LAKE) }
    var customCategory by remember { mutableStateOf(RodCategory.CARP) }

    // Anlık Fiziksel Otomatik Eşitleme Hesaplaması
    val customPhysicsInput = remember(
        customName, customLengthMeters, customTestCurveLbs, customAction, customMaterial, customWater, customCategory
    ) {
        RodPhysicsInput(
            rodName = if (customName.isBlank()) "Özel Kamış" else customName,
            lengthMeters = customLengthMeters,
            sensorDistanceMeters = 0.60,
            testCurveLbs = customTestCurveLbs,
            material = customMaterial,
            action = customAction,
            environment = customWater,
            category = customCategory
        )
    }
    val customCalibResult: CalculatedSensorCalibration = remember(customPhysicsInput) {
        RodPhysicsEngine.calculate(customPhysicsInput, rod.id)
    }

    val commonBaits = listOf(
        "🌽 Mısır",
        "🪱 Solucan",
        "🍡 Pop-up Boili",
        "🐟 Canlı Yem",
        "🟤 Pelet",
        "🍞 Hamur / Ekmek",
        "🦐 Karides / Kalamar"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phishing,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Olta, Kamış ve Yem Ayarları",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Olta Adı / Merası
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Olta Adı / Merası") },
                    placeholder = { Text("Örn: Sol Sazlık, Ada Önü, Derin Çukur") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rod_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0xFF283042),
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. KAMIŞ SEÇİMİ SEKMELERİ (HAZIR KÜTÜPHANE vs KENDİ KAMIŞINI EKLE)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kamış Modeli & Esneme Profili (Otomatik Kalibrasyon):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color(0xFF0F131D),
                        contentColor = NeonGreen,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = NeonGreen
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                    text = "🎣 Hazır Kütüphane",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 0) NeonGreen else Color(0xFF94A3B8)
                                )
                            }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                    text = "➕ Özel Kamış Tanımla",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == 1) NeonGreen else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }

                    // ==================== TAB 0: HAZIR KAMIŞ KÜTÜPHANESİ ====================
                    if (selectedTabIndex == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Arama Çubuğu
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Kamış ara (örn: Shimano, Daiwa, 3.5lb, Surf, Fox...)") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = Color(0xFF242C3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Marka Filtre Chips (Yatay Kaydırılabilir)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                brandFilters.forEach { brand ->
                                    val isSel = selectedBrandFilter == brand
                                    Surface(
                                        color = if (isSel) Color(0xFF1E2B3E) else Color(0xFF111520),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSel) NeonGreen else Color(0xFF263044)),
                                        modifier = Modifier.clickable { selectedBrandFilter = brand }
                                    ) {
                                        Text(
                                            text = brand,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) NeonGreen else Color(0xFF94A3B8),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Kamış Seçenekleri Listesi
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (filteredProfiles.isEmpty()) {
                                    Text(
                                        text = "Aradığınız kriterde kamış bulunamadı. '➕ Özel Kamış Tanımla' sekmesinden hemen ekleyebilirsiniz.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    filteredProfiles.forEach { profile ->
                                        val isSelected = selectedProfile.name == profile.name
                                        Surface(
                                            color = if (isSelected) Color(0xFF162534) else Color(0xFF0F131D),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) NeonGreen else Color(0xFF222B3D)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProfile = profile }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Phishing,
                                                        contentDescription = null,
                                                        tint = if (isSelected) NeonGreen else Color(0xFF64748B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = profile.name,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 11.5.sp
                                                            )
                                                        )
                                                        Text(
                                                            text = "${profile.lengthFormatted} • ${profile.testCurveFormatted} • ${profile.actionType}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = if (isSelected) Color(0xFF86EFAC) else Color(0xFF7E8B9B),
                                                                fontSize = 10.sp
                                                            )
                                                        )
                                                    }
                                                }

                                                if (isSelected) {
                                                    Surface(
                                                        color = NeonGreen.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Seçildi",
                                                            color = NeonGreen,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== TAB 1: ➕ ÖZEL KAMIŞ TANIMLA & OTOMATİK EŞİTLE ====================
                    if (selectedTabIndex == 1) {
                        Surface(
                            color = Color(0xFF0D121B),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF222F42)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Kendi Kamışını Tanımla:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                // 1. Kamış Adı / Modeli
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Kamış Adı / Modeli") },
                                    placeholder = { Text("Örn: Daiwa Emcast / Kendi Karbon Kamışım") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonGreen,
                                        unfocusedBorderColor = Color(0xFF2A344A),
                                        focusedLabelColor = NeonGreen,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                // 2. Kamış Boyu (Metre) Chips
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Kamış Boyu: ${String.format(Locale.US, "%.2f m", customLengthMeters)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(2.40, 2.70, 3.00, 3.30, 3.60, 3.90, 4.20, 4.50, 7.00).forEach { len ->
                                            val isSel = customLengthMeters == len
                                            Surface(
                                                color = if (isSel) Color(0xFF1B2D3D) else Color(0xFF131822),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isSel) NeonGreen else Color(0xFF263348)),
                                                modifier = Modifier.clickable { customLengthMeters = len }
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", len)} m",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Test Curve (lb / Sertlik Gücü) Chips
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Test Curve / Atar Gücü: ${String.format(Locale.US, "%.2f lb", customTestCurveLbs)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(1.50, 2.00, 2.50, 2.75, 3.00, 3.25, 3.50, 3.75, 4.00, 4.50).forEach { tc ->
                                            val isSel = customTestCurveLbs == tc
                                            Surface(
                                                color = if (isSel) Color(0xFF1B2D3D) else Color(0xFF131822),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isSel) NeonGreen else Color(0xFF263348)),
                                                modifier = Modifier.clickable { customTestCurveLbs = tc }
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", tc)} lb",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 10.5.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. Kamış Aksiyonu (Esneklik Karakteri)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Kamış Aksiyonu (Esneklik):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val actions: List<Pair<RodAction, String>> = listOf(
                                            RodAction.FAST_ACTION to "Hızlı Aksiyon (Uçtan Sert)",
                                            RodAction.MEDIUM_FAST_ACTION to "Yarı Parabolik (Dengeli)",
                                            RodAction.MEDIUM_PARABOLIC to "Parabolik (Yumuşak)",
                                            RodAction.SLOW_FULL_PARABOLIC to "Tam Parabolik (Esnek)"
                                        )
                                        actions.forEach { (act, label) ->
                                            val isSel = customAction == act
                                            Surface(
                                                color = if (isSel) Color(0xFF1B2D3D) else Color(0xFF131822),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isSel) NeonGreen else Color(0xFF263348)),
                                                modifier = Modifier.clickable { customAction = act }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5. ANLIK OTOMATİK EŞİTLEME ÖNİZLEME KARTI
                                Surface(
                                    color = Color(0xFF0B1713),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = NeonGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "⚡ Otomatik Kalibrasyon Eşitlendi",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = NeonGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "• Şok Vuruş Eşiği: ${customCalibResult.accelThresholdMilliG} mG\n• Boşa Düşme Açısı: ${String.format(Locale.US, "%.1f°", customCalibResult.pitchAngleDropThresholdDeg)}\n• Gürültü Filtresi: ${String.format(Locale.US, "%.1f Hz", customCalibResult.filterCutoffHz)}\n• Önerilen Kart Hassasiyeti: ${customCalibResult.normalizedSensitivityLevel} / 10",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF86EFAC),
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        )
                                    }
                                }

                                // "Bu Kamışı Kaydet ve Seç" Butonu
                                Button(
                                    onClick = {
                                        val finalName = if (customName.isBlank()) {
                                            "Özel Kamış (${String.format(Locale.US, "%.2f", customLengthMeters)}m - ${String.format(Locale.US, "%.2f", customTestCurveLbs)}lb)"
                                        } else {
                                            customName.trim()
                                        }
                                        val newCreatedProfile = RodProfile(
                                            name = finalName,
                                            lengthMeters = customLengthMeters,
                                            testCurveLbs = customTestCurveLbs,
                                            materialType = customMaterial.name,
                                            actionType = customAction.name,
                                            waterType = customWater.name,
                                            categoryType = customCategory.name,
                                            isPreset = false
                                        )
                                        onSaveCustomProfile(newCreatedProfile)
                                        selectedProfile = newCreatedProfile
                                        selectedTabIndex = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "💾 Bu Kamışı Kütüphaneye Ekle ve Seç",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Seçili Kamış Bilgi Rozeti
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1A1B))
                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Aktif Kamış: ${selectedProfile.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Text(
                                    text = "Sensör eşikleri ${selectedProfile.testCurveFormatted} esneme matematiğine göre kilitlenir.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF9AE6B4),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Yem / İğne Notu
                OutlinedTextField(
                    value = baitText,
                    onValueChange = { baitText = it },
                    label = { Text("Takılı Yem / Takım Notu") },
                    placeholder = { Text("Örn: Mısır & Solucan, Pop-up Boili...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rod_bait_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0xFF283042),
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Hızlı Yem Seçenekleri (Chips)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Hızlı Yem Ekle:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonBaits.forEach { bait ->
                            Surface(
                                color = Color(0xFF1B202D),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E384D)),
                                modifier = Modifier.clickable {
                                    baitText = if (baitText.isBlank()) bait else "$baitText, $bait"
                                }
                            ) {
                                Text(
                                    text = bait,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (nameText.isBlank()) "Olta ${rod.id}" else nameText.trim()
                    onSave(finalName, baitText.trim(), selectedProfile)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_rod_info_btn")
            ) {
                Text(
                    text = "💾 Ayarları Kaydet",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "İptal",
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF141724),
        shape = RoundedCornerShape(20.dp)
    )
}
