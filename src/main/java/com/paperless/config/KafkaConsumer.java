package com.paperless.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperless.service.*;
import com.paperless.model.Document;
import com.paperless.model.DocumentStatus;
import com.paperless.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final GenAiWorkerService genAiWorkerService;
    private final DocumentRepository documentRepository;
    private final ElasticService elasticService;

    @KafkaListener(topics = "${spring.kafka.consumer.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            String json = message;
            if (json != null && json.trim().startsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }

            Document doc = objectMapper.readValue(json, Document.class);
            log.info("Received document for OCR: id={}, storagePath={}", doc.getId(), doc.getStoragePath());

            byte[] pdfBytes = storageService.getPdf(doc.getStoragePath());
            String extractedText = ocrService.extractText(pdfBytes);

            // Step 1: mark OCR done
            documentRepository.findById(doc.getId()).ifPresent(entity -> {
                entity.setStatus(DocumentStatus.OCR_DONE);
                documentRepository.save(entity);
                log.info("Updated document {} status to OCR_DONE", entity.getId());
            });

            //log.info("Starting AI summary...this will take some time");
            //String summary = genAiWorkerService.summarize(extractedText);
            //log.info("Generated summary for document {}: {}", doc.getId(), summary);

            // Step 3: mark summarized
            /*documentRepository.findById(doc.getId()).ifPresent(entity -> {
                entity.setStatus(DocumentStatus.SUMMARIZED);
                documentRepository.save(entity);
                log.info("Updated document {} status to SUMMARIZED", entity.getId());
            });*/

            // Step 4: index into Elasticsearch
            elasticService.indexDocument(doc.getId().toString(), extractedText);
            log.info("Indexed document {} into Elasticsearch", doc.getId());

        } catch (Exception e) {
            log.error("Failed to process document message: {}", e.getMessage(), e);
        }
    }
}
