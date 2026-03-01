package com.gestion.ui.restaurant;

import com.gestion.controllers.RestaurantController;
import com.gestion.entities.Restaurant;
//import com.gestion.ui.navigation.NavigationUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des restaurants
 */
public class RestaurantListeController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterActifCombo;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Restaurant> listView;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnVoirDetails;
    @FXML
    private Button btnNouveau;
    @FXML
    private Button btnActualiser;
    @FXML
    private Button btnReserver;

    private final RestaurantController controller = new RestaurantController();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private FilteredList<Restaurant> filteredRestaurants;
    private Restaurant selectedRestaurant;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        setupFilters();
        loadRestaurants();
        setUserMode(false); // Default to admin mode until set otherwise
    }

    public void setUserMode(boolean userMode) {
        if (btnNouveau != null) {
            btnNouveau.setVisible(!userMode);
            btnNouveau.setManaged(!userMode);
        }
        if (btnActualiser != null) {
            btnActualiser.setVisible(!userMode);
            btnActualiser.setManaged(!userMode);
        }
        if (btnModifier != null) {
            btnModifier.setVisible(!userMode);
            btnModifier.setManaged(!userMode);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setVisible(!userMode);
            btnSupprimer.setManaged(!userMode);
        }
        if (btnReserver != null) {
            btnReserver.setVisible(userMode);
            btnReserver.setManaged(userMode);
        }
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Restaurant>() {
            @Override
            protected void updateItem(Restaurant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createRestaurantCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedRestaurant = newSelection;
            boolean hasSelection = newSelection != null;
            if (btnModifier != null)
                btnModifier.setDisable(!hasSelection);
            if (btnSupprimer != null)
                btnSupprimer.setDisable(!hasSelection);
            if (btnVoirDetails != null)
                btnVoirDetails.setDisable(!hasSelection);
            if (btnReserver != null)
                btnReserver.setDisable(!hasSelection);
        });
    }

    private void setupFilters() {
        filterActifCombo.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
        filterActifCombo.setValue("Tous");
        filterActifCombo.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private javafx.scene.Node createRestaurantCard(Restaurant item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Icon
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.web("#e7f1ff"));
        javafx.scene.text.Text icon = new javafx.scene.text.Text("🍽️");
        icon.setFont(javafx.scene.text.Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content
        VBox content = new VBox(5);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(item.getNom());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadge.getStyleClass().add("status-badge");
        if (item.isActif()) {
            statusBadge.setStyle("-fx-background-color: #28a745;");
        } else {
            statusBadge.setStyle("-fx-background-color: #dc3545;");
        }

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        details.add(createDetailLabel("📍 Adresse:", item.getAdresse()), 0, 0);
        details.add(createDetailLabel("📞 Tél:", item.getTelephone()), 1, 0);
        details.add(createDetailLabel("✉️ Email:", item.getEmail()), 0, 1);

        com.gestion.interfaces.RestaurantService restService = new com.gestion.services.RestaurantServiceImpl();
        int remaining = restService.getPlacesRestantes(item.getId(), null);
        Label capLabel = new Label("🪑 " + remaining + " / " + item.getNombrePlaces() + " places");
        capLabel.setStyle("-fx-text-fill: #FACC15; -fx-font-weight: 800; -fx-font-size: 12px;");
        details.add(capLabel, 1, 1);

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            Label desc = new Label(item.getDescription());
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            desc.setWrapText(true);
            desc.setMaxWidth(400);
            details.add(desc, 0, 2, 2, 1);
        }

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(iconPane, content);

        return card;
    }

    private HBox createDetailLabel(String labelText, String valueText) {
        HBox box = new HBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("card-label");
        Label value = new Label(valueText != null ? valueText : "Non renseigné");
        value.getStyleClass().add("card-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    private void loadRestaurants() {
        restaurants.setAll(controller.getAllRestaurants());
        filteredRestaurants = new FilteredList<>(restaurants, p -> true);
        listView.setItems(filteredRestaurants);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredRestaurants == null)
            return;

        String searchText = searchField.getText().toLowerCase();
        String actifFilter = filterActifCombo.getValue();

        filteredRestaurants.setPredicate(restaurant -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    (restaurant.getNom() != null && restaurant.getNom().toLowerCase().contains(searchText)) ||
                    (restaurant.getAdresse() != null && restaurant.getAdresse().toLowerCase().contains(searchText));

            boolean matchesActif = "Tous".equals(actifFilter) ||
                    ("Actifs".equals(actifFilter) && restaurant.isActif()) ||
                    ("Inactifs".equals(actifFilter) && !restaurant.isActif());

            return matchesSearch && matchesActif;
        });

        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = filteredRestaurants != null ? filteredRestaurants.size() : restaurants.size();
        countLabel.setText(count + " restaurant" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void onNouveauRestaurant() {
        openForm(null);
    }

    @FXML
    private void onModifier() {
        if (selectedRestaurant != null) {
            openForm(selectedRestaurant);
        }
    }

    @FXML
    private void onSupprimer() {
        if (selectedRestaurant != null) {
            handleDelete(selectedRestaurant);
        }
    }

    @FXML
    private void onVoirDetails() {
        if (selectedRestaurant != null) {
            openDetailPage(selectedRestaurant);
        }
    }

    @FXML
    private void onReserver() {
        if (selectedRestaurant != null) {
            statusLabel.setText("Réservation confirmée pour : " + selectedRestaurant.getNom());
            showAlert(Alert.AlertType.INFORMATION, "Réservation", "Place réservée",
                    "Vous avez réservé une place au restaurant : " + selectedRestaurant.getNom());

            // In a real app, this would update the participation or a reservation table
        }
    }

    private void openDetailPage(Restaurant restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/restaurant/restaurant-details.fxml"));
            Parent root = loader.load();
            RestaurantDetailsController detailsController = loader.getController();
            detailsController.setRestaurant(restaurant);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détail – " + restaurant.getNom());
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de détail", e.getMessage());
        }
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        selectedRestaurant = listView.getSelectionModel().getSelectedItem();
        boolean hasSelection = selectedRestaurant != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
        btnVoirDetails.setDisable(!hasSelection);

        if (event.getClickCount() == 2 && hasSelection) {
            openDetailPage(selectedRestaurant);
        }
    }

    @FXML
    public void onActualiser() {
        loadRestaurants();
        statusLabel.setText("Liste actualisée");
    }

    @FXML
    private void onReinitialiserFiltres() {
        searchField.clear();
        filterActifCombo.setValue("Tous");
        applyFilters();
    }

    private void handleEdit(Restaurant restaurant) {
        if (restaurant != null) {
            openForm(restaurant);
        }
    }

    private void handleDelete(Restaurant restaurant) {
        if (restaurant == null)
            return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le restaurant ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + restaurant.getNom()
                + "\" ?\n\nCette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (controller.deleteRestaurant(restaurant.getId())) {
                        restaurants.remove(restaurant);
                        updateCountLabel();
                        statusLabel.setText("Restaurant supprimé avec succès");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée",
                                "Impossible de supprimer le restaurant.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression",
                            e.getMessage());
                }
            }
        });
    }

    private void openForm(Restaurant restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/restaurant/restaurant-form.fxml"));
            Parent root = loader.load();

            RestaurantFormController formController = loader.getController();
            formController.setRestaurant(restaurant);
            formController.setListeController(this);
            formController.setController(controller);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(restaurant == null ? "Nouveau Restaurant" : "Modifier Restaurant");
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();

            // Refresh list after form closes
            loadRestaurants();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire",
                    e.getMessage());
        }
    }

    public void refreshList() {
        loadRestaurants();
    }

    private String getRestaurantDetails(Restaurant r) {
        return "Nom: " + r.getNom() + "\n" +
                "Adresse: " + (r.getAdresse() != null ? r.getAdresse() : "Non renseignée") + "\n" +
                "Téléphone: " + (r.getTelephone() != null ? r.getTelephone() : "Non renseigné") + "\n" +
                "Email: " + (r.getEmail() != null ? r.getEmail() : "Non renseigné") + "\n" +
                "Description: " + (r.getDescription() != null ? r.getDescription() : "Non renseignée") + "\n" +
                "Statut: " + (r.isActif() ? "Actif" : "Inactif") + "\n" +
                "Date de création: " + (r.getDateCreation() != null ? r.getDateCreation().toString() : "Inconnue")
                + "\n";
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
