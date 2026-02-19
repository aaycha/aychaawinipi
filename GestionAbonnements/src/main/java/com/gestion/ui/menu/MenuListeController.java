package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import javafx.geometry.Insets;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des menus
 */
public class MenuListeController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Restaurant> filterRestaurantCombo;
    @FXML
    private ComboBox<String> filterActifCombo;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Menu> listView;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnVoirDetails;

    private final MenuController controller = new MenuController();
    private ObservableList<Menu> menus = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private FilteredList<Menu> filteredMenus;
    private Menu selectedMenu;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        setupFilters();
        loadRestaurants();
        loadMenus();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Menu>() {
            @Override
            protected void updateItem(Menu item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createMenuCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedMenu = newSelection;
            boolean hasSelection = newSelection != null;
            btnModifier.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            btnVoirDetails.setDisable(!hasSelection);
        });
    }

    private javafx.scene.Node createMenuCard(Menu item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconPane = new StackPane();
        Circle bg = new Circle(20, Color.web("#fff9db"));
        Text icon = new Text("🍽️");
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.getNom());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadge.getStyleClass().add("status-badge");
        if (item.isActif()) {
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

        details.add(createDetailLabel("🏪 Resto:", item.getRestaurantNom() != null ? item.getRestaurantNom() : "N/A"),
                0, 0);
        details.add(createDetailLabel("📅 Période:", getPeriodeStr(item)), 1, 0);

        Label descLabel = new Label(item.getDescription());
        descLabel.getStyleClass().add("card-label");
        descLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #6c757d;");
        descLabel.setWrapText(true);
        details.add(descLabel, 0, 1, 2, 1);

        content.getChildren().addAll(header, details);

        // Right side: Price and Actions
        VBox rightSide = new VBox(8);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        Label price = new Label(String.format("%.2f €", item.getPrix() != null ? item.getPrix() : 0.0));
        price.getStyleClass().add("card-price");

        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✏️");
        Button deleteBtn = new Button("🗑️");
        editBtn.getStyleClass().add("btn-warning-sm");
        deleteBtn.getStyleClass().add("btn-danger-sm");
        editBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 10px;");
        deleteBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 10px;");

        editBtn.setOnAction(e -> handleEdit(item));
        deleteBtn.setOnAction(e -> handleDelete(item));

        actions.getChildren().addAll(editBtn, deleteBtn);
        rightSide.getChildren().addAll(price, actions);

        card.getChildren().addAll(iconPane, content, rightSide);
        return card;
    }

    private String getPeriodeStr(Menu m) {
        var debut = m.getDateDebut();
        var fin = m.getDateFin();
        if (debut != null && fin != null)
            return debut + " → " + fin;
        if (debut != null)
            return "Depuis " + debut;
        if (fin != null)
            return "Jusqu'au " + fin;
        return "Permanent";
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
    private void onListClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            Menu selection = listView.getSelectionModel().getSelectedItem();
            if (selection != null) {
                openDetailPage(selection);
            }
        }
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

    private void loadMenus() {
        menus.setAll(controller.getAllMenus());
        filteredMenus = new FilteredList<>(menus, p -> true);
        listView.setItems(filteredMenus);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredMenus == null)
            return;

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
        if (menu == null)
            return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le menu ?");
        confirmAlert.setContentText(
                "Êtes-vous sûr de vouloir supprimer \"" + menu.getNom() + "\" ?\n\nCette action est irréversible.");

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
                m.getDateDebut() != null || m.getDateFin() != null ? (m.getDateDebut() + " → " + m.getDateFin())
                        : "Permanent",
                m.isActif() ? "Actif" : "Inactif");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
