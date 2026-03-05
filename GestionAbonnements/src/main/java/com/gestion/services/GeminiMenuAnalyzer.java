package com.gestion.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class GeminiMenuAnalyzer {
        private static String API_KEY = "";
        private static String DISCOVERED_MODEL = null;
        private static String DISCOVERED_VERSION = null;

        private static final String PROMPT = "Act as a professional chef. Analyze this food image and return ONLY a JSON object with: "
                        +
                        "\"nom\" (Creative French Name), \"description\" (Appetizing French marketing description), " +
                        "\"prixEstime\" (number), \"tags\" (comma separated).";

        private final ObjectMapper mapper = new ObjectMapper();
        private final HttpClient http = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(20))
                        .build();

        public GeminiMenuAnalyzer() {
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

        private synchronized void discoverBestModel() {
                if (DISCOVERED_MODEL != null)
                        return;
                String[] versions = { "v1", "v1beta" };
                for (String v : versions) {
                        try {
                                String url = "https://generativelanguage.googleapis.com/" + v + "/models?key="
                                                + API_KEY;
                                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                                if (resp.statusCode() == 200) {
                                        JsonNode root = mapper.readTree(resp.body());
                                        JsonNode models = root.get("models");
                                        if (models != null && models.isArray()) {
                                                List<String> candidates = new ArrayList<>();
                                                for (JsonNode m : models) {
                                                        JsonNode methods = m.get("supportedGenerationMethods");
                                                        if (methods != null && methods.isArray()) {
                                                                for (JsonNode meth : methods) {
                                                                        if (meth.asText().equals("generateContent"))
                                                                                candidates.add(m.get("name").asText());
                                                                }
                                                        }
                                                }
                                                String selected = null;
                                                for (String c : candidates) {
                                                        if (c.contains("1.5-flash")) {
                                                                selected = c;
                                                                break;
                                                        }
                                                }
                                                if (selected == null && !candidates.isEmpty())
                                                        selected = candidates.get(0);
                                                if (selected != null) {
                                                        DISCOVERED_MODEL = selected;
                                                        DISCOVERED_VERSION = v;
                                                        return;
                                                }
                                        }
                                }
                        } catch (Exception e) {
                        }
                }
        }

        public static class MenuAnalysisResult {
                public String nom, description, tags;
                public double prixEstime;

                public MenuAnalysisResult(String n, String d, double p, String t) {
                        this.nom = n;
                        this.description = d;
                        this.prixEstime = p;
                        this.tags = t;
                }
        }

        public MenuAnalysisResult analyze(File imageFile) {
                try {
                        discoverBestModel();
                        if (DISCOVERED_MODEL == null)
                                return solveRadically(imageFile);

                        byte[] bytes = Files.readAllBytes(imageFile.toPath());
                        String b64 = Base64.getEncoder().encodeToString(bytes);
                        String mime = imageFile.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

                        ObjectNode root = mapper.createObjectNode();
                        ArrayNode contents = root.putArray("contents");
                        ObjectNode contentObj = contents.addObject();
                        ArrayNode parts = contentObj.putArray("parts");
                        parts.addObject().put("text", PROMPT);
                        ObjectNode inlineData = parts.addObject().putObject("inline_data");
                        inlineData.put("mime_type", mime);
                        inlineData.put("data", b64);

                        String apiUrl = String.format(
                                        "https://generativelanguage.googleapis.com/%s/%s:generateContent?key=%s",
                                        DISCOVERED_VERSION, DISCOVERED_MODEL, API_KEY);

                        HttpRequest req = HttpRequest.newBuilder()
                                        .uri(URI.create(apiUrl))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                                        .build();

                        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                        if (resp.statusCode() == 200) {
                                JsonNode resJson = mapper.readTree(resp.body());
                                String text = resJson.at("/candidates/0/content/parts/0/text").asText();
                                if (text != null && !text.isBlank())
                                        return parseSafe(text);
                        }
                } catch (Exception e) {
                        System.err.println("Menu AI Error: " + e.getMessage());
                }
                return solveRadically(imageFile);
        }

        private MenuAnalysisResult parseSafe(String text) {
                try {
                        int start = text.indexOf('{');
                        int end = text.lastIndexOf('}');
                        if (start >= 0 && end > start) {
                                JsonNode res = mapper.readTree(text.substring(start, end + 1));
                                return new MenuAnalysisResult(
                                                res.path("nom").asText("Plat Spécial"),
                                                res.path("description")
                                                                .asText("Un plat délicieux préparé par notre chef."),
                                                res.path("prixEstime").asDouble(15.0),
                                                res.path("tags").asText("nouveauté"));
                        }
                } catch (Exception e) {
                }
                return null;
        }

        private MenuAnalysisResult solveRadically(File f) {
                String name = f.getName().toLowerCase();
                Random r = new Random(f.length());
                String nom = "Menu du Jour Créatif", desc = "Sélection gourmande artisanale.", tags = "Fait maison";
                double prix = 14.50 + r.nextInt(10);
                if (name.contains("pizza")) {
                        nom = "Pizza Royale";
                        desc = "Pâte artisanale, sauce tomate, mozzarella.";
                        prix = 12.00;
                } else if (name.contains("burger")) {
                        nom = "Burger Gourmet";
                        desc = "Bœuf, cheddar, oignons caramélisés.";
                        prix = 15.50;
                }
                return new MenuAnalysisResult(nom, desc, prix, tags);
        }

        public String generateMenuDescription(java.util.List<String> dishNames) {
                if (dishNames == null || dishNames.isEmpty())
                        return "Sélection de nos meilleurs plats.";
                return callTextOnly("Act as a chef. Create a SHORT poetic French description for a menu of: "
                                + String.join(", ", dishNames));
        }

        public String generateMenuName(java.util.List<String> dishNames, int seed) {
                if (dishNames == null || dishNames.isEmpty())
                        return "Menu Gourmand";
                return callTextOnly(
                                "Suggest a UNIQUE creative 3-word French name for: " + String.join(", ", dishNames));
        }

        public String generateMenuDates(java.util.List<String> dishNames, int seed) {
                LocalDate start = LocalDate.now().plusDays(seed % 14);
                return start.toString() + "|" + start.plusDays(7).toString();
        }

        private String callTextOnly(String prompt) {
                try {
                        discoverBestModel();
                        if (DISCOVERED_MODEL == null)
                                return "Gourmandise du Chef";
                        ObjectNode root = mapper.createObjectNode();
                        root.putArray("contents").addObject().putArray("parts").addObject().put("text", prompt);
                        String apiUrl = String.format(
                                        "https://generativelanguage.googleapis.com/%s/%s:generateContent?key=%s",
                                        DISCOVERED_VERSION, DISCOVERED_MODEL, API_KEY);
                        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiUrl))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                                        .build();
                        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200) {
                                return mapper.readTree(resp.body()).at("/candidates/0/content/parts/0/text").asText()
                                                .trim();
                        }
                } catch (Exception e) {
                }
                return "Signature du Chef";
        }
}
