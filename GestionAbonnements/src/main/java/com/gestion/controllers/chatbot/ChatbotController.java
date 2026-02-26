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

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Objects;

import com.gestion.services.IngredientServiceImpl;
import com.gestion.services.EvenementService;
import com.gestion.services.AbonnementServiceImpl;
import com.gestion.services.ParticipationServiceImpl;
import com.gestion.entities.Ingredient;
import com.gestion.entities.Evenement;
import com.gestion.entities.Abonnement;
import com.gestion.entities.Participation;

public class ChatbotController implements Initializable {

    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // OpenAI API Configuration (Switch from Gemini to GPT)
    private static final String OPENAI_API_KEY = "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";
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_CONTEXT = "You are LAMMA AI Assistant, a helpful and friendly chatbot for an outdoor camping/hiking adventure platform called LAMMA. "
            + "You help users with restaurants, subscriptions (Mensuel, Annuel, Premium), meal tickets, and participations. "
            + "Answer concisely (2 sentences). Reply in French or English depending on user.";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addMessage(
                "Bonjour ! Je suis l'assistant AI de LAMMA \uD83C\uDFD5\uFE0F. Posez-moi vos questions sur les repas, les événements ou les abonnements !",
                false);
        scrollPane.vvalueProperty().bind(chatContainer.heightProperty());
    }

    private String fetchAppDataContext() {
        StringBuilder sb = new StringBuilder("Current App Data Context:\n");

        try {
            // Ingredients
            IngredientServiceImpl ingService = new IngredientServiceImpl();
            List<Ingredient> ingredients = ingService.findAll();
            sb.append("- Ingredients available: ").append(ingredients.stream()
                    .map(i -> i.getNom() + " (" + i.getPrixSupplement() + " TND supplement)")
                    .collect(Collectors.joining(", "))).append("\n");

            // Events & Participations
            EvenementService eventService = new EvenementService();
            ParticipationServiceImpl partService = new ParticipationServiceImpl();
            List<Evenement> events = eventService.findAll();
            sb.append("- Active Events & Participation counts: ");
            for (Evenement e : events) {
                long count = partService.findAll().stream()
                        .filter(p -> p.getEvenementId() != null && p.getEvenementId().intValue() == e.getIdEvent())
                        .count();
                sb.append(e.getTitre()).append(" (").append(count).append(" participants), ");
            }
            sb.append("\n");

            // Revenue
            AbonnementServiceImpl abService = new AbonnementServiceImpl();
            BigDecimal totalRevenue = abService.findAll().stream()
                    .map(Abonnement::getPrix)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            sb.append("- Total Revenue from subscriptions: ").append(totalRevenue).append(" TND\n");

            // Weather
            com.gestion.services.WeatherService.WeatherInfo weather = com.gestion.services.WeatherService.getInstance()
                    .getCurrentWeather("Tunis");
            sb.append("- Current Weather: ").append(weather.temp).append(", ").append(weather.condition).append(" in ")
                    .append(weather.city).append("\n");

        } catch (Exception e) {
            sb.append("(Error fetching live data: ").append(e.getMessage()).append(")\n");
        }

        return sb.toString();
    }

    @FXML
    private void handleSendMessage() {
        String msg = inputField.getText();
        if (msg == null || msg.trim().isEmpty())
            return;

        addMessage(msg, true);
        inputField.clear();

        // Show typing indicator
        Label typingLabel = new Label("...");
        typingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-padding: 5 10;");
        HBox typingRow = new HBox(typingLabel);
        typingRow.setAlignment(Pos.CENTER_LEFT);
        chatContainer.getChildren().add(typingRow);

        // Build OpenAI API request body
        String requestBody = buildOpenAIRequestBody(msg);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> {
                    try {
                        JsonNode root = mapper.readTree(body);
                        JsonNode choices = root.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            String reply = choices.get(0)
                                    .get("message")
                                    .get("content")
                                    .asText();
                            Platform.runLater(() -> {
                                chatContainer.getChildren().remove(typingRow);
                                addMessage(reply.trim(), false);
                            });
                        } else {
                            // Check for OpenAI errors (401, 429, etc)
                            JsonNode error = root.get("error");
                            String errorMsg = (error != null) ? error.get("message").asText() : "Unknown API Error";
                            System.err.println("OpenAI API error: " + errorMsg);

                            new Thread(() -> {
                                try {
                                    Thread.sleep(800);
                                } catch (InterruptedException ignored) {
                                }
                                Platform.runLater(() -> {
                                    chatContainer.getChildren().remove(typingRow);
                                    handleFallback(msg);
                                });
                            }).start();
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing OpenAI response: " + e.getMessage());
                        new Thread(() -> {
                            try {
                                Thread.sleep(800);
                            } catch (InterruptedException ignored) {
                            }
                            Platform.runLater(() -> {
                                chatContainer.getChildren().remove(typingRow);
                                handleFallback(msg);
                            });
                        }).start();
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("OpenAI API call failed: " + ex.getMessage());
                    new Thread(() -> {
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(() -> {
                            chatContainer.getChildren().remove(typingRow);
                            handleFallback(msg);
                        });
                    }).start();
                    return null;
                });
    }

    /**
     * Builds the JSON request body for OpenAI API.
     */
    private String buildOpenAIRequestBody(String userMessage) {
        String liveContext = fetchAppDataContext();
        String escapedContext = escapeJson(SYSTEM_CONTEXT + "\n\n" + liveContext);
        String escapedMessage = escapeJson(userMessage);

        return "{"
                + "\"model\": \"gpt-4o-mini\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \"" + escapedContext + "\"},"
                + "  {\"role\": \"user\", \"content\": \"" + escapedMessage + "\"}"
                + "],"
                + "\"temperature\": 0.7,"
                + "\"max_tokens\": 256"
                + "}";
    }

    /**
     * Escapes special JSON characters in a string.
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void handleFallback(String userMsg) {
        String lower = userMsg.toLowerCase();
        String reply;

        if (lower.contains("bonjour") || lower.contains("salut") || lower.contains("hello") || lower.contains("hi")) {
            reply = "Bonjour ! Ravi de vous voir au camp de base de LAMMA. Comment puis-je vous aider aujourd'hui ? \uD83C\uDFD5\uFE0F";
        } else if (lower.contains("faim") || lower.contains("manger") || lower.contains("repas")
                || lower.contains("food") || lower.contains("plat") || lower.contains("menu")
                || lower.contains("chef")) {
            reply = "Le Chef pr\u00e9pare des repas adapt\u00e9s (V\u00e9gan, Sans Gluten, etc.) dans l'Espace Restauration. Les menus sont con\u00e7us pour l'\u00e9nergie en montagne ! \uD83C\uDF72";
        } else if (lower.contains("prix") || lower.contains("cher") || lower.contains("price") || lower.contains("pay")
                || lower.contains("co\u00fbt") || lower.contains("payer")) {
            reply = "Le prix d\u00e9pend de votre forfait. Le Pass Unique (25€) est id\u00e9al pour un tour ponctuel, mais les abonnements Mensuels ou Premium offrent des r\u00e9ductions jusqu'\u00e0 25% ! \uD83D\uDCB0";
        } else if (lower.contains("carte") || lower.contains("map") || lower.contains("trouver")
                || lower.contains("refuge") || lower.contains("position") || lower.contains("gps")) {
            reply = "Consultez la Carte Interactive pour voir les refuges, les sources d'eau et les points de rendez-vous. N'oubliez pas de t\u00e9l\u00e9charger la carte pour l'usage offline ! \uD83D\uDDFA\uFE0F";
        } else if (lower.contains("facture") || lower.contains("receipt") || lower.contains("invoice")
                || lower.contains("justifiant") || lower.contains("paiement") || lower.contains("pay\u00e9")) {
            reply = "Vos factures sont accessibles imm\u00e9diatement apr\u00e8s validation d'un pass. Elles incluent le d\u00e9tail du prix et l'ID de transaction unique. \uD83D\uDCC4";
        } else if (lower.contains("revenu") || lower.contains("total") || lower.contains("argent")
                || lower.contains("revenue")) {
            BigDecimal revenue = BigDecimal.ZERO;
            try {
                revenue = new com.gestion.services.AbonnementServiceImpl().findAll().stream()
                        .map(com.gestion.entities.Abonnement::getPrix)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception e) {
            }
            reply = "Le revenu total des abonnements est actuellement de " + revenue + " TND. \uD83D\uDCB8";
        } else if (lower.contains("place") || lower.contains("lieu") || lower.contains("restaurant")) {
            reply = "Nos restaurants partenaires se trouvent principalement au camp de base et pr\u00e8s des refuges de montagne. \uD83C\uDFD5\uFE0F";
        } else if (lower.contains("ingrediant") || lower.contains("ingr\u00e9dient") || lower.contains("composition")) {
            String ings = "";
            try {
                ings = new com.gestion.services.IngredientServiceImpl().findAll().stream()
                        .map(com.gestion.entities.Ingredient::getNom)
                        .collect(Collectors.joining(", "));
            } catch (Exception e) {
            }
            reply = "Voici quelques ingr\u00e9dients disponibles : " + ings + ". \uD83E\uDDBA";
        } else if (lower.contains("participant") || lower.contains("nombre") || lower.contains("combien")) {
            reply = "Chaque \u00e9v\u00e9nement a un nombre d\u00e9fini de participants. En moyenne, nous accueillons 20 personnes par randonn\u00e9e. \uD83D\uDC63";
        } else if (lower.contains("participer") || lower.contains("inscription") || lower.contains("event")
                || lower.contains("\u00e9v\u00e9nement") || lower.contains("rejoindre") || lower.contains("go")) {
            reply = "Pour rejoindre une exp\u00e9dition, allez dans 'Participations'. Si vous n'avez pas de pass, le syst\u00e8me vous proposera automatiquement une solution adapt\u00e9e. \uD83C\uDFD4\uFE0F";
        } else if (lower.contains("abonnement") || lower.contains("forfait") || lower.contains("pass")
                || lower.contains("global") || lower.contains("souscrire") || lower.contains("premium")) {
            reply = "Nous avons 3 forfaits : Mensuel (flexible), Annuel (meilleur rapport prix) et Premium (tout inclus + aide prioritaire). Gérez cela dans 'Mon Profil'. \uD83C\uDFAB";
        } else if (lower.contains("fidelit\u00e9") || lower.contains("point") || lower.contains("loyal")
                || lower.contains("badge") || lower.contains("recompense")) {
            reply = "Vos points de fid\u00e9lit\u00e9 s'accumulent \u00e0 chaque repas et participation. Ils vous permettent de d\u00e9bloquer des r\u00e9ductions sp\u00e9ciales sur la boutique LAMMA ! \u2B50";
        } else if (lower.contains("admin") || lower.contains("gestion") || lower.contains("dashboard")
                || lower.contains("statistique")) {
            reply = "Le Tableau de Bord Admin permet de g\u00e9rer les stocks, de suivre les inscriptions et de g\u00e9n\u00e9rer les rapports m\u00e9t\u00e9o pour les guides. \uD83D\uDCCA";
        } else if (lower.contains("aide") || lower.contains("besoin") || lower.contains("help")
                || lower.contains("comment") || lower.contains("marche")) {
            reply = "Besoin d'aide ? Je connais tout sur les repas, les pass, les factures et m\u00eame la m\u00e9t\u00e9o ! Posez-moi une question pr\u00e9cise. \uD83D\uDE4B";
        } else if (lower.contains("weather") || lower.contains("meteo") || lower.contains("m\u00e9t\u00e9o")
                || lower.contains("temps") || lower.contains("pluie") || lower.contains("soleil")) {
            reply = "La m\u00e9t\u00e9o en montagne est impr\u00e9visible ! Regardez le widget sur votre Dashboard pour les alertes de temp\u00eate et le vent. \u2600\uFE0F";
        } else if (lower.contains("merci") || lower.contains("thanks") || lower.contains("cool")
                || lower.contains("top")) {
            reply = "Avec plaisir ! Profitez bien de votre s\u00e9jour avec LAMMA ! \uD83C\uDFD4\uFE0F";
        } else {
            reply = "Je suis l'Assistant Local de LAMMA \uD83D\uDCE1. Pour l'instant, je ne reconnais pas cette question, mais essayez de me parler de 'repas', 'facture', 'abonnement' ou 'm\u00e9t\u00e9o'.";
        }

        addMessage(reply, false);
    }

    private void addMessage(String text, boolean isUser) {
        HBox row = new HBox();
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(280);
        lbl.setStyle(isUser
                ? "-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-padding: 10 15; -fx-background-radius: 15 15 2 15;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 10 15; -fx-background-radius: 15 15 15 2;");

        row.getChildren().add(lbl);
        chatContainer.getChildren().add(row);
    }
}
