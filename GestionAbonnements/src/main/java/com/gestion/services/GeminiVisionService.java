package com.gestion.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal Gemini Adaptor (RADICAL SOLUTION 2026).
 * Automatically handles model deprecation and discovery at runtime.
 */
public class GeminiVisionService {
    private String API_KEY = "";
    private String discoveredModel = null;
    private String discoveredVersion = null;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public GeminiVisionService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("afilnet.properties")) {
            if (input != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(input);
                API_KEY = props.getProperty("gemini.api_key", "");
            }
        } catch (Exception e) {
            System.err.println("Gemini Config Error: " + e.getMessage());
        }
    }

    /**
     * Tries to discover the best available model.
     * If discovery fails, callGemini will use a fallback cycle.
     */
    private synchronized void discoverBestModel(String key) {
        if (discoveredModel != null)
            return;

        String[] versions = { "v1", "v1beta" };
        for (String v : versions) {
            try {
                String url = "https://generativelanguage.googleapis.com/" + v + "/models?key=" + key;
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    JsonNode root = mapper.readTree(resp.body());
                    JsonNode models = root.get("models");
                    if (models != null && models.isArray()) {
                        List<String> candidates = new ArrayList<>();
                        for (JsonNode m : models) {
                            String name = m.get("name").asText();
                            JsonNode methods = m.get("supportedGenerationMethods");
                            boolean supportsGen = false;
                            if (methods != null && methods.isArray()) {
                                for (JsonNode meth : methods) {
                                    if (meth.asText().equals("generateContent"))
                                        supportsGen = true;
                                }
                            }
                            if (supportsGen)
                                candidates.add(name);
                        }

                        // Priority order for 2026: 2.5 Flash -> 2.5 Pro -> 2.0 Flash -> Any
                        String selected = null;
                        String[] priorities = { "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash",
                                "2.0-flash-lite", "gemini-flash" };
                        for (String p : priorities) {
                            for (String c : candidates) {
                                if (c.contains(p)) {
                                    selected = c;
                                    break;
                                }
                            }
                            if (selected != null)
                                break;
                        }

                        if (selected == null && !candidates.isEmpty()) {
                            selected = candidates.get(0);
                        }

                        if (selected != null) {
                            discoveredModel = selected;
                            discoveredVersion = v;
                            System.out.println("GEMINI ADAPTOR: Auto-discovered " + discoveredModel + " (" + v + ")");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Discovery Error (" + v + "): " + e.getMessage());
            }
        }
    }

    public String analyzeImage(String input, String contextPrompt) {
        return analyzeImage(input, contextPrompt, null);
    }

    public String analyzeImage(String input, String contextPrompt, String customKey) {
        try {
            String activeKey = (customKey != null && !customKey.isEmpty()) ? customKey : API_KEY;
            if (activeKey == null || activeKey.isEmpty())
                return "Error: Gemini API key missing.";

            discoverBestModel(activeKey);

            String b64Data;
            String mimeType = "image/jpeg";

            if (input == null || input.trim().isEmpty())
                return "Error: Input image source is empty.";

            if (input.startsWith("http")) {
                b64Data = downloadAndEncode(input);
                if (b64Data == null)
                    return "Error: Failed to fetch image from URL.";
            } else {
                File file = new File(input);
                if (file.exists()) {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    b64Data = Base64.getEncoder().encodeToString(bytes);
                    mimeType = Files.probeContentType(file.toPath());
                    if (mimeType == null)
                        mimeType = "image/jpeg";
                } else {
                    return "Error: Local image not found: " + input;
                }
            }

            return callGemini(b64Data, mimeType, contextPrompt, activeKey);
        } catch (Exception e) {
            return "Fatal Route Error: " + e.getMessage();
        }
    }

    private String downloadAndEncode(String urlString) {
        try {
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                byte[] bytes = in.readAllBytes();
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            System.err.println("Download Error: " + e.getMessage());
            return null;
        }
    }

    private String callGemini(String b64, String mime, String contextPrompt, String key) {
        // RADICAL FALLBACK CYCLE (2026 Deprecation Resistance)
        List<String[]> routes = new ArrayList<>();
        if (discoveredModel != null) {
            routes.add(new String[] { discoveredVersion, discoveredModel });
        }
        // Fallback routes with actual available models (2026)
        routes.add(new String[] { "v1beta", "models/gemini-2.5-flash" });
        routes.add(new String[] { "v1", "models/gemini-2.5-flash" });
        routes.add(new String[] { "v1beta", "models/gemini-2.5-pro" });
        routes.add(new String[] { "v1", "models/gemini-2.0-flash" });
        routes.add(new String[] { "v1beta", "models/gemini-2.0-flash" });
        routes.add(new String[] { "v1", "models/gemini-2.0-flash-lite" });

        String lastStatus = "Unknown";
        for (String[] route : routes) {
            String v = route[0];
            String m = route[1];
            try {
                String apiUrl = String.format("https://generativelanguage.googleapis.com/%s/%s:generateContent?key=%s",
                        v, m, key);

                String prompt = (contextPrompt != null && !contextPrompt.isEmpty()) ? contextPrompt
                        : "Analyze this image and describe it in French.";

                ObjectNode root = mapper.createObjectNode();
                ArrayNode contents = root.putArray("contents");
                ObjectNode contentObj = contents.addObject();
                ArrayNode parts = contentObj.putArray("parts");

                parts.addObject().put("text", prompt);
                ObjectNode inlineData = parts.addObject().putObject("inline_data");
                inlineData.put("mime_type", mime);
                inlineData.put("data", b64);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    JsonNode resJson = mapper.readTree(resp.body());
                    String text = resJson.at("/candidates/0/content/parts/0/text").asText();
                    if (text != null && !text.isBlank()) {
                        discoveredModel = m; // Lock in the successful model
                        discoveredVersion = v;
                        return text.trim();
                    }
                } else if (resp.statusCode() == 404) {
                    lastStatus = "404 - Model " + m + " not found on " + v;
                    continue; // Try next route
                } else if (resp.statusCode() == 403) {
                    lastStatus = "403 - Access denied (API key may be invalid or leaked)";
                    System.err.println("GEMINI 403 for " + m + ": " + resp.body());
                    // All routes will likely fail with same key, but still try
                    continue;
                } else {
                    lastStatus = resp.statusCode() + " - " + m + " on " + v;
                    System.err.println("GEMINI " + resp.statusCode() + ": " + resp.body());
                    continue; // Try next route instead of hard-stop
                }
            } catch (Exception e) {
                lastStatus = "Comms Error: " + e.getMessage();
            }
        }
        return "Universal AI Blockade: " + lastStatus
                + "\nAll Gemini endpoints returned 404/Error. Suggest verification of API key or network restrictions.";
    }
}
