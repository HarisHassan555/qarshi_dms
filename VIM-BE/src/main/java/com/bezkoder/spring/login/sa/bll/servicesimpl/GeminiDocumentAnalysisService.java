package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GeminiDocumentAnalysisService {

    private static final Logger logger = LogManager.getLogger(GeminiDocumentAnalysisService.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final Set<String> LABEL_PRESETS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "FINANCE",
            "PROCUREMENT",
            "LEGAL",
            "HR",
            "OPERATIONS",
            "COMPLIANCE",
            "GENERAL"
    )));

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String geminiModel;

    public static class AnalysisResult {
        public String title;
        public String summary;
        public String label;
        public int pageCount;
        public boolean analyzedByAi;
        public String message;
    }

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.trim().isEmpty();
    }

    public AnalysisResult analyze(MultipartFile file, String originalFileName) {
        AnalysisResult result = new AnalysisResult();
        result.title = fallbackTitle(originalFileName);
        result.summary = "";
        result.label = "GENERAL";
        result.analyzedByAi = false;
        result.pageCount = countPdfPages(file);

        if (!isConfigured()) {
            result.message = "Gemini API key is not configured; document stored without AI metadata.";
            return result;
        }

        try {
            String prompt = buildPrompt();
            String body = buildRequestBody(file, prompt);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiModel.trim()
                    + ":generateContent?key="
                    + geminiApiKey.trim();

            HttpResponse<String> response = Unirest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asString();

            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                logger.warn("Gemini API returned " + response.getStatus() + ": " + response.getBody());
                result.message = "AI analysis failed; document stored with default metadata.";
                return result;
            }

            String text = extractModelText(response.getBody());
            applyParsedJson(text, result);
            result.analyzedByAi = true;
            result.message = "Analyzed with Gemini.";
        } catch (Exception ex) {
            logger.error("Gemini analysis error: " + ex.getMessage(), ex);
            result.message = "AI analysis failed; document stored with default metadata.";
        }
        return result;
    }

    private void applyParsedJson(String text, AnalysisResult result) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String json = stripCodeFence(text.trim());
        JsonNode root = MAPPER.readTree(json);
        if (root.hasNonNull("title")) {
            result.title = root.get("title").asText("").trim();
        }
        if (root.hasNonNull("summary")) {
            result.summary = root.get("summary").asText("").trim();
        }
        if (root.hasNonNull("label")) {
            String label = root.get("label").asText("GENERAL").trim().toUpperCase(Locale.ROOT);
            result.label = normalizeLabel(label);
        }
        if (result.title == null || result.title.isEmpty()) {
            result.title = "Untitled document";
        }
    }

    private String normalizeLabel(String label) {
        if (LABEL_PRESETS.contains(label)) {
            return label;
        }
        return "GENERAL";
    }

    private String stripCodeFence(String text) {
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                return text.substring(firstNl + 1, lastFence).trim();
            }
        }
        return text;
    }

    private String extractModelText(String responseBody) throws IOException {
        JsonNode root = MAPPER.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            return "";
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.hasNonNull("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString();
    }

    private String buildPrompt() {
        return "You are archiving official company documents. Analyze the provided document (invoice, "
                + "contract, record, or scan). Respond with JSON only, no markdown, using this shape:\n"
                + "{\n"
                + "  \"title\": \"short searchable title\",\n"
                + "  \"summary\": \"clear summary in 2-4 sentences covering purpose, parties, dates, and amounts if present\",\n"
                + "  \"label\": \"one of: FINANCE, PROCUREMENT, LEGAL, HR, OPERATIONS, COMPLIANCE, GENERAL\"\n"
                + "}";
    }

    private String buildRequestBody(MultipartFile file, String prompt) throws IOException {
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String lowerMime = mime.toLowerCase(Locale.ROOT);

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        java.util.List<Map<String, Object>> parts = new java.util.ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        if (lowerMime.contains("pdf")) {
            String extracted = extractPdfText(file.getBytes());
            if (extracted != null && !extracted.trim().isEmpty()) {
                Map<String, Object> docPart = new HashMap<>();
                docPart.put("text", "Document text (all pages):\n" + truncate(extracted, 120000));
                parts.add(docPart);
            } else {
                addInlineFilePart(parts, file.getBytes(), "application/pdf");
            }
        } else if (lowerMime.startsWith("image/")) {
            addInlineFilePart(parts, file.getBytes(), mime);
        } else {
            Map<String, Object> docPart = new HashMap<>();
            docPart.put("text", "Filename: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"));
            parts.add(docPart);
        }

        content.put("parts", parts);
        root.put("contents", Collections.singletonList(content));
        return MAPPER.writeValueAsString(root);
    }

    private void addInlineFilePart(java.util.List<Map<String, Object>> parts, byte[] bytes, String mime) {
        Map<String, Object> inline = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mime_type", mime);
        inlineData.put("data", Base64.getEncoder().encodeToString(bytes));
        inline.put("inline_data", inlineData);
        parts.add(inline);
    }

    private String extractPdfText(byte[] pdfBytes) {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (Exception ex) {
            logger.debug("PDF text extraction failed: " + ex.getMessage());
            return "";
        }
    }

    private int countPdfPages(MultipartFile file) {
        String mime = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!mime.contains("pdf")) {
            return 1;
        }
        try (PDDocument doc = PDDocument.load(file.getBytes())) {
            return doc.getNumberOfPages();
        } catch (Exception ex) {
            return 0;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String fallbackTitle(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "Untitled document";
        }
        String name = originalFileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            return name.substring(0, dot);
        }
        return name;
    }
}
