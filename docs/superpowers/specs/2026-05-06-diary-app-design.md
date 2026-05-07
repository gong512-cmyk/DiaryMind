<!-- /autoplan restore point: /Users/frank/.gstack/projects/My_test_book/main-autoplan-restore-20260507-220952.md -->
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

---

## /autoplan Review

### CEO DUAL VOICES — CONSENSUS TABLE

| Dimension | Claude | Codex | Consensus |
|-----------|--------|-------|-----------|
| 1. Premises valid? | 2/6 critical flaws | N/A | FLAGGED — see below |
| 2. Right problem to solve? | Yes, but framing too narrow | N/A | Yes — personal data curation is a real need |
| 3. Scope calibration correct? | MVP overloaded | N/A | No — 3 AI tiers + 4 input channels is too much for v1 |
| 4. Alternatives sufficiently explored? | No — missing plugin/keyboard/Notion paths | N/A | No — insufficient alternatives analysis |
| 5. Competitive/market risks covered? | High risk — Day One, Notion, phone OEMs | N/A | High risk — low differentiation moat |
| 6. 6-month trajectory sound? | High regret risk if core assumptions wrong | N/A | Risky — 3 high-confidence regret scenarios |

**Codex status:** [codex-unavailable] — CLI version incompatible with model (gpt-5.5 requires newer Codex). Proceeding with Claude subagent only.

### CEO Review Findings

#### Premise Challenge (0A)

**Right problem, wrong framing.** The core pain point — "I have scattered thoughts but don't have energy to organize them" — is real and widespread. But the plan frames the solution as a "diary app," which limits its ambition. A 10x reframing: **personal data pipeline / digital life curator**. Diary is just the daily output format. Weekly reviews, relationship maintenance nudges, and annual retrospectives are natural extensions.

**Key premise flaws identified:**

| Premise | Status | Rationale |
|---------|--------|-----------|
| Users already have a "fragment recording habit" | **WRONG** | Most users don't record fragments. MVP must include passive collection (notifications, location, photos) |
| PERMA model appeals to general users | **WRONG** | General users don't know PERMA. Five-dimension scoring feels clinical and potentially offensive |
| Users want AI to critique their life | **WRONG** | Diary is deeply personal. Users need control over tone (gentle/direct/off) |
| Markdown/Obsidian compatibility is a differentiator | **MISLEADING** | Obsidian users are a tiny niche. In-app reading experience matters more |
| Users will read 5000-word AI-generated entries | **WRONG** | Attention is scarce. Summary mode should be default, full mode optional |
| ML Kit can generate coherent diary entries | **WRONG** | ML Kit has minimal text generation capability. The "basic tier" may only do keyword extraction |

#### Implementation Alternatives (0C-bis)

**APPROACH A: Minimal Viable (Single Backend)**
- Summary: One AI backend (DeepSeek API), basic text input only, no share extension, no widget
- Effort: M
- Risk: Low
- Pros: Fastest to ship; validates core hypothesis; lower complexity
- Cons: Requires internet; limited input channels; less impressive demo

**APPROACH B: Full Architecture (Current Plan)**
- Summary: 3-tier AI, multiple input channels, Obsidian sync, widgets
- Effort: L/XL
- Risk: Medium
- Pros: Comprehensive; offline support; ecosystem integration
- Cons: Long dev time; high complexity; risk of shipping nothing

**APPROACH C: Hybrid MVP (RECOMMENDED)**
- Summary: Single AI backend + basic input first. Add tiers and channels incrementally based on feedback
- Effort: M
- Risk: Low-Medium
- Pros: Balanced; ships fast then iterates; learns from real users
- Cons: Less impressive v1; delayed offline support

**Recommendation: Approach C** — ship core value fast, then iterate. The 3-tier AI architecture and 4 input channels in the current MVP create excessive complexity before validating the core hypothesis.

#### 6-Month Regret Scenarios

