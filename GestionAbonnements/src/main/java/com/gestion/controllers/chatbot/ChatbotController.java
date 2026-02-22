package com.gestion.controllers.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ResourceBundle;

public class ChatbotController implements Initializable {

    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addMessage("Bonjour ! Je suis l'assistant AI de LAMMA. Comment puis-je vous aider ?", false);
        scrollPane.vvalueProperty().bind(chatContainer.heightProperty());
    }

    @FXML
    private void handleSendMessage() {
        String msg = inputField.getText();
        if (msg == null || msg.trim().isEmpty())
            return;

        addMessage(msg, true);
        inputField.clear();

        // Call internal API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8081/api/chatbot?q=" + URLEncoder.encode(msg)))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    try {
                        JsonNode node = mapper.readTree(body);
                        String reply = node.get("reply").asText();
                        Platform.runLater(() -> addMessage(reply, false));
                    } catch (Exception e) {
                        Platform.runLater(() -> handleFallback(msg));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> handleFallback(msg));
                    return null;
                });
    }

    private void handleFallback(String userMsg) {
        String lower = userMsg.toLowerCase();
        String reply;

        if (lower.contains("bonjour") || lower.contains("salut")) {
            reply = "Bonjour ! Comment se passe votre expédition aujourd'hui ?";
        } else if (lower.contains("faim") || lower.contains("manger") || lower.contains("repas")) {
            reply = "Je vous conseille de consulter l'Espace Restauration. Le Chef a préparé des rations spéciales Haute Montagne !";
        } else if (lower.contains("prix") || lower.contains("cher")) {
            reply = "Nos abonnements sont conçus pour s'adapter à tous les budgets d'explorateurs. Vérifiez la section Abonnements.";
        } else if (lower.contains("carte") || lower.contains("map") || lower.contains("trouver")) {
            reply = "La carte interactive affiche tous nos refuges partenaires. N'oubliez pas d'activer votre GPS !";
        } else if (lower.contains("aide") || lower.contains("besoin")) {
            reply = "Je suis là pour ça ! Vous pouvez me poser des questions sur les repas, la carte ou vos abonnements.";
        } else {
            reply = "C'est une excellente question pour un scout ! Malheureusement, ma connexion au serveur central est limitée, mais je peux vous dire que l'esprit d'équipe est la clé du succès.";
        }

        addMessage(reply, false);
    }

    private void addMessage(String text, boolean isUser) {
        HBox row = new HBox();
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(250);
        lbl.setStyle(isUser
                ? "-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");

        row.getChildren().add(lbl);
        chatContainer.getChildren().add(row);
    }
}

class URLEncoder {
    public static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
