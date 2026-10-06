package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
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
fun ApiKeyGuideScreen(
    currentRoute: String = "apikey_guide",
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hướng dẫn lấy API Key", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ================ INTRO ================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Tại sao cần API Key?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Việt AI không có server riêng. App gọi trực tiếp đến " +
                                "API của nhà cung cấp AI bằng key của bạn. Điều này có " +
                                "nghĩa: (1) bạn toàn quyền kiểm soát dữ liệu gửi đi, " +
                                "(2) không qua trung gian, (3) không mất phí cho chúng tôi.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ================ 1. GEMINI ================
            GuideSection(
                step = "1",
                provider = "Google Gemini",
                emoji = "🌟",
                badgeColor = Color(0xFF4285F4),
                free = "Có bậc miễn phí (giới hạn/ngày)",
                steps = listOf(
                    "Mở trình duyệt, truy cập Google AI Studio" to
                        "https://aistudio.google.com/app/apikey",
                    "Đăng nhập bằng tài khoản Google của bạn" to "",
                    "Nhấn nút \"Create API key\" màu xanh" to "",
                    "Chọn project (hoặc tạo project mới)" to "",
                    "Copy API key vừa tạo (dạng AIzaSy...)" to "",
                    "Quay lại Việt AI → Cài đặt → dán vào ô Gemini API Key" to ""
                ),
                warnings = listOf(
                    "KHÔNG chia sẻ key với người khác — ai có key đều dùng được quota của bạn",
                    "Bậc miễn phí giới hạn 15 request/phút, 1 triệu token/ngày",
                    "Nếu dùng nhiều, bật billing để không bị chặn giữa chừng"
                )
            )

            // ================ 2. GROQ ================
            GuideSection(
                step = "2",
                provider = "Groq Cloud",
                emoji = "⚡",
                badgeColor = Color(0xFFF55036),
                free = "Có bậc miễn phí (nhanh nhất hiện nay)",
                steps = listOf(
                    "Truy cập Groq Console" to
                        "https://console.groq.com/keys",
                    "Đăng ký / đăng nhập (Google, GitHub hoặc email)" to "",
                    "Vào mục \"API Keys\" → nhấn \"Create API Key\"" to "",
                    "Đặt tên gợi nhớ (VD: VietAI-Android)" to "",
                    "Copy key ngay — chỉ hiện 1 lần duy nhất, dạng gsk_..." to "",
                    "Quay lại Việt AI → Cài đặt → chọn Groq → dán key" to ""
                ),
                warnings = listOf(
                    "Groq miễn phí tốc độ rất nhanh nhưng có rate limit theo phút",
                    "Key Groq chỉ hiện 1 lần — nếu quên phải tạo key mới",
                )
            )

            // ================ 3. OPENAI ================
            GuideSection(
                step = "3",
                provider = "OpenAI / Groq-compatible / Local",
                emoji = "🤖",
                badgeColor = Color(0xFF10A37F),
                free = "Trả phí (không có bậc miễn phí)",
                steps = listOf(
                    "Truy cập OpenAI Platform" to
                        "https://platform.openai.com/api-keys",
                    "Đăng ký tài khoản (cần xác minh số điện thoại)" to "",
                    "Nạp tiền vào tài khoản (tối thiểu 5 USD)" to
                        "https://platform.openai.com/account/billing",
                    "Vào \"API Keys\" → \"Create new secret key\"" to "",
                    "Copy key dạng sk-proj-... (chỉ hiện 1 lần)" to "",
                    "Quay lại Việt AI → Cài đặt → chọn OpenAI → dán key" to ""
                ),
                warnings = listOf(
                    "Tính tiền theo lượng token sử dụng — dùng ít thì rẻ, dùng nhiều tốn",
                    "Bạn cũng có thể dùng cho endpoint khác (Ollama, LM Studio) bằng cách " +
                        "đổi Base URL trong Cài đặt"
                )
            )

            // ================ 4. LOCAL ================
            GuideSection(
                step = "4",
                provider = "Local / Self-hosted",
                emoji = "💻",
                badgeColor = Color(0xFF6366F1),
                free = "Miễn phí 100% — không cần API key",
                steps = listOf(
                    "Cài Ollama trên máy tính" to "https://ollama.com/download",
                    "Chạy model local: ollama pull llama3.1" to "",
                    "Bật server: ollama serve (mặc định cổng 11434)" to "",
                    "Trong Cài đặt Việt AI → OpenAI → Base URL điền:" to "",
                    "http://<IP-máy-tính>:11434/v1/chat/completions" to "",
                    "Model điền: llama3.1 (hoặc model bạn đã pull)" to "",
                    "API Key để trống hoặc điền gì cũng được" to ""
                ),
                warnings = listOf(
                    "Điện thoại và máy tính phải cùng mạng WiFi",
                    "Tốc độ phụ thuộc vào CPU/GPU của máy chạy",
                    "Model nhỏ (3B-7B) trả lời chậm hơn nhưng bảo mật tuyệt đối"
                )
            )

            Divider()

            // ================ LƯU Ý CHUNG ================
            Text(
                "Lưu ý an toàn",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            SafetyNote(
                icon = Icons.Default.Lock,
                text = "Tuyệt đối KHÔNG chia sẻ API key cho bất kỳ ai, kể cả người " +
                    "quen. Key của bạn = quyền truy cập tài khoản của bạn."
            )
            SafetyNote(
                icon = Icons.Default.Warning,
                text = "Nếu nghi ngờ key bị lộ, vào console của nhà cung cấp để " +
                    "REVOKE (thu hồi) ngay và tạo key mới."
            )
            SafetyNote(
                icon = Icons.Default.CheckCircle,
                text = "Key được lưu cục bộ trên máy bạn, KHÔNG gửi về server nào của " +
                    "chúng tôi (vì chúng tôi không có server)."
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuideSection(
    step: String,
    provider: String,
    emoji: String,
    badgeColor: Color,
    free: String,
    steps: List<Pair<String, String>>,
    warnings: List<String>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(step, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$emoji $provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        free,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Steps
            steps.forEachIndexed { i, (text, url) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${i + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                        if (url.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.clickable {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        )
                                    } catch (_: Exception) {}
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Warnings
            warnings.forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚠", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        w,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyNote(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
        }
    }
}