1. **"We built a tool nobody opens"** — If the app depends on users actively recording fragments, DAU will crater. Fix: push-generated content to users via notifications
2. **"PERMA scoring feels like a machine judging me"** — Users recording personal struggles will find numerical ratings alienating. Fix: visual radar charts, story-like commentary, opt-out
3. **"ML Kit tier doesn't work but we spent months on it"** — ML Kit cannot do generative text. The "basic tier" will disappoint. Fix: honest capability assessment; basic tier = keyword extraction only

#### Competitive Risk

- **Direct:** Day One, Notion AI, Obsidian plugins already exist. Feature parity is easy to copy
- **Indirect:** Phone OEMs (Xiaomi, Samsung) already auto-generate photo stories. Adding text is trivial for them
- **Structural:** LLM API costs are dropping fast. Any journal app can add AI features quickly

**Moat strategy:** Data network effect (the more you use it, the better it understands your voice) + privacy-first positioning (on-device processing, data never leaves phone)

#### NOT in Scope (Deferred)

| Item | Rationale |
|------|-----------|
| Gemini Nano pipeline | Device coverage too limited for v1; add when Pixel/S24+ penetration increases |
| Obsidian Vault sync | Niche feature; validate demand before building |
| Desktop widget | Nice-to-have; not essential for core loop validation |
| Calendar view | v1.5+ feature; timeline view is sufficient for MVP |
| PERMA trend charts | Requires data accumulation; add after 30+ days of user data |
| Share Sheet extension | Adds significant Android complexity; defer to v1.2 |

#### What Already Exists

No existing code in repository. Greenfield project.

### Decision Audit Trail

| # | Phase | Decision | Classification | Principle | Rationale |
|---|-------|----------|----------------|-----------|-----------|
| 1 | CEO | Recommend Hybrid MVP over Full Architecture | Taste | P5 (Explicit) + P3 (Pragmatic) | Core hypothesis must be validated before complexity investment |
| 2 | CEO | Defer Obsidian sync, widget, calendar, trend charts | Mechanical | P2 (Boil lakes) + P3 (Pragmatic) | Out of blast radius for core diary generation loop |
| 3 | CEO | Defer share extension | Mechanical | P3 (Pragmatic) | Significant Android complexity for unvalidated channel |
| 4 | CEO | Challenge PERMA as default framework | Taste | P1 (Completeness) | User research needed before committing to academic model |
| 5 | CEO | Flag ML Kit capability gap | Mechanical | P5 (Explicit) | ML Kit cannot do generative text; basic tier scope must shrink |

---

### DESIGN DUAL VOICES — CONSENSUS TABLE

| Dimension | Claude | Codex | Consensus |
|-----------|--------|-------|-----------|
| 1. Information hierarchy sound? | No — no home screen defined | N/A | No — missing home screen, navigation structure |
| 2. Interaction states specified? | No — empty/error/loading all missing | N/A | No — critical state gaps throughout |
| 3. User journey designed? | Partial — missing delight moment and emotional safety | N/A | Partial — emotional arc incomplete |
| 4. Specificity adequate? | No — zero concrete UI descriptions | N/A | No — all descriptions are feature lists, not UI specs |
| 5. Accessibility considered? | Not mentioned | N/A | Not addressed |

**Codex status:** [codex-unavailable]

### Design Review Findings

#### Information Hierarchy

**Critical gap: No home screen defined.** The document describes user flows and feature modules but never specifies what the user sees when they open the app. Is it a fragment input screen? A timeline of past entries? Today's generated diary?

**Recommendation:** Default home screen should be "Today's Summary" — showing fragment count, PERMA radar chart (if enabled), and a generate-diary CTA. Secondary tabs: Timeline and Settings.

#### Missing States (Critical)

| State | Where Missing | Impact |
|-------|---------------|--------|
| Empty state | First open, no fragments, no diary, no PERMA data | User sees blank screen, no guidance |
| Error state | AI pipeline failure at any of 5 steps | User doesn't know what happened or what to do |
| Loading state | Pipeline steps vary from ms to seconds | No progress indication for long operations |
| Network/offline | External API selected but no connection | App behavior undefined |
| Partial success | Some pipeline steps succeed, others fail | User sees incomplete or broken output |
| API key invalid | User enters wrong/expired key | No error handling defined |

