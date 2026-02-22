package com.gestion.ui.abonnement;

import com.gestion.controllers.AbonnementController;
import com.gestion.controllers.MainController;
import com.gestion.entities.Abonnement;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.MouseEvent;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AbonnementViewController {

    @FXML
    private ListView<Abonnement> listView;

    // Filtres et recherche
    @FXML
    private ComboBox<String> filterStatut;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private TextField filterUserId;
    @FXML
    private TextField searchField;

    // Formulaire
    @FXML
    private TextField inputUserId;
    @FXML
    private ComboBox<Abonnement.TypeAbonnement> inputType;
    @FXML
    private DatePicker inputDateDebut;
    @FXML
    private DatePicker inputDateFin;
    @FXML
    private TextField inputPrix;
    @FXML
    private ComboBox<Abonnement.StatutAbonnement> inputStatut;
    @FXML
    private CheckBox inputAutoRenew;

    // Boutons
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    // Labels
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Label countLabel;

    private final AbonnementController controller = new AbonnementController();
    private final ObservableList<Abonnement> data = FXCollections.observableArrayList();
    private FilteredList<Abonnement> filteredData;
    private Abonnement selectedAbonnement = null;

    @FXML
    public void initialize() {
        try {
            setupListView();
            setupFilters();

            // Initialiser le filtered list
            filteredData = new FilteredList<>(data, p -> true);
            listView.setItems(filteredData);

            // Charger les données
            onActualiser();

            // Mise à jour du compteur
            updateCount();

            updateButtons();

            System.out.println("AbonnementViewController initialisé avec succès");
        } catch (Exception e) {
            System.err.println("Erreur initialisation AbonnementViewController: " + e.getMessage());
            e.printStackTrace();
        }
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
                    setGraphic(createCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createCard(Abonnement item) {
        // Main Card Container
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon / Avatar
        StackPane iconPane = new StackPane();
        Circle bg = new Circle(20, Color.web("#e7f1ff"));
        Text icon = new Text(getItemIcon(item.getType()));
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content Area
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Header: User ID + Statut
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Utilisateur #" + item.getUserId());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.getStatut().name());
        statusBadge.getStyleClass().addAll("status-badge", item.getStatut().name());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        // Details Grid
        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        // Row 1
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        details.add(createDetailLabel("📅 Début:", item.getDateDebut().format(formatter)), 0, 0);
        if (item.getDateFin() != null) {
            details.add(createDetailLabel("🏁 Fin:", item.getDateFin().format(formatter)), 1, 0);
        } else {
            details.add(createDetailLabel("🏁 Fin:", "Illimité"), 1, 0);
        }

        // Row 2
        details.add(createDetailLabel("💎 Type:", item.getType().name()), 0, 1);
        details.add(createDetailLabel("🔄 Auto-Renew:", item.isAutoRenew() ? "Oui" : "Non"), 1, 1);

        content.getChildren().addAll(header, details);

        // Price & Points (Right Side)
        VBox rightSide = new VBox(5);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        Label price = new Label(String.format("%.2f €", item.getPrix()));
        price.getStyleClass().add("card-price");

        Label points = new Label(item.getPointsAccumules() + " pts");
        points.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        rightSide.getChildren().addAll(price, points);

        card.getChildren().addAll(iconPane, content, rightSide);
        return card;
    }

    private String getItemIcon(Abonnement.TypeAbonnement type) {
        if (type == null)
            return "📄";
        switch (type) {
            case PREMIUM:
                return "🌟";
            case ANNUEL:
                return "📅";
            case MENSUEL:
                return "🗓️";
            default:
                return "📄";
        }
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

    private void setupFilters() {
        filterStatut.getItems().setAll("", "ACTIF", "EXPIRE", "SUSPENDU", "EN_ATTENTE");
        filterType.getItems().setAll("", "MENSUEL", "ANNUEL", "PREMIUM");
    }

    @FXML
    void onNouveau() {
        openForm(null);
    }

    @FXML
    void onActualiser() {
        try {
            List<Abonnement> list = controller.getAll();
            Platform.runLater(() -> {
                data.setAll(list);
                updateCount();
                updateStatusInfo("Données actualisées avec succès");
                listView.refresh();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    @FXML
    void onFiltrer() {
        applyFilters();
    }

    @FXML
    void onRechercher() {
        applyFilters();
    }

    @FXML
    void onReinitialiserFiltres() {
        filterStatut.getSelectionModel().clearSelection();
        filterType.getSelectionModel().clearSelection();
        filterUserId.clear();
        searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statut = filterStatut.getValue();
        String type = filterType.getValue();
        Long userId = parseLong(filterUserId.getText());

        filteredData.setPredicate(abonnement -> {
            boolean match = true;

            // Recherche textuelle
            if (searchText != null && !searchText.isEmpty()) {
                String searchable = abonnement.getId() + " " +
                        abonnement.getUserId() + " " +
                        abonnement.getType().name() + " " +
                        abonnement.getStatut().name();
                match = match && searchable.toLowerCase().contains(searchText);
            }

            // Filtre statut
            if (statut != null && !statut.isEmpty()) {
                match = match && abonnement.getStatut().name().equals(statut);
            }

            // Filtre type
            if (type != null && !type.isEmpty()) {
                match = match && abonnement.getType().name().equals(type);
            }

            // Filtre userId
            if (userId != null) {
                match = match && abonnement.getUserId().equals(userId);
            }

            return match;
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedAbonnement = listView.getSelectionModel().getSelectedItem();
        updateButtons();
        if (event.getClickCount() == 2 && selectedAbonnement != null) {
            onModifier();
        }
    }

    @FXML
    void onModifier() {
        if (selectedAbonnement == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un abonnement à modifier");
            return;
        }
        openForm(selectedAbonnement);
    }

    private void openForm(Abonnement a) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/abonnement/abonnement-form.fxml"));
            javafx.scene.Parent root = loader.load();
            AbonnementFormController ctrl = loader.getController();
            ctrl.setAbonnement(a);
            ctrl.setOnSave(this::onActualiser);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle(a == null ? "Nouvelle Expédition" : "Modifier l'Expédition");
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    void onSupprimer() {
        if (selectedAbonnement == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un abonnement à supprimer");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer l'abonnement ?");
        confirmAlert.setContentText("ID: " + selectedAbonnement.getId() +
                "\nUser ID: " + selectedAbonnement.getUserId() +
                "\nType: " + selectedAbonnement.getType().name());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = controller.delete(selectedAbonnement.getId());
                    if (deleted) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Abonnement supprimé avec succès");
                        onActualiser();
                        selectedAbonnement = null;
                        updateButtons();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'abonnement");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    void onAnnuler() {
        selectedAbonnement = null;
        listView.getSelectionModel().clearSelection();
        updateButtons();
    }

    @FXML
    void onStatistiques() {
        try {
            BigDecimal totalPoints = controller.getTotalPointsAccumules();
            List<Abonnement> prochesExpiration = controller.findProchesExpiration(30);

            StringBuilder stats = new StringBuilder();
            stats.append("📊 Statistiques des Abonnements\n\n");
            stats.append("Total des points accumulés: ").append(totalPoints).append("\n");
            stats.append("Abonnements proches expiration (30 jours): ").append(prochesExpiration.size()).append("\n");
            stats.append("Total des abonnements: ").append(data.size());

            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText(stats.toString());
            statsAlert.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du calcul des statistiques: " + e.getMessage());
        }
    }

    private void updateButtons() {
        boolean hasSelection = selectedAbonnement != null;
        boolean admin = isAdmin();
        btnModifier.setDisable(!admin || !hasSelection);
        btnSupprimer.setDisable(!admin || !hasSelection);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : data.size();
        countLabel.setText(count + " abonnement(s) tracked");
    }

    private void updateStatusInfo(String message) {
        if (statusInfoLabel != null) {
            statusInfoLabel.setText(message);
        }
    }

    private boolean isAdmin() {
        return MainController.getCurrentRole() == MainController.Role.ADMIN;
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
