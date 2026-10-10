package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SponsorActionType
import com.example.model.SponsorBanner
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.delay

/**
 * 📢 KENDİ DOĞRUDAN YÖNETECEĞİNİZ ÖZEL SPONSOR & REKLAM BANNER KARTI
 * - Komisyonsuz, bağımsız, doğrudan sponsor anlaşmalarınızı yayınlayan canlı reklam alanı
 * - Birden fazla sponsor varsa belirli aralıklarla otomatik döner (Carousel)
 * - Tıklandığında Web, WhatsApp, Telefon Arama veya İndirim Kuponu Kopyalama tetikler
 */
@Composable
fun CustomSponsorCard(
    sponsorBanners: List<SponsorBanner>,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onDismissBanner: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activeBanners = remember(sponsorBanners) { sponsorBanners.filter { it.isActive } }

    if (!isVisible || activeBanners.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentBanner = activeBanners[currentIndex % activeBanners.size]

    // Otomatik Banner Döngüsü (Her 8 saniyede bir sonraki sponsora geçer)
    LaunchedEffect(activeBanners.size) {
        if (activeBanners.size > 1) {
            while (true) {
                delay(8000)
                currentIndex = (currentIndex + 1) % activeBanners.size
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_sponsor_card"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D1520),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFF00FF88), Color(0xFFFFD700))
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // 1. ÜST ETİKET VE SPONSOR BAŞLIĞI
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E2D42),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = currentBanner.badgeText,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }

                        if (activeBanners.size > 1) {
                            Text(
                                text = "${(currentIndex % activeBanners.size) + 1}/${activeBanners.size}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (onDismissBanner != null) {
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. AÇIKLAMA METNİ VE EYLEM BUTONU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentBanner.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentBanner.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // EYLEM TETİKLEYİCİ BUTON
                    Button(
                        onClick = {
                            handleSponsorClick(context, currentBanner)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF88),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sponsor_action_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (currentBanner.actionType) {
                                    SponsorActionType.OPEN_URL -> Icons.Default.OpenInNew
                                    SponsorActionType.OPEN_WHATSAPP -> Icons.Default.OpenInNew
                                    SponsorActionType.CALL_PHONE -> Icons.Default.Call
                                    SponsorActionType.COPY_PROMO_CODE -> Icons.Default.ContentCopy
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentBanner.buttonText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleSponsorClick(context: Context, banner: SponsorBanner) {
    try {
        when (banner.actionType) {
            SponsorActionType.OPEN_URL -> {
                val url = if (!banner.actionTarget.startsWith("http://") && !banner.actionTarget.startsWith("https://")) {
                    "https://${banner.actionTarget}"
                } else banner.actionTarget
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            SponsorActionType.OPEN_WHATSAPP -> {
                val waUrl = if (banner.actionTarget.startsWith("http")) {
                    banner.actionTarget
                } else {
                    "https://wa.me/${banner.actionTarget.replace("+", "").replace(" ", "")}"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                context.startActivity(intent)
            }
            SponsorActionType.CALL_PHONE -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${banner.actionTarget}"))
                context.startActivity(intent)
            }
            SponsorActionType.COPY_PROMO_CODE -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Sponsor Kuponu", banner.actionTarget)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "🎁 İndirim Kuponu Kopyalandı: ${banner.actionTarget}", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Sponsor bağlantısı açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