#### User Journey & Emotional Arc

The designed emotional arc has a critical gap at step 4 (the "delight moment"):
1. Curiosity (first open) → 2. Ease (quick capture) → 3. Anticipation (waiting for generation) → **4. ??? (reading the diary)** → 5. Reflection (PERMA review)

**Step 4 is undesigned.** If the AI-generated diary is mediocre, users will churn immediately. The document provides no quality benchmark, no examples, no "wow" moment design.

**PERMA emotional risk:** Users recording personal struggles will find numerical ratings (e.g., "Relationships 3/10") alienating and judgmental. The document has no emotional safety design.

**Fixes:**
- Provide 2-3 "fragment → diary" example pairs to define quality bar
- Design notification copy and open animation for diary completion to create delight
- Add "review tone" setting (gentle/direct/off)
- Low-score days use encouraging framing, not cold numbers
- Default to trend visualization over absolute scores

#### Specificity: Zero Concrete UI

Every UI description is a feature list, not a design spec:
- "快速文字输入界面" — fullscreen or bottom sheet? Single-field or multi-field?
- "时间线/日历两种视图" — vertical list or card flow? Monthly or weekly calendar?
- "PERMA 趋势图表" — radar chart, line graph, or bar chart?

**Fix:** Add wireframe descriptions or reference apps for core screens.

#### Ambiguous Decisions That Will Haunt Implementers

1. **Pagination vs. UI abstraction** — Files split at 5000 chars, but users should see "one diary entry." Is pagination a filesystem detail only, or does it affect UI?
2. **Fragment-Diary relationship** — One-to-one or many-to-many? What if a fragment is recorded at 12:01 AM — yesterday's diary or today's?
3. **Pipeline transactionality** — If step 3 fails, do steps 1-2 roll back? Can the pipeline resume after app kill?
4. **Obsidian sync direction** — One-way (app → Obsidian) or bidirectional? What if the user edits the .md file in Obsidian?
5. **Markdown rendering scope** — Obsidian syntax (`[[links]]`, `#tags`, `![[embeds]]`, callouts) — render or display as plain text?

| # | Phase | Decision | Classification | Principle | Rationale |
|---|-------|----------|----------------|-----------|-----------|
| 6 | Design | Defer share extension widget to v1.2+ | Mechanical | P2 (Boil lakes) | Out of core loop blast radius |
| 7 | Design | Require home screen wireframe before implementation | Mechanical | P1 (Completeness) | Critical UI gap blocks development |
| 8 | Design | Require state definitions (empty/error/loading/offline) | Mechanical | P1 (Completeness) | Missing states will produce broken UX |
| 9 | Design | Add "review tone" setting (gentle/direct/off) | Mechanical | P5 (Explicit) | Emotional safety is not optional for diary apps |
| 10 | Design | Fragment-Diary relation = many-to-many with association table | Mechanical | P5 (Explicit) | Current model cannot support re-processing |
| 11 | Design | Pagination is filesystem-only; UI shows unified entry | Mechanical | P5 (Explicit) | User mental model is "one diary per day" |

---

### ENG DUAL VOICES — CONSENSUS TABLE

| Dimension | Claude | Codex | Consensus |
|-----------|--------|-------|-----------|
| 1. Architecture sound? | Partial — coupling issues in data model and AI interface | N/A | Partial — needs Fragment-Diary association table, pipeline state machine |
| 2. Test coverage sufficient? | No — 9 test gaps identified | N/A | No — missing offline, long text, rapid-click, timezone tests |
| 3. Performance risks addressed? | No — WorkManager Doze, 500-fragment batching undefined | N/A | No — batching and background constraints not designed |
| 4. Security threats covered? | 6 issues found | N/A | Partial — API key storage, input validation, privacy consent missing |
| 5. Error paths handled? | No — partial pipeline success, API failure, nil paths undefined | N/A | No — error paths are the biggest gap |
| 6. Deployment risk manageable? | Room migration untested; 3-tier AI creates version matrix | N/A | Risky — complexity before validation |

