package ek.osnb.demo.coffeeshop.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@EnableConfigurationProperties(LlmConfig.class)
public class AiClientService {
    private final RestClient restClient;
    private final String endpoint;
    private final String model;

    public AiClientService(RestClient.Builder builder, LlmConfig llmConfig) {
        this.restClient = builder.baseUrl(llmConfig.baseUrl())
                .defaultHeader("Authorization", "Bearer " + llmConfig.apiKey())
                .build();
        this.endpoint = llmConfig.endpoint();
        this.model = llmConfig.model();
    }

    private AiResponse sendRequest(AiRequest request) {
        return restClient.post()
                .uri(this.endpoint)
                .body(request)
                .retrieve()
                .body(AiResponse.class);
    }

    public String chat(String prompt) {
        AiRequest request = new AiRequest(this.model, List.of(new AiRequest.Message("user", prompt)), 100);
        AiResponse response = sendRequest(request);
        return response.choices().stream().findFirst()
                .map(choice -> choice.message().content())
                .orElse("No response from the model.");
    }
}
