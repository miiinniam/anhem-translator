#!/bin/bash
# ============================================================
# anhem 发布脚本 — release.sh
# 用法: ./scripts/release.sh v1.2.1 "fix: 修复xxx"
# 要求:
#   - 先构建好 APK（apk/ 目录）
#   - 先构建好 exe（desktop/dist/ 目录）
#   - git commit 已提交
# ============================================================
set -e

VERSION="$1"
NOTES="${2:-$(date +'自动发布 %Y-%m-%d')}"

if [ -z "$VERSION" ]; then
  echo "❌ 用法: ./scripts/release.sh v1.2.1 \"更新日志内容\""
  echo "  示例: ./scripts/release.sh v1.2.0 \"feat: 新增xx功能\""
  exit 1
fi

echo "=== anhem 发布 $VERSION ==="
cd "$(dirname "$0")/.."

# 1. 检查构建产物
APK=$(ls -t apk/【请安装这个】anhem快译-Android/anhem快译-*.apk 2>/dev/null | head -1)
WIN_SETUP=$(ls -t desktop/dist/anhem*Setup*.exe 2>/dev/null | head -1)
WIN_PORTABLE=$(ls -t desktop/dist/anhem*便携版*.exe 2>/dev/null | head -1)

echo "APK  : ${APK:-未找到}"
echo "安装版: ${WIN_SETUP:-未找到}"
echo "便携版: ${WIN_PORTABLE:-未找到}"

if [ -z "$APK" ] && [ -z "$WIN_SETUP" ]; then
  echo "⚠️  未找到构建产物，请先构建。继续创建空 release…"
fi

# 2. 从 app.js 读取版本号
APP_VER=$(grep -oP 'APP_VERSION\s*=\s*"\K[^"]+' app/app.js || echo "0.0.0")
echo "app.js 版本: $APP_VER"

# 3. 创建 git tag
if git rev-parse "$VERSION" >/dev/null 2>&1; then
  echo "⚠️  tag $VERSION 已存在，将重用"
else
  git tag -a "$VERSION" -m "anhem $VERSION"
  echo "✅ tag $VERSION 已创建"
fi

# 4. 先组织 assets 列表
ASSETS=()
if [ -n "$APK" ]; then ASSETS+=("$APK"); fi
if [ -n "$WIN_SETUP" ]; then ASSETS+=("$WIN_SETUP"); fi
if [ -n "$WIN_PORTABLE" ]; then ASSETS+=("$WIN_PORTABLE"); fi

echo "即将上传 ${#ASSETS[@]} 个文件"
git push origin "$VERSION"

# 5. 用 curl + git credential 创建 release
# 从 git credential helper 获取 token
TOKEN=$(printf "protocol=https\nhost=github.com\npath=/repos/miiinniam/anhem-translator\n\n" | git credential fill 2>/dev/null | grep "^password=" | sed 's/^password=//')

if [ -z "$TOKEN" ]; then
  echo "❌ 无法获取 GitHub Token"
  echo "请运行 gh auth login 或配置 git credential helper"
  exit 1
fi

echo "=== 创建 GitHub Release 并上传（调用 gh_release.py 规避 JSON 转义问题） ==="
# 用 Python 助手创建 release + 上传所有资产（多行/中文 notes 自动转义）
GH_TOKEN="$TOKEN" env -u PYTHONPATH python "$(dirname "$0")/gh_release.py" "$VERSION" "$NOTES" "${ASSETS[@]}"

echo ""
echo "=========================================="
echo "🎉 anhem $VERSION 已发布！"
echo "官网（https://anhem.vercel.app）将在用户刷新后自动显示新版。"
echo "=========================================="