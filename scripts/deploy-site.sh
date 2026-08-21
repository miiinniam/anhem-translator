#!/bin/bash
# ============================================================
# anhem 官网部署脚本 — deploy-site.sh
# 用法: ./scripts/deploy-site.sh [--prod]
# 作用:
#   1. 部署 site/ 到 Vercel（默认 preview，加 --prod 部署生产）
#   2. 同步 alias anhem-app.vercel.app → 最新部署（防止旧版复发）
# 注意: Vercel alias 是静态绑定，不自动跟随新 deployment，
#       每次部署后必须执行本脚本的 alias set 步骤。
# ============================================================
set -e
cd "$(dirname "$0")/../site"

PROD_FLAG=""
if [ "$1" = "--prod" ]; then
  PROD_FLAG="--prod"
  echo "=== 部署 production + 同步 alias ==="
else
  echo "=== 部署 preview + 同步 alias ==="
fi

# 1. 部署
echo "--- Vercel 部署 $PROD_FLAG ---"
DEPLOY_URL=$(npx vercel deploy $PROD_FLAG --confirm 2>&1 | grep -oE "https://[a-z0-9-]+\.vercel\.app" | head -1)
if [ -z "$DEPLOY_URL" ]; then
  echo "❌ 部署失败，未获取到部署 URL"
  exit 1
fi
echo "✅ 部署完成: $DEPLOY_URL"

# 2. 同步 alias（静态绑定 → 指向最新部署）
echo "--- 同步 alias anhem-app.vercel.app ---"
npx vercel alias set "$DEPLOY_URL" anhem-app.vercel.app 2>&1 | tail -2

echo ""
echo "=========================================="
echo "✅ 官网已更新: https://anhem-app.vercel.app"
echo "=========================================="