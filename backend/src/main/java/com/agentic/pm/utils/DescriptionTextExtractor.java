package com.agentic.pm.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescriptionTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DescriptionTextExtractor.class);

    public String extractText(String key, ResponseInputStream<GetObjectResponse> inputStream) throws IOException {
        String lowerKey = key.toLowerCase();
        try {
            if (lowerKey.endsWith(".pdf")) {
                return extractFromPdf(inputStream);
            } else if (lowerKey.endsWith(".docx")) {
                return extractFromDocx(inputStream);
            } else {
                return extractFromText(inputStream);
            }
        } catch (Exception e) {
            log.error("Document text extraction failed. key={}", key, e);
            if (e instanceof IOException io) {
                throw io;
            }
            throw new IOException("Document text extraction failed: " + e.getMessage(), e);
        }
    }

    private String extractFromText(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private String extractFromPdf(InputStream in) throws IOException {
        try (PDDocument document = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(InputStream in) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append('\n'));
            return sb.toString();
        }
    }
}

