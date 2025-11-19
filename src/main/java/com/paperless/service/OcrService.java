package com.paperless.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.paperless.service.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class OcrService {

    @Value("${tesseract.data-path:/usr/share/tessdata}")
    private String tessDataPath;

    @Value("${tesseract.language:eng}")
    private String language;

    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            String plainText = extractTextWithPdfBox(document);
            if (plainText != null && plainText.trim().length() > 50) {
                return plainText;
            }

            return ocrPdfByRendering(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PDF for OCR", e);
        }
    }

    private String extractTextWithPdfBox(PDDocument document) {
        try {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text;
        } catch (IOException e) {
            return "";
        }
    }

    private String ocrPdfByRendering(PDDocument document) {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder sb = new StringBuilder();
        int pageCount = document.getNumberOfPages();

        for (int page = 0; page < pageCount; page++) {
            try {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);


                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage(language);

                String pageText;
                try {
                    pageText = tesseract.doOCR(image);
                } catch (TesseractException te) {
                    pageText = "";
                }

                sb.append(pageText);
                if (page < pageCount - 1) {
                    sb.append(System.lineSeparator()).append("--page-break--").append(System.lineSeparator());
                }
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }
}
