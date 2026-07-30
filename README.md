# 主次越南语翻译器 · 工作区总览

> anhem — 中越双语 AI 翻译器，支持 8 种身份语气、灵动岛通知  
> 驱动引擎：DeepSeek API | 作者：fayypp | 所在地：河内，越南

---

## 📂 目录结构速查

| 目录 | 用途 | 状态 |
|------|------|------|
| [app/](app/) | 📱 **主应用** — v4.0 PWA 灵动岛版，当前使用的翻译器 | ✅ 活跃 |
| [docs/](docs/) | 📝 **文档报告** — 使用说明、Prompt 规范、审查报告 | ✅ 活跃 |
| [archive/](archive/) | 📦 **历史归档** — v0.8/v3.0 旧版 + 备份文件 | 📥 归档 |
| [mobile/](mobile/) | 📱 **移动端项目** — Android 原生 + PWA 独立版 | 🔧 开发中 |
| [skills/](skills/) | 🧠 **AI 技能** — Craft Agent 子代理驱动开发技能套件 | 🤖 系统 |
| [sessions/](sessions/) | 💬 **会话记录** — Craft Agent 对话历史 | 🤖 系统 |

---

## 🤖 Craft Agent 系统目录

以下目录由 Craft Agent 自动管理：

| 目录/文件 | 说明 |
|-----------|------|
| `config.json` | 工作区配置（名称、权限模式、MCP 服务） |
| `events.jsonl` | 事件日志（~2.4MB） |
| `views.json` | 视图配置 |
| `labels/` | 标签定义（Development / Content / Priority） |
| `statuses/` | 状态定义（Backlog → Todo → Review → Done） |
| `sources/` | 外部数据源配置（当前为空） |
| `projects/` | 项目配置（当前为空） |
| `.claude/` | Claude IDE 启动配置 |
| `.claude-plugin/` | Claude 插件配置 |
| `.codegraph/` | 代码关系图数据库 |
| `.hermes/` | Hermes 代理计划 & iOS 补丁 |
| `.omo/` | Omo 代理会话 & 草稿 & 计划 |

---

## 🚀 快速开始

1. 打开 `app/index-v4.0-PWA灵动岛.html`
2. 首次使用需配置 DeepSeek API Key（platform.deepseek.com）
3. 详细说明见 `docs/使用说明.txt`

---

## 🔗 关键文件索引

| 要找什么 | 在这里 |
|----------|--------|
| 用户使用手册 | [docs/使用说明.txt](docs/使用说明.txt) |
| 翻译 Prompt 规范 | [docs/翻译提示词-skill.md](docs/翻译提示词-skill.md) |
| 项目创意规划 | [docs/IDEA.md](docs/IDEA.md) |
| Prompt 质量报告 | [docs/Prompt工程深度扫描报告.md](docs/Prompt工程深度扫描报告.md) |
| UI/UX 审查报告 | [docs/UI-UX审查报告.md](docs/UI-UX审查报告.md) |
| 旧版页面 | [archive/](archive/) |
| Android 源码 | [mobile/android/](mobile/android/) |
