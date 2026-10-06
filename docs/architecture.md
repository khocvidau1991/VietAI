# Kiến trúc ứng dụng

Việt AI hiện là một ứng dụng Android một module (`:app`) viết bằng Kotlin. UI dùng Jetpack Compose/Material 3.

## Các lớp chính

- `ui/`: màn hình và Compose components.
- `ui/ChatViewModel.kt`: trạng thái và luồng hội thoại.
- `data/`: Room cho lịch sử chat/nhân vật và SharedPreferences cho cài đặt hiện tại.
- `api/`: client AI; chỉ hội thoại AI sử dụng mạng.
- `service/LocalMusicService.kt`: Media3 ExoPlayer và MediaSessionService cho playback nền.
- `data/LocalMusicRepository.kt`: truy vấn MediaStore và playlist URI đã lưu.

## Luồng nhạc

Tab Nhạc xin quyền đọc audio phù hợp với API Android đang chạy để truy vấn MediaStore. Người dùng có thể chọn nhiều audio qua SAF thay vì cấp quyền thư viện. URI được đưa vào hàng đợi Media3; playlist do người dùng chọn được lưu cục bộ. MediaSession cung cấp notification và điều khiển màn hình khóa.

Không có luồng nhạc từ xa: media source là URI `content://` cục bộ; không gọi API, CDN hoặc WebView nhạc. Quyền `INTERNET` vẫn cần cho nhà cung cấp AI và không được dùng cho playback.
