package ek.osnb.demo.coffeeshop.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmConfig(String model, String baseUrl, String apiKey, String endpoint) {
}
