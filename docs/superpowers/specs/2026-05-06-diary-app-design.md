# DiaryMind — 碎片信息整理日记 APP 设计文档

- **日期:** 2026-05-06
- **状态:** 草案

---

## 1. 项目概述

**DiaryMind** 是一款 Android 原生手机 APP，核心功能是将用户日常的碎片化记录自动整理、整合，基于 PERMA 积极心理学模型进行评估，生成高质量日记。日记以 Markdown 格式输出，兼容 Obsidian 资料库。

### 1.1 产品定位

大众市场的个人效率与自我觉察工具，服务于有记录习惯但缺乏整理精力的普通用户。

### 1.2 核心原则

1. **保留原味** — 去除语气词和明显错误，但不改变原意和情感色彩
2. **PERMA 评估** — 从积极情绪、投入、关系、意义、成就五个维度评价每日生活
3. **诚实反馈** — AI 评价不全是正向的，该提的意见要提

---

## 2. 用户流程

### 第一阶段：碎片收集（全渠道）

| 渠道 | 方式 |
|------|------|
| APP 内快速输入 | 打开 APP → 点击"+" → 输入文字/语音 → 保存 |
| 系统分享扩展 | 任意 APP 分享 → 选择"日记助手" → 自动存入碎片箱 |
| 语音速记 | 桌面小部件 → 一键录音 → 语音转文字 |
| 被动接收 | 定时推送通知提醒记录 |

### 第二阶段：AI 处理管线

```
原始碎片 → 预处理 → 归类合并 → PERMA 评估 → 日记生成 → AI 评价
```

1. **预处理** — 去语气词、纠错、分段
2. **归类合并** — 按主题/时间聚类碎片
3. **PERMA 评估** — 五维打分
4. **日记生成** — 连贯叙事，保留原味
5. **AI 评价** — 正向肯定 + 改进意见 + 明日计划

### 第三阶段：日记产出

- **触发方式:** 每晚 9:00 自动生成 + 用户可随时手动触发
- **文件格式:** Markdown（.md）
- **标题格式:** `YYYY-MM-DD-摘要.md`（含日期总长 ≤ 30 字）
- **字数:** 正文无严格上限，单篇 > 5000 字时自动分页（`xxx-1.md`、`xxx-2.md`）

---

## 3. AI 处理引擎

三级后可选架构，用户可在设置中选择默认后端：

| 等级 | 方案 | 适用场景 | 依赖 |
|------|------|---------|------|
| 🟢 基础 | ML Kit + 规则引擎 | 离线低端设备 | 系统内置 |
| 🔵 进阶 | Gemini Nano | 离线高端设备（Pixel 8+/S24+） | 端侧 LLM |
| 🟣 旗舰 | 外部 API（DeepSeek 等） | 联网时追求最佳质量 | API Key（用户自备）|

所有处理器实现统一接口：

```kotlin
interface DiaryAIProcessor {
    suspend fun preprocess(fragments: List<Fragment>): List<ProcessedFragment>
    suspend fun assessPERMA(text: String): PermaScore
    suspend fun generateDiary(fragments: List<ProcessedFragment>): String
    suspend fun generateReview(permaScore: PermaScore): Review
}
```

---

## 4. 数据模型

### 4.1 Fragment（碎片记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| content | String | 碎片内容文字 |
| type | Enum | text / voice |
| sourceApp | String? | 来源 APP（分享扩展） |
| createdAt | Long | 记录时间戳 |
| isProcessed | Boolean | 是否已被日记纳入处理 |

### 4.2 DiaryEntry（日记条目）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long (PK) | 自增主键 |
| date | String | 日记日期 (YYYY-MM-DD) |
| title | String | ≤ 30 字 |
| content | String | Markdown 正文 |
| wordCount | Int | 正文字数 |
| isPaginated | Boolean | 是否被分页 |
| pageIndex | Int | 页码（0 开始） |
| localPath | String | 本地 .md 文件路径 |
| obsidianPath | String? | Obsidian 路径（可选） |
| createdAt | Long | 生成时间戳 |

### 4.3 PermaScore（PERMA 评估）

| 字段 | 类型 | 说明 |
|------|------|------|
| diaryId | Long (FK) | 关联的日记 ID |
| positiveEmotion | Float | 积极情绪评分 (0-10) |
| engagement | Float | 投入评分 (0-10) |
| relationships | Float | 关系评分 (0-10) |
| meaning | Float | 意义评分 (0-10) |
| accomplishment | Float | 成就评分 (0-10) |
| aiReview | String | AI 综合评语 |
| suggestions | String | 后续计划与建议 |

