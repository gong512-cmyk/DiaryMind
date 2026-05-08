# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

DiaryMind is an Android app that collects daily text fragments and uses AI (DeepSeek API) to generate diary entries with PERMA psychological scoring. Output is Markdown compatible with Obsidian.

**Key design doc:** `docs/superpowers/specs/2026-05-06-diary-app-design.md` (full product spec with autoplan review decisions).
**Deferred items:** `TODOS.md` (items deferred from CEO/Design/Eng reviews).

## Build & Development Commands

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.diarymind.domain.usecase.PipelineOrchestratorTest"

# Run a single test method
./gradlew test --tests "PipelineOrchestratorTest.executePipeline emits running states then success"

# Install debug APK
./gradlew installDebug

# Clean build
./gradlew clean
```

**Requirements:** Android SDK (path in `local.properties`), JDK 17, Kotlin 1.9.20, Android Gradle Plugin 8.2.0.

## Architecture

### Layer Structure

```
UI (Compose Screens)
  ↕
ViewModel (DiaryViewModel — StateFlow, Hilt-injected)
  ↕
Domain (Use Cases)
  ├── PipelineOrchestrator — state machine (Idle → Preprocessed → Generated → Assessed → Completed)
  ├── ExternalAPIProcessor — DeepSeek API calls (preprocess, generateDiary, assessPERMA)
  └── MarkdownExporter — writes .md files to app filesDir/diaries/
  ↕
Data Layer
  ├── Room DB (single source of truth)
  │   ├── FragmentDao, DiaryDao, PermaDao, FragmentDiaryCrossRefDao
  │   └── DiaryRepository (singleton, aggregates all DAOs)
  ├── Retrofit — DeepSeekApi (base URL: https://api.deepseek.com/)
  └── File System — .md export only, not a source of truth
```

### Key Architectural Decisions

- **Room DB is the single source of truth.** `.md` files are export-only; there is no bidirectional Obsidian sync.
- **Fragment-Diary relationship is many-to-many** via `FragmentDiaryCrossRef`. Fragments are linked to diaries after generation.
- **Pipeline state machine** tracks progress per fragment (`PipelineStep` enum). On failure, fragments are marked `FAILED`; there is no automatic retry.
- **AI backend is DeepSeek API only for v1.0.** ML Kit generative pipeline was removed (ML Kit cannot generate coherent text). Gemini Nano is deferred.
- **API keys stored in `EncryptedSharedPreferences`** with Android Keystore (`AES256_GCM`). See `util/EncryptedPrefs.kt`.
- **Daily diary generation** is scheduled via WorkManager (`DiaryWorkScheduler`) on app startup. The worker (`DiaryGenerationWorker`) runs at 21:00 daily, requires privacy consent + API key, and posts a notification on completion/failure.
- **Deep link scheme:** `diarymind://diaryDetail/{diaryId}` (registered in `MainActivity` manifest).

### Navigation

Compose NavHost in `MainActivity.kt`:
- `home` → `capture` → `diaryList` → `diaryDetail/{diaryId}` → `settings`

### Data Models

- **Fragment** — user input text/voice fragments; `pipelineStep` tracks pipeline progress.
- **DiaryEntry** — generated diary; `isPaginated` + `totalPages` for >5000 char entries.
- **PermaScore** — 5-dimension psychological scoring + AI review + suggestions; 1:1 with DiaryEntry.
- **FragmentDiaryCrossRef** — many-to-many join table.

### Testing

- Unit tests use MockK + Turbine + kotlinx-coroutines-test.
- Room testing dependency included but no migration tests exist yet.
- `PipelineOrchestratorTest` covers happy path, AI failure, and title extraction.

## Important Implementation Notes

- `ExternalAPIProcessor` parses JSON responses from DeepSeek manually with Gson. Malformed JSON falls back to default `PermaScoreResult` values.
- `MarkdownExporter` writes to `context.filesDir/diaries/`. Filename format: `YYYY-MM-DD-{safeTitle}.md`.
- `MainActivity` seeds a demo diary (id=2) on first launch for development.
- The app uses Aliyun Maven mirrors (`settings.gradle.kts`) for dependency resolution in China.
