package com.gestion.ui.utilisateur;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import com.gestion.ui.abonnement.FactureController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vue simplifiée "Mon Abonnement" pour l'espace utilisateur.
 * Affiche les abonnements filtrés sur userId.
 */
public class AbonnementChoixController {

    @FXML
    private TabPane mainTabPane;
    @FXML
    private ListView<Abonnement> listView;
    @FXML
    private Label statusLabel;

    private final AbonnementController controller = new AbonnementController();
    // Récupéré depuis l'utilisateur connecté
    private Long currentUserId = com.gestion.tools.Session.getInstance().getCurrentUserId() != null
            ? Long.valueOf(com.gestion.tools.Session.getInstance().getCurrentUserId())
            : 1L;

    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

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

    // Handlers pour les nouveaux boutons d'abonnement
    @FXML
    private void onSubscribeMensuel() {
        subscribe(Abonnement.TypeAbonnement.MENSUEL, 14.99);
    }

    @FXML
    private void onSubscribeAnnuel() {
        subscribe(Abonnement.TypeAbonnement.ANNUEL, 149.99);
    }

    @FXML
    private void onSubscribePremium() {
        subscribe(Abonnement.TypeAbonnement.PREMIUM, 299.99);
    }

    private void subscribe(Abonnement.TypeAbonnement type, double prix) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/abonnement/abonnement-form.fxml"));
            Parent root = loader.load();
            com.gestion.ui.abonnement.AbonnementFormController ctrl = loader.getController();

            Abonnement template = new Abonnement(currentUserId, null, type, java.time.LocalDate.now(),
                    java.math.BigDecimal.valueOf(prix), true);

            ctrl.setAbonnement(template);
            ctrl.setReadOnly(true); // Mandatory validation for subscription through this list
            ctrl.setOnSave(() -> {
                Platform.runLater(() -> {
                    onActualiser();
                    if (mainTabPane != null) {
                        mainTabPane.getSelectionModel().select(0);
                    }
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    // Close the choice window if it's still open
                    if (statusLabel != null && statusLabel.getScene() != null) {
                        ((Stage) statusLabel.getScene().getWindow()).close();
                    }
                });
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Confirmation de votre Abonnement");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Échec de l'ouverture du formulaire");
            error.setContentText(e.getMessage());
            error.show();
        }
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

    private void showFacture(Abonnement a) {
        if (a == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/abonnement/facture.fxml"));
            Parent root = loader.load();
            FactureController ctrl = loader.getController();
            ctrl.setData(a);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Facture LAMMA");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
