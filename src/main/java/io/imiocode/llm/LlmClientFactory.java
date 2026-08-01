package io.imiocode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.llm.provider.anthropic.AnthropicClient;
import io.imiocode.llm.provider.deepseek.DeepSeekClient;
import io.imiocode.llm.provider.openai.OpenAiClient;
import io.imiocode.llm.transport.HttpClientFactory;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.SseEventReader;
import io.imiocode.prompt.PromptAssembler;
import io.imiocode.prompt.SystemPromptBuilder;
import io.imiocode.tool.ToolRegistry;

import java.net.http.HttpClient;

public final class LlmClientFactory {
    public LlmClient create(AppConfig config) {
        return create(config, new ToolRegistry());
    }

    public LlmClient create(AppConfig config, ToolRegistry tools) {
        PromptAssembler prompts = new PromptAssembler(
                SystemPromptBuilder.defaults(),
                tools);
        return create(config, prompts);
    }

    public LlmClient create(AppConfig config, PromptAssembler prompts) {
        HttpClient httpClient = new HttpClientFactory().create(config);
        ObjectMapper objectMapper = new ObjectMapper();
        SseEventReader eventReader = new SseEventReader();
        HttpErrorMapper errorMapper = new HttpErrorMapper();
        return switch (config.provider()) {
            case OPENAI -> new OpenAiClient(
                    config, httpClient, objectMapper, eventReader, errorMapper, prompts);
            case ANTHROPIC -> new AnthropicClient(
                    config, httpClient, objectMapper, eventReader, errorMapper, prompts);
            case DEEPSEEK -> new DeepSeekClient(
                    config, httpClient, objectMapper, eventReader, errorMapper, prompts);
        };
    }
}
