# Việt AI

Ứng dụng Android mã nguồn mở trợ lý AI tiếng Việt, xây dựng bằng Kotlin và Jetpack Compose. Việt AI hỗ trợ trò chuyện với các nhà cung cấp AI do người dùng lựa chọn, nhập liệu bằng giọng nói, đọc phản hồi, quản lý nhân vật và phát nhạc có sẵn trên thiết bị mà không dùng máy chủ nhạc bên ngoài.

> **English** — VietAI is an open-source Vietnamese AI assistant for Android, built with Kotlin and Jetpack Compose. It supports configurable AI chat providers, speech input/output, personas, chat history, and offline playback of audio stored on the device.

## Tính năng

- Trò chuyện với Gemini, Groq, OpenAI-compatible endpoints hoặc máy chủ LLM tự quản lý. Người dùng tự cung cấp khóa API.
- Nhập giọng nói, đọc phản hồi bằng Android Text-to-Speech và gửi ảnh cho mô hình có hỗ trợ vision.
- Nhân vật AI dựng sẵn hoặc tùy chỉnh; lịch sử hội thoại lưu cục bộ bằng Room.
- Phát nhạc cục bộ từ thư viện MediaStore hoặc chọn tệp qua Storage Access Framework (SAF). Có điều khiển phát/dừng, bài trước/sau, tua, phát ngẫu nhiên, lặp, hàng đợi đã lưu và điều khiển media ở nền/màn hình khóa.
- Giao diện Compose với Material 3, màu động trên Android hỗ trợ, chế độ sáng/tối theo hệ thống, thanh điều hướng dưới và hỗ trợ tiếng Việt.

### Giao diện

Ứng dụng có các tab Trò chuyện, Nhạc, Dữ liệu cục bộ, Quyền riêng tư và Cài đặt. Tab Nhạc hiển thị thư viện trên thiết bị, nút thêm tệp, danh sách phát và điều khiển bài đang phát. Ảnh chụp màn hình sẽ được bổ sung khi có ảnh được cấp phép phù hợp.

## Kiến trúc

- **UI:** Activity và các màn hình Jetpack Compose/Material 3 trong `app/src/main/java/com/example/ui`.
- **Trạng thái:** `ChatViewModel` quản lý hội thoại; Room lưu tin nhắn và nhân vật.
- **Dữ liệu:** `data/` chứa Room, cài đặt và truy vấn MediaStore cho thư viện nhạc.
- **Âm thanh:** Media3 ExoPlayer và MediaSessionService phát URI cục bộ. Quyền đọc âm thanh chỉ dùng để liệt kê nhạc; SAF cho phép người dùng chọn tệp cụ thể.
- **Mạng:** Chỉ các tính năng AI/STT của ứng dụng mới cần mạng. Phát nhạc không truy cập Internet, máy chủ, CDN hay nội dung Web.

## Yêu cầu

- Android Studio tương thích với Android Gradle Plugin 7.4.2, JDK 17 và Android SDK 34.
- Android 7.0 (API 24) trở lên. Quyền đọc thư viện nhạc được hỏi lúc chạy trên Android 13 trở lên; trên Android 12 trở xuống ứng dụng dùng quyền lưu trữ tương ứng. Có thể dùng SAF mà không cấp quyền đọc toàn bộ thư viện.
- Kết nối Internet và khóa API chỉ cần cho các tính năng AI trực tuyến; nhạc cục bộ hoạt động ngoại tuyến.

## Build

```bash
./gradlew assembleDebug
./gradlew lint test
```

Hoặc dùng script tiện ích (có thể truyền `debug` hoặc `release`):

```bash
./build-apk.sh
./build-apk.sh debug
```

APK được tạo trong `app/build/outputs/apk/`. Build release không được ký bằng khóa dùng chung trong repository; hãy cấu hình khóa phát hành riêng an toàn nếu cần ký bản phát hành.

## Cấu hình

`.env.example` chỉ là tài liệu mẫu; ứng dụng hiện tại **không nạp tệp dotenv**. Nhập khóa API từ màn hình Cài đặt. Không ghi khóa thật vào source, `.env`, issue hoặc pull request. CI chỉ build và chạy kiểm tra, không cần khóa API.

## Lộ trình

- [x] Phát nhạc cục bộ bằng Media3, thư viện MediaStore và chọn tệp SAF.
- [x] Điều khiển media nền và tách nhạc khỏi dịch vụ mạng.
- [ ] Bổ sung ảnh chụp giao diện đã được cấp phép và kiểm thử trên nhiều thiết bị.
- [ ] Mở rộng playlist và tinh chỉnh accessibility/đa ngôn ngữ.
- [ ] Nâng cấp bộ công cụ Android/Compose sau khi xác nhận tương thích với toàn bộ tính năng và plugin AI.

## Đóng góp

Đọc [CONTRIBUTING.md](CONTRIBUTING.md), quy tắc ứng xử [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) và các mẫu issue/pull request. Báo cáo lỗ hổng riêng tư theo [SECURITY.md](SECURITY.md). Mọi đóng góp cần giữ nguyên nguyên tắc: nhạc chỉ phát nguồn cục bộ, không thêm stream từ xa hoặc dịch vụ nhạc bên thứ ba.

## Giấy phép

Dự án được phát hành theo giấy phép [MIT](LICENSE).

---

# VietAI (English)

VietAI is a Vietnamese-language Android AI assistant. It offers configurable AI providers, speech input/output, personas, local chat history, and offline playback of audio files selected from the device. See the Vietnamese sections above for build instructions and project details. Licensed under MIT.
