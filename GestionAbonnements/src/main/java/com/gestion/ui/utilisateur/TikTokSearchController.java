package com.gestion.ui.utilisateur;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

/**
 * Radical Fix for TikTok Search using TikWM API.
 * High stability, direct scraping, no authorization required in standard mode.
 */
public class TikTokSearchController implements Initializable {

    @FXML
    private TextField inputSearch;
    @FXML
    private TilePane videoContainer;
    @FXML
    private Button btnSearch;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox emptyState;

    private static final String TIKWM_SEARCH_URL = "https://www.tikwm.com/api/feed/search";

    private static String pendingSearchQuery = null;

    public static void setPendingSearch(String query) {
        pendingSearchQuery = query;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (pendingSearchQuery != null && !pendingSearchQuery.isEmpty()) {
            inputSearch.setText(pendingSearchQuery);
            pendingSearchQuery = null;
            onSearch();
        }
    }

    @FXML
    private void onSearch() {
        String query = inputSearch.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("⚠️ Enter a destination (e.g. Sousse)");
            return;
        }

        btnSearch.setDisable(true);
        btnSearch.setText("🛸 TUNING...");
        statusLabel.setText("📡 Intercepting TikWM frequency: " + query);
        videoContainer.getChildren().clear();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String api = TIKWM_SEARCH_URL + "?keywords=" + encoded + "&count=12&cursor=0";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(java.net.URI.create(api))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    processTikWMResults(response.body());
                } else {
                    final int code = response.statusCode();
                    Platform.runLater(() -> {
                        statusLabel.setText("⚠️ TikWM Interference: HTTP " + code);
                        resetButton();
                        showEmpty();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Comm Failure: " + e.getMessage());
                    resetButton();
                    showEmpty();
                });
            }
        }).start();
    }

    private void processTikWMResults(String json) {
        Platform.runLater(() -> {
            try {
                videoContainer.getChildren().clear();

                // TikWM structure: {"data": {"videos": [...]}}
                int videosIdx = json.indexOf("\"videos\"");
                if (videosIdx == -1) {
                    statusLabel.setText("🔇 Zero viral frequencies found for this coordinate.");
                    showEmpty();
                    resetButton();
                    return;
                }

                int count = 0;
                int cursor = videosIdx;

                while (cursor < json.length() && count < 12) {
                    int objStart = json.indexOf("{", cursor);
                    if (objStart == -1)
                        break;

                    String id = extract(json, objStart, "\"video_id\"", 500);
                    String title = extract(json, objStart, "\"title\"", 500);
                    String cover = extract(json, objStart, "\"cover\"", 500);
                    String dur = extract(json, objStart, "\"duration\"", 500);

                    if (!id.isEmpty()) {
                        String link = "https://www.tiktok.com/@user/video/" + id;
                        // Use direct play link if available as a fallback or for preview
                        videoContainer.getChildren().add(createVideoCard(title, link, cover, dur));
                        count++;
                    }

                    cursor = json.indexOf("},", objStart);
                    if (cursor == -1)
                        break;
                    cursor += 2;
                }

                if (count == 0) {
                    statusLabel.setText("🔇 Signal too weak. No intel retrieved.");
                    showEmpty();
                } else {
                    statusLabel.setText("✅ Sync complete. " + count + " viral segments acquired.");
                }

            } catch (Exception e) {
                statusLabel.setText("⚠️ Protocol error: " + e.getMessage());
                showEmpty();
            }
            resetButton();
        });
    }

    private String extract(String json, int from, String key, int range) {
        int limit = Math.min(json.length(), from + range);
        int kIdx = json.indexOf(key, from);
        if (kIdx == -1 || kIdx > limit)
            return "";

        int vStart = json.indexOf(":", kIdx) + 1;
        while (vStart < json.length() && (json.charAt(vStart) == ' ' || json.charAt(vStart) == '"'))
            vStart++;

        int vEnd = vStart;
        char endChar = json.charAt(vStart - 1) == '"' ? '"' : ',';
        while (vEnd < json.length() && json.charAt(vEnd) != endChar)
            vEnd++;

        if (vEnd >= json.length())
            return "";
        return json.substring(vStart, vEnd).replace("\\/", "/");
    }

    private void resetButton() {
        btnSearch.setDisable(false);
        btnSearch.setText("🚀 FETCH INTEL");
    }

    private void showEmpty() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private VBox createVideoCard(String title, String link, String thumbUrl, String duration) {
        VBox card = new VBox(8);
        card.getStyleClass().add("modern-card");
        card.setPadding(new Insets(8));
        card.setPrefWidth(240);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
                "-fx-border-color: rgba(254,44,85,0.3); -fx-border-radius: 12; " +
                        "-fx-background-radius: 12; -fx-background-color: rgba(15,15,15,0.95);");

        StackPane thumbPane = new StackPane();
        thumbPane.setPrefHeight(130);
        thumbPane.setStyle("-fx-background-color: #000; -fx-background-radius: 8;");

        ImageView iv = new ImageView();
        iv.setFitWidth(224);
        iv.setFitHeight(130);
        iv.setPreserveRatio(true);
        if (!thumbUrl.isEmpty()) {
            try {
                iv.setImage(new Image(thumbUrl, true));
            } catch (Exception ignored) {
            }
        }

        Rectangle clip = new Rectangle(224, 130);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        iv.setClip(clip);

        Label tiktokBadge = new Label("TikWM");
        tiktokBadge.setStyle("-fx-background-color: rgba(254,44,85,0.8); -fx-text-fill: white; " +
                "-fx-font-size: 8; -fx-font-weight: bold; -fx-padding: 2 5; -fx-background-radius: 3;");
        StackPane.setAlignment(tiktokBadge, Pos.TOP_LEFT);
        StackPane.setMargin(tiktokBadge, new Insets(5));

        thumbPane.getChildren().addAll(iv, tiktokBadge);

        Label titleLbl = new Label(
                title.isEmpty() ? "Viral TikTok Clip" : (title.length() > 50 ? title.substring(0, 47) + "..." : title));
        titleLbl.setWrapText(true);
        titleLbl.setMaxHeight(35);
        titleLbl.setStyle("-fx-text-fill: #fff; -fx-font-size: 10; -fx-font-weight: bold;");
        titleLbl.setAlignment(Pos.CENTER);

        Button watchBtn = new Button("▶  STREAM");
        watchBtn.setStyle("-fx-background-color: #fe2c55; -fx-text-fill: white; -fx-font-size: 9; " +
                "-fx-padding: 5 12; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-weight: bold;");
        watchBtn.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(link));
            } catch (Exception ex) {
            }
        });

        card.getChildren().addAll(thumbPane, titleLbl, watchBtn);
        return card;
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