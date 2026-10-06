package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    currentRoute: String = "about",
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenApiKeyGuide: () -> Unit = {}
) {
    val context = LocalContext.current

    val authorName = "Dương Văn Thành"
    val authorAddress = "Bảo Lý, Phú Bình, Thái Nguyên"
    val authorEmail = "tinhviet9x@gmail.com"
    val authorPhone = "0962221005"
    val appName = "Việt AI"
    val appVersion = "1.0"
    val appBuild = "2026.09"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giới thiệu", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = """
                            $appName v$appVersion
                            Trợ lý AI tiếng Việt đa năng
                            Tác giả: $authorName
                            Liên hệ: $authorEmail | $authorPhone
                        """.trimIndent()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Giới thiệu $appName")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Chia sẻ"))
                    }) {
                        Icon(Icons.Default.Share, "Chia sẻ")
                    }
                }
            )
        },
        bottomBar = { AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Text("V", color = Color(0xFF006A60),
                        fontWeight = FontWeight.Black, fontSize = 48.sp)
                    Text("A", color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Black, fontSize = 48.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text("Trợ lý AI tiếng Việt đa năng",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("v$appVersion")
                InfoChip("Build $appBuild")
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Về ứng dụng")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "$appName là trợ lý AI cá nhân được phát triển cho " +
                        "người Việt, hoạt động hoàn toàn bằng tiếng Việt tự nhiên. " +
                        "Ứng dụng kết hợp sức mạnh của nhiều mô hình ngôn ngữ lớn " +
                        "(Gemini, Groq, OpenAI) với khả năng nhận diện giọng nói, " +
                        "đọc phản hồi, phát nhạc và 234 nhân vật chuyên môn — " +
                        "tất cả trong một giao diện đơn giản, thân thiện.\n\n" +
                        "Toàn bộ dữ liệu được lưu cục bộ trên thiết bị. Không có " +
                        "máy chủ trung gian, không thu thập, không theo dõi, không " +
                        "quảng cáo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Tính năng mới nhất")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FeatureRow(
                        emoji = "🎭",
                        title = "234 nhân vật chuyên môn",
                        desc = "Cây thư mục chia theo Tiểu học, THCS, THPT, " +
                            "Đại học, Điện lạnh. Đầy đủ 22 ngành đại học + " +
                            "17 nhân vật điện lạnh chuyên sâu."
                    )
                    FeatureRow(
                        emoji = "⚡",
                        title = "Chuyển nhân vật bằng chat",
                        desc = "Gõ \"chuyển sang gia sư toán\" hoặc \"đổi sang " +
                            "Daikin\" — AI tự chuyển. Nếu mơ hồ, AI sẽ hỏi lại."
                    )
                    FeatureRow(
                        emoji = "🔍",
                        title = "Tìm kiếm nhân vật",
                        desc = "Search theo tên, mô tả, đường dẫn. Tìm nhanh " +
                            "trong 234 nhân vật chỉ bằng 1 câu."
                    )
                    FeatureRow(
                        emoji = "➕",
                        title = "Tự tạo nhân vật",
                        desc = "Tạo persona riêng với emoji, mô tả, system " +
                            "prompt, voice TTS, cho phép phát nhạc."
                    )
                    FeatureRow(
                        emoji = "🔑",
                        title = "Hướng dẫn API key",
                        desc = "Hướng dẫn từng bước lấy API key Gemini, Groq, " +
                            "OpenAI, Local (Ollama) ngay trong app."
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenApiKeyGuide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hướng dẫn lấy API key miễn phí")
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Tính năng chính")
            Spacer(Modifier.height(8.dp))

            FeatureRow("💬", "Trò chuyện thông minh",
                "Chat bằng text hoặc giọng nói, phản hồi tự nhiên như người thật.")
            FeatureRow("🎙️", "Trò chuyện rảnh tay (LIVE)",
                "Chế độ LIVE cho phép nói chuyện liên tục không cần chạm màn hình.")
            FeatureRow("🎵", "Phát nhạc qua MCP",
                "Yêu cầu bằng giọng nói, AI tự tìm và phát nhạc từ YouTube.")
            FeatureRow("🎨", "Cảm xúc động",
                "AI thể hiện 18 trạng thái cảm xúc qua hình ảnh động.")
            FeatureRow("🌐", "Đa nhà cung cấp AI",
                "Tự chọn Gemini, Groq, OpenAI hoặc local. Nhập API key riêng.")
            FeatureRow("🔒", "Bảo mật cục bộ",
                "Lịch sử chat, cài đặt, API key đều lưu trên máy. Không analytics.")
            FeatureRow("🎬", "Video & PiP",
                "Xem video trong app, tự động thu nhỏ (PiP) khi thoát ra ngoài.")
            FeatureRow("📝", "Đa ngôn ngữ",
                "Hỗ trợ 12 ngôn ngữ cho nhận diện giọng nói và đọc phản hồi.")

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Lưu ý quan trọng")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Mọi phản hồi của AI đều có thể SAI hoặc KHÔNG CHÍNH XÁC. " +
                            "Không dùng AI làm nguồn duy nhất cho quyết định y tế, " +
                            "pháp lý, tài chính hoặc an toàn tính mạng. Xem chi tiết " +
                            "ở mục Quyền riêng tư → Cảnh báo quan trọng về AI.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Tác giả")
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF006A60), Color(0xFF22C55E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DVT", color = Color.White,
                            fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(authorName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Nhà phát triển ứng dụng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Thái Nguyên, Việt Nam",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            ContactRow(Icons.Default.Home, "Địa chỉ", authorAddress) {
                val uri = Uri.parse("geo:0,0?q=" + Uri.encode("$authorAddress, Việt Nam"))
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(context, authorAddress, Toast.LENGTH_LONG).show()
                }
            }
            ContactRow(Icons.Default.Email, "Email", authorEmail) {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$authorEmail")
                    })
                } catch (_: Exception) {
                    Toast.makeText(context, authorEmail, Toast.LENGTH_LONG).show()
                }
            }
            ContactRow(Icons.Default.Phone, "Điện thoại", authorPhone) {
                try {
                    context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$authorPhone")
                    })
                } catch (_: Exception) {
                    Toast.makeText(context, authorPhone, Toast.LENGTH_LONG).show()
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            SectionTitle("Lời cảm ơn")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Cảm ơn bạn đã tin tưởng và sử dụng $appName. Ứng dụng " +
                            "được phát triển với mong muốn mang AI đến gần hơn với " +
                            "người Việt — không rào cản ngôn ngữ, không phức tạp, " +
                            "không xâm phạm quyền riêng tư.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Made with ", style = MaterialTheme.typography.bodySmall)
                        Icon(Icons.Default.Favorite, contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp))
                        Text(" in Thái Nguyên", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("© 2026 $authorName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Bảo lưu mọi quyền",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.width(4.dp).height(20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.Star, contentDescription = "Mở",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)
        ).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium)
    }
}
