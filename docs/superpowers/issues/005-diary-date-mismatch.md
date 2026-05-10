# Issue 005: 日记文件名/标题与碎片实际日期不一致

## 描述
当前日记生成时，`date` 字段固定使用 `LocalDate.now()`（即生成时刻的日期），而非碎片实际记录的日期。当用户跨天生成日记时，文件名和标题日期会与碎片内容的真实日期不符。

例如：
- 5 月 8 日 23:00 记录了碎片
- 5 月 9 日 00:30 点击"生成日记"
- 生成的日记文件名为 `2026-05-09-xxx.md`，标题为 `2026-05-09 xxx`
- 但日记内容描述的是 5 月 8 日的事情

## 根因分析

`PipelineOrchestrator.executePipeline()` 第 41 行：
```kotlin
val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
```

日记的 `date` 字段完全取决于用户点击"生成"按钮的时刻，与碎片的 `createdAt` 无关。这导致：
1. **文件名日期错误**：`MarkdownExporter.buildFileName()` 使用 `diary.date` 作为前缀
2. **标题日期错误**：`extractTitle()` 将 `date` 拼接在标题前
3. **前端 Matter 错误**：Markdown 导出中的 `date:` 字段同样使用 `diary.date`

## 期望行为

日记的日期应当反映碎片所记录的**实际日期**，而非生成时刻。建议按以下优先级确定：
1. 取所有参与生成的碎片中最早的 `createdAt` 对应的日期
2. 若碎片跨天，以最早日期为准（因为日记是对过去记录的整理）

## 影响范围

- `domain/usecase/PipelineOrchestrator.kt` — `executePipeline()` 的日期计算逻辑
- `domain/usecase/MarkdownExporter.kt` — 文件名和 front matter 构造（随上游修复自动生效）

## 修复方案

1. 在 `executePipeline()` 中，从 `fragments` 列表提取最早 `createdAt` 的时间戳
2. 将时间戳转换为本地时区的 `LocalDate`，作为日记的 `date` 字段
3. `extractTitle()` 和 `MarkdownExporter` 继续使用 `diary.date`，无需改动

```kotlin
val diaryDate = fragments.minOfOrNull { it.createdAt }
    ?.let { timestamp ->
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    ?: LocalDate.now()
val dateStr = diaryDate.format(DateTimeFormatter.ISO_DATE)
```

## 验收标准

- [x] 使用昨天记录的碎片在今天生成日记时，文件名和标题显示昨天的日期
- [x] 多天的碎片混合生成时，以最早碎片的日期为准
- [x] 空碎片列表时回退到 `LocalDate.now()`（防御性处理）
- [x] 现有单元测试通过
