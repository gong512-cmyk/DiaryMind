# Issue 008: 设置页保存 API 配置后，HomeScreen 仍提示未配置 API Key

## 问题描述

用户在设置页输入并保存 API Key 后，返回首页点击"生成今日日记"，系统仍提示"请先在设置中配置 DeepSeek API Key"。

用户发现必须先点击"清除所有 API Key 与隐私授权"按钮，再重新输入保存，才能让首页识别到 API Key。但此过程中没有任何提示告知用户原因。

## 根因分析

存储层存在两个不同的 Key：
- **旧版 Key**: `KEY_API_KEY = "deepseek_api_key"` — `getApiKey()` 读取此 Key
- **新版 Key**: `KEY_LLM_API_KEY = "llm_api_key"` — `saveLlmConfig()` 保存到此 Key

`HomeScreen.kt` 在检查 API Key 时调用的是旧版 `getApiKey()`：
```kotlin
getApiKey(context) == null -> viewModel.showError("请先在设置中配置 DeepSeek API Key")
```

而设置页保存配置时调用 `saveLlmConfig()`，API Key 被写入 `KEY_LLM_API_KEY`。因此：
- 新用户首次配置：`KEY_LLM_API_KEY` 有值，`KEY_API_KEY` 为空 → `getApiKey()` 返回 null → 首页报错
- 旧用户升级后：如果先点"清除"，两个 Key 都被清空；再保存时写入新版 Key，但首页仍读旧版 Key → 仍然报错

`getLlmConfig()` 内部有从旧版 Key 回退的逻辑（`?: prefs.getString(KEY_API_KEY, "")`），但 `getApiKey()` 没有从新版的回退。

## 期望行为

- 设置页保存的 API Key，首页应能正确识别。
- 错误提示应更新为通用提示（不再特指 DeepSeek）。
- 保持向后兼容：如果用户只有旧版 Key，也应能正常工作。

## 修复方案

**方案 A（推荐）**：修改 `HomeScreen.kt`，使用 `getLlmConfig(context).apiKey` 替代 `getApiKey(context)` 进行非空检查。

**方案 B**：修改 `getApiKey()`，增加从新版的回退读取：
```kotlin
fun getApiKey(context: Context): String? {
    val prefs = getEncryptedPrefs(context)
    return prefs.getString(KEY_LLM_API_KEY, null)
        ?: prefs.getString(KEY_API_KEY, null)
}
```

建议采用方案 A，逐步淘汰旧版存储 Key。

## 影响范围

- `ui/screens/HomeScreen.kt` — 更新 API Key 检查和错误提示
- `util/EncryptedPrefs.kt` — 可选：更新 `getApiKey()` 增加回退（若选方案 B）

## 验收标准

- [x] 设置页保存 API Key 后，首页能立即识别并允许生成日记
- [x] 未配置 API Key 时的错误提示改为"请先在设置中配置 API Key"
- [x] 旧版存储的 API Key 仍能正常工作（向后兼容）
