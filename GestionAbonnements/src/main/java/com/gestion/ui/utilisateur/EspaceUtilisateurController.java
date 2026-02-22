package com.gestion.ui.utilisateur;

import com.gestion.controllers.MainController;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Contrôleur de l'espace utilisateur : liste scrollable avec Mes
 * Participations,
 * Mes Repas, Restauration, Abonnement et boutons de navigation.
 */
public class EspaceUtilisateurController {

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private Label sectionLabel;

    private static byte[] readImageBytes(String imagePath) {
        try {
            return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(imagePath));
        } catch (Exception e) {
            return null;
        }
    }

    private static class UserSection {
        private final String id;
        private final String titre;
        private final String description;
        private final String fxmlPath;
        private final String icon;

        public UserSection(String id, String titre, String description, String fxmlPath, String icon) {
            this.id = id;
            this.titre = titre;
            this.description = description;
            this.fxmlPath = fxmlPath;
            this.icon = icon;
        }

        public String id() {
            return id;
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

    private static final List<UserSection> SECTIONS = List.of(
            new UserSection("evenement", "Découvrir Événements",
                    "Explorer les treks, soirées et séjours nature de 2026",
                    "/views/evenement/evenement-liste.fxml", "🏔️"),
            new UserSection("participation", "Mes Participations",
                    "Consulter et gérer mes inscriptions aux événements",
                    "/views/utilisateur/mes-participations.fxml", "👥"),
            new UserSection("repas", "Mes Repas",
                    "Voir mes plats et compositions",
                    "/views/repas/repas-liste.fxml", "🍕"),
            new UserSection("restauration", "Restauration",
                    "Menus, repas et personnalisation 2026",
                    "/views/utilisateur/restauration-2026.fxml", "🍴"),
            new UserSection("abonnement", "Mon Abonnement",
                    "Gérer mon abonnement LAMMA",
                    "/views/utilisateur/abonnement-choix.fxml", "📋"),
            new UserSection("chatbot", "Assistant LAMMA",
                    "Discuter avec l'IA pour obtenir de l'aide",
                    "/views/chatbot/chatbot-modal.fxml", "🤖"),
            new UserSection("map", "Carte Interactive",
                    "Explorer les refuges et restaurants sur la carte",
                    "/views/map/map-view.fxml", "🗺️"));

    @FXML
    public void initialize() {
        if (cardsContainer == null)
            return;
        cardsContainer.getChildren().clear();
        for (UserSection section : SECTIONS) {
            VBox card = createCard(section);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createCard(UserSection section) {
        VBox card = new VBox(12);
        card.getStyleClass().add("voyage-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(280);
        card.setMinHeight(140);
        card.setMaxWidth(320);

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
}
