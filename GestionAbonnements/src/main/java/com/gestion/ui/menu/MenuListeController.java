package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import com.gestion.tools.Session;
import com.gestion.entities.Participation;
import com.gestion.interfaces.ParticipationService;
import com.gestion.services.ParticipationServiceImpl;
import java.util.List;
import java.util.Optional;
import javafx.scene.shape.SVGPath;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
    @FXML
    private HBox adminActionBar;

    private Participation currentParticipation;

    private final MenuController controller = new MenuController();
    private ObservableList<Menu> menus = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private FilteredList<Menu> filteredMenus;
    private Menu selectedMenu;
    private final ParticipationService participationService = new ParticipationServiceImpl();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        setupFilters();
        loadRestaurants();
        loadMenus();

        if (!isAdmin() && adminActionBar != null) {
            adminActionBar.setVisible(false);
            adminActionBar.setManaged(false);
        }
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
        VBox card = new VBox(0);
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setStyle("-fx-background-color: #fcfaf5; " +
                "-fx-border-color: #2d5a27; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");
        card.setMaxWidth(Double.MAX_VALUE);

        // Header: Restaurant & Status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label restoLabel = new Label(
                (item.getRestaurantNom() != null ? item.getRestaurantNom() : "RESTO").toUpperCase());
        restoLabel.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 10; -fx-text-fill: #2d5a27; -fx-font-weight: bold; -fx-letter-spacing: 1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadge.setStyle("-fx-font-size: 9; -fx-padding: 3 8; -fx-background-radius: 10; -fx-text-fill: white; " +
                "-fx-background-color: " + (item.isActif() ? "#2d5a27" : "#64748b") + ";");

        header.getChildren().addAll(restoLabel, spacer, statusBadge);

        // Title
        Label title = new Label(item.getNom().toUpperCase());
        title.setStyle(
                "-fx-font-family: 'Times New Roman'; -fx-font-size: 20; -fx-text-fill: #2d5a27; -fx-font-weight: bold;");
        title.setWrapText(true);

        // Separator (Wave SVG inspired line)
        SVGPath wave = new SVGPath();
        wave.setContent("M0,0 Q25,10 50,0 T100,0");
        wave.setFill(Color.TRANSPARENT);
        wave.setStroke(Color.web("#2d5a27"));
        wave.setStrokeWidth(1.5);
        wave.setOpacity(0.5);

        // Description
        Label desc = new Label(item.getDescription());
        desc.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 11; -fx-text-fill: #4a4a4a; -fx-font-style: italic;");
        desc.setWrapText(true);
        desc.setMaxHeight(40);

        // Footer: Price & Date
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.BOTTOM_LEFT);

        VBox priceAndDate = new VBox(2);
        Label dateRange = new Label(getPeriodeStr(item));
        dateRange.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 11; -fx-text-fill: #64748b;");

        priceAndDate.getChildren().addAll(dateRange);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        boolean isAdmin = isAdmin();
        if (isAdmin) {
            Button editBtn = new Button("✏️");
            Button deleteBtn = new Button("🗑️");
            editBtn.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: #2d5a27; -fx-border-radius: 15; -fx-text-fill: #2d5a27; -fx-cursor: hand; -fx-padding: 4 8; -fx-font-size: 10;");
            deleteBtn.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: #ef4444; -fx-border-radius: 15; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 4 8; -fx-font-size: 10;");

            editBtn.setOnAction(e -> handleEdit(item));
            deleteBtn.setOnAction(e -> handleDelete(item));
            actions.getChildren().addAll(editBtn, deleteBtn);
        } else {
            Button selectBtn = new Button("CHOISIR 🍴");
            selectBtn.getStyleClass().addAll("button-capsule-glass");
            selectBtn.setStyle(
                    "-fx-border-color: #1890ff; -fx-text-fill: #1890ff; -fx-font-size: 10px; -fx-padding: 4 10;");

            // Highlight if already selected
            if (currentParticipation != null && item.getId().equals(currentParticipation.getMenuId())) {
                selectBtn.setText("SÉLECTIONNÉ ✅");
                selectBtn.setStyle(
                        "-fx-background-color: #1890ff; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 10;");
            }

            selectBtn.setOnAction(e -> handleSelectMenu(item));
            actions.getChildren().add(selectBtn);
        }

        footer.getChildren().addAll(priceAndDate, footerSpacer, actions);

        card.getChildren().addAll(header, title, wave, desc, footer);

        // Single click on card opens details
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                openDetailPage(item);
            }
        });

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f5f1e8; -fx-border-color: #1b3d1b; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #fcfaf5; -fx-border-color: #2d5a27; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;"));

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
        var user = Session.getInstance().getCurrentUser();
        if (user != null && "ADMIN".equals(user.getRole())) {
            menus.setAll(controller.getAllMenus());
        } else if (user != null) {
            // Filter by participation restaurant
            List<Participation> participations = participationService.findByUserId((long) user.getId());
            Optional<Participation> p = participations.stream()
                    .filter(part -> part.getRestaurantId() != null)
                    .findFirst();

            if (p.isPresent()) {
                currentParticipation = p.get();
                menus.setAll(controller.getMenusByRestaurant(p.get().getRestaurantId()));
                statusLabel.setText("Menus pour votre restaurant : " + p.get().getRestaurantId());
            } else {
                currentParticipation = null;
                menus.clear();
                statusLabel.setText("⚠️ Veuillez participer à un événement pour voir les menus.");
                showAlert(Alert.AlertType.INFORMATION, "Information", "Aucun restaurant associé",
                        "Veuillez d'abord vous inscrire à un événement avec restauration pour voir les menus correspondants.");
            }
        } else {
            currentParticipation = null;
            menus.setAll(controller.getAllMenus());
        }

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

    private boolean isAdmin() {
        var user = Session.getInstance().getCurrentUser();
        return user != null && "ADMIN".equals(user.getRole());
    }

    private void handleSelectMenu(Menu menu) {
        if (currentParticipation == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucune participation",
                    "Vous devez être inscrit à un événement pour choisir un menu.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Choisir ce menu");
        confirm.setHeaderText("Confirmation de sélection");
        confirm.setContentText("Voulez-vous choisir le menu \"" + menu.getNom() + "\" pour votre expédition ?");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                currentParticipation.setMenuId(menu.getId());
                if (participationService.update(currentParticipation) != null) {
                    statusLabel.setText("Menu \"" + menu.getNom() + "\" sélectionné !");
                    // Refresh UI to show checkmark
                    listView.refresh();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la sélection",
                            "Impossible de mettre à jour votre participation.");
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
