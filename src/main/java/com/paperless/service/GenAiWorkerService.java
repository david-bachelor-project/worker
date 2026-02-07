package com.paperless.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Service
public class GenAiWorkerService {

    private final WebClient webClient;

    public GenAiWorkerService(WebClient.Builder builder,
            @Value("${paperless.genai.url}") String genAiUrl
    ) {
        this.webClient = builder
                .baseUrl(genAiUrl)
                .build();
    }
    public String summarize(String text) {
        String prompt = "Summarize the following text in 3 sentences:\n\n" + text;

        OllamaRequest request = new OllamaRequest("mistral", prompt);

        OllamaResponse response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .block();

        return response != null ? response.getResponse() : "";
    }

    static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream = false;

        public OllamaRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
        public boolean isStream() { return stream; }
    }

    static class OllamaResponse {
        private String response;
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }
}