**Codex status:** [codex-unavailable]

### Engineering Review Findings

#### Architecture Issues

**1. Data model coupling (High)**
`Fragment` uses `isProcessed` boolean for implicit Diary association. No support for re-processing, cross-midnight fragments, or multi-diary inclusion.
**Fix:** Add `FragmentDiaryCrossRef` association table. Remove `isProcessed` from Fragment.

**2. AI processor interface lacks transaction boundaries (High)**
`DiaryAIProcessor` bundles 4 operations. If step 2 fails, step 1 results are lost. No resume capability.
**Fix:** Pipeline state machine with persistent step tracking per fragment.

**3. Three-layer storage lacks consistency protocol (High)**
Room DB, local .md, and Obsidian Vault have no sync contract. External edits to .md files create drift.
**Fix:** Room DB is single source of truth. .md files are export-only. Obsidian sync is one-way (APP → Obsidian).

#### Edge Cases

| Scenario | Risk | Severity |
|----------|------|----------|
| 500 fragments/day | Pipeline blocks for minutes | High |
| WorkManager killed by Doze | Diary never generates | High |
| Cross-midnight fragment | Belongs to wrong date | High |
| API key expired | No fallback, app appears broken | High |
| 10,000-char fragment | Memory pressure, AI truncation | High |
| Rapid-click "generate" | Race condition, duplicate entries | High |
| Storage full | File write fails, Room rolls back | High |
| Timezone/DST change | Date misattribution | Medium |
| System time tampered | Fragment date wrong | Medium |

#### Security

| Issue | Severity | Fix |
|-------|----------|-----|
| API Key stored unencrypted | **High** | Use `EncryptedSharedPreferences` + Android Keystore |
| External API sends diary content off-device without consent | **High** | Mandatory privacy dialog on first external API selection |
| Share extension receives unvalidated input | Medium | Length limit (10K chars), filter invisible control chars |
| Markdown frontmatter injection | Medium | YAML-escape frontmatter fields, sanitize HTML in content |

#### Hidden Complexity

1. **ML Kit cannot generate text** — "Basic tier" is unbuildable as designed. ML Kit = keyword extraction + template fill only.
2. **"Preserve original meaning" vs. AI rewrite** — Fundamental tension. Needs quantifiable quality metrics (keyword retention >80%, sentiment consistency).
3. **Voice widget complexity** — Background recording, foreground service, battery whitelist. Way harder than it looks.
4. **PERMA calibration across backends** — Same diary, different scores from ML Kit/Gemini/DeepSeek. Breaks trend comparability.

#### Architecture ASCII Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                       │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌─────────────────┐  │
│  │ Capture │ │ Timeline │ │ Reader  │ │    Settings     │  │
│  └────┬────┘ └────┬─────┘ └────┬────┘ └─────────────────┘  │
└───────┼───────────┼────────────┼────────────────────────────┘
        │           │            │
        ▼           ▼            ▼
