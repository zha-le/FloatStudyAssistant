package com.example.floatwindowdemo.config;

import com.example.floatwindowdemo.entity.ModelProvider;

public class MyConfig {

    private static final String AliyunApiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String VolcApiUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String NewApiUrl = "https://api.btylbt.top/v1/chat/completions";
    private static final String AliyunApiKey = "sk-xxx";
    private static final String VolcApiKey = "8afxxx";
    private static final String NewApiKey = "sk-xxx";

    public static final String[] SYSTEM_PROMPT = {
            "You're a helpful assistant.",
            "假装你是派蒙，以派蒙的口吻与用户交谈",
            "请**参考**高教出版社《习近平新时代中国特色社会主义思想概论》教材内容，进行**流畅简短概括性叙述性**地回答用户发送的试题。**注意**不要使用markdown格式，直接输出单段(或多段)纯文本。"
    };

    public static final ModelProvider[] MODEL_PROVIDER = {
            new ModelProvider("通义千问3-Max","qwen3-max", AliyunApiUrl, AliyunApiKey, false),
            new ModelProvider("DeepSeek-V3.2","deepseek-v3.2", AliyunApiUrl, AliyunApiKey, false),
            new ModelProvider("DeepSeek-V3.2(深度思考)","deepseek-v3.2", AliyunApiUrl, AliyunApiKey, true),
            new ModelProvider("豆包1.6","doubao-seed-1-6-251015",VolcApiUrl,VolcApiKey,false),
            new ModelProvider("Gemini-3","gemini-3-flash-preview",NewApiUrl,NewApiKey,false),
            new ModelProvider("ChatGPT-5.1","gpt-5.1",NewApiUrl,NewApiKey,false),
    };

}
