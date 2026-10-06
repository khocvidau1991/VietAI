#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="${1:-release}"

case "$VARIANT" in
    debug) VARIANT_CAP=Debug ;;
    release) VARIANT_CAP=Release ;;
    *)
        echo "Cách dùng: $0 [debug|release]" >&2
        exit 2
        ;;
esac

"$PROJECT_DIR/gradlew" "assemble${VARIANT_CAP}"
APK="$PROJECT_DIR/app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"

if [[ ! -f "$APK" ]]; then
    echo "Không tìm thấy APK tại: $APK" >&2
    exit 1
fi

echo "APK đã được tạo: $APK"
