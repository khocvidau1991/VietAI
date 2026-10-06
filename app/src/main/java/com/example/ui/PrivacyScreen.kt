package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    currentRoute: String = "privacy",
    onNavigate: (String) -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Quyền riêng tư & Trách nhiệm") }) },
        bottomBar = { AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ==================== CẢNH BÁO QUAN TRỌNG ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "CẢNH BÁO QUAN TRỌNG VỀ AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "TẤT CẢ phản hồi của Việt AI đều do TRÍ TUỆ NHÂN TẠO " +
                            "sinh ra tự động. AI có thể SAI, có thể BỊA, có thể " +
                            "KHÔNG CHÍNH XÁC — kể cả khi câu trả lời nghe rất " +
                            "thuyết phục và tự tin.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "TUYỆT ĐỐI KHÔNG sử dụng thông tin từ AI làm căn cứ " +
                            "duy nhất cho các quyết định quan trọng về y tế, " +
                            "pháp lý, tài chính, an toàn tính mạng hoặc bất kỳ " +
                            "vấn đề nào có thể gây thiệt hại cho bạn hoặc người khác.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ==================== CAM KẾT BẢO MẬT ====================
            Text(
                "Cam kết bảo mật",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            PrivacyItem(
                title = "Lưu trữ cục bộ 100%",
                desc = "Toàn bộ tin nhắn, hình ảnh và cài đặt được lưu trong " +
                    "bộ nhớ riêng của ứng dụng trên thiết bị của bạn. Không có " +
                    "máy chủ nào của chúng tôi lưu dữ liệu của bạn."
            )
            PrivacyItem(
                title = "API Key được bảo vệ",
                desc = "Key được lưu bằng SharedPreferences trong sandbox " +
                    "của app. Android ngăn các ứng dụng khác đọc file này."
            )
            PrivacyItem(
                title = "Không theo dõi, không analytics",
                desc = "Ứng dụng không gửi log, không thu thập số liệu, không " +
                    "quảng cáo, không có SDK theo dõi nào."
            )
            PrivacyItem(
                title = "Kết nối trực tiếp",
                desc = "App gọi trực tiếp API Gemini / Groq / OpenAI mà bạn " +
                    "cấu hình. Không qua máy chủ trung gian nào."
            )
            PrivacyItem(
                title = "Xoá là xoá vĩnh viễn",
                desc = "Khi bạn xoá lịch sử hoặc bộ nhớ đệm, dữ liệu bị xoá " +
                    "vật lý khỏi thiết bị. Không có bản sao lưu."
            )

            Divider()
            Spacer(Modifier.height(4.dp))

            // ==================== TRÁCH NHIỆM NGƯỜI DÙNG ====================
            Text(
                "Trách nhiệm của người dùng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            WarningItem(
                title = "Kiểm chứng thông tin từ AI",
                desc = "Bạn phải tự kiểm chứng mọi thông tin quan trọng từ " +
                    "AI bằng nguồn chính thống: bác sĩ, luật sư, cơ quan chức " +
                    "năng, sách giáo khoa, trang web uy tín."
            )
            WarningItem(
                title = "Không dùng cho dữ liệu nhạy cảm",
                desc = "KHÔNG nhập mật khẩu, số CCCD, số thẻ tín dụng, thông " +
                    "tin y tế cá nhân, bí mật kinh doanh hoặc bất kỳ dữ liệu " +
                    "mật nào vào chat."
            )
            WarningItem(
                title = "Không dùng cho mục đích bất hợp pháp",
                desc = "Nghiêm cấm sử dụng app để: tạo nội dung vi phạm pháp " +
                    "luật, lừa đảo, quấy rối, xúc phạm, kích động thù địch, " +
                    "tạo tin giả, xâm phạm quyền riêng tư người khác."
            )
            WarningItem(
                title = "Tuân thủ pháp luật Việt Nam",
                desc = "Người dùng có trách nhiệm tuân thủ Luật An ninh mạng, " +
                    "Luật An toàn thông tin mạng, Luật Bảo vệ dữ liệu cá nhân " +
                    "và các quy định pháp luật hiện hành của Việt Nam."
            )
            WarningItem(
                title = "Không dùng cho trẻ em không giám sát",
                desc = "Trẻ em dưới 13 tuổi cần có sự giám sát của phụ huynh " +
                    "khi sử dụng. Phụ huynh chịu trách nhiệm về nội dung con " +
                    "em trao đổi với AI."
            )
            WarningItem(
                title = "Bản quyền nội dung AI",
                desc = "Nội dung do AI sinh ra có thể trùng lặp với tài liệu " +
                    "có bản quyền. Người dùng tự chịu trách nhiệm khi sử dụng " +
                    "nội dung đó cho mục đích công khai, thương mại."
            )

            Divider()
            Spacer(Modifier.height(4.dp))

            // ==================== GIỚI HẠN TRÁCH NHIỆM ====================
            Text(
                "Giới hạn trách nhiệm của nhà phát triển",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        "Ứng dụng được cung cấp \"NGUYÊN TRẠNG\" (as-is) với " +
                            "mục đích hỗ trợ tham khảo. Nhà phát triển KHÔNG " +
                            "chịu trách nhiệm pháp lý cho bất kỳ thiệt hại nào " +
                            "phát sinh từ việc sử dụng ứng dụng, bao gồm nhưng " +
                            "không giới hạn:",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    BulletLine("Thông tin sai lệch do AI sinh ra")
                    BulletLine("Quyết định y tế, pháp lý, tài chính dựa vào AI")
                    BulletLine("Mất mát dữ liệu trên thiết bị")
                    BulletLine("Chi phí API phát sinh khi dùng key của bạn")
                    BulletLine("Gián đoạn dịch vụ của bên thứ ba (Google, Groq, OpenAI)")
                    BulletLine("Hành vi vi phạm pháp luật của người dùng")
                }
            }

            Divider()
            Spacer(Modifier.height(4.dp))

            // ==================== DỮ LIỆU GỬI TỚI BÊN THỨ BA ====================
            Text(
                "Dữ liệu gửi tới bên thứ ba",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            WarningItem(
                title = "Nội dung chat",
                desc = "Khi bạn gửi tin nhắn, nội dung + lịch sử hội thoại " +
                    "sẽ được gửi tới nhà cung cấp AI bạn chọn (Google / Groq " +
                    "/ OpenAI / server local của bạn). Họ có chính sách riêng."
            )
            WarningItem(
                title = "Hình ảnh",
                desc = "Ảnh bạn gửi để AI phân tích sẽ được mã hoá base64 và " +
                    "gửi tới API vision của nhà cung cấp. KHÔNG gửi ảnh chứa " +
                    "thông tin nhạy cảm."
            )
            WarningItem(
                title = "Giọng nói",
                desc = "Nhận diện giọng nói dùng dịch vụ của Google (STT hệ " +
                    "thống). Âm thanh có thể được gửi tới server Google để " +
                    "xử lý, tùy cấu hình thiết bị của bạn."
            )
            WarningItem(
                title = "Nhạc tải từ YouTube",
                desc = "Tính năng phát nhạc tải audio từ YouTube qua server " +
                    "do bạn cấu hình. Chỉ dùng cho mục đích cá nhân, không " +
                    "phân phối lại."
            )

            Divider()
            Spacer(Modifier.height(4.dp))

            // ==================== LƯU Ý PHÁP LUẬT ====================
            Text(
                "Lưu ý pháp luật Việt Nam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        "Người dùng cần tuân thủ các quy định pháp luật hiện " +
                            "hành của Việt Nam khi sử dụng ứng dụng, bao gồm:",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    BulletLine("Luật An ninh mạng 2018")
                    BulletLine("Luật An toàn thông tin mạng 2015")
                    BulletLine("Nghị định 13/2023/NĐ-CP về Bảo vệ dữ liệu cá nhân")
                    BulletLine("Luật Sở hữu trí tuệ (khi dùng nội dung AI)")
                    BulletLine("Luật Báo chí, Luật Xuất bản (khi phát tán nội dung AI)")
                    BulletLine("Các quy định về nội dung số, thông tin sai lệch")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Nhà phát triển không chịu trách nhiệm nếu người dùng " +
                            "sử dụng ứng dụng để thực hiện hành vi vi phạm pháp luật.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider()
            Spacer(Modifier.height(4.dp))

            // ==================== THÔNG TIN ====================
            Text(
                "Thông tin ứng dụng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Phiên bản 1.0 • Build 2026.09",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Việt AI Android — Dương Văn Thành",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacyItem(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun WarningItem(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("•", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 17.sp
        )
    }
}
