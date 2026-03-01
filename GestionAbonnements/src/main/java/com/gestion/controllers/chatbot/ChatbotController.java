package com.gestion.controllers.chatbot;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Objects;

import com.gestion.services.IngredientServiceImpl;
import com.gestion.services.EvenementService;
import com.gestion.services.ParticipationServiceImpl;
import com.gestion.entities.Ingredient;

public class ChatbotController implements Initializable {

    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addMessage(
                "Hello! I am the LAMA Navigator \uD83C\uDFD4\uFE0F. "
                        + "I can help you with today's menus, ingredient details, or active trail events. "
                        + "How can I assist your mission?",
                false);

        chatContainer.heightProperty().addListener((obs, old, newVal) -> {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });

        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleSendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty())
            return;

        addMessage(message, true);
        inputField.clear();

        // Process with smart local responses using live database data
        Platform.runLater(() -> {
            String reply = generateSmartResponse(message);
            addMessage(reply, false);
        });
    }

    @FXML
    private void handleQuickAction(javafx.event.ActionEvent event) {
        javafx.scene.control.Button btn = (javafx.scene.control.Button) event.getSource();
        String text = btn.getText();

        if (text.contains("Menu")) {
            inputField.setText("Quel est le menu du jour ?");
        } else if (text.contains("Pass")) {
            inputField.setText("Quels sont mes pass actifs ?");
        } else if (text.contains("Allergies")) {
            inputField.setText("J'ai une allergie aux arachides, que puis-je manger ?");
        }

        handleSendMessage();
    }

    // ===================== SMART RESPONSE ENGINE =====================

    private String generateSmartResponse(String userMsg) {
        String lower = userMsg.toLowerCase();

        // --- MENU / RESTAURANT questions: answer with LIVE data ---
        if (lower.contains("menu") || lower.contains("plat") || lower.contains("dish")
                || lower.contains("restaurant") || lower.contains("manger") || lower.contains("repas")
                || lower.contains("food") || lower.contains("chef") || lower.contains("cuisine")
                || lower.contains("today") || lower.contains("jour")) {
            return buildLiveMenuResponse(lower);
        }

        // --- ALLERGY questions: cross-reference live data ---
        if (lower.contains("allerg") || lower.contains("intol\u00e9rance") || lower.contains("gluten")
                || lower.contains("arachide") || lower.contains("lactose") || lower.contains("noix")
                || lower.contains("nut") || lower.contains("soja") || lower.contains("oeuf")) {
            return buildAllergyResponse(lower);
        }

        // --- INGREDIENT questions ---
        if (lower.contains("ingr\u00e9dient") || lower.contains("ingrediant") || lower.contains("composition")
                || lower.contains("ingredient")) {
            return buildIngredientResponse();
        }

        // --- EVENT / PARTICIPATION questions ---
        if (lower.contains("participer") || lower.contains("inscription") || lower.contains("event")
                || lower.contains("\u00e9v\u00e9nement") || lower.contains("rejoindre") || lower.contains("evenement")
                || lower.contains("participant") || lower.contains("nombre") || lower.contains("combien")) {
            return buildEventResponse();
        }

        // --- SUBSCRIPTION / PASS questions ---
        if (lower.contains("abonnement") || lower.contains("forfait") || lower.contains("pass")
                || lower.contains("souscrire") || lower.contains("premium") || lower.contains("mensuel")
                || lower.contains("annuel")) {
            return "3 forfaits disponibles : \uD83C\uDFAB\n"
                    + "\u2022 Mensuel (flexible, id\u00e9al pour d\u00e9couvrir)\n"
                    + "\u2022 Annuel (meilleur prix, \u00e9conomisez jusqu'\u00e0 25%)\n"
                    + "\u2022 Premium (tout inclus + priorit\u00e9)\n"
                    + "G\u00e9rez vos abonnements dans 'Mon Profil'.";
        }

        // --- PRICE / REVENUE questions ---
        if (lower.contains("prix") || lower.contains("cher") || lower.contains("price") || lower.contains("pay")
                || lower.contains("co\u00fbt") || lower.contains("payer")) {
            return "Le prix d\u00e9pend de votre forfait. Le Pass Unique (25\u20ac) est id\u00e9al pour un tour ponctuel, "
                    + "mais les abonnements offrent jusqu'\u00e0 25% de r\u00e9duction ! \uD83D\uDCB0";
        }

        if (lower.contains("revenu") || lower.contains("total") || lower.contains("argent")
                || lower.contains("revenue")) {
            BigDecimal revenue = BigDecimal.ZERO;
            try {
                revenue = new com.gestion.services.AbonnementServiceImpl().findAll().stream()
                        .map(com.gestion.entities.Abonnement::getPrix)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception ignored) {
            }
            return "Le revenu total des abonnements est actuellement de " + revenue + " TND. \uD83D\uDCB8";
        }

        // --- FACTURE / INVOICE ---
        if (lower.contains("facture") || lower.contains("receipt") || lower.contains("invoice")
                || lower.contains("justifiant") || lower.contains("paiement")) {
            return "Vos factures sont disponibles imm\u00e9diatement apr\u00e8s validation d'un pass. "
                    + "Elles incluent le d\u00e9tail et l'ID de transaction. \uD83D\uDCC4";
        }

        // --- WEATHER ---
        if (lower.contains("weather") || lower.contains("meteo") || lower.contains("m\u00e9t\u00e9o")
                || lower.contains("temps") || lower.contains("pluie") || lower.contains("soleil")) {
            return buildWeatherResponse();
        }

        // --- GREETINGS ---
        if (lower.contains("bonjour") || lower.contains("salut") || lower.contains("hello") || lower.contains("hi")) {
            return "Bonjour ! Ravi de vous voir au camp de base de LAMMA. Comment puis-je vous aider aujourd'hui ? \uD83C\uDFD5\uFE0F";
        }

        // --- THANKS ---
        if (lower.contains("merci") || lower.contains("thanks") || lower.contains("cool") || lower.contains("top")) {
            return "Avec plaisir ! Profite bien de ton s\u00e9jour LAMMA ! \uD83C\uDFD5\uFE0F";
        }

        // --- HELP ---
        if (lower.contains("aide") || lower.contains("besoin") || lower.contains("help")
                || lower.contains("comment") || lower.contains("marche")) {
            return "Besoin d'aide ? Je peux vous renseigner sur :\n"
                    + "\u2022 \uD83C\uDF72 Les menus et plats du jour\n"
                    + "\u2022 \u26A0\uFE0F Les allerg\u00e8nes des plats\n"
                    + "\u2022 \uD83C\uDFAB Les abonnements et pass\n"
                    + "\u2022 \uD83C\uDFD4\uFE0F Les \u00e9v\u00e9nements et participations\n"
                    + "\u2022 \u2600\uFE0F La m\u00e9t\u00e9o\n"
                    + "Posez-moi une question pr\u00e9cise ! \uD83D\uDE4B";
        }

        // --- DASHBOARD / ADMIN ---
        if (lower.contains("admin") || lower.contains("gestion") || lower.contains("dashboard")
                || lower.contains("statistique")) {
            return "Le Tableau de Bord Admin permet de g\u00e9rer stocks, inscriptions et rapports m\u00e9t\u00e9o pour les guides. \uD83D\uDCCA";
        }

        // --- LOYALTY ---
        if (lower.contains("fid\u00e9lit\u00e9") || lower.contains("point") || lower.contains("loyal")
                || lower.contains("badge") || lower.contains("r\u00e9compense")) {
            return "Tes points de fid\u00e9lit\u00e9 s'accumulent \u00e0 chaque repas et participation. Ils d\u00e9bloquent des r\u00e9ductions boutique ! \u2B50";
        }

        // --- MAP ---
        if (lower.contains("carte") || lower.contains("map") || lower.contains("trouver")
                || lower.contains("refuge") || lower.contains("position") || lower.contains("gps")) {
            return "Consultez la Carte Interactive pour voir les refuges, sources d'eau et points de rendez-vous. T\u00e9l\u00e9chargez-la pour l'offline ! \uD83D\uDDFA\uFE0F";
        }

        // --- DEFAULT ---
        return "Je suis l'assistant LAMMA \uD83D\uDEF0\uFE0F. Je peux vous aider avec :\n"
                + "\u2022 Les menus du jour \uD83C\uDF72\n"
                + "\u2022 Les allerg\u00e8nes \u26A0\uFE0F\n"
                + "\u2022 Les abonnements \uD83C\uDFAB\n"
                + "\u2022 Les \u00e9v\u00e9nements \uD83C\uDFD4\uFE0F\n"
                + "Essayez de me poser une question !";
    }

    // ===================== LIVE DATA RESPONSES =====================

    private String buildLiveMenuResponse(String lower) {
        try {
            com.gestion.interfaces.RestaurantService restService = new com.gestion.services.RestaurantServiceImpl();
            com.gestion.interfaces.MenuService menuService = new com.gestion.services.MenuServiceImpl();
            com.gestion.interfaces.RepasDetailleService dishService = new com.gestion.services.RepasDetailleServiceImpl();

            java.time.LocalDate today = java.time.LocalDate.now();

            List<com.gestion.entities.Restaurant> restaurants = restService.findActifs();
            List<com.gestion.entities.Menu> allMenus = menuService.findAll();
            List<com.gestion.entities.RepasDetaille> allDishes = dishService.findAll();

            if (restaurants.isEmpty()) {
                return "Aucun restaurant actif pour le moment. V\u00e9rifiez plus tard ! \uD83C\uDFD5\uFE0F";
            }

            // Check if user asked about a specific restaurant
            com.gestion.entities.Restaurant targetRestaurant = null;
            for (com.gestion.entities.Restaurant r : restaurants) {
                if (lower.contains(r.getNom().toLowerCase())) {
                    targetRestaurant = r;
                    break;
                }
            }

            StringBuilder sb = new StringBuilder();

            if (targetRestaurant != null) {
                // Show menus for the specific restaurant
                sb.append("\uD83C\uDF72 Menus du restaurant **").append(targetRestaurant.getNom()).append("** :\n\n");
                appendRestaurantMenus(sb, targetRestaurant, allMenus, allDishes, today);
            } else {
                // Show all restaurants and their menus
                sb.append("\uD83C\uDF72 Voici les restaurants disponibles et leurs menus du jour :\n\n");
                for (com.gestion.entities.Restaurant r : restaurants) {
                    sb.append("\uD83C\uDFD5\uFE0F ").append(r.getNom()).append(" :\n");
                    appendRestaurantMenus(sb, r, allMenus, allDishes, today);
                    sb.append("\n");
                }
            }

            String result = sb.toString().trim();
            if (result.isEmpty()
                    || result.equals("\uD83C\uDF72 Voici les restaurants disponibles et leurs menus du jour :")) {
                return "Aucun menu disponible aujourd'hui. Les chefs pr\u00e9parent de nouveaux plats ! \uD83D\uDC68\u200D\uD83C\uDF73";
            }
            return result;

        } catch (Exception e) {
            System.err.println("Error fetching menu data: " + e.getMessage());
            return "Le Chef pr\u00e9pare des repas adapt\u00e9s (V\u00e9gan, Sans Gluten, etc.) dans l'Espace Restauration. "
                    + "Les menus sont con\u00e7us pour l'\u00e9nergie en montagne ! \uD83C\uDF72";
        }
    }

    private void appendRestaurantMenus(StringBuilder sb, com.gestion.entities.Restaurant restaurant,
            List<com.gestion.entities.Menu> allMenus, List<com.gestion.entities.RepasDetaille> allDishes,
            java.time.LocalDate today) {

        List<com.gestion.entities.Menu> dailyMenus = allMenus.stream()
                .filter(m -> m.getRestaurantId().equals(restaurant.getId()) && m.isActif())
                .filter(m -> !today.isBefore(m.getDateDebut()) && !today.isAfter(m.getDateFin()))
                .collect(Collectors.toList());

        if (dailyMenus.isEmpty()) {
            // Also show all active menus if no daily menu
            dailyMenus = allMenus.stream()
                    .filter(m -> m.getRestaurantId().equals(restaurant.getId()) && m.isActif())
                    .collect(Collectors.toList());
        }

        if (dailyMenus.isEmpty()) {
            sb.append("  Pas de menu disponible actuellement.\n");
        } else {
            for (com.gestion.entities.Menu m : dailyMenus) {
                sb.append("  \uD83D\uDCCB ").append(m.getNom());
                if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                    sb.append(" - ").append(m.getDescription());
                }
                sb.append("\n");

                List<com.gestion.entities.RepasDetaille> menuDishes = allDishes.stream()
                        .filter(d -> m.getDishesIds().contains(d.getId()))
                        .collect(Collectors.toList());

                for (com.gestion.entities.RepasDetaille d : menuDishes) {
                    sb.append("    \u2022 ").append(d.getNom());
                    String allergens = d.getAllergenesAsString();
                    if (allergens != null && !allergens.isEmpty() && !allergens.equals("Aucun")) {
                        sb.append(" \u26A0\uFE0F(").append(allergens).append(")");
                    }
                    sb.append("\n");
                }
            }
        }
    }

    private String buildAllergyResponse(String lower) {
        try {
            com.gestion.interfaces.RepasDetailleService dishService = new com.gestion.services.RepasDetailleServiceImpl();
            List<com.gestion.entities.RepasDetaille> allDishes = dishService.findAll();

            if (allDishes.isEmpty()) {
                return "Aucun plat n'est disponible actuellement pour v\u00e9rifier les allerg\u00e8nes. \u26A0\uFE0F";
            }

            // Detect what allergy the user has
            List<String> userAllergens = new ArrayList<>();
            if (lower.contains("arachide") || lower.contains("cacahu\u00e8te") || lower.contains("peanut"))
                userAllergens.add("Arachides");
            if (lower.contains("gluten"))
                userAllergens.add("Gluten");
            if (lower.contains("lactose") || lower.contains("lait") || lower.contains("dairy"))
                userAllergens.add("Lactose");
            if (lower.contains("noix") || lower.contains("nut"))
                userAllergens.add("Noix");
            if (lower.contains("soja") || lower.contains("soy"))
                userAllergens.add("Soja");
            if (lower.contains("oeuf") || lower.contains("egg"))
                userAllergens.add("Oeufs");
            if (lower.contains("poisson") || lower.contains("fish"))
                userAllergens.add("Poisson");
            if (lower.contains("crustac") || lower.contains("shellfish"))
                userAllergens.add("Crustac\u00e9s");

            if (userAllergens.isEmpty()) {
                userAllergens.add("allerg\u00e8ne");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\u26A0\uFE0F Analyse des allerg\u00e8nes pour : ").append(String.join(", ", userAllergens))
                    .append("\n\n");

            List<com.gestion.entities.RepasDetaille> safeDishes = new ArrayList<>();
            List<com.gestion.entities.RepasDetaille> dangerousDishes = new ArrayList<>();

            for (com.gestion.entities.RepasDetaille d : allDishes) {
                String allergens = d.getAllergenesAsString().toLowerCase();
                boolean dangerous = false;
                for (String ua : userAllergens) {
                    if (allergens.contains(ua.toLowerCase())) {
                        dangerous = true;
                        break;
                    }
                }
                if (dangerous) {
                    dangerousDishes.add(d);
                } else {
                    safeDishes.add(d);
                }
            }

            if (!dangerousDishes.isEmpty()) {
                sb.append("\u274C Plats \u00e0 \u00e9viter :\n");
                for (com.gestion.entities.RepasDetaille d : dangerousDishes) {
                    sb.append("  \u2022 ").append(d.getNom()).append(" (").append(d.getAllergenesAsString())
                            .append(")\n");
                }
                sb.append("\n");
            }

            if (!safeDishes.isEmpty()) {
                sb.append("\u2705 Plats s\u00fbrs pour vous :\n");
                for (com.gestion.entities.RepasDetaille d : safeDishes) {
                    sb.append("  \u2022 ").append(d.getNom()).append("\n");
                }
            }

            return sb.toString().trim();

        } catch (Exception e) {
            return "Impossible de v\u00e9rifier les allerg\u00e8nes pour le moment. Consultez le personnel du restaurant ! \u26A0\uFE0F";
        }
    }

    private String buildIngredientResponse() {
        try {
            IngredientServiceImpl ingService = new IngredientServiceImpl();
            List<Ingredient> ingredients = ingService.findAll();
            if (ingredients.isEmpty()) {
                return "Aucun ingr\u00e9dient disponible pour le moment. \uD83E\uDDBA";
            }
            StringBuilder sb = new StringBuilder("\uD83E\uDDBA Ingr\u00e9dients disponibles :\n\n");
            for (Ingredient i : ingredients) {
                sb.append("  \u2022 ").append(i.getNom());
                if (i.getPrixSupplement() != null) {
                    sb.append(" (+").append(i.getPrixSupplement()).append(" TND)");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Impossible de charger les ingr\u00e9dients pour le moment. \uD83E\uDDBA";
        }
    }

    private String buildEventResponse() {
        try {
            EvenementService eventService = new EvenementService();
            ParticipationServiceImpl partService = new ParticipationServiceImpl();
            List<com.gestion.entities.Evenement> events = eventService.findAll();

            if (events.isEmpty()) {
                return "Aucun \u00e9v\u00e9nement actif pour le moment. Restez \u00e0 l'\u00e9coute ! \uD83C\uDFD4\uFE0F";
            }

            StringBuilder sb = new StringBuilder("\uD83C\uDFD4\uFE0F \u00c9v\u00e9nements actifs :\n\n");
            for (com.gestion.entities.Evenement e : events) {
                long count = partService.findAll().stream()
                        .filter(p -> p.getEvenementId() != null && p.getEvenementId().intValue() == e.getIdEvent())
                        .count();
                sb.append("  \u2022 ").append(e.getTitre()).append(" (").append(count).append(" participants)\n");
            }
            sb.append("\nPour rejoindre, allez dans 'Participations' !");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Impossible de charger les \u00e9v\u00e9nements pour le moment. \uD83C\uDFD4\uFE0F";
        }
    }

    private String buildWeatherResponse() {
        try {
            com.gestion.services.WeatherService.WeatherInfo weather = com.gestion.services.WeatherService.getInstance()
                    .getCurrentWeather("Tunis");
            return "\u2600\uFE0F M\u00e9t\u00e9o \u00e0 Tunis : " + weather.temp + " (" + weather.condition + ")\n"
                    + "V\u00e9rifiez le widget Dashboard pour les alertes temp\u00eate ! \u2744\uFE0F";
        } catch (Exception e) {
            return "La m\u00e9t\u00e9o en montagne est impr\u00e9visible ! V\u00e9rifie le widget Dashboard pour alertes. \u2600\uFE0F\u2744\uFE0F";
        }
    }

    // ===================== UI HELPERS =====================

    private void addMessage(String text, boolean isUser) {
        HBox row = new HBox(8);
        row.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);

        if (isUser) {
            bubble.setStyle(
                    "-fx-background-color: #3b82f6; -fx-text-fill: white; "
                            + "-fx-padding: 10 14; -fx-background-radius: 18 18 4 18; -fx-font-size: 13;");
        } else {
            bubble.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; "
                            + "-fx-padding: 10 14; -fx-background-radius: 18 18 18 4; -fx-font-size: 13; "
                            + "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 18 18 18 4;");
        }

        row.getChildren().add(bubble);
        chatContainer.getChildren().add(row);

        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
            scrollPane.requestLayout();
        });
    }
}
