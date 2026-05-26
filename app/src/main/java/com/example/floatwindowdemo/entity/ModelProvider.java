package com.example.floatwindowdemo.entity;

public class ModelProvider {
    public final String displayName;
    public final String modelId;
    public final String requestUrl;
    public final String apiKey;
    public final boolean extraBodyEnableThinking;

    public ModelProvider(String displayName, String modelId, String requestUrl, String apiKey, boolean extraBodyEnableThinking) {
        this.displayName = displayName;
        this.modelId = modelId;
        this.requestUrl = requestUrl;
        this.apiKey = apiKey;
        this.extraBodyEnableThinking = extraBodyEnableThinking;
    }

}
