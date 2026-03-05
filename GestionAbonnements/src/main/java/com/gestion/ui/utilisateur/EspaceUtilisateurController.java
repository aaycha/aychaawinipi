package com.gestion.ui.utilisateur;

import com.gestion.controllers.MainController;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import com.gestion.tools.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur de l'espace utilisateur : liste scrollable avec Mes
 * Participations,
 * Mes Repas, Restauration, Abonnement et boutons de navigation.
 */
public class EspaceUtilisateurController {

    @FXML
    private TilePane cardsContainer;
    @FXML
    private Label sectionLabel;
    @FXML
    private Label weatherTemp;
    @FXML
    private Label weatherCondition;
    @FXML
    private Label weatherLocation;

    private final com.gestion.services.WeatherService weatherService = com.gestion.services.WeatherService
            .getInstance();

    private static class UserSection {
        private final String titre;
        private final String description;
        private final String fxmlPath;
        private final String icon;

        public UserSection(String id, String titre, String description, String fxmlPath, String icon) {
            this.titre = titre;
            this.description = description;
            this.fxmlPath = fxmlPath;
            this.icon = icon;
        }

        public String titre() {
            return titre;
        }

        public String description() {
            return description;
        }

        public String fxmlPath() {
            return fxmlPath;
        }

        public String icon() {
            return icon;
        }
    }

    private static final List<UserSection> BASE_SECTIONS = List.of(
            new UserSection("participation", "Mes Participations",
                    "Consulter et gérer mes inscriptions aux événements",
                    "/views/utilisateur/mes-participations.fxml", "👥"),
            new UserSection("restauration", "Trail Rations 2026",
                    "Explorer et commander vos provisions de haute montagne",
                    "/views/utilisateur/restauration-2026.fxml", "🏔️"),
            new UserSection("composition", "Ma Composition",
                    "Cuisiner et personnaliser vos repas d'expédition",
                    "/views/utilisateur/dish-composition-modal.fxml", "🍳"),
            new UserSection("repas", "Catalogue des Repas",
                    "Découvrir tous les plats disponibles sur le trail",
                    "/views/repas/repas-liste.fxml", "🍱"),
            new UserSection("menu", "Menus d'Expédition",
                    "Consulter les menus complets du camp de base",
                    "/views/menu/menu-liste.fxml", "📜"),
            new UserSection("evenements", "Liste des Événements",
                    "Découvrir les treks, soirées et séjours nature à venir",
                    "/Feryel/ListeEvenements.fxml", "🎪"),
            new UserSection("map", "Carte Interactive",
                    "Explorer les refuges et restaurants sur la carte",
                    "/views/map/map-view.fxml", "🗺️"),
            new UserSection("abonnement", "Mon Abonnement",
                    "Gérer mon abonnement LAMMA",
                    "/views/utilisateur/abonnement-choix.fxml", "📋"),
            new UserSection("panier", "Mon Panier",
                    "Gérer vos commandes, modifier les quantités ou supprimer des articles avant paiement",
                    "/views/utilisateur/checkout-modal.fxml", "🛒"),
            new UserSection("chatbot", "LAMA AI Assistant",
                    "Posez vos questions sur les menus, ingrédients ou événements",
                    "/views/chatbot/chatbot-modal.fxml", "🤖"),
            new UserSection("communaute", "Communauté LAMMA",
                    "Partagez vos aventures et échangez avec les autres explorateurs",
                    "/views/reddit.fxml", "💬"),
            new UserSection("visualscout", "Visual Scout satellite",
                    "Analysez les environs et les refuges grâce à l'IA satellite",
                    "/views/utilisateur/visual-scout.fxml", "🛰️"),
            new UserSection("tiktok", "TikTok Video Intel",
                    "Découvrez les retours viraux sur vos destinations d'aventure",
                    "/views/utilisateur/tiktok-search.fxml", "🎵"),
            new UserSection("boutique", "Boutique LAMA",
                    "Gérez vos équipements de trekking (Achat/Location)",
                    "/views/wael/EquipementStoreView.fxml", "🛒"),
            new UserSection("messagerie", "Messagerie Chat",
                    "Échangez en temps réel avec la communauté LAMMA",
                    "/views/wael/ChatView.fxml", "💬"));

    @FXML
    public void initialize() {
        if (cardsContainer == null)
            return;
        cardsContainer.getChildren().clear();

        List<UserSection> sectionsToDisplay = new ArrayList<>(BASE_SECTIONS);

        // Dynamic Check: Keep only for specialized logic if needed, otherwise just list
        // base sections
        try {
            Session session = Session.getInstance();
            if (session.isLoggedIn()) {
                // We keep the dynamic check section empty or remove if fully static
                // User requested them to be added, so they are now in BASE_SECTIONS
            }
        } catch (Exception e) {
            System.err.println("Error checking dynamic sections: " + e.getMessage());
        }

        for (UserSection section : sectionsToDisplay) {
            VBox card = createCard(section);
            cardsContainer.getChildren().add(card);
        }
        fetchWeather();
    }

    private void fetchWeather() {
        javafx.application.Platform.runLater(() -> {
            new Thread(() -> {
                com.gestion.services.WeatherService.WeatherInfo info = weatherService.getCurrentWeather("Tunis");
                javafx.application.Platform.runLater(() -> {
                    if (weatherTemp != null)
                        weatherTemp.setText(info.temp);
                    if (weatherCondition != null)
                        weatherCondition.setText(info.condition);
                    if (weatherLocation != null)
                        weatherLocation.setText("\uD83D\uDCCD " + info.city);
                });
            }).start();
        });
    }

    private VBox createCard(UserSection section) {
        VBox card = new VBox(12);
        card.getStyleClass().add("voyage-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setMinHeight(180);
        card.setMaxWidth(300);

        card.setOnMouseEntered(e -> card.setCursor(Cursor.HAND));
        card.setOnMouseClicked(e -> chargerSection(section));

        Label icon = new Label(section.icon());
        icon.getStyleClass().add("voyage-card-icon");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #1890ff;");

        Label titre = new Label(section.titre());
        titre.getStyleClass().add("voyage-card-titre");
        titre.setWrapText(true);

        Label desc = new Label(section.description());
        desc.getStyleClass().add("voyage-card-desc");
        desc.setWrapText(true);
        desc.setMaxWidth(260);

        Button btn = new Button("Accéder →");
        btn.getStyleClass().addAll("voyage-card-btn", "btn-primary");
        btn.setOnAction(e -> chargerSection(section));

        card.getChildren().addAll(icon, titre, desc, btn);
        return card;
    }

    private void chargerSection(UserSection section) {
        if (sectionLabel != null) {
            sectionLabel.setText("Section : " + section.titre());
        }
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadUserSection(section.fxmlPath(), section.titre());
        }
    }

    @FXML
    void onRetourAccueil() {
        MainController main = MainController.getInstance();
        if (main != null) {
            main.retourDashboard();
        }
        if (sectionLabel != null) {
            sectionLabel.setText("Sélectionnez une section ci-dessous");
        }
    }

    @FXML
    void onCommunityNavigation() {
        if (sectionLabel != null) {
            sectionLabel.setText("Section : Communauté LAMMA");
        }
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadUserSection("/views/reddit.fxml", "Communauté LAMMA");
        }
    }

    @FXML
    void onTikTokNavigation() {
        if (sectionLabel != null) {
            sectionLabel.setText("Section : TikTok Expedition Intel");
        }
        MainController main = MainController.getInstance();
        if (main != null) {
            main.loadUserSection("/views/utilisateur/tiktok-search.fxml", "TikTok Expedition Intel");
        }
    }
}
