package com.gestion.ui.utilisateur;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vue simplifiée "Mon Abonnement" pour l'espace utilisateur.
 * Affiche les abonnements filtrés sur userId.
 */
public class AbonnementChoixController {

    @FXML
    private ListView<Abonnement> listView;
    @FXML
    private Label statusLabel;

    private final AbonnementController controller = new AbonnementController();
    // TODO: remplacer par l'utilisateur connecté
    private Long currentUserId = 1L;

    @FXML
    public void initialize() {
        setupListView();
        onActualiser();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Abonnement>() {
            @Override
            protected void updateItem(Abonnement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createAbonnementCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createAbonnementCard(Abonnement item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconPane = new StackPane();
        Circle bg = new Circle(20, Color.web("#e7f5ff"));
        Text icon = new Text("🎫");
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.getType() != null ? item.getType().getLabel() : "Abonnement #" + item.getId());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.getStatut() != null ? item.getStatut().getLabel().toUpperCase() : "INCONNU");
        statusBadge.getStyleClass().add("status-badge");
        if (item.getStatut() == Abonnement.StatutAbonnement.ACTIF) {
            statusBadge.setStyle("-fx-background-color: #28a745;");
        } else {
            statusBadge.setStyle("-fx-background-color: #6c757d;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFin = item.getDateFin() != null ? item.getDateFin().format(df) : "Indéfinie";

        details.add(createDetailLabel("📅 Expire:", dateFin), 0, 0);
        details.add(createDetailLabel("🔄 Renouvellement:", item.isAutoRenew() ? "Auto" : "Manuel"), 1, 0);

        content.getChildren().addAll(header, details);

        // Right side: Price
        VBox rightSide = new VBox(5);
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%.2f €", item.getPrix() != null ? item.getPrix() : 0.0));
        price.getStyleClass().add("card-price");
        rightSide.getChildren().add(price);

        card.getChildren().addAll(iconPane, content, rightSide);
        return card;
    }

    private HBox createDetailLabel(String labelText, String valueText) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("card-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("card-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    @FXML
    public void onActualiser() {
        if (listView == null)
            return;
        Platform.runLater(() -> {
            try {
                List<Abonnement> all = controller.getAll();
                List<Abonnement> mine = all.stream()
                        .filter(a -> a.getUserId() != null && a.getUserId().equals(currentUserId))
                        .collect(Collectors.toList());
                listView.getItems().setAll(mine);
                if (statusLabel != null) {
                    statusLabel.setText(mine.isEmpty()
                            ? "Aucun abonnement trouvé pour l'utilisateur " + currentUserId
                            : mine.size() + " abonnement(s) trouvé(s)");
                }
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("Erreur chargement : " + e.getMessage());
                }
            }
        });
    }
}
