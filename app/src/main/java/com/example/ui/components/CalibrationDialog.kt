package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.NeonGreen
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrationDialog(
    rod: FishingRod,
    rodProfiles: List<RodProfile>,
    onDismiss: () -> Unit,
    onApplyCalibration: (rodId: Int, profile: RodProfile, result: CalculatedSensorCalibration) -> Unit,
    onSaveNewProfile: (profile: RodProfile) -> Unit
) {
    val activeLibrary = if (rodProfiles.isEmpty()) AppDatabase.DEFAULT_PRESETS else rodProfiles

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RodCategory.ALL) }

    var selectedProfile by remember {
        mutableStateOf(
            activeLibrary.find { it.name == rod.assignedRodProfileName } ?: activeLibrary.first()
        )
    }

    var showCustomInputSection by remember { mutableStateOf(false) }
    var showFormulaDetails by remember { mutableStateOf(false) }

    // Canlı Matematiksel Fizik Parametreleri (Kullanıcı bunları anlık değiştirebilir)
    var customName by remember { mutableStateOf(selectedProfile.name) }
    var currentLengthMeters by remember { mutableDoubleStateOf(selectedProfile.lengthMeters) }
    var currentSensorDistCm by remember { mutableIntStateOf(selectedProfile.sensorDistanceCm) }
    var currentTestCurveLbs by remember { mutableDoubleStateOf(selectedProfile.testCurveLbs) }
    var currentMaterial by remember { mutableStateOf(selectedProfile.getMaterial()) }
    var currentAction by remember { mutableStateOf(selectedProfile.getAction()) }
    var currentEnvironment by remember { mutableStateOf(selectedProfile.getEnvironment()) }
    var currentCategory by remember { mutableStateOf(selectedProfile.getCategory()) }

    // Profil seçildiğinde tüm matematik parametrelerini senkronize et
    fun syncWithProfile(profile: RodProfile) {
        selectedProfile = profile
        customName = profile.name
        currentLengthMeters = profile.lengthMeters
        currentSensorDistCm = profile.sensorDistanceCm
        currentTestCurveLbs = profile.testCurveLbs
        currentMaterial = profile.getMaterial()
        currentAction = profile.getAction()
        currentEnvironment = profile.getEnvironment()
        currentCategory = profile.getCategory()
    }

    // Filtrelenmiş Kamış Listesi (Arama & Kategori)
    val filteredProfiles by remember(searchQuery, selectedCategory, activeLibrary) {
        derivedStateOf {
            activeLibrary.filter { profile ->
                val matchesCategory = when (selectedCategory) {
                    RodCategory.ALL -> true
                    RodCategory.CUSTOM -> !profile.isPreset
                    else -> profile.categoryType == selectedCategory.name
                }
                val matchesSearch = searchQuery.isBlank() ||
                        profile.name.contains(searchQuery, ignoreCase = true) ||
                        profile.lengthFormatted.contains(searchQuery, ignoreCase = true) ||
                        profile.testCurveFormatted.contains(searchQuery, ignoreCase = true)

                matchesCategory && matchesSearch
            }
        }
    }

    // EULER-BERNOULLI FİZİK MOTORU ÇIKTISI (ANLIK HESAPLANIR)
    val physicsCalculation by remember(
        customName, currentLengthMeters, currentSensorDistCm,
        currentTestCurveLbs, currentMaterial, currentAction, currentEnvironment, currentCategory
    ) {
        derivedStateOf {
            val input = RodPhysicsInput(
                rodName = customName,
                lengthMeters = currentLengthMeters,
                sensorDistanceMeters = currentSensorDistCm / 100.0,
                testCurveLbs = currentTestCurveLbs,
                material = currentMaterial,
                action = currentAction,
                environment = currentEnvironment,
                category = currentCategory
            )
            RodPhysicsEngine.calculate(input, rod.id)
        }
    }

    val lengthQuickOptions = listOf(2.70, 3.00, 3.60, 3.90, 4.20, 6.00, 7.00, 8.00)
    val tcQuickOptions = listOf(2.00, 2.75, 3.00, 3.25, 3.50, 4.00, 5.00)

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LuxuryGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = null,
                        tint = LuxuryGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Kamış & Sensör Kalibrasyonu",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${rod.name} • Dünya Standartları Kamış Kütüphanesi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. ARAMA VE KATEGORİ SEÇİMİ
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Model ara (Örn: 7m, Fox, Daiwa, Albastar, Surf...)", fontSize = 12.sp, color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxuryGold,
                        unfocusedBorderColor = Color(0xFF263044),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rod_search_input")
                )

                // Kategori Sekmeleri (Yatay Kaydırılabilir)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RodCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.icon} ${cat.title}", fontSize = 11.sp, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuxuryGold,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF161C2C),
                                labelColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                // Seçili Kamış Kartı / Hızlı Seçim Listesi (Compact)
                Surface(
                    color = Color(0xFF141926),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF26334D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Seçili Model: ${selectedProfile.name}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LuxuryGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${selectedProfile.lengthFormatted} • ${selectedProfile.testCurveFormatted} • ${selectedProfile.getMaterial().title.substringBefore(" ")}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 10.sp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sonuç Listesi (Max 150dp Yükseklik)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (filteredProfiles.isEmpty()) {
                                Text(
                                    text = "Aramaya uygun kamış bulunamadı.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                filteredProfiles.forEach { profile ->
                                    val isSelected = profile.id == selectedProfile.id || profile.name == selectedProfile.name
                                    Surface(
                                        color = if (isSelected) Color(0xFF1E2D48) else Color(0xFF0F131E),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSelected) LuxuryGold else Color(0xFF1E2638)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { syncWithProfile(profile) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = profile.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSelected) LuxuryGold else Color.White,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Text(
                                                    text = "${profile.lengthFormatted} • ${profile.testCurveFormatted}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 9.sp)
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 🌟 ÖNERİLEN ALTIN STANDART TAVSİYESİ KARTI (EN AZ 2.20 METRE)
                Surface(
                    color = Color(0xFF1B1A0E),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LuxuryGold.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = LuxuryGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "4.20m'ye Kadar Olan Kamışlarda: En Az 2.20m",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = LuxuryGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Surface(
                                color = LuxuryGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ALTIN KONUM",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "4.20 metreye kadar olan kamışlarda sensörün en az 2.20m (2. parçanın üstü) seviyesine takılması, yer/su sinyal emilimini sıfırlayarak 250m+ maksimum RF menzil ve mikro vuruşlarda en yüksek titreşim hassasiyetini (SNR) sağlar.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        )

                        Button(
                            onClick = {
                                currentSensorDistCm = 220
                                showCustomInputSection = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("apply_recommended_220cm_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⚡ 2.20m (220 cm) Optimum Yüksekliği Uygula",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 2. MATEMATİKSEL HESAPLANAN DİJİTAL KALİBRASYON KARTI (CANLI ÇIKTI)
                Surface(
                    color = Color(0xFF0D1424),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, NeonGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Hesaplanmış Titreşim & Eşik Verisi",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Surface(
                                color = NeonGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Hassasiyet: ${physicsCalculation.normalizedSensitivityLevel}/10",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 4'lü Sayısal Değer Tablosu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MathMetricBox(label = "İvme Eşiği", value = "${physicsCalculation.accelThresholdMilliG} mG")
                            MathMetricBox(label = "Boşa Düşme", value = "${physicsCalculation.pitchAngleDropThresholdDeg}°")
                            MathMetricBox(label = "Rezonans fn", value = "${physicsCalculation.resonanceFrequencyHz} Hz")
                            MathMetricBox(label = "Filtre Cutoff", value = "${physicsCalculation.filterCutoffHz} Hz")
                        }

                        // RF SİNYAL GÜCÜ & MENZİL VERİM GÖSTERGESİ
                        Surface(
                            color = Color(0xFF141D30),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = if (physicsCalculation.rfEfficiencyPercent >= 90) NeonGreen else LuxuryGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "RF Sinyal Gücü & Görüş Hattı",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = "%${physicsCalculation.rfEfficiencyPercent} Verim",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (physicsCalculation.rfEfficiencyPercent >= 90) NeonGreen else if (physicsCalculation.rfEfficiencyPercent >= 65) LuxuryGold else Color(0xFFFF5252),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { physicsCalculation.rfEfficiencyPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (physicsCalculation.rfEfficiencyPercent >= 90) NeonGreen else if (physicsCalculation.rfEfficiencyPercent >= 65) LuxuryGold else Color(0xFFFF5252),
                                    trackColor = Color(0xFF1E2638)
                                )

                                Text(
                                    text = physicsCalculation.rfEfficiencyDescription,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }

                        // Detaylı Fizik Formülü Aç/Kapat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormulaDetails = !showFormulaDetails },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (showFormulaDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showFormulaDetails) "Mekanik Formül Detaylarını Gizle" else "Euler-Bernoulli Formül Detayını Göster",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        AnimatedVisibility(visible = showFormulaDetails) {
                            Surface(
                                color = Color(0xFF13192B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = physicsCalculation.formulaSummary,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // 3. MATEMATİKSEL DEĞİŞKENLERİ KENDİN AYARLA (HASSAS AYAR FORMU)
                Surface(
                    color = Color(0xFF161A29),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (showCustomInputSection) LuxuryGold else Color(0xFF2A344A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomInputSection = !showCustomInputSection },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCustomInputSection) Icons.Default.Tune else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = LuxuryGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (showCustomInputSection) "Teknik Parametreleri Gizle" else "🔧 Kamışın Gerçek Değerlerini Kendin Ayarla",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = LuxuryGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (showCustomInputSection) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Kamış İsmi
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Kamış Adı / Modeli", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LuxuryGold,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_rod_name_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // SENSÖRÜN TAKILDIĞI MESAFE SLIDER (0.20m - 3.50m)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 Sensör Takılma Yüksekliği (x):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (currentSensorDistCm >= 220) "$currentSensorDistCm cm (⭐ 2.20m+ Optimum)" else "$currentSensorDistCm cm",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (currentSensorDistCm >= 220) NeonGreen else LuxuryGold,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                            Slider(
                                value = currentSensorDistCm.toFloat(),
                                onValueChange = { currentSensorDistCm = it.toInt() },
                                valueRange = 20f..350f,
                                steps = 65,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (currentSensorDistCm >= 220) NeonGreen else LuxuryGold,
                                    activeTrackColor = if (currentSensorDistCm >= 220) NeonGreen else LuxuryGold,
                                    inactiveTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("sensor_distance_slider")
                            )

                            // Hızlı Yükseklik Seçim Butonları (Chips)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val distanceOptions = listOf(50, 100, 150, 180, 220, 260)
                                distanceOptions.forEach { dist ->
                                    val isRec = dist == 220
                                    val isSelected = currentSensorDistCm == dist
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { currentSensorDistCm = dist },
                                        label = {
                                            Text(
                                                text = if (isRec) "220 cm ⭐ (Önerilen)" else "$dist cm",
                                                fontSize = 10.sp,
                                                fontWeight = if (isRec || isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isRec) NeonGreen else LuxuryGold,
                                            selectedLabelColor = Color.Black,
                                            containerColor = if (isRec) Color(0xFF1B2A1E) else Color(0xFF1E2433),
                                            labelColor = if (isRec) NeonGreen else Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // KAMIŞ TAM BOYU (L: 1.80m - 9.00m)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📏 Kamış Boyu (L):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = String.format("%.2f m", currentLengthMeters),
                                    style = MaterialTheme.typography.labelSmall.copy(color = LuxuryGold, fontWeight = FontWeight.Bold)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                lengthQuickOptions.forEach { len ->
                                    FilterChip(
                                        selected = (currentLengthMeters * 100).roundToInt() == (len * 100).roundToInt(),
                                        onClick = { currentLengthMeters = len },
                                        label = { Text("${String.format("%.2f", len)} m", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuxuryGold,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E2433),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // MALZEME TÜRÜ (YOUNG MODÜLÜ - E)
                            Text(
                                text = "🧪 Malzeme / Karbon Modülü (Young Modülü E):",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                RodMaterial.values().forEach { mat ->
                                    Surface(
                                        color = if (currentMaterial == mat) Color(0xFF1E3A5F) else Color(0xFF141926),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (currentMaterial == mat) Color(0xFF38BDF8) else Color(0xFF263044)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { currentMaterial = mat }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (currentMaterial == mat) Icons.Default.Check else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (currentMaterial == mat) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${mat.title} (${mat.youngsModulusGPa.toInt()} GPa)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = mat.description,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 9.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // AKSİYON / ESNEKLİK TÜRÜ
                            Text(
                                text = "🏹 Aksiyon / Bükülme Tipi (Taper):",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RodAction.values().forEach { act ->
                                    FilterChip(
                                        selected = currentAction == act,
                                        onClick = { currentAction = act },
                                        label = { Text(act.title.substringBefore(" "), fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuxuryGold,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E2433),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // TEST CURVE (lbs)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚖️ Test Curve / Atar (lbs):",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$currentTestCurveLbs lbs",
                                    style = MaterialTheme.typography.labelSmall.copy(color = LuxuryGold, fontWeight = FontWeight.Bold)
                                )
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tcQuickOptions.forEach { tc ->
                                    FilterChip(
                                        selected = currentTestCurveLbs == tc,
                                        onClick = { currentTestCurveLbs = tc },
                                        label = { Text("$tc lbs", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LuxuryGold,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E2433),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // SU VE AV ORTAMI
                            Text(
                                text = "🌊 Su ve Rüzgar Ortamı (Gürültü Filtresi):",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                WaterEnvironment.values().forEach { env ->
                                    FilterChip(
                                        selected = currentEnvironment == env,
                                        onClick = { currentEnvironment = env },
                                        label = { Text(env.title, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E2433),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Yeni Kamış Olarak Kütüphaneye Kaydet Butonu
                            Button(
                                onClick = {
                                    val nameToSave = if (customName.isBlank()) "Özel Kamış ${currentLengthMeters}m" else customName.trim()
                                    val newProfile = RodProfile(
                                        name = nameToSave,
                                        lengthMeters = currentLengthMeters,
                                        sensorDistanceCm = currentSensorDistCm,
                                        testCurveLbs = currentTestCurveLbs,
                                        materialType = currentMaterial.name,
                                        actionType = currentAction.name,
                                        waterType = currentEnvironment.name,
                                        categoryType = currentCategory.name,
                                        isPreset = false
                                    )
                                    onSaveNewProfile(newProfile)
                                    selectedProfile = newProfile
                                    showCustomInputSection = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_custom_rod_profile_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⭐ Bu Ayarları Kütüphaneme Kaydet", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activeProfile = RodProfile(
                        name = customName.ifBlank { selectedProfile.name },
                        lengthMeters = currentLengthMeters,
                        sensorDistanceCm = currentSensorDistCm,
                        testCurveLbs = currentTestCurveLbs,
                        materialType = currentMaterial.name,
                        actionType = currentAction.name,
                        waterType = currentEnvironment.name,
                        categoryType = currentCategory.name,
                        isPreset = false
                    )
                    onApplyCalibration(rod.id, activeProfile, physicsCalculation)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("apply_physics_calibration_btn")
            ) {
                Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "💾 Hafızaya Al & Oltaya Uygula",
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
                Text(text = "Kapat", color = Color.White)
            }
        },
        containerColor = Color(0xFF141724),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun MathMetricBox(label: String, value: String) {
    Surface(
        color = Color(0xFF182236),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontSize = 9.sp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}
