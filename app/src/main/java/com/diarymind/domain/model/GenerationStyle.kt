package com.diarymind.domain.model

enum class GenerationStyle(
    val displayName: String,
    val systemPrompt: String,
    val userPromptPrefix: String
) {
    RAW(
        "原文拼贴",
        "你是一位严谨的编辑助手。你的任务是将用户的碎片记录按时间顺序整理成分段文本。",
        "请按时间顺序拼接以下碎片记录，仅做最小必要整理：去除明显重复，保持原句不变。" +
                "禁止添加过渡语句、禁止总结、禁止扩写、禁止改变措辞。"
    ),
    LIGHT(
        "轻度整理",
        "你是一位细致的编辑，擅长去除冗余同时保留原意。",
        "请整理以下碎片记录，去除明显的重复和口癖，保留原句措辞。" +
                "禁止扩写、禁止改变措辞、禁止添加新的观点或过渡语句。"
    ),
    NATURAL(
        "自然润色",
        "你是一位善于倾听和表达的朋友，擅长将零散的想法整理成温暖真诚的文字。",
        "请基于以下碎片记录，生成一篇连贯、温暖、真诚的日记。\n" +
                "要求：\n" +
                "1. 保留原始记录的情感色彩和关键信息\n" +
                "2. 去除重复和冗余，但不过度改写\n" +
                "3. 使用第一人称，语气自然亲切\n" +
                "4. 仅在必要处添加过渡语句使文章流畅\n" +
                "5. 总字数控制在 300-2000 字之间\n" +
                "6. 不要编造碎片中没有的信息"
    ),
    DEEP(
        "深度改写",
        "你是一位富有创造力的写作者，擅长在保留核心信息的基础上重新组织和表达。",
        "请基于以下碎片记录，重新组织并写成一篇流畅的日记。你可以：\n" +
                "1. 调整段落结构，使文章更有层次感\n" +
                "2. 在保留核心信息的前提下，适当丰富表达\n" +
                "3. 使用第一人称，语气真诚有温度\n" +
                "4. 总字数控制在 300-2000 字之间\n" +
                "5. 不要编造碎片中没有的信息"
    );

    companion object {
        fun fromKey(key: String): GenerationStyle =
            entries.find { it.name == key } ?: NATURAL

        const val PREFS_KEY = "generation_style"
        const val CUSTOM_PROMPT_KEY = "custom_generation_prompt"
    }
}
