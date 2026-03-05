package com.gestion.ui.utilisateur;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import com.gestion.services.GeminiVisionService;
import java.io.File;
import java.net.URL;
import javafx.event.ActionEvent;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

/**
 * VisualScoutController - Satellite AI Analysis & Viral TikTok Discovery.
 * Prioritizes Gemini 1.5 Flash for mission-critical stability.
 */
public class VisualScoutController implements Initializable {

    @FXML
    private TextField inputImageUrl;
    @FXML
    private ImageView previewImage;
    @FXML
    private TextArea analysisResult;
    @FXML
    private Button btnScan;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox resultContainer;
    @FXML
    private Label lblNoSignal;
    @FXML
    private Button btnTikTokDiscovery;

    private static final String SEARCHAPI_KEY = "ZyQMxEsvWrKx3sXz";
    private final GeminiVisionService geminiService = new GeminiVisionService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        resultContainer.setVisible(false);
        if (btnTikTokDiscovery != null)
            btnTikTokDiscovery.setVisible(false);
        if (lblNoSignal != null)
            lblNoSignal.setVisible(true);

        inputImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                if (newVal.startsWith("http") || new File(newVal).exists()) {
                    try {
                        String imgUrl = newVal.startsWith("http") ? newVal : new File(newVal).toURI().toString();
                        previewImage.setImage(new Image(imgUrl, true));
                        if (lblNoSignal != null)
                            lblNoSignal.setVisible(false);
                    } catch (Exception e) {
                        previewImage.setImage(null);
                        if (lblNoSignal != null)
                            lblNoSignal.setVisible(true);
                    }
                }
            } else {
                if (lblNoSignal != null)
                    lblNoSignal.setVisible(true);
            }
        });
    }

    @FXML
    void onBrowseImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(btnScan.getScene().getWindow());
        if (selectedFile != null) {
            inputImageUrl.setText(selectedFile.getAbsolutePath());
            previewImage.setImage(new Image(selectedFile.toURI().toString()));
            if (lblNoSignal != null)
                lblNoSignal.setVisible(false);
            statusLabel.setText("Local sensor locked on local file.");
        }
    }

    @FXML
    private void onScan() {
        String input = inputImageUrl.getText();
        if (input == null || input.trim().isEmpty()) {
            statusLabel.setText("⚠️ No image source detected.");
            return;
        }

        btnScan.setDisable(true);
        btnScan.setText("🛰️ SCANNING...");
        resultContainer.setVisible(true);
        analysisResult.setText("📡 Intercepting satellite data...\n🔍 Engaging Gemini Nexus 1.5...");

        new Thread(() -> {
            try {
                // ── PRIMARY: Gemini 1.5 Flash (RADICAL STABILITY) ───────────
                String analysis = geminiService.analyzeImage(input,
                        "Tu es un expert explorateur. Analyse cette image en détail. " +
                                "Identifie le lieu, l'ambiance et les points d'intérêt. " +
                                "Réponds en français avec un ton aventurier.");

                if (analysis != null && !analysis.contains("AI Route Failed") && !analysis.contains("Critical Error")) {
                    Platform.runLater(() -> {
                        analysisResult.setText(analysis);
                        statusLabel.setText("✅ Satellite Scan Complete (Gemini Priority).");
                        finalizeScan();
                    });
                    return;
                }

                // ── FALLBACK: SearchAPI (Google Lens) ────────────────────────
                Platform.runLater(() -> statusLabel.setText("⚠️ Gemini Offline. Switching to SearchAPI backup..."));
                if (input.startsWith("http")) {
                    String jsonBody = "{\"engine\": \"google_lens\", \"url\": \"" + input + "\"}";
                    String apiUrl = "https://www.searchapi.io/api/v1/search?api_key=" + SEARCHAPI_KEY;

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(java.net.URI.create(apiUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String desc = parseDescription(response.body());
                        Platform.runLater(() -> {
                            analysisResult.setText(desc);
                            statusLabel.setText("✅ Satellite Scan Complete (SearchAPI Fallback).");
                            finalizeScan();
                        });
                        return;
                    }
                }

                Platform.runLater(() -> {
                    statusLabel.setText("❌ Mission Failed: All Sensors Offline.");
                    analysisResult.setText("Unable to establish AI uplink.\nVerify API keys or internet connection.");
                    btnScan.setDisable(false);
                    btnScan.setText("🛰️ RETRY");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("⚠️ Comms Error: " + e.getMessage());
                    btnScan.setDisable(false);
                });
            }
        }).start();
    }

    private void finalizeScan() {
        btnScan.setDisable(false);
        btnScan.setText("🛰️ RE-SCAN");
        if (btnTikTokDiscovery != null)
            btnTikTokDiscovery.setVisible(true);
    }

    @FXML
    void onTikTokDiscovery(ActionEvent event) {
        String analysis = analysisResult.getText();
        if (analysis == null || analysis.isEmpty())
            return;

        String searchTerm = extractSearchTerm(analysis);
        if (searchTerm.isEmpty())
            searchTerm = "Tourism Destination";

        statusLabel.setText("🎵 TikTok Discovery Hand-off: " + searchTerm);

        // Use the new TikWM-powered search via TikTokSearchController
        TikTokSearchController.setPendingSearch(searchTerm);

        if (com.gestion.controllers.DashboardController.getInstance() != null) {
            com.gestion.controllers.DashboardController.getInstance()
                    .loadModule("/views/utilisateur/tiktok-search.fxml");
        }
    }

    private String extractSearchTerm(String analysis) {
        // Simple extraction: find first 3-5 capitalized words or nouns
        String[] lines = analysis.split("\n");
        for (String line : lines) {
            if (line.contains(":") && (line.toLowerCase().contains("lieu") || line.toLowerCase().contains("endroit"))) {
                return line.split(":")[1].trim();
            }
        }
        String cleaned = analysis.replaceAll("[^a-zA-Z\\s]", "");
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.length() > 3 && count < 3) {
                sb.append(w).append(" ");
                count++;
            }
        }
        return sb.toString().trim();
    }

    private String parseDescription(String json) {
        try {
            if (json.contains("\"title\":")) {
                int s = json.indexOf("\"title\":") + 9;
                int e = json.indexOf("\"", s);
                return "MATCH IDENTIFIED: " + json.substring(s, e);
            }
        } catch (Exception ignored) {
        }
        return "Unidentified Geographical Object.";
    }

    @FXML
    private void onBack() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        } else if (com.gestion.controllers.DashboardController.getInstance() != null) {
            com.gestion.controllers.DashboardController.getInstance()
                    .loadModule("/views/utilisateur/espace-utilisateur.fxml");
        }
    }
}