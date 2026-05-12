package com.diarymind.domain.model

enum class EditLevel(
    val displayName: String,
    val systemPrompt: String,
    val userPromptPrefix: String
) {
    TYPO_ONLY(
        "仅纠错",
        "你是一位严谨的文字校对助手。只做最必要修正，绝不改写。",
        "请按原始顺序拼接以下碎片。仅修正错别字、标点错误、明显语气词重复。" +
                "不改原句结构，不加过渡句，不总结，不扩写。直接输出拼接文本。"
    ),
    GENTLE_TIDY(
        "去噪整理",
        "你是一位细致的编辑，擅长在不改变原意的前提下提升可读性。",
        "请整理以下碎片。你可以：1) 修正错别字标点 2) 删除明显口癖和重复。" +
                "禁止扩充内容、添加观点、编造信息。直接输出整理文本。"
    ),
    NATURAL_FLOW(
        "自然串联",
        "你是一位善于倾听和表达的朋友，能将零散记录串联成自然流畅的文字。",
        "请将以下碎片串联成一篇连贯日记，忠实保留原意。\n" +
                "要求：\n" +
                "1. 保留原始情感色彩和所有关键信息\n" +
                "2. 仅在必要处添加过渡使行文流畅\n" +
                "3. 使用第一人称，语气自然\n" +
                "4. 不编造碎片中没有的信息\n" +
                "5. 总字数控制在碎片总字数的 80%-150%"
    );

    companion object {
        fun fromKey(key: String): EditLevel =
            entries.find { it.name == key } ?: TYPO_ONLY

        const val PREFS_KEY = "edit_level"
    }
}

enum class ReviewDepth(
    val displayName: String,
    val systemPrompt: String,
    val reviewPrompt: String,
    val maxReviewChars: Int
) {
    BRIEF(
        "简洁",
        "你是一位积极心理学教练，擅长用寥寥数语点出日常记录的闪光点。",
        "请用100字以内给出今日点评，只需点出一个值得关注的亮点。不需要给建议。",
        100
    ),
    STANDARD(
        "标准",
        "你是一位专业的积极心理学教练，善于从日常记录中发现亮点并给出建设性反馈。",
        "请给出300字以内的综合点评和3条明日建议。引用今日记录中的具体细节，用温暖鼓励的语气。",
        300
    ),
    DEEP(
        "深度",
        "你是一位资深积极心理学教练，擅长通过深度对话式反馈帮助人获得认知成长。",
        "请给出500字以内的教练式反馈：\n" +
                "1. 今日记录中的闪光点（引用原文细节）\n" +
                "2. 值得深入思考和反思的部分\n" +
                "3. 3-5条可操作的明日行动建议\n" +
                "语气温暖而真诚，像一位了解你的导师。",
        500
    );

    companion object {
        fun fromKey(key: String): ReviewDepth =
            entries.find { it.name == key } ?: STANDARD

        const val PREFS_KEY = "review_depth"
    }
}

const val CUSTOM_PROMPT_KEY = "custom_generation_prompt"
