#!/bin/bash
# ============================================================
# anhem 官网 alias 同步脚本
# 用法: ./scripts/sync_alias.sh
# 作用: 把 anhem-app.vercel.app 指向最新 Production Ready 部署
# 背景: Vercel alias 是静态绑定, vercel --prod 新部署不会自动跟随,
#        必须手动 alias set, 否则官网停留在旧版。
# ============================================================
set -e
cd "$(dirname "$0")/../site"

echo "=== 查找最新 Production 部署 ==="
# 非 TTY 下 vercel ls 输出纯 URL 列表(最新在前); --prod 过滤生产环境
URL=$(vercel ls site --prod 2>/dev/null | grep -m1 'https://.*\.vercel\.app')
if [ -z "$URL" ]; then
  echo "❌ 未找到 Production Ready 部署, 请先执行 vercel --prod"
  exit 1
fi
echo "最新部署: $URL"

echo "=== 同步 alias ==="
vercel alias set "$URL" anhem-app.vercel.app

echo "=== 验证 ==="
CODE=$(curl -s -o /dev/null -w '%{http_code}' -L "https://anhem-app.vercel.app/")
echo "https://anhem-app.vercel.app/ -> HTTP $CODE"
[ "$CODE" = "200" ] && echo "✅ alias 已同步" || echo "⚠️ 状态码异常, 请检查"
