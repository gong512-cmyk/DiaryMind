# Issue 002: 支持多供应商模型自定义 & 提升 API Key 存储安全性

## 背景
当前应用仅支持 DeepSeek 单一供应商，且 API Key 存储方式对用户而言透明度不足。用户希望拥有更大的自主权：自主选择供应商、模型和输入自己的 API Key。

## 需求拆分

### 1. 多供应商模型自定义
**现状**：`ExternalAPIProcessor` 里硬编码了 DeepSeek 的 base URL、model (`deepseek-chat`)、temperature、max_tokens 和系统提示词。

**期望**：
- 在**设置页**提供可配置项：
  - **供应商选择**：DeepSeek / OpenAI / 阿里云百炼 / 自定义（OpenAI-compatible API）
  - **Base URL**：根据供应商自动填充，支持手动修改（兼容自定义代理或私有部署）
  - **模型 ID**：可手动输入，如 `deepseek-chat`、`gpt-4o`、`qwen-max` 等
  - **API Key**：对应供应商的 Key
- 底层网络层（`DeepSeekRetrofitClient` / `DeepSeekApi`）需要改为**动态 base URL**，或抽象为统一的 `LLMProvider` 接口，不同供应商实现各自的请求/响应转换。
- 提示词和参数（temperature、max_tokens）可根据供应商做默认适配，但允许高级用户覆盖。

### 2. API Key 安全性
**现状**：`EncryptedSharedPreferences` + Android Keystore (`AES256_GCM`) 已做了一层加密存储，但：
- Key 的存储位置、加密方式对用户不透明
- 没有擦除/更换 Key 的快捷入口

**期望**：
- 设置页提供**查看/修改/删除 API Key** 的能力
- 考虑支持生物识别（指纹/面容）解锁后才可查看明文 Key
- 提供一键"清除所有 API Key 与隐私授权"的重置入口
- 在隐私确认对话框中明确告知用户 Key 的存储方式（设备端加密）

## 影响范围
- `ui/screens/SettingsScreen.kt` — 新增/调整配置 UI
- `data/remote/DeepSeekRetrofitClient.kt` / `DeepSeekApi.kt` — 抽象为通用 LLM 接口
- `domain/usecase/ExternalAPIProcessor.kt` — 接收动态配置而非硬编码
- `util/EncryptedPrefs.kt` — 扩展为多 Key 存储
- `di/AppModule.kt` — Hilt 注入需要支持运行时切换 base URL

## 验收标准
- [x] 设置页可配置供应商、Base URL、模型、API Key、Temperature、Max Tokens
- [x] 切换供应商后，日记生成立即使用新模型，无需重启 App（动态 base URL interceptor）
- [x] API Key 在设备上以 AES256_GCM 加密存储（EncryptedSharedPreferences）
- [x] 支持标准的 OpenAI-compatible API 格式（绝大多数国产模型均兼容）
- [ ] 生物识别解锁查看明文 Key（超出当前范围）
- [x] 一键"清除所有 API Key 与隐私授权"
