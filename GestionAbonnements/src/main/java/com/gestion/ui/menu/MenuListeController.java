package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
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
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des menus
 */
public class MenuListeController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Restaurant> filterRestaurantCombo;
    @FXML private ComboBox<String> filterActifCombo;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Menu> menuTable;
    @FXML private TableColumn<Menu, Long> colId;
    @FXML private TableColumn<Menu, String> colNom;
    @FXML private TableColumn<Menu, String> colRestaurant;
    @FXML private TableColumn<Menu, String> colDescription;
    @FXML private TableColumn<Menu, String> colPrix;
    @FXML private TableColumn<Menu, String> colPeriode;
    @FXML private TableColumn<Menu, String> colStatut;
    @FXML private TableColumn<Menu, Void> colActions;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnVoirDetails;

    private final MenuController controller = new MenuController();
    private ObservableList<Menu> menus = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private FilteredList<Menu> filteredMenus;
    private Menu selectedMenu;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        loadRestaurants();
        loadMenus();
        setupTableSelectionListener();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getId()));
        colNom.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNom()));
        colRestaurant.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getRestaurantNom() != null ? cellData.getValue().getRestaurantNom() : "N/A"));
        colDescription.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        colPrix.setCellValueFactory(cellData -> {
            var prix = cellData.getValue().getPrix();
            return new SimpleStringProperty(prix != null ? String.format("%.2f €", prix) : "-");
        });
        colPeriode.setCellValueFactory(cellData -> {
            var debut = cellData.getValue().getDateDebut();
            var fin = cellData.getValue().getDateFin();
            String periode;
            if (debut != null && fin != null) {
                periode = debut + " → " + fin;
            } else if (debut != null) {
                periode = "À partir du " + debut;
            } else if (fin != null) {
                periode = "Jusqu'au " + fin;
            } else {
                periode = "Permanent";
            }
            return new SimpleStringProperty(periode);
        });
        colStatut.setCellValueFactory(cellData -> {
            boolean actif = cellData.getValue().isActif();
            return new SimpleStringProperty(actif ? "✅ Actif" : "❌ Inactif");
        });

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

        filterRestaurantCombo.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void loadRestaurants() {
        restaurants.setAll(controller.getAvailableRestaurants());
        filterRestaurantCombo.setItems(restaurants);
        filterRestaurantCombo.setPromptText("Tous les restaurants");
    }

    private void setupTableSelectionListener() {
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedMenu = newSelection;
            boolean hasSelection = newSelection != null;
            btnModifier.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            btnVoirDetails.setDisable(!hasSelection);
        });
        menuTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Menu> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailPage(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadMenus() {
        menus.setAll(controller.getAllMenus());
        filteredMenus = new FilteredList<>(menus, p -> true);
        menuTable.setItems(filteredMenus);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredMenus == null) return;

        String searchText = searchField.getText().toLowerCase();
        String actifFilter = filterActifCombo.getValue();
        Restaurant restaurantFilter = filterRestaurantCombo.getValue();

        filteredMenus.setPredicate(menu -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    (menu.getNom() != null && menu.getNom().toLowerCase().contains(searchText));

            boolean matchesActif = "Tous".equals(actifFilter) ||
                    ("Actifs".equals(actifFilter) && menu.isActif()) ||
                    ("Inactifs".equals(actifFilter) && !menu.isActif());

            boolean matchesRestaurant = restaurantFilter == null ||
                    (menu.getRestaurantId() != null && menu.getRestaurantId().equals(restaurantFilter.getId()));

            return matchesSearch && matchesActif && matchesRestaurant;
        });

        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = filteredMenus != null ? filteredMenus.size() : menus.size();
        countLabel.setText(count + " menu" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void onNouveauMenu() {
        openForm(null);
    }

    @FXML
    private void onModifier() {
        if (selectedMenu != null) {
            openForm(selectedMenu);
        }
    }

    @FXML
    private void onSupprimer() {
        if (selectedMenu != null) {
            handleDelete(selectedMenu);
        }
    }

    @FXML
    private void onVoirDetails() {
        if (selectedMenu != null) {
            openDetailPage(selectedMenu);
        }
    }

    private void openDetailPage(Menu menu) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/menu/menu-details.fxml"));
            Parent root = loader.load();
            MenuDetailsController detailsController = loader.getController();
            detailsController.setMenu(menu);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détail – " + menu.getNom());
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
        loadMenus();
        statusLabel.setText("Liste actualisée");
    }

    @FXML
    private void onReinitialiserFiltres() {
        searchField.clear();
        filterActifCombo.setValue("Tous");
        filterRestaurantCombo.setValue(null);
        applyFilters();
    }

    private void handleEdit(Menu menu) {
        if (menu != null) {
            openForm(menu);
        }
    }

    private void handleDelete(Menu menu) {
        if (menu == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le menu ?");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + menu.getNom() + "\" ?\n\nCette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (controller.deleteMenu(menu.getId())) {
                        menus.remove(menu);
                        updateCountLabel();
                        statusLabel.setText("Menu supprimé avec succès");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée",
                                "Impossible de supprimer le menu.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression",
                            e.getMessage());
                }
            }
        });
    }

    private void openForm(Menu menu) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/menu/menu-form.fxml"));
            Parent root = loader.load();

            MenuFormController formController = loader.getController();
            formController.setMenu(menu);
            formController.setListeController(this);
            formController.setRestaurants(restaurants);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(menu == null ? "Nouveau Menu" : "Modifier Menu");
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();

            loadMenus();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire",
                    e.getMessage());
        }
    }

    public void refreshList() {
        loadMenus();
    }

    private String getMenuDetails(Menu m) {
        return String.format("""
                Nom: %s
                Restaurant: %s
                Prix: %.2f €
                Description: %s
                Période: %s
                Statut: %s
                """,
                m.getNom(),
                m.getRestaurantNom() != null ? m.getRestaurantNom() : "N/A",
                m.getPrix() != null ? m.getPrix() : 0,
                m.getDescription() != null ? m.getDescription() : "Non renseignée",
                m.getDateDebut() != null || m.getDateFin() != null ?
                        (m.getDateDebut() + " → " + m.getDateFin()) : "Permanent",
                m.isActif() ? "Actif" : "Inactif"
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
