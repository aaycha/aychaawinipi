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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;
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
    @FXML
    private FlowPane offersFlowPane;

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
        loadAvailableOffers();
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

    private void loadAvailableOffers() {
        if (offersFlowPane == null)
            return;
        Platform.runLater(() -> {
            try {
                offersFlowPane.getChildren().clear();
                List<Abonnement> templates = controller.getAll().stream()
                        .filter(a -> a.getUserId() != null && a.getUserId() == 1L)
                        .collect(Collectors.toList());

                for (Abonnement template : templates) {
                    offersFlowPane.getChildren().add(createOfferCard(template));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private VBox createOfferCard(Abonnement template) {
        VBox card = new VBox(15);
        card.getStyleClass().add("modern-card");
        card.setPrefWidth(280);
        card.setStyle(
                "-fx-padding: 24; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 12; -fx-background-color: rgba(255,255,255,0.02);");
        card.setAlignment(Pos.CENTER);

        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label icon = new Label("✨");
        icon.setStyle("-fx-font-size: 32px;");
        Label typeLabel = new Label(template.getType().name());
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: white;");
        Label nameLabel = new Label(template.getNom() != null ? template.getNom() : "Abonnement Standard");
        nameLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        header.getChildren().addAll(icon, typeLabel, nameLabel);

        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER);
        Label price = new Label(String.format("%.2f €", template.getPrix()));
        price.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #10b981;");
        Label period = new Label("/ " + (template.getType() == Abonnement.TypeAbonnement.MENSUEL ? "mois" : "an"));
        period.setStyle("-fx-text-fill: #64748b;");
        priceBox.getChildren().addAll(price, period);

        VBox details = new VBox(8);
        details.getChildren().add(new Label(
                "• Accès: " + (template.getRestrictionType() != null ? template.getRestrictionType() : "Global")));
        details.getChildren().add(new Label("• Status: Scout Verified"));
        details.setStyle("-fx-text-fill: #d1d5db; -fx-font-size: 13px;");

        Button subscribeBtn = new Button("S'abonner →");
        subscribeBtn.setMaxWidth(Double.MAX_VALUE);
        subscribeBtn.getStyleClass().add("btn-primary");
        subscribeBtn.setStyle("-fx-background-radius: 20; -fx-padding: 10; -fx-font-weight: bold;");
        subscribeBtn.setOnAction(e -> subscribeToTemplate(template));

        card.getChildren().addAll(header, priceBox, details, subscribeBtn);
        return card;
    }

    private void subscribeToTemplate(Abonnement template) {
        subscribe(template.getType(), template.getPrix().doubleValue(), template);
    }

    private void subscribe(Abonnement.TypeAbonnement type, double prix, Abonnement template) {
        try {
            com.gestion.services.StripePaymentService stripeService = com.gestion.services.StripePaymentService
                    .getInstance();
            stripeService.openCheckoutInBrowser(java.math.BigDecimal.valueOf(prix),
                    "Abonnement " + (template != null ? template.getNom() : type.getLabel()));

            Alert payConfirm = new Alert(Alert.AlertType.CONFIRMATION);
            payConfirm.setTitle("Stripe Payment Verification");
            payConfirm.setHeaderText("Secure Gateway Launched");
            payConfirm.setContentText("Complete the transaction in your browser and confirm here to activate.");

            ButtonType btnConfirm = new ButtonType("CONFIRM PAYMENT");
            ButtonType btnCancel = new ButtonType("CANCEL", ButtonBar.ButtonData.CANCEL_CLOSE);
            payConfirm.getButtonTypes().setAll(btnConfirm, btnCancel);

            payConfirm.showAndWait().ifPresent(response -> {
                if (response == btnConfirm) {
                    try {
                        Abonnement newAbo = new Abonnement();
                        newAbo.setUserId(currentUserId);
                        newAbo.setNom(template != null ? template.getNom() : "Expedition Pass " + type.getLabel());
                        newAbo.setType(type);
                        newAbo.setDateDebut(java.time.LocalDate.now());
                        newAbo.setDateFin(java.time.LocalDate.now().plusMonths(1));
                        newAbo.setPrix(java.math.BigDecimal.valueOf(prix));
                        newAbo.setAutoRenew(true);
                        newAbo.setStatut(Abonnement.StatutAbonnement.ACTIF);

                        if (template != null) {
                            newAbo.setRestrictionType(template.getRestrictionType());
                            newAbo.setEvenementId(template.getEvenementId());
                        }

                        // Save directly to DB
                        Abonnement savedAbo = controller.create(newAbo);
                        if (savedAbo != null) {
                            newAbo.setId(savedAbo.getId());
                        }

                        Platform.runLater(() -> {
                            onActualiser();
                            if (mainTabPane != null)
                                mainTabPane.getSelectionModel().select(0);
                            showFacture(newAbo);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the Facture (Invoice) window for the given abonnement.
     * The facture contains a "Send Notification" button for SMS validation.
     */
    private void showFacture(Abonnement abonnement) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/abonnement/facture.fxml"));
            Parent root = loader.load();
            com.gestion.ui.abonnement.FactureController ctrl = loader.getController();
            ctrl.setData(abonnement);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Facture — " + abonnement.getType().getLabel());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur ouverture facture: " + e.getMessage());
        }
    }

    private javafx.scene.Node createAbonnementCard(Abonnement item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon - Scout Box Style
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(40, 40);
        iconPane.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;");
        Text icon = new Text("🎫");
        icon.setFont(Font.font("Segoe UI Emoji", 18));
        icon.setFill(Color.WHITE);
        iconPane.getChildren().addAll(icon);

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
            statusBadge.setStyle(
                    "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 9;");
            statusBadge.setText("✓ VERIFIED");
        } else {
            statusBadge.setStyle(
                    "-fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 9;");
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
