# Issue 006: 日记详情页缺少复制功能

## 描述
用户在日记详情页无法直接复制日记内容到剪贴板。当前仅提供"分享"按钮，必须通过系统分享对话框间接传递内容，操作路径过长。

## 现状

`DiaryDetailScreen.kt` 的 TopAppBar actions 中只有一个分享按钮：
```kotlin
IconButton(onClick = {
    val file = viewModel.exportDiary(it, permaScore)
    file?.let { f -> shareMarkdown(context, f.absolutePath) }
}) {
    Icon(Icons.Default.Share, contentDescription = "分享")
}
```

用户反馈的场景：
- 想复制日记内容粘贴到聊天软件发送给朋友
- 想复制 PERMA 评分结果记录到其他地方
- 不想生成文件、走系统分享流程，只想一键复制文本

## 期望功能

在日记详情页 TopAppBar 增加"复制"按钮，点击后将日记完整内容（含标题、正文、PERMA 评估）复制到系统剪贴板，并弹出 Toast 提示"已复制"。

### 复制内容格式

```
# {title}

{content}

---
PERMA 评估
积极情绪: {positiveEmotion}/10 | 投入: {engagement}/10 | 关系: {relationships}/10 | 意义: {meaning}/10 | 成就: {accomplishment}/10

AI 评价: {aiReview}

明日建议: {suggestions}
```

## 影响范围

- `ui/screens/DiaryDetailScreen.kt` — 新增复制按钮和剪贴板操作
- `ui/viewmodel/DiaryViewModel.kt` — 可选：若需在 ViewModel 中封装复制逻辑

## 修复方案

1. 在 TopAppBar actions 中增加复制按钮（使用 `Icons.Default.ContentCopy`）
2. 构造纯文本格式的日记内容字符串
3. 使用 `ClipboardManager` 写入剪贴板
4. 显示 Toast "已复制到剪贴板"

```kotlin
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast

// 在 IconButton 中
val copyText = buildString {
    appendLine("# ${diary.title}")
    appendLine()
    appendLine(diary.content)
    appendLine()
    appendLine("---")
    permaScore?.let { p ->
        appendLine("PERMA 评估")
        appendLine("积极情绪: ${p.positiveEmotion}/10 | 投入: ${p.engagement}/10 | 关系: ${p.relationships}/10 | 意义: ${p.meaning}/10 | 成就: ${p.accomplishment}/10")
        appendLine()
        appendLine("AI 评价: ${p.aiReview}")
        appendLine()
        appendLine("明日建议: ${p.suggestions}")
    }
}
val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
clipboard.setPrimaryClip(ClipData.newPlainText("日记", copyText))
Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
```

## 验收标准

- [x] 日记详情页 TopAppBar 出现复制按钮
- [x] 点击后日记完整内容（含 PERMA）被写入系统剪贴板
- [x] 复制成功后显示 Toast 提示
- [x] 复制内容格式整洁，可直接粘贴使用
