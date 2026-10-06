# Hướng dẫn build

## Môi trường

- JDK 17
- Android SDK Platform 34 và Android Build Tools tương thích
- Gradle wrapper đi kèm repository (Gradle 7.5)

Trong Android Studio, mở thư mục repository và chờ Gradle sync. Trên máy không có Android Studio:

```bash
./gradlew assembleDebug
./gradlew lint test
```

Nếu quyền thực thi wrapper chưa được giữ khi tải source:

```bash
chmod +x gradlew
```

APK debug nằm tại `app/build/outputs/apk/debug/`. Script `./build-apk.sh [debug|release]` gọi Gradle wrapper và in vị trí APK. Bản release không dùng khóa chung; tự cấu hình signing secrets ngoài repository nếu cần APK ký phát hành.

Không cần `.env` để build. API key AI được nhập trong ứng dụng và không thuộc quy trình CI.
