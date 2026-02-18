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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des restaurants
 */
public class RestaurantListeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterActifCombo;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Restaurant> restaurantTable;
    @FXML private TableColumn<Restaurant, Long> colId;
    @FXML private TableColumn<Restaurant, String> colNom;
    @FXML private TableColumn<Restaurant, String> colAdresse;
    @FXML private TableColumn<Restaurant, String> colTelephone;
    @FXML private TableColumn<Restaurant, String> colEmail;
    @FXML private TableColumn<Restaurant, String> colStatut;
    @FXML private TableColumn<Restaurant, Void> colActions;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnVoirDetails;

    private final RestaurantController controller = new RestaurantController();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private FilteredList<Restaurant> filteredRestaurants;
    private Restaurant selectedRestaurant;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        loadRestaurants();
        setupTableSelectionListener();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colNom.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNom()));
        colAdresse.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdresse()));
        colTelephone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTelephone()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        colStatut.setCellValueFactory(cellData -> {
            boolean actif = cellData.getValue().isActif();
            return new SimpleStringProperty(actif ? "✅ Actif" : "❌ Inactif");
        });

        // Actions column with buttons
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 4 8;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 4 8;");
                editBtn.setOnAction(event -> handleEdit(getTableRow().getItem()));
                deleteBtn.setOnAction(event -> handleDelete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupFilters() {
        filterActifCombo.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
        filterActifCombo.setValue("Tous");
        filterActifCombo.setOnAction(e -> applyFilters());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupTableSelectionListener() {
        restaurantTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedRestaurant = newSelection;
            boolean hasSelection = newSelection != null;
            btnModifier.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            btnVoirDetails.setDisable(!hasSelection);
        });
        restaurantTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Restaurant> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailPage(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadRestaurants() {
        restaurants.setAll(controller.getAllRestaurants());
        filteredRestaurants = new FilteredList<>(restaurants, p -> true);
        restaurantTable.setItems(filteredRestaurants);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredRestaurants == null) return;

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
        if (restaurant == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le restaurant ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + restaurant.getNom() + "\" ?\n\nCette action est irréversible.");

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
        return String.format("""
                Nom: %s
                Adresse: %s
                Téléphone: %s
                Email: %s
                Description: %s
                Statut: %s
                Date de création: %s
                """,
                r.getNom(),
                r.getAdresse() != null ? r.getAdresse() : "Non renseignée",
                r.getTelephone() != null ? r.getTelephone() : "Non renseigné",
                r.getEmail() != null ? r.getEmail() : "Non renseigné",
                r.getDescription() != null ? r.getDescription() : "Non renseignée",
                r.isActif() ? "Actif" : "Inactif",
                r.getDateCreation() != null ? r.getDateCreation().toString() : "Inconnue"
        );
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
