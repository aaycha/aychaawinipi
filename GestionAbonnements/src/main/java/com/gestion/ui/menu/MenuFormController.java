package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.services.GeminiMenuAnalyzer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.services.RepasDetailleServiceImpl;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import javafx.fxml.FXMLLoader;
import java.io.IOException;

/**
 * Contrôleur pour le formulaire de menu
 * — avec génération automatique des détails via Google Gemini Vision API.
 */
public class MenuFormController implements Initializable {

    // ── Champs du formulaire ──────────────────────────────────────────────────
    @FXML
    private Label formTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<Restaurant> comboRestaurant;
    @FXML
    private TextArea inputDescription;
    @FXML
    private DatePicker inputDateDebut;
    @FXML
    private DatePicker inputDateFin;
    @FXML
    private CheckBox checkActif;
    @FXML
    private Label errorNom;
    @FXML
    private Label errorRestaurant;
    @FXML
    private Label errorDates;
    @FXML
    private Label errorDescription;
    @FXML
    private Label hintNom;
    @FXML
    private VBox errorContainer;
    @FXML
    private Label globalErrorMessage;

    @FXML
    private Button btnGenerateNameIA;
    @FXML
    private ProgressIndicator nameAiProgress;
    @FXML
    private Button btnGenerateDatesIA;
    @FXML
    private ProgressIndicator datesAiProgress;

    @FXML
    private VBox dishesSelectionList;
    @FXML
    private Button btnGenerateDescIA;
    @FXML
    private ProgressIndicator descAiProgress;

    private final RepasDetailleService dishService = new RepasDetailleServiceImpl();
    private final List<CheckBox> dishCheckboxes = new ArrayList<>();

    private static final int MAX_NOM_MENU = 100;
    private final MenuController controller = new MenuController();
    private final GeminiMenuAnalyzer geminiAnalyzer = new GeminiMenuAnalyzer();

    // private MenuListeController listeController; // Field is not used
    private ObservableList<Restaurant> restaurants;
    private Menu menu;
    private boolean isEditMode = false;
    private int nameClickCount = 0;
    private int datesClickCount = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    public void initialize(URL url, ResourceBundle resourceBundle) {
        checkActif.setSelected(true);
        setupValidationListeners();
        updateHintNom();
        loadAvailableDishes();
    }

    private void loadAvailableDishes() {
        dishesSelectionList.getChildren().clear();
        dishCheckboxes.clear();
        List<RepasDetaille> dishes = dishService.findAll();
        for (RepasDetaille dish : dishes) {
            CheckBox cb = new CheckBox(dish.getNom() + " (" + dish.getPrix() + " €)");
            cb.setUserData(dish);
            cb.getStyleClass().add("card-label");
            cb.setStyle("-fx-text-fill: #1d3c34; -fx-font-size: 11px; -fx-font-weight: bold;");
            dishCheckboxes.add(cb);
            dishesSelectionList.getChildren().add(cb);
        }
    }

    @FXML
    private void onGenerateNameIA() {
        List<String> selectedNames = dishCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((RepasDetaille) cb.getUserData()).getNom())
                .collect(Collectors.toList());

