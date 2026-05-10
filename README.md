# DiaryMind

DiaryMind 是一个 Android 日记应用。它用于收集每天的碎片记录，并通过 OpenAI-compatible AI 接口生成结构化日记，同时给出 PERMA 积极心理学五维评分和建议。生成结果可导出为 Markdown，适合后续同步到 Obsidian 等知识库。

## 核心功能

- 碎片记录：快速记录当天想法、事件、情绪和待整理内容。
- AI 生成日记：将碎片整理为完整日记正文。
- PERMA 评分：从积极情绪、投入、关系、意义、成就五个维度生成心理状态评估。
- 多模型配置：支持 DeepSeek、OpenAI、阿里云百炼和自定义 OpenAI-compatible API。
- API Key 加密存储：使用 `EncryptedSharedPreferences` + Android Keystore 保存密钥。
- 历史日记：按日期查看过往生成内容。
- Markdown 导出/分享：导出 `.md` 文件，便于归档。
- 定时生成：通过 WorkManager 每日定时尝试生成日记。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- Retrofit + OkHttp
- WorkManager
- Android Security Crypto
- JUnit + MockK + kotlinx-coroutines-test

## 项目结构

```text
app/src/main/java/com/diarymind/
├── data/
│   ├── local/          # Room DB、DAO、Migration
│   ├── remote/         # Retrofit API、动态 Base URL
│   └── repository/     # 数据仓库
├── domain/
│   ├── model/          # Fragment、DiaryEntry、PermaScore 等模型
│   └── usecase/        # AI 处理、流水线编排、Markdown 导出
├── ui/
│   ├── screens/        # Compose 页面
│   ├── components/     # UI 组件
│   ├── theme/          # 主题
│   └── viewmodel/      # DiaryViewModel
├── worker/             # 每日生成任务
├── util/               # 加密配置、偏好设置
└── MainActivity.kt     # 导航入口
```

## 数据流

```text
用户碎片
  -> Room(Fragment)
  -> PipelineOrchestrator
  -> AI 预处理
  -> AI 生成日记
  -> AI 生成 PERMA 评分
  -> Room(DiaryEntry + PermaScore + CrossRef)
  -> MarkdownExporter
```

Room 是主数据源；Markdown 文件是导出结果，不作为主数据源反向同步。

## 环境要求

- Android Studio
- JDK 17
- Android SDK
- Gradle Wrapper 已包含在项目中
- 最低 Android 版本：API 26
- 编译 SDK：API 34

## 构建

```bash
./gradlew :app:assembleDebug
```

生成 APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接设备：

```bash
./gradlew :app:installDebug
```

或使用 adb：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 测试

运行单元测试：

```bash
./gradlew test
```

编译 Android instrumentation test APK：

```bash
./gradlew :app:assembleDebugAndroidTest
```

连接真机或模拟器后运行 instrumentation tests：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## AI 配置

打开应用后进入「设置」，配置：

- 供应商：DeepSeek / OpenAI / 阿里云百炼 / 自定义
- Base URL
- 模型 ID
- API Key
- Temperature
- Max Tokens

常见示例：

```text
DeepSeek: https://api.deepseek.com/             deepseek-chat
OpenAI:   https://api.openai.com/v1/            gpt-4o
百炼:     https://dashscope.aliyuncs.com/compatible-mode/v1/  qwen-max
```

应用会在生成日记时读取最新配置，无需重启。

## 隐私说明

- API Key 存储在设备本地加密偏好设置中。
- 日记生成会将碎片内容发送到你配置的 AI 服务商。
- 首次生成前需要隐私授权。
- 设置页提供「清除所有 API Key 与隐私授权」入口。

## 数据库

Room 数据库版本：`2`

主要表：

- `fragments`：碎片记录
- `diary_entries`：生成日记
- `perma_scores`：PERMA 评分
- `fragment_diary_cross_ref`：碎片和日记多对多关联

迁移测试位于：

```text
app/src/androidTest/java/com/diarymind/data/local/DiaryDatabaseMigrationTest.kt
```

Room schema 位于：

```text
app/schemas/
```

## 开发命令

```bash
# 清理
./gradlew clean

# 编译 debug APK
./gradlew :app:assembleDebug

# 运行全部单元测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.diarymind.domain.usecase.PipelineOrchestratorTest"

# 查看连接设备
adb devices
```

## 当前状态

项目已具备基础日记生成闭环：

- 碎片采集
- AI 生成日记
- PERMA 评估
- Markdown 导出
- 多供应商配置
- Room 迁移
- 单元测试覆盖核心流水线

后续可继续完善：

- 真机端到端测试
- UI 细节 polish
- 更完整的离线/失败重试策略
- Obsidian Vault 同步
- 更多统计图表
