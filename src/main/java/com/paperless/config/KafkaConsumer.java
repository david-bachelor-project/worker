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
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final DocumentRepository documentRepository;

    @KafkaListener(topics = "${spring.kafka.consumer.topic}", groupId = "${spring.kafka.consumer.group-id}")
 
    @KafkaListener(topics = "${spring.kafka.consumer.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            String json = message;
            // If payload is a JSON string literal (e.g. "\"{...}\""), unwrap it
            if (json != null && json.trim().startsWith("\"")) {
                // first parse the outer string to get the inner JSON text
                json = objectMapper.readValue(json, String.class);
            }

            Document doc = objectMapper.readValue(json, Document.class);
            log.info("Received document for OCR: id={}, storagePath={}", doc.getId(), doc.getStoragePath());

            byte[] pdfBytes = storageService.getPdf(doc.getStoragePath());
            String extractedText = ocrService.extractText(pdfBytes);

            documentRepository.findById(doc.getId()).ifPresent(entity -> {
                entity.setStatus(DocumentStatus.OCR_DONE);
                documentRepository.save(entity);
                log.info("Updated document {} status to OCR_DONE", entity.getId());
            });

        } catch (Exception e) {
            log.error("Failed to process document message: {}", e.getMessage(), e);
        }
    }
}
