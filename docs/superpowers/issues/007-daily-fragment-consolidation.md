# Issue 007: 每日碎片应整合为单篇日记，支持覆盖同天已有日记

## 问题描述

当前行为：每次点击"生成今日日记"，系统会将当日未完成的碎片生成一篇新日记，但没有处理"同一天已存在日记"的情况。这会导致：

1. 一天内多次生成时，会产生多篇同一天的日记，而非整合覆盖。
2. 后添加的碎片无法与之前碎片合并生成更完整的日记。
3. 用户不清楚已有日记会被如何处理。

## 期望行为

### 核心规则
- **每日仅一篇日记**：同一自然日（按碎片最早创建日期）的所有碎片，最终只整合成一篇日记。
- **覆盖机制**：如果当天已存在日记，后续生成应基于当天**所有碎片**（包括之前已完成的和新增的）重新生成，并覆盖旧的日记内容。
- **手动生成覆盖提醒**：用户在 HomeScreen 手动点击"生成今日日记"时，如果当天已有日记，应弹出确认对话框提示"当天已有日记，重新生成将覆盖旧内容，是否继续？"
- **定时生成静默覆盖**：WorkManager 定时任务（21:00 自动生成）直接覆盖，无需弹窗提醒。

### 覆盖时的数据清理
- 删除旧的 `DiaryEntry`
- 删除旧的 `PermaScore`
- 删除旧的 `FragmentDiaryCrossRef` 关联
- 重新建立碎片与新日记的关联
- 旧的 Markdown 文件保留（文件名带时间戳），或生成新文件覆盖

## 影响范围

- `ui/screens/HomeScreen.kt` — 添加"当天已有日记"确认对话框
- `ui/viewmodel/DiaryViewModel.kt` — `generateDiary()` 需要支持 `forceOverwrite` 参数
- `domain/usecase/PipelineOrchestrator.kt` — 覆盖时需要先删除旧日记及相关数据
- `data/repository/DiaryRepository.kt` — 添加删除日记级联数据的方法
- `worker/DiaryGenerationWorker.kt` — 定时任务调用时传 `forceOverwrite = true`
- `data/local/DiaryDao.kt` / `PermaDao.kt` / `FragmentDiaryCrossRefDao.kt` — 可能需要级联删除

## 验收标准

- [ ] 同一天多次生成日记只会保留一篇（最新）
- [ ] 手动生成时，若当天已有日记，弹出覆盖确认对话框
- [ ] 定时生成时，直接静默覆盖旧日记
- [ ] 覆盖后，旧日记的 PERMA 评分也被更新
- [ ] 所有当天碎片（无论之前是否已 completed）都参与重新生成
- [ ] 碎片编辑后重新生成，能正确整合修改后的内容
