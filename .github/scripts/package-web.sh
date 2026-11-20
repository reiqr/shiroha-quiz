#!/usr/bin/env bash
# 打包 Web 版发布 ZIP（与官方 Release 一致：默认不含约 88MB 的离线扩展资源）。
#
# 用法：bash .github/scripts/package-web.sh <web 版本号> [输出目录]
# 环境变量：INCLUDE_OFFLINE_LIBS=true 时把 PDF.js 增强资源 / MathJax / Tesseract 一并打包。
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${1:?用法: package-web.sh <web 版本号> [输出目录]}"
OUT_DIR="${2:-${ROOT_DIR}/dist}"
INCLUDE_OFFLINE_LIBS="${INCLUDE_OFFLINE_LIBS:-false}"

STAGE_DIR="$(mktemp -d)"
trap 'rm -rf "${STAGE_DIR}"' EXIT

cp -a "${ROOT_DIR}/apps/web/." "${STAGE_DIR}/"

# 开发 / 测试文件不进发布包
rm -rf "${STAGE_DIR}/tests"

# 约 88MB 的离线扩展资源，按 README 约定默认不打包，用户可在 libs/ 下用脚本按需下载
if [[ "${INCLUDE_OFFLINE_LIBS}" != "true" ]]; then
  rm -rf "${STAGE_DIR}/libs/mathjax" \
         "${STAGE_DIR}/libs/ocr" \
         "${STAGE_DIR}/libs/cmaps" \
         "${STAGE_DIR}/libs/iccs" \
         "${STAGE_DIR}/libs/image_decoders" \
         "${STAGE_DIR}/libs/standard_fonts" \
         "${STAGE_DIR}/libs/wasm"
fi

mkdir -p "${OUT_DIR}"
ZIP_NAME="Shiroha-Quiz-v${VERSION}-web.zip"
rm -f "${OUT_DIR}/${ZIP_NAME}"
(cd "${STAGE_DIR}" && zip -r -q -X "${OUT_DIR}/${ZIP_NAME}" .)

echo "已生成 ${OUT_DIR}/${ZIP_NAME}"
ls -lh "${OUT_DIR}/${ZIP_NAME}"
