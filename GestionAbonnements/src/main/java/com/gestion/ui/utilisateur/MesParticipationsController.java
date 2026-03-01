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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Vue simplifiée "Mes participations" pour l'espace utilisateur.
 * Affiche les participations filtrées par userId courant.
 */
public class MesParticipationsController {

    @FXML
    private ListView<Participation> listView;
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
        setupListView();
        onActualiser();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Participation>() {
            @Override
            protected void updateItem(Participation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createCard(Participation item) {
        // Main Card Container
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconPane = new StackPane();
        Circle bg = new Circle(20, Color.web("#e7f1ff"));
        Text icon = new Text(getItemIcon(item.getType()));
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Participation #" + item.getId());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.getStatut() != null ? item.getStatut().name() : "N/A");
        statusBadge.getStyleClass().add("status-badge");
        if (item.getStatut() != null) {
            statusBadge.getStyleClass().add(item.getStatut().name());
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = item.getDateInscription() != null ? item.getDateInscription().format(formatter) : "N/A";

        details.add(createDetailLabel("📅 Date:", dateStr), 0, 0);
        details.add(createDetailLabel("🎟️ Event:", "#" + item.getEvenementId()), 1, 0);
        details.add(createDetailLabel("👥 Groupe:", item.getTotalParticipants() + " pers."), 0, 1);
        details.add(createDetailLabel("📄 Plan:",
                item.getTypeAbonnementChoisi() != null ? item.getTypeAbonnementChoisi() : "Standard"), 1, 1);

        content.getChildren().addAll(header, details);

        // Right side: Price + Badge button
        VBox rightSide = new VBox(8);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        String priceStr = String.format("%.2f €", item.getMontantCalcule() != null ? item.getMontantCalcule() : 0.0);
        Label price = new Label(priceStr);
        price.getStyleClass().add("card-price");

        Button badgeBtn = new Button("📥 Badge");
        boolean isConfirmed = item.getStatut() == Participation.StatutParticipation.CONFIRME;
        badgeBtn.setDisable(!isConfirmed);
        badgeBtn.setTooltip(new Tooltip(isConfirmed ? "Télécharger le badge" : "Attente d'approbation admin"));
        badgeBtn.setStyle("-fx-background-color: " + (isConfirmed ? "rgba(99,102,241,0.85)" : "rgba(148,163,184,0.4)")
                + "; -fx-text-fill: white;"
                + " -fx-font-size: 11; -fx-font-weight: bold; -fx-padding: 6 14;"
                + " -fx-background-radius: 20; -fx-cursor: " + (isConfirmed ? "hand" : "default") + ";");
        badgeBtn.setOnAction(e -> downloadBadgeForParticipation(item));
        rightSide.getChildren().addAll(price, badgeBtn);

        card.getChildren().addAll(iconPane, content, rightSide);
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

    private HBox createDetailLabel(String labelText, String valueText) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("card-label");
        Label value = new Label(valueText);
        label.getStyleClass().add("card-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            Participation selection = listView.getSelectionModel().getSelectedItem();
            if (selection != null) {
                // Future: open details
            }
        }
    }

    @FXML
    public void onActualiser() {
        Platform.runLater(() -> {
            try {
                var participations = participationService.findByUserId(currentUserId);
                listView.getItems().setAll(participations);
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
        Participation selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Sélection requise");
            info.setHeaderText(null);
            info.setContentText("Veuillez d'abord sélectionner une participation dans la liste.");
            info.showAndWait();
            return;
        }
        downloadBadgeForParticipation(selected);
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

        Stage stage = (Stage) listView.getScene().getWindow();
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