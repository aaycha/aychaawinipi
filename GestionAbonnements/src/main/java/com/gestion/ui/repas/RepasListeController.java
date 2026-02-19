package com.gestion.ui.repas;

import com.gestion.controllers.RepasController;
import com.gestion.entities.Menu;
import com.gestion.entities.Repas;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des repas (plats)
 */
public class RepasListeController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Restaurant> filterRestaurantCombo;
    @FXML
    private ComboBox<Menu> filterMenuCombo;
    @FXML
    private ComboBox<Repas.Categorie> filterCategorieCombo;
    @FXML
    private ComboBox<Repas.TypePlat> filterTypePlatCombo;
    @FXML
    private ComboBox<String> filterDisponibleCombo;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Repas> listView;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnVoirDetails;

    private final RepasController controller = new RepasController();
    private ObservableList<Repas> repas = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private ObservableList<Menu> menus = FXCollections.observableArrayList();
    private FilteredList<Repas> filteredRepas;
    private Repas selectedRepas;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        setupFilters();
        loadRestaurants();
        loadMenus();
        loadRepas();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Repas>() {
            @Override
            protected void updateItem(Repas item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createRepasCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedRepas = newSelection;
            boolean hasSelection = newSelection != null;
            btnModifier.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            btnVoirDetails.setDisable(!hasSelection);
        });
    }

    private void setupFilters() {
        filterCategorieCombo.setItems(FXCollections.observableArrayList(Repas.Categorie.values()));
        filterTypePlatCombo.setItems(FXCollections.observableArrayList(Repas.TypePlat.values()));
        filterDisponibleCombo.setItems(FXCollections.observableArrayList("Tous", "Disponibles", "Non disponibles"));
        filterDisponibleCombo.setValue("Tous");

        filterDisponibleCombo.setOnAction(e -> applyFilters());
        filterCategorieCombo.setOnAction(e -> applyFilters());
        filterTypePlatCombo.setOnAction(e -> applyFilters());
        filterRestaurantCombo.setOnAction(e -> applyFilters());
        filterMenuCombo.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void loadRestaurants() {
        restaurants.setAll(controller.getAvailableRestaurants());
        filterRestaurantCombo.setItems(restaurants);
        filterRestaurantCombo.setPromptText("Tous les restaurants");
    }

    private void loadMenus() {
        menus.setAll(controller.getAvailableMenus());
        filterMenuCombo.setItems(menus);
        filterMenuCombo.setPromptText("Tous les menus");
    }

    private javafx.scene.Node createRepasCard(Repas item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Icon
        javafx.scene.layout.StackPane iconPane = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.web("#fff0f6"));
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

        Label statusBadge = new Label(item.isDisponible() ? "DISPO" : "ÉPUISÉ");
        statusBadge.getStyleClass().add("status-badge");
        if (item.isDisponible()) {
            statusBadge.setStyle("-fx-background-color: #28a745;");
        } else {
            statusBadge.setStyle("-fx-background-color: #6c757d;");
        }

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        details.add(createDetailLabel("🏨 Resto:", item.getRestaurantNom()), 0, 0);
        details.add(createDetailLabel("📜 Menu:", item.getMenuNom()), 1, 0);
        details.add(createDetailLabel("📂 Cat:", item.getCategorie() != null ? item.getCategorie().getLabel() : "N/A"),
                0, 1);
        details.add(createDetailLabel("⏱️ Prép:", item.getTempsPreparation() + " min"), 1, 1);

        content.getChildren().addAll(header, details);

        // Right Side: Price
        VBox rightSide = new VBox(5);
        rightSide.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        rightSide.setMinWidth(80);

        Label price = new Label(String.format("%.2f €", item.getPrix() != null ? item.getPrix() : 0.0));
        price.getStyleClass().add("card-price");

        Label typeLabel = new Label(item.getTypePlat() != null ? item.getTypePlat().getLabel() : "");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

        rightSide.getChildren().addAll(price, typeLabel);

        card.getChildren().addAll(iconPane, content, rightSide);

        return card;
    }

    private HBox createDetailLabel(String labelText, String valueText) {
        HBox box = new HBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("card-label");
        Label value = new Label(valueText != null ? valueText : "-");
        value.getStyleClass().add("card-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    private void loadRepas() {
        repas.setAll(controller.getAllRepas());
        filteredRepas = new FilteredList<>(repas, p -> true);
        listView.setItems(filteredRepas);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredRepas == null)
            return;

        String searchText = searchField.getText().toLowerCase();
        String dispoFilter = filterDisponibleCombo.getValue();
        Repas.Categorie categorieFilter = filterCategorieCombo.getValue();
        Repas.TypePlat typePlatFilter = filterTypePlatCombo.getValue();
        Restaurant restaurantFilter = filterRestaurantCombo.getValue();
        Menu menuFilter = filterMenuCombo.getValue();

        filteredRepas.setPredicate(repas -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    (repas.getNom() != null && repas.getNom().toLowerCase().contains(searchText));

            boolean matchesDispo = "Tous".equals(dispoFilter) ||
                    ("Disponibles".equals(dispoFilter) && repas.isDisponible()) ||
                    ("Non disponibles".equals(dispoFilter) && !repas.isDisponible());

            boolean matchesCategorie = categorieFilter == null || categorieFilter.equals(repas.getCategorie());
            boolean matchesTypePlat = typePlatFilter == null || typePlatFilter.equals(repas.getTypePlat());
            boolean matchesRestaurant = restaurantFilter == null ||
                    (repas.getRestaurantId() != null && repas.getRestaurantId().equals(restaurantFilter.getId()));
            boolean matchesMenu = menuFilter == null ||
                    (repas.getMenuId() != null && repas.getMenuId().equals(menuFilter.getId()));

            return matchesSearch && matchesDispo && matchesCategorie && matchesTypePlat &&
                    matchesRestaurant && matchesMenu;
        });

        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = filteredRepas != null ? filteredRepas.size() : repas.size();
        countLabel.setText(count + " plat" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void onNouveauRepas() {
        openForm(null);
    }

    @FXML
    private void onModifier() {
        if (selectedRepas != null) {
            openForm(selectedRepas);
        }
    }

    @FXML
    private void onSupprimer() {
        if (selectedRepas != null) {
            handleDelete(selectedRepas);
        }
    }

    @FXML
    private void onVoirDetails() {
        if (selectedRepas != null) {
            openDetailPage(selectedRepas);
        }
    }

    private void openDetailPage(Repas repas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/repas/repas-details.fxml"));
            Parent root = loader.load();
            RepasDetailsController detailsController = loader.getController();
            detailsController.setRepas(repas);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détail – " + repas.getNom());
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de détail", e.getMessage());
        }
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        selectedRepas = listView.getSelectionModel().getSelectedItem();
        boolean hasSelection = selectedRepas != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
        btnVoirDetails.setDisable(!hasSelection);

        if (event.getClickCount() == 2 && hasSelection) {
            openDetailPage(selectedRepas);
        }
    }

    @FXML
    public void onActualiser() {
        loadRestaurants();
        loadMenus();
        loadRepas();
        statusLabel.setText("Liste actualisée");
    }

    @FXML
    private void onReinitialiserFiltres() {
        searchField.clear();
        filterDisponibleCombo.setValue("Tous");
        filterCategorieCombo.setValue(null);
        filterTypePlatCombo.setValue(null);
        filterRestaurantCombo.setValue(null);
        filterMenuCombo.setValue(null);
        applyFilters();
    }

    private void handleEdit(Repas repas) {
        if (repas != null) {
            openForm(repas);
        }
    }

    private void handleDelete(Repas repas) {
        if (repas == null)
            return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le plat ?");
        confirmAlert.setContentText(
                "Êtes-vous sûr de vouloir supprimer \"" + repas.getNom() + "\" ?\n\nCette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (controller.deleteRepas(repas.getId())) {
                        this.repas.remove(repas);
                        updateCountLabel();
                        statusLabel.setText("Plat supprimé avec succès");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée",
                                "Impossible de supprimer le plat.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression",
                            e.getMessage());
                }
            }
        });
    }

    private void openForm(Repas repas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/repas/repas-form.fxml"));
            Parent root = loader.load();

            RepasFormController formController = loader.getController();
            formController.setRepas(repas);
            formController.setListeController(this);
            formController.setController(controller);
            formController.setRestaurants(restaurants);
            formController.setMenus(menus);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(repas == null ? "Nouveau Plat" : "Modifier Plat");
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();

            loadRepas();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire",
                    e.getMessage());
        }
    }

    public void refreshList() {
        loadRepas();
    }

    private String getRepasDetails(Repas r) {
        return String.format("""
                Nom: %s
                Restaurant: %s
                Menu: %s
                Catégorie: %s
                Type: %s
                Prix: %s
                Temps de préparation: %d min
                Description: %s
                Disponible: %s
                """,
                r.getNom(),
                r.getRestaurantNom() != null ? r.getRestaurantNom() : "N/A",
                r.getMenuNom() != null ? r.getMenuNom() : "N/A",
                r.getCategorie() != null ? r.getCategorie().getLabel() : "N/A",
                r.getTypePlat() != null ? r.getTypePlat().getLabel() : "N/A",
                r.getPrix() != null ? r.getPrix() + " €" : "Non renseigné",
                r.getTempsPreparation() != null ? r.getTempsPreparation() : 0,
                r.getDescription() != null ? r.getDescription() : "Non renseignée",
                r.isDisponible() ? "Oui" : "Non");
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
