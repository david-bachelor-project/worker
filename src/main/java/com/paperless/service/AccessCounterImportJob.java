package com.paperless.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.paperless.repository.DocumentRepository;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class AccessCounterImportJob {

    private final DocumentRepository repository;

    public AccessCounterImportJob(DocumentRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 */5 * * * *") // every 5 minutes
    public void run() {
        log.info("AccessCounterImportJob started");

        File folder = new File("/opt/paperless/access-logs/incoming");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".xml"));

        if (files == null || files.length == 0) {
            log.info("No XML files found to process");
            return;
        }

        log.info("Found {} XML file(s) to process", files.length);

        for (File file : files) {
            log.info("Processing file: {}", file.getName());
            processFile(file);
        }
    }

    private void processFile(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document xml = builder.parse(file);

            NodeList nodes = xml.getElementsByTagName("document");

            log.info("Found {} <document> entries in {}", nodes.getLength(), file.getName());

            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);

                Long id = Long.valueOf(element.getAttribute("id"));
                int accesses = Integer.parseInt(element.getAttribute("accesses"));

                log.info("Updating document {} with accessCounter={}", id, accesses);
                updateAccessCounter(id, accesses);
            }

            archiveFile(file);
            log.info("Archived file {}", file.getName());

        } catch (Exception e) {
            log.error("Failed to process file {}: {}", file.getName(), e.getMessage());
            moveToErrorFolder(file);
        }
    }

    private void updateAccessCounter(Long id, int accesses) {
        repository.findById(id).ifPresent(doc -> {
            doc.setAccessCounter(accesses);
            repository.save(doc);
            log.info("Successfully updated document {} to accessCounter={}", id, accesses);
        });
    }

    private void archiveFile(File file) throws IOException {
        Files.move(
            file.toPath(),
            Paths.get("/opt/paperless/access-logs/archive/" + file.getName()),
            StandardCopyOption.REPLACE_EXISTING
        );
    }

    private void moveToErrorFolder(File file) {
        try {
            Files.move(
                file.toPath(),
                Paths.get("/opt/paperless/access-logs/error/" + file.getName()),
                StandardCopyOption.REPLACE_EXISTING
            );
            log.warn("Moved file {} to error folder", file.getName());
        } catch (IOException ignored) {}
    }
}
