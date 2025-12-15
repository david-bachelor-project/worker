package com.paperless.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
@Service
public class ElasticService {

    private final WebClient webClient;

    public ElasticService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:9200")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void indexDocument(String id, String ocrText, String summary) {
        Map<String, Object> payload = Map.of(
            "ocrText", ocrText,
            "summary", summary
        );

        webClient.put()
                .uri("/paperless/_doc/{id}", id)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
