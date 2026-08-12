package io.imiocode.config;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 从工作目录读取严格类型的根 YAML 配置文档。
 *
 * <p>解析错误只向终端暴露字段路径和行列，不回显原始值，避免配置中的凭据进入诊断信息。</p>
 */
final class YamlConfigLoader {
    static final String FILE_NAME = "config.yaml";

    private final ObjectMapper objectMapper;

    YamlConfigLoader() {
        // 禁止字符串到数字或布尔值的宽松转换，使拼写错误在启动阶段明确失败。
        objectMapper = JsonMapper.builder(new YAMLFactory())
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build();
    }

    ConfigDocument load(Path workingDirectory) {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Path configPath = workingDirectory.resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            return ConfigDocument.empty();
        }
        if (!Files.isRegularFile(configPath)) {
            throw new ConfigException("config.yaml 必须是普通文件");
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ConfigDocument document = objectMapper.readValue(reader, ConfigDocument.class);
            if (document == null) {
                return ConfigDocument.empty();
            }
            validateProviders(document);
            return document;
        } catch (JsonProcessingException exception) {
            throw safeParseException(exception);
        } catch (IOException exception) {
            throw new ConfigException("无法读取 config.yaml", exception);
        }
    }

    private static void validateProviders(ConfigDocument document) {
        for (String providerName : document.providers().keySet()) {
            try {
                Provider.parse(providerName);
            } catch (ConfigException exception) {
                throw new ConfigException("config.yaml 的 providers 包含未知厂商");
            }
        }
    }

    private static ConfigException safeParseException(JsonProcessingException exception) {
        String path = mappingPath(exception);
        JsonLocation location = exception.getLocation();
        String position = location == null
                ? ""
                : "（第 " + location.getLineNr() + " 行，第 " + location.getColumnNr() + " 列）";
        return new ConfigException("config.yaml 格式错误" + path + position, exception);
    }

    private static String mappingPath(JsonProcessingException exception) {
        if (!(exception instanceof JsonMappingException mappingException) || mappingException.getPath().isEmpty()) {
            return "";
        }
        StringBuilder path = new StringBuilder("，字段：");
        for (JsonMappingException.Reference reference : mappingException.getPath()) {
            if (reference.getFieldName() != null) {
                if (path.charAt(path.length() - 1) != '：') {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            }
        }
        return path.toString();
    }
}
