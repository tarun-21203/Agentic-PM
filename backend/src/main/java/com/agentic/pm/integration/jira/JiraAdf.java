package com.agentic.pm.integration.jira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jira Cloud REST API v3 expects rich-text fields (e.g. {@code description}) as
 * <a href="https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/">Atlassian Document Format</a>,
 * not plain strings.
 */
public final class JiraAdf {

    private JiraAdf() {
    }

    /**
     * Builds a minimal ADF document: one paragraph per line (preserves line breaks).
     * Blank / null input yields a single empty paragraph.
     */
    public static Map<String, Object> documentFromPlainText(String text) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            blocks.add(emptyParagraph());
        } else {
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
            for (String line : normalized.split("\n", -1)) {
                blocks.add(line.isEmpty() ? emptyParagraph() : paragraphWithText(line));
            }
        }
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", blocks);
        return doc;
    }

    private static Map<String, Object> emptyParagraph() {
        return Map.of(
                "type", "paragraph",
                "content", List.of()
        );
    }

    private static Map<String, Object> paragraphWithText(String line) {
        Map<String, Object> textNode = new HashMap<>();
        textNode.put("type", "text");
        textNode.put("text", line);
        return Map.of(
                "type", "paragraph",
                "content", List.of(textNode)
        );
    }
}