        if (selectedNames.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucun plat sélectionné",
                    "Veuillez sélectionner au moins un plat pour générer un nom.");
            return;
        }

        btnGenerateNameIA.setDisable(true);
        nameAiProgress.setVisible(true);
        nameAiProgress.setManaged(true);

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return geminiAnalyzer.generateMenuName(selectedNames, ++nameClickCount);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                inputNom.setText(task.getValue());
                btnGenerateNameIA.setDisable(false);
                nameAiProgress.setVisible(false);
                nameAiProgress.setManaged(false);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnGenerateNameIA.setDisable(false);
                nameAiProgress.setVisible(false);
                nameAiProgress.setManaged(false);
                showAlert(Alert.AlertType.ERROR, "Erreur IA", "Génération du nom échouée",
                        task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    @FXML
    private void onGenerateDatesIA() {
        List<String> selectedNames = dishCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((RepasDetaille) cb.getUserData()).getNom())
                .collect(Collectors.toList());

        btnGenerateDatesIA.setDisable(true);
        datesAiProgress.setVisible(true);
        datesAiProgress.setManaged(true);

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return geminiAnalyzer.generateMenuDates(selectedNames, ++datesClickCount);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String result = task.getValue();
                if (result != null && result.contains("|")) {
                    String[] parts = result.split("\\|");
                    try {
                        inputDateDebut.setValue(LocalDate.parse(parts[0]));
                        inputDateFin.setValue(LocalDate.parse(parts[1]));
                    } catch (Exception ex) {
                        System.err.println("Parse date error: " + ex.getMessage());
                    }
                }
                btnGenerateDatesIA.setDisable(false);
                datesAiProgress.setVisible(false);
                datesAiProgress.setManaged(false);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnGenerateDatesIA.setDisable(false);
                datesAiProgress.setVisible(false);
                datesAiProgress.setManaged(false);
                showAlert(Alert.AlertType.ERROR, "Erreur IA", "Génération des dates échouée",
                        task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    @FXML
    private void onGenerateDescriptionIA() {
        List<String> selectedNames = dishCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((RepasDetaille) cb.getUserData()).getNom())
                .collect(Collectors.toList());

        if (selectedNames.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Aucun plat sélectionné",
                    "Veuillez sélectionner au moins un plat pour générer une description.");
            return;
        }

        btnGenerateDescIA.setDisable(true);
        descAiProgress.setVisible(true);
        descAiProgress.setManaged(true);

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return geminiAnalyzer.generateMenuDescription(selectedNames);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                inputDescription.setText(task.getValue());
                btnGenerateDescIA.setDisable(false);
                descAiProgress.setVisible(false);
                descAiProgress.setManaged(false);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnGenerateDescIA.setDisable(false);
                descAiProgress.setVisible(false);
                descAiProgress.setManaged(false);
                showAlert(Alert.AlertType.ERROR, "Erreur IA", "Génération échouée", task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    private void setupValidationListeners() {
        inputNom.textProperty().addListener((obs, o, n) -> {
            clearError(errorNom);
            updateHintNom();
        });
        comboRestaurant.valueProperty().addListener((obs, o, n) -> {
            clearError(errorRestaurant);
        });
        inputDateDebut.valueProperty().addListener((obs, o, n) -> clearError(errorDates));
        inputDateFin.valueProperty().addListener((obs, o, n) -> clearError(errorDates));
        if (inputDescription != null)
            inputDescription.textProperty().addListener((obs, o, n) -> {
                clearError(errorDescription);
            });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS — IA IMAGE
    // ── IA Image Handlers removed as requested ────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // SETTERS
    // ─────────────────────────────────────────────────────────────────────────

    public void setMenu(Menu menu) {
        this.menu = menu;
        this.isEditMode = (menu != null);
        if (isEditMode) {
            formTitle.setText("Modifier Menu");
            populateFields();
        } else {
            formTitle.setText("Nouveau Menu");
        }
    }

    public void setListeController(MenuListeController controller) {
        // Method kept for compatibility with MenuListeController, but internal field is
        // unused.
    }

    public void setRestaurants(ObservableList<Restaurant> restaurants) {
        this.restaurants = restaurants;
        comboRestaurant.setItems(restaurants);
        comboRestaurant.setPromptText("Sélectionnez un restaurant");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void populateFields() {
        if (menu == null)
            return;
        inputNom.setText(menu.getNom());
        inputDescription.setText(menu.getDescription());
        inputDateDebut.setValue(menu.getDateDebut());
        inputDateFin.setValue(menu.getDateFin());
        checkActif.setSelected(menu.isActif());

        if (restaurants != null && menu.getRestaurantId() != null) {
            for (Restaurant r : restaurants) {
                if (r.getId().equals(menu.getRestaurantId())) {
                    comboRestaurant.setValue(r);
                    break;
                }
            }
        }

        // Populate dishes
        if (menu.getDishesIds() != null) {
            for (CheckBox cb : dishCheckboxes) {
                RepasDetaille dish = (RepasDetaille) cb.getUserData();
                if (menu.getDishesIds().contains(dish.getId())) {
                    cb.setSelected(true);
                }
            }
        }
    }

    @FXML
    private void onGenerateMenuCard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/menu/menu-export-card.fxml"));
            Parent cardRoot = loader.load();

            // Populate Card
            Label lblRestaurant = (Label) cardRoot.lookup("#restaurantName");
            Label lblMenuTitle = (Label) cardRoot.lookup("#menuTitle");
            Label lblDescription = (Label) cardRoot.lookup("#menuDescription");
            Label lblValidity = (Label) cardRoot.lookup("#validityPeriod");
            VBox dishesContainer = (VBox) cardRoot.lookup("#dishesContainer");

            String rName = comboRestaurant.getValue() != null ? comboRestaurant.getValue().getNom() : "LICERIA & CO.";
            lblRestaurant.setText(rName.toUpperCase());
            lblMenuTitle.setText(inputNom.getText().toUpperCase());
            lblDescription.setText(inputDescription.getText());

            String validity = "Valid from " + (inputDateDebut.getValue() != null ? inputDateDebut.getValue() : "Today")
                    +
                    " - " + (inputDateFin.getValue() != null ? inputDateFin.getValue() : "Forever");
            lblValidity.setText(validity);

            // Add Dishes Grouped by Category
            dishesContainer.getChildren().clear();
            List<RepasDetaille> selectedItems = dishCheckboxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (RepasDetaille) cb.getUserData())
                    .collect(Collectors.toList());

            if (selectedItems.isEmpty()) {
                Label noDishes = new Label("NO ITEMS SELECTED");
                noDishes.setStyle(
                        "-fx-font-family: 'Arial'; -fx-font-size: 14; -fx-text-fill: #2d5a27; -fx-opacity: 0.5;");
                dishesContainer.getChildren().add(noDishes);
            } else {
                // Group by Category
                java.util.Map<String, List<RepasDetaille>> grouped = selectedItems.stream()
                        .collect(Collectors.groupingBy(d -> d.getTypeRepas() != null ? d.getTypeRepas() : "AUTRES"));

                for (java.util.Map.Entry<String, List<RepasDetaille>> entry : grouped.entrySet()) {
                    // Category Header
                    Label catHeader = new Label("——— " + entry.getKey().replace("_", " ") + " ———");
                    catHeader.setStyle(
                            "-fx-font-family: 'Arial'; -fx-font-size: 14; -fx-text-fill: #2d5a27; -fx-font-weight: bold; -fx-letter-spacing: 2;");
                    VBox.setMargin(catHeader, new Insets(10, 0, 5, 0));
                    dishesContainer.getChildren().add(catHeader);

                    for (RepasDetaille dish : entry.getValue()) {
                        HBox itemRow = createDishRow(dish.getNom().toUpperCase(), dish.getPrix() + " €");
                        dishesContainer.getChildren().add(itemRow);
                    }
                }
            }

            // Preview Window
            Stage previewStage = new Stage();
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.setTitle("Aperçu de la Carte Menu");

            // Wrap card in a ScrollPane for preview
            ScrollPane scrollPane = new ScrollPane(cardRoot);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: #fcfaf5;");

            VBox layout = new VBox(10, scrollPane);
            layout.setPadding(new Insets(10));
            layout.setAlignment(Pos.CENTER);
            layout.setStyle("-fx-background-color: #fcfaf5;");

            Button btnExport = new Button("📸 Exporter en Image (PNG)");
            btnExport.setStyle(
                    "-fx-background-color: #2d5a27; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 20;");
            btnExport.setOnAction(e -> exportToImage(cardRoot, previewStage));

            layout.getChildren().add(btnExport);

            Scene scene = new Scene(layout, 650, 900);
            previewStage.setScene(scene);
            previewStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer la carte", e.getMessage());
        }
    }

    private HBox createDishRow(String name, String price) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.BOTTOM_CENTER);
        row.setPadding(new Insets(0, 50, 0, 50));

        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 16; -fx-text-fill: #2d5a27;");

        Region dots = new Region();
        HBox.setHgrow(dots, Priority.ALWAYS);
        dots.setPrefHeight(1);
        dots.setStyle(
                "-fx-border-color: #2d5a27; -fx-border-width: 0 0 1 0; -fx-border-style: dotted; -fx-opacity: 0.3;");

        Label lblPrice = new Label(price);
        lblPrice.setStyle(
                "-fx-font-family: 'Georgia'; -fx-font-size: 16; -fx-text-fill: #2d5a27; -fx-font-weight: bold;");

        row.getChildren().addAll(lblName, dots, lblPrice);
        return row;
    }

    private void exportToImage(Parent node, Stage stage) {
        try {
            WritableImage snapshot = node.snapshot(new SnapshotParameters(), null);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer l'image du menu");
            chooser.setInitialFileName("Menu_" + inputNom.getText().replace(" ", "_") + ".png");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));

            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Image exportée",
                        "La carte menu a été enregistrée avec succès.");
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'export", e.getMessage());
        }
    }

    @FXML
    private void onEnregistrer() {
        clearAllErrors();

        Menu m = isEditMode ? menu : new Menu();
        m.setNom(inputNom.getText().trim());

        Restaurant selectedRestaurant = comboRestaurant.getValue();
        if (selectedRestaurant != null) {
            m.setRestaurantId(selectedRestaurant.getId());
            m.setRestaurantNom(selectedRestaurant.getNom());
        } else {
            m.setRestaurantId(null);
        }

        m.setPrix(null);

        m.setDescription(inputDescription.getText().trim());
        m.setDateDebut(inputDateDebut.getValue());
        m.setDateFin(inputDateFin.getValue());
        m.setActif(checkActif.isSelected());

        // Collect selected dishes
        List<Long> selectedDishes = dishCheckboxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((RepasDetaille) cb.getUserData()).getId())
                .collect(Collectors.toList());
        m.setDishesIds(selectedDishes);

        ValidationResult validation = m.validate();
        if (validation.hasErrors()) {
            displayValidationErrors(validation);
            return;
        }

        try {
            if (isEditMode) {
                controller.updateMenu(m);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu mis à jour",
                        "Le menu a été modifié avec succès.");
            } else {
                controller.createMenu(m);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu créé",
                        "Le menu a été créé avec succès.");
            }
            closeForm();
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        closeForm();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void updateHintNom() {
        if (hintNom == null)
            return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM_MENU + " car."
                + (len == 0 ? " — Obligatoire" : len > MAX_NOM_MENU ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM_MENU
                ? "-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: #5e7d75; -fx-font-size: 11px;");
    }

    private void clearAllErrors() {
        clearError(errorNom);
        clearError(errorRestaurant);
        clearError(errorDates);
        if (errorDescription != null)
            clearError(errorDescription);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
    }

    private void clearError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void showError(Label label, String message) {
        label.setText("⚠ " + message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void displayValidationErrors(ValidationResult validation) {
        if (!isEmpty(validation.getFieldErrors("nom")))
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        if (!isEmpty(validation.getFieldErrors("restaurant")))
            showError(errorRestaurant, validation.getFieldErrors("restaurant").get(0));
        if (!isEmpty(validation.getFieldErrors("dates")))
            showError(errorDates, validation.getFieldErrors("dates").get(0));
        if (errorDescription != null && !isEmpty(validation.getFieldErrors("description")))
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        if (validation.hasErrors())
            showGlobalError("Veuillez corriger les erreurs indiquées avant d'enregistrer.");
    }

    private boolean isEmpty(java.util.List<?> list) {
        return list == null || list.isEmpty();
    }

    private void showGlobalError(String message) {
        globalErrorMessage.setText(message);
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
    }

    private void closeForm() {
        Stage stage = (Stage) formTitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue", message);
    }
}