---

## 5. 日记文件格式（Obsidian 兼容）

### 5.1 文件名

```
YYYY-MM-DD-摘要.md     // 标题总长 ≤ 30 字（含日期部分）
YYYY-MM-DD-摘要-1.md   // 超 5000 字时分页
```

### 5.2 文件内容

```markdown
---
date: 2026-05-06
tags: [diary, perma]
mood_score: 7.5
---

# 2026-05-06 摘要

## 日记正文

...

## PERMA 评估

| 维度 | 评分 | 说明 |
|------|:----:|------|
| 积极情绪 | 8/10 | ... |
| 投入 | 6/10 | ... |
| 关系 | 7/10 | ... |
| 意义 | 9/10 | ... |
| 成就 | 5/10 | ... |

## AI 评价

[正向肯定 + 建设性批评]

## 明日建议

1. ...
2. ...
3. ...
```

---

## 6. 功能模块

### 6.1 碎片收集模块
- 快速文字输入界面
- 语音转文字（集成系统 STT）
- Android Share Sheet 扩展
- 桌面小部件（一键录音/打字）

### 6.2 日记管理模块
- 内置 Markdown 阅读器（渲染 Obsidian 兼容格式）
- 日记列表（时间线/日历两种视图）
- PERMA 趋势图表（日/周/月维度）
- 全文搜索

### 6.3 导出集成模块
- Obsidian Vault 同步（可选，用户配置路径）
- 本地 .md 文件缓存（APP 沙盒）
- 分享为图片/纯文本

### 6.4 设置模块
- AI 后端选择 & API Key 配置
- Obsidian Vault 路径
- 日记生成时间（默认 21:00）
- 标题字数上限（默认 30 字）
- 存储位置管理

---

## 7. 三层次存储策略

| 层次 | 存储内容 | 保存策略 |
|------|---------|---------|
| Room DB | 原始碎片 + PERMA 评分 + 日记元数据 | 始终 |
| 本地 .md 缓存 | APP 沙盒内完整日记文件 | 始终 |
| Obsidian Vault | 用户配置路径下的 .md 文件 | 可选（配置后同步）|

未配置 Obsidian 时，APP 完全自包含工作，所有数据在本地沙盒内。

---

## 8. 技术架构

### 8.1 技术选型

| 层面 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 数据库 | Room |
| 依赖注入 | Hilt |
| 端侧 AI | ML Kit / Gemini Nano SDK |
| 云端 API | Retrofit（仅 DeepSeek 等外部 API） |
| 后台任务 | WorkManager（定时生成 + 备份） |
| Markdown 渲染 | Markwon 或 Compose Markdown |

### 8.2 分层结构

```
UI Layer (Compose)
    ↕
ViewModel Layer
    ↕
Domain Layer (Use Cases)
    ↕
Data Layer
    ├── Room DB
    ├── File System (.md)
    └── ContentProvider (分享扩展)
```

### 8.3 AI 管线设计（策略模式）

```
DiaryAIProcessor 接口
    ├── MLKitProcessor（基础，通用设备）
    ├── GeminiNanoProcessor（进阶，高端设备）
    └── ExternalAPIProcessor（旗舰，DeepSeek 等）
```

---

## 9. MVP 范围

### v1.0 核心功能
- [ ] 碎片收集：APP 内输入 + 语音 + 分享扩展
- [ ] AI 处理：ML Kit 管线（基础版）+ DeepSeek API 管线
- [ ] 日记管理：内置 Markdown 阅读器 + 时间线列表
- [ ] 本地存储：Room DB + .md 文件缓存
- [ ] 基础设置：AI 后端选择、API Key、存储路径
- [ ] PERMA 评估与 AI 评价

### 后续规划
- Gemini Nano 管线（设备覆盖扩大后）
- Obsidian 同步
- PERMA 趋势图表
- 桌面小部件
- 日历视图

---

## 10. 附录：PERMA 模型简介

**PERMA** 由积极心理学之父马丁·塞利格曼提出：

- **Positive Emotion（积极情绪）** — 快乐、感恩、宁静等
- **Engagement（投入）** — 心流状态，沉浸体验
- **Relationships（人际关系）** — 归属感、深度连接
- **Meaning（意义）** — 服务于超越自身的宏大事物
- **Accomplishment（成就）** — 精通、目标达成
