#!/usr/bin/env bash
# Build the frontend and sync the output into the backend's static resources,
# so the next `mvn package` produces a jar serving the current UI.
#
# Usage: bash tools/sync-frontend.sh   (from the repo root)
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[1/3] Building frontend (vue-tsc + vite)..."
(cd point-frontend && npm run build)

echo "[2/3] Syncing dist into point-boot static resources..."
rm -f point-boot/src/main/resources/static/assets/*
cp point-frontend/dist/assets/* point-boot/src/main/resources/static/assets/
cp point-frontend/dist/index.html point-boot/src/main/resources/static/index.html

echo "[3/3] Done. Rebuild the backend jar with: mvn package -pl point-boot -am"
