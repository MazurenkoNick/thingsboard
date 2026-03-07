#!/usr/bin/env bash
#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of ThingsBoard, Inc. and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to ThingsBoard, Inc.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
#
# Dissemination of this information or reproduction of this material is strictly forbidden
# unless prior written permission is obtained from COMPANY.
#
# Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
# managers or contractors who have executed Confidentiality and Non-disclosure agreements
# explicitly covering such access.
#
# The copyright notice above does not evidence any actual or intended publication
# or disclosure  of  this source code, which includes
# information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
# ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
# OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
# THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
# AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
# THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
# DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
# OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
#

set -euo pipefail

# === Configuration ===
TMP_DIR="/tmp/tb-release"
RELEASE="4.2.2pe"
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

# 3) Web report packages
copy_and_rename "msa/web-report/target/tb-web-report.deb"        "tb-web-report" "deb"
copy_and_rename "msa/web-report/target/tb-web-report.rpm"        "tb-web-report" "rpm"
copy_and_rename "msa/web-report/target/tb-web-report-windows.zip" "tb-web-report-windows" "zip"

# 4) Upload to S3
echo "Uploading all files in $TMP_DIR to $S3_BUCKET ..."
aws s3 sync "$TMP_DIR" "$S3_BUCKET" --no-progress

echo "Done! Artifacts for release $RELEASE are now in $S3_BUCKET."
