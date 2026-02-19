package com.gestion.ui.utilisateur;

import com.gestion.entities.Participation;
import com.gestion.interfaces.ParticipationService;
import com.gestion.services.ParticipationServiceImpl;
import com.gestion.ui.participation.ParticipationFormController;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;

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

    private final ParticipationService participationService = new ParticipationServiceImpl();
    // TODO: à remplacer par l'utilisateur connecté
    private Long currentUserId = 1L;

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

        // Right side: Price
        VBox rightSide = new VBox(5);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        String priceStr = String.format("%.2f €", item.getMontantCalcule() != null ? item.getMontantCalcule() : 0.0);
        Label price = new Label(priceStr);
        price.getStyleClass().add("card-price");
        rightSide.getChildren().add(price);

        card.getChildren().addAll(iconPane, content, rightSide);
        return card;
    }

    private String getItemIcon(Participation.TypeParticipation type) {
        if (type == null)
            return "🎫";
        return switch (type) {
            case GROUPE -> "👥";
            case HEBERGEMENT -> "🏨";
            case SIMPLE -> "👤";
            default -> "🎫";
        };
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation/participation-form.fxml"));
            Parent root = loader.load();

            ParticipationFormController formController = loader.getController();
            formController.setAdminMode(false);
            formController.setCurrentUserId(currentUserId);
            formController.setOnSaved(this::onActualiser);

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
}