#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# Script build + ký APK tự động cho Việt AI
# Cách dùng:
#   bash build-apk.sh           # Build release (khuyến nghị)
#   bash build-apk.sh debug     # Build debug
# ============================================================

VARIANT="${1:-release}"
VARIANT_CAP="$(echo "$VARIANT" | sed 's/^./\U&/')"

PROJECT="/storage/emulated/0/AndroidIDEProjects/AI-viet"
BT="/data/data/com.tom.rv2ide/files/home/android-sdk/build-tools/35.0.1"
KEYSTORE="$PROJECT/debug.keystore"
OUTPUT_DIR="/storage/emulated/0/Download"
OUTPUT_NAME="viet-ai-${VARIANT}.apk"

cd "$PROJECT" || { echo "ERROR: Không vào được $PROJECT"; exit 1; }

echo "════════════════════════════════════════"
echo "  BUILD VIỆT AI - variant: $VARIANT"
echo "════════════════════════════════════════"

# ---- 1. Build ----
echo "[1/5] Đang build..."
./gradlew "assemble$VARIANT_CAP" --no-build-cache 2>&1 | tail -5

APK="$PROJECT/app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"
if [ ! -f "$APK" ]; then
    echo "ERROR: Không tìm thấy APK tại $APK"
    exit 1
fi
echo "     APK gốc: $(ls -lh "$APK" | awk '{print $5}')"

# ---- 2. Xoá chữ ký cũ ----
echo "[2/5] Xoá chữ ký cũ..."
zip -d "$APK" "META-INF/*.SF" "META-INF/*.RSA" "META-INF/*.DSA" "META-INF/*.MF" 2>/dev/null
echo "     OK"

# ---- 3. Zipalign ----
echo "[3/5] Zipalign..."
ALIGNED="$PROJECT/app/build/outputs/apk/$VARIANT/aligned.apk"
$BT/zipalign -f -p 4 "$APK" "$ALIGNED"
echo "     OK"

# ---- 4. Ký v1+v2+v3 ----
echo "[4/5] Ký v1+v2+v3..."
OUTPUT="$OUTPUT_DIR/$OUTPUT_NAME"
$BT/apksigner sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --min-sdk-version 24 \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$OUTPUT" \
    "$ALIGNED"
echo "     OK"

# ---- 5. Verify ----
echo "[5/5] Verify APK..."
$BT/apksigner verify --verbose "$OUTPUT" 2>&1 | head -8

echo ""
echo "════════════════════════════════════════"
echo "  ✅ HOÀN THÀNH"
echo "  📦 APK: $OUTPUT"
echo "  📏 Size: $(ls -lh "$OUTPUT" | awk '{print $5}')"
echo "════════════════════════════════════════"
echo ""
echo "Cài APK qua Files by Google → Downloads → $OUTPUT_NAME"