┌─────────────────────────────────────────────────────────────┐
│                  ViewModel Layer                             │
│         (StateFlow, Hilt-injected UseCases)                  │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Domain Layer (Use Cases)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │CaptureFrag  │  │GenerateDiary│  │   ExportDiary       │ │
│  └──────┬──────┘  └──────┬──────┘  └─────────────────────┘ │
│         │                │                                  │
│         ▼                ▼                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              PipelineOrchestrator                       │ │
│  │  (state machine: Idle → Preprocessed → Clustered →     │ │
│  │   Assessed → Generated → Completed | FailedAt{Step})   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Room DB    │  │  File System │  │ ContentProvider  │  │
│  │  (SSoT)      │  │  (.md export)│  │ (share ext v1.2+)│  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│         │
│         ▼
│  ┌────────────────────────────────────────────────────────┐ │
│  │  FragmentDao │ DiaryDao │ PermaDao │ FragmentDiaryXRef │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│              AI Backend (Strategy Pattern)                   │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ ExternalAPIProc  │  │ GeminiNanoProc   │  (v1.5+)       │
│  │ (DeepSeek/etc)   │  │ (on-device)      │                │
│  └──────────────────┘  └──────────────────┘                │
│  NOTE: MLKitProcessor removed — insufficient text gen       │
└─────────────────────────────────────────────────────────────┘
```

#### Test Plan

| Codepath | Test Type | Coverage |
|----------|-----------|----------|
| Fragment capture (text) | Unit + UI | Required |
| Fragment capture (voice) | UI + Integration | Required |
| Pipeline execution (happy path) | Integration | Required |
| Pipeline failure at each step | Integration | **MISSING** |
| Empty state → first diary | UI | **MISSING** |
| Error state (API key invalid) | UI | **MISSING** |
| Offline with external API selected | Integration | **MISSING** |
| 500 fragments batch processing | Performance | **MISSING** |
| Rapid "generate" click debounce | Unit | **MISSING** |
| Room DB migration v1→v2 | Migration | **MISSING** |
| Markdown render (Obsidian syntax) | UI | **MISSING** |
| File I/O (storage full) | Integration | **MISSING** |
| Timezone change | Unit | **MISSING** |
| API key encryption/decryption | Unit | **MISSING** |
| Share extension (unvalidated input) | Integration | **MISSING** |
| Notification (diary ready) | UI | Required |

#### NOT in Scope (Deferred from Eng Review)

| Item | Rationale |
|------|-----------|
| Bidirectional Obsidian sync | Data consistency too complex for v1 |
| ML Kit generative pipeline | Technically infeasible; replaced with keyword extraction only |
| Background voice recording widget | Foreground service + battery whitelist complexity |
| Calendar view | UI complexity; timeline sufficient |
| Cross-device sync | Requires backend infrastructure |
| Automated PERMA calibration across backends | Requires statistically significant dataset |

| # | Phase | Decision | Classification | Principle | Rationale |
|---|-------|----------|----------------|-----------|-----------|
| 12 | Eng | Remove ML Kit generative pipeline | Mechanical | P5 (Explicit) | ML Kit cannot generate coherent text; scope must match capability |
| 13 | Eng | Room DB = single source of truth | Mechanical | P5 (Explicit) | Prevents data drift between 3 storage layers |
| 14 | Eng | Add Fragment-Diary association table | Mechanical | P5 (Explicit) | Current model cannot support re-processing |
| 15 | Eng | Pipeline state machine with persistent steps | Mechanical | P1 (Completeness) | Required for resume after app kill / failure |
| 16 | Eng | API key → EncryptedSharedPreferences | Mechanical | P1 (Completeness) | Security requirement for v1 |
| 17 | Eng | Mandatory privacy dialog for external API | Mechanical | P1 (Completeness) | User consent before data leaves device |
| 18 | Eng | Defer voice widget, share extension | Mechanical | P3 (Pragmatic) | High complexity, low validation value for core loop |

---

### Final Approval Status

**Status:** APPROVED with adjustments
**Date:** 2026-05-07
**Decisions:** 18 total (17 mechanical, 1 taste)

**Approved adjustments:**
1. ML Kit removed from generative pipeline; replaced with keyword extraction only
2. Hybrid MVP approach adopted — single AI backend (DeepSeek API) for v1.0
3. Share extension, widget, Obsidian sync, calendar view, trend charts deferred to v1.2+
4. Fragment-Diary association table added to data model
5. Pipeline state machine with persistent step tracking required
6. Room DB designated as single source of truth
7. API key storage must use EncryptedSharedPreferences + Android Keystore
8. Mandatory privacy consent dialog before external API use
9. Home screen wireframe and all empty/error/loading states must be defined before implementation
10. "Review tone" setting (gentle/direct/off) added to settings

**Next step:** Implementation
| 18 | Eng | Defer voice widget, share extension | Mechanical | P3 (Pragmatic) | High complexity, low validation value for core loop |
