package com.gestion.ui.utilisateur;

import com.gestion.entities.Participation;
import com.gestion.services.TicketBadgeGenerator;
import com.gestion.ui.participation.ParticipationFormController;
// import com.gestion.ui.restaurant.RestaurantListeController; // Unused
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Vue simplifiée "Mes participations" pour l'espace utilisateur.
 * Affiche les participations filtrées par userId courant.
 */
public class MesParticipationsController {

    @FXML
    private FlowPane flowEvents;
    @FXML
    private Label statusLabel;
    @FXML
    private Label countLabel;

    private final com.gestion.interfaces.ParticipationService participationService = new com.gestion.services.ParticipationServiceImpl();
    private final com.gestion.interfaces.AbonnementService abonnementService = new com.gestion.services.AbonnementServiceImpl();
    private Long currentUserId = com.gestion.tools.Session.getInstance().getCurrentUserId() != null
            ? Long.valueOf(com.gestion.tools.Session.getInstance().getCurrentUserId())
            : null;

    @FXML
    public void initialize() {
        onActualiser();
    }

    private javafx.scene.Node createCard(Participation item) {
        // Main Card Container - Grid Style
        VBox card = new VBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new javafx.geometry.Insets(20));
        card.setPrefWidth(260); // Fixed width for grid
        card.setMinWidth(260);
        card.setMaxWidth(260);
        card.setMinHeight(320);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15;");

        // Icon - Scout Box Style
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(44, 44);
        iconPane.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;");
        Text icon = new Text(getItemIcon(item.getType()));
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        icon.setFill(Color.WHITE);
        iconPane.getChildren().addAll(icon);

        // Content
        VBox content = new VBox(8);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Mission #" + item.getId());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 800;");

        Label statusBadge = new Label(item.getStatut() != null ? item.getStatut().getLabel().toUpperCase() : "INCONNU");
        statusBadge.getStyleClass().add("status-badge");
        if (item.getStatut() == Participation.StatutParticipation.CONFIRME) {
            statusBadge.setStyle(
                    "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 9;");
        } else if (item.getStatut() == Participation.StatutParticipation.EN_ATTENTE) {
            statusBadge.setStyle(
                    "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 9;");
        } else {
            statusBadge.setStyle(
                    "-fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 9;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        GridPane details = new GridPane();
        details.setHgap(30);
        details.setVgap(6);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH);
        String dateStr = item.getDateInscription() != null ? item.getDateInscription().format(formatter)
                : "Non programmée";

        details.add(createDetailLabel("📅", "Date", dateStr), 0, 0);
        details.add(createDetailLabel("⛰️", "Evenement", "EXP-" + item.getEvenementId()), 1, 0);
        details.add(createDetailLabel("👥", "Escouade", item.getTotalParticipants() + " Explorateurs"), 0, 1);
        details.add(createDetailLabel("🍱", "Ravitaillement",
                item.getMealOption() != null ? item.getMealOption().getLabel() : "Standard"), 1, 1);

        content.getChildren().addAll(header, details);

        // Right side: Price + Actions
        VBox rightSide = new VBox(10);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        String priceStr = String.format("%.2f €", item.getMontantCalcule() != null ? item.getMontantCalcule() : 0.0);
        Label price = new Label(priceStr);
        price.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 800;");

        Button badgeBtn = new Button("📥 Badge PDF");
        boolean isConfirmed = item.getStatut() == Participation.StatutParticipation.CONFIRME;
        badgeBtn.setDisable(!isConfirmed);
        badgeBtn.setStyle(isConfirmed
                ? "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: rgba(148,163,184,0.1); -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 8;");
        badgeBtn.setOnAction(e -> downloadBadgeForParticipation(item));

        rightSide.getChildren().addAll(price, badgeBtn);

        card.getChildren().addAll(iconPane, content, rightSide);

        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 15; -fx-border-color: rgba(34, 197, 94, 0.3); -fx-border-radius: 15; -fx-translate-y: -5;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 15; -fx-translate-y: 0;"));

        return card;
    }

