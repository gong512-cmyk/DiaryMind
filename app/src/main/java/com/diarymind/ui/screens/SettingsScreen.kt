package com.diarymind.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.diarymind.domain.model.GenerationStyle
import com.diarymind.util.LlmConfig
import com.diarymind.util.clearAllApiKeys
import com.diarymind.util.clearCustomPrompt
import com.diarymind.util.getCustomPrompt
import com.diarymind.util.getGenerationStyle
import com.diarymind.util.getLlmConfig
import com.diarymind.util.saveCustomPrompt
import com.diarymind.util.saveGenerationStyle
import com.diarymind.util.saveLlmConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // AI Backend Settings
            Text(
                text = "AI 后端配置",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "支持 DeepSeek / OpenAI / 阿里云百炼及任何 OpenAI-compatible API",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val presets = remember {
                mapOf(
                    "deepseek" to Pair("https://api.deepseek.com/", "deepseek-chat"),
                    "openai" to Pair("https://api.openai.com/v1/", "gpt-4o"),
                    "bailian" to Pair("https://dashscope.aliyuncs.com/compatible-mode/v1/", "qwen-max"),
                    "custom" to Pair("", "")
                )
            }
            val presetNames = remember {
                mapOf(
                    "deepseek" to "DeepSeek",
                    "openai" to "OpenAI",
                    "bailian" to "阿里云百炼",
                    "custom" to "自定义"
                )
            }

            var config by remember {
                mutableStateOf(try { getLlmConfig(context) } catch (_: Exception) { LlmConfig() })
            }

            // Supplier selection
            Column(modifier = Modifier.selectableGroup()) {
                presets.keys.forEach { key ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .selectable(
                                selected = (config.supplier == key),
                                onClick = {
                                    val preset = presets[key]!!
                                    config = config.copy(
                                        supplier = key,
                                        baseUrl = if (config.supplier == key) config.baseUrl else preset.first,
                                        model = if (config.supplier == key) config.model else preset.second
                                    )
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (config.supplier == key),
                            onClick = null
                        )
                        Text(
                            text = presetNames[key] ?: key,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = config.baseUrl,
                onValueChange = { config = config.copy(baseUrl = it) },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = config.model,
                onValueChange = { config = config.copy(model = it) },
                label = { Text("模型 ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = config.apiKey,
                onValueChange = { config = config.copy(apiKey = it) },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = config.temperature.toString(),
                    onValueChange = {
                        config = config.copy(temperature = it.toFloatOrNull() ?: 0.7f)
                    },
                    label = { Text("Temperature") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = config.maxTokens.toString(),
                    onValueChange = {
                        config = config.copy(maxTokens = it.toIntOrNull() ?: 4096)
                    },
                    label = { Text("Max Tokens") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { saveLlmConfig(context, config) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存配置")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    config = LlmConfig()
                    saveLlmConfig(context, config)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复默认")
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.material3.OutlinedButton(
                onClick = { clearAllApiKeys(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清除所有 API Key 与隐私授权")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // Generation Style
            Text(
                text = "生成风格",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "选择日记生成时的润色程度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            var selectedStyle by remember { mutableStateOf(getGenerationStyle(context)) }
            var customPrompt by remember { mutableStateOf(getCustomPrompt(context) ?: "") }
            var isAdvancedMode by remember { mutableStateOf(getCustomPrompt(context) != null) }

            Column(modifier = Modifier.selectableGroup()) {
                GenerationStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .selectable(
                                selected = (selectedStyle == style),
                                onClick = { selectedStyle = style },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedStyle == style),
                            onClick = null
                        )
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced: custom prompt
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isAdvancedMode,
                    onClick = { isAdvancedMode = !isAdvancedMode }
                )
                Text(
                    text = "高级：自定义提示词",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (isAdvancedMode) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("自定义提示词") },
                    placeholder = { Text("使用 {fragments} 作为碎片占位符") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    minLines = 4,
                    maxLines = 6
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        saveGenerationStyle(context, selectedStyle)
                        if (isAdvancedMode && customPrompt.isNotBlank()) {
                            saveCustomPrompt(context, customPrompt)
                        } else {
                            clearCustomPrompt(context)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存风格设置")
                }
                if (isAdvancedMode) {
                    Spacer(modifier = Modifier.height(8.dp).weight(0.1f))
                    Button(
                        onClick = {
                            customPrompt = selectedStyle.userPromptPrefix + "\n\n碎片记录：\n{fragments}"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("恢复默认")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Notice
            Text(
                text = "隐私说明",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "使用外部 API 时，您的日记内容将被发送到您配置的第三方服务器进行处理。" +
                        "API Key 在设备端以 AES256_GCM 加密存储。如需完全离线处理，请等待后续版本的端侧 AI 支持。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // About
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DiaryMind v1.0.0\n基于 PERMA 积极心理学模型的智能日记助手",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}