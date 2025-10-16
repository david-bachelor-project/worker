package com.paperless;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class KafkaConsumer {

    @KafkaListener(topics = "ocr-queue", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        System.out.println("Received message from ocr-queue: " + message);
    }
}