    private String getItemIcon(Participation.TypeParticipation type) {
        if (type == null)
            return "🎫";
        switch (type) {
            case GROUPE:
                return "👥";
            case HEBERGEMENT:
                return "🏨";
            case SIMPLE:
                return "👤";
            default:
                return "🎫";
        }
    }

    private HBox createDetailLabel(String icon, String labelText, String valueText) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size: 12px;");

        VBox texts = new VBox(0);
        Label label = new Label(labelText.toUpperCase());
        label.setStyle("-fx-text-fill: #64748b; -fx-font-size: 8px; -fx-font-weight: bold;");

        Label value = new Label(valueText);
        value.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 500;");

        texts.getChildren().addAll(label, value);
        box.getChildren().addAll(lblIcon, texts);
        return box;
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        // Obsolete for grid view
    }

    @FXML
    public void onActualiser() {
        Platform.runLater(() -> {
            try {
                var participations = participationService.findByUserId(currentUserId);
                flowEvents.getChildren().clear();

                if (participations.isEmpty()) {
                    VBox emptyState = new VBox(15);
                    emptyState.setAlignment(Pos.CENTER);
                    emptyState.setPadding(new javafx.geometry.Insets(60));
                    Label l1 = new Label("⛰️ AUCUNE EXPÉDITION");
                    l1.setStyle("-fx-font-size: 18; -fx-text-fill: #64748b; -fx-font-weight: 800;");
                    Label l2 = new Label("Rejoignez une expédition pour commencer votre aventure.");
                    l2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-text-alignment: center;");
                    emptyState.getChildren().addAll(l1, l2);
                    flowEvents.getChildren().add(emptyState);
                } else {
                    for (Participation p : participations) {
                        flowEvents.getChildren().add(createCard(p));
                    }
                }

                if (statusLabel != null) {
                    statusLabel.setText(participations.isEmpty()
                            ? "Aucune participation trouvée"
                            : participations.size() + " participation(s) chargée(s)");
                }
                if (countLabel != null) {
                    countLabel.setText(participations.size() + " participation(s)");
                }
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("Erreur chargement : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Ouverture du formulaire dédié pour permettre à l'utilisateur
     * de créer lui‑même une participation.
     */
    @FXML
    public void onCreerParticipation() {
        if (!hasActiveSubscription()) {
            showSubscriptionChoice();
        } else {
            openParticipationForm();
        }
    }

    private boolean hasActiveSubscription() {
        if (currentUserId == null)
            return false;
        return abonnementService.findByUserId(currentUserId).stream()
                .anyMatch(a -> a.getStatut() == com.gestion.entities.Abonnement.StatutAbonnement.ACTIF
                        && a.getDateFin().isAfter(java.time.LocalDate.now()));
    }

    private void showSubscriptionChoice() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Choix de Participation");
        alert.setHeaderText("Souscription Requise");
        alert.setContentText("Vous n'avez pas d'abonnement actif. Comment souhaitez-vous participer ?");

        ButtonType btnAbonnement = new ButtonType("S'abonner (Global)");
        ButtonType btnPass = new ButtonType("Prendre un Pass (Unique)");
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnAbonnement, btnPass, btnCancel);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnAbonnement) {
                com.gestion.controllers.MainController.getInstance()
                        .loadUserSection("/views/utilisateur/abonnement-choix.fxml", "Abonnements");
            } else if (type == btnPass) {
                openParticipationForm();
            }
        });
    }

    private void openParticipationForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation/participation-form.fxml"));
            Parent root = loader.load();

            ParticipationFormController formController = loader.getController();
            formController.setAdminMode(false);
            formController.setCurrentUserId(currentUserId);

            formController.setOnSaved(participation -> {
                onActualiser();
                handleMealRouting(participation);
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Créer une participation");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Impossible d'ouvrir le formulaire de participation");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    private void handleMealRouting(Participation p) {
        if (p == null || p.getMealOption() == null)
            return;

        String targetFxml = null;
        String title = "";

        switch (p.getMealOption()) {
            case AVEC_REPAS:
            case AVEC_MENU:
            case COMPOSITION_SUR_PLACE:
                targetFxml = "/views/utilisateur/restauration-2026.fxml";
                title = (p.getMealOption() == com.gestion.entities.Participation.MealOption.COMPOSITION_SUR_PLACE)
                        ? "Composez votre plat (Expédition)"
                        : "Catalogue des plats";
                break;
            case SANS_REPAS:
            case AU_RESTAURANT:
                targetFxml = "/views/restaurant/restaurant-liste.fxml";
                title = "Explorer les restaurants du camp";
                break;
            default:
                return; // Nothing to do
        }

        if (targetFxml != null) {
            com.gestion.controllers.MainController main = com.gestion.controllers.MainController.getInstance();
            if (main != null) {
                if (targetFxml.contains("restaurant-liste.fxml")) {
                    main.loadUserSection(targetFxml, title, ctrl -> {
                        if (ctrl instanceof Restauration2026Controller && p.getRestaurantId() != null) {
                            ((Restauration2026Controller) ctrl).setRestaurantFilter(p.getRestaurantId());
                        }
                    });
                } else {
                    main.loadUserSection(targetFxml, title);
                }
                showInfo("🧭 Redirection", "Selon votre choix de repas (" + p.getMealOption().getLabel()
                        + "), nous avons ouvert : " + title);
            }
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show(); // Non-blocking
    }

    // ─── BADGE PDF ─────────────────────────────────────────────────────────

    /**
     * Handler du bouton global : télécharge le badge de la participation
     * sélectionnée dans la liste.
     */
    @FXML
    public void onTelechargerBadge() {
        // Selection is now handled visually or via specific card buttons
        showInfo("💡 Astuce",
                "Veuillez cliquer sur le bouton '📥 Badge PDF' directement sur la carte de votre expédition confirmée.");
    }

    /**
     * Ouvre un FileChooser, génère et sauvegarde le badge PDF.
     */
    private void downloadBadgeForParticipation(Participation item) {
        if (item.getStatut() != Participation.StatutParticipation.CONFIRME) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Expédition non confirmée");
            warn.setHeaderText("Téléchargement impossible");
            warn.setContentText(
                    "Votre participation doit être approuvée par l'administration avant de pouvoir générer votre badge.");
            warn.showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le badge PDF");
        chooser.setInitialFileName("badge_participation_" + item.getId() + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        Stage stage = (Stage) flowEvents.getScene().getWindow();
        File dest = chooser.showSaveDialog(stage);
        if (dest == null)
            return; // annulé

        // Préparer les données
        String ticketCode = item.getBadgeAssocie() != null && !item.getBadgeAssocie().isEmpty()
                ? "TKT-" + item.getId() + "-" + item.getBadgeAssocie()
                : "TKT-" + item.getId() + "-" + System.currentTimeMillis();

        // Nom utilisateur depuis la session
        String userName = "Utilisateur #" + (currentUserId != null ? currentUserId : "?");
        try {
            com.gestion.tools.Session session = com.gestion.tools.Session.getInstance();
            if (session.getCurrentUser() != null && session.getCurrentUser().getName() != null) {
                userName = session.getCurrentUser().getName();
            }
        } catch (Exception ignored) {
        }

        String eventName = "Événement #" + item.getEvenementId();
        String statut = item.getStatut() != null ? item.getStatut().getLabel() : "N/A";
        String type = item.getType() != null ? item.getType().getLabel() : "N/A";
        String montant = item.getMontantCalcule() != null
                ? String.format("%.2f %s", item.getMontantCalcule(),
                        item.getDevise() != null ? item.getDevise() : "EUR")
                : "Gratuit";

        try {
            TicketBadgeGenerator.generateBadge(
                    dest, ticketCode, userName, eventName,
                    item.getId() != null ? item.getId() : 0L,
                    item.getDateInscription(),
                    item.getBadgeAssocie() != null ? item.getBadgeAssocie() : "N/A",
                    statut, type,
                    item.getTotalParticipants(),
                    montant);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Badge généré !");
            ok.setHeaderText(null);
            ok.setContentText("✅ Badge PDF sauvegardé :\n" + dest.getAbsolutePath());
            ok.showAndWait();

        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur de génération");
            err.setHeaderText("Impossible de générer le badge");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }
}