#!/usr/bin/env bash
set -euo pipefail

# === Configuration ===
TMP_DIR="/tmp/tb-release"
RELEASE="4.0.2pe"
S3_BUCKET="s3://cf-simple-s3-origin-tb-pe-cdn-156597721064/"

# Clean & recreate temp dir
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"

# Helper: copy & rename one file
#   $1 = source file
#   $2 = target base name (without version or extension)
#   $3 = extension (e.g. deb, rpm, zip)
copy_and_rename() {
  local src="$1" base="$2" ext="$3"
  if [[ -f "$src" ]]; then
    local dst="${TMP_DIR}/${base}-${RELEASE}.${ext}"
    echo "Copying $src → $dst"
    cp "$src" "$dst"
  else
    echo "Warning: $src not found, skipping."
  fi
}

# 1) Core application packages
copy_and_rename "application/target/thingsboard.deb"        "thingsboard" "deb"
copy_and_rename "application/target/thingsboard.rpm"        "thingsboard" "rpm"
copy_and_rename "application/target/thingsboard-windows.zip" "thingsboard-windows" "zip"

# 2) All integrations, except “executor”
for dir in integration/*/; do
  name=$(basename "$dir")
  [[ "$name" == "executor" ]] && continue
  target="$dir/target"
  # expected artifact prefix: tb-<name>-integration
  prefix="tb-${name}-integration"
  copy_and_rename "${target}/${prefix}.deb"            "${prefix}" "deb"
  copy_and_rename "${target}/${prefix}.rpm"            "${prefix}" "rpm"
done

# 3) Upload to S3
echo "Uploading all files in $TMP_DIR to $S3_BUCKET ..."
aws s3 sync "$TMP_DIR" "$S3_BUCKET" --no-progress

echo "Done! Artifacts for release $RELEASE are now in $S3_BUCKET."
