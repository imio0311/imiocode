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

/**
 * 根据统一运行配置组装对应 Provider 的 LLM 客户端。
 *
 * <p>工厂在此集中创建 HTTP、JSON、SSE 和 Prompt 依赖，使三个 Provider 共享相同的超时、
 * 工具定义与错误映射边界。</p>
 */
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
        // Provider 客户端不跨配置实例复用，避免切换工作区后沿用旧模型、密钥或超时设置。
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
