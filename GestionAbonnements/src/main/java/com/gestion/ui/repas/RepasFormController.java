package com.gestion.ui.repas;

import com.gestion.entities.Menu;
import com.gestion.entities.Repas;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.FlowPane;
import com.gestion.entities.Ingredient;
import com.gestion.services.IngredientServiceImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Contrôleur pour le formulaire de repas (plat)
 */
public class RepasFormController implements Initializable {

    @FXML
    private Label formTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<Restaurant> comboRestaurant;
    @FXML
    private ComboBox<Menu> comboMenu;
    @FXML
    private TextField inputPrix;
    @FXML
    private ComboBox<Repas.Categorie> comboCategorie;
    @FXML
    private ComboBox<Repas.TypePlat> comboTypePlat;
    @FXML
    private TextField inputTempsPreparation;
    @FXML
    private TextArea inputDescription;
    @FXML
    private TextField inputImageUrl;
    @FXML
    private CheckBox checkDisponible;
    @FXML
    private Label errorNom;
    @FXML
    private Label errorRestaurantMenu;
    @FXML
    private Label errorPrix;
    @FXML
    private Label errorTempsPreparation;
    @FXML
    private Label errorCategorie;
    @FXML
    private Label errorTypePlat;
    @FXML
    private Label errorDescription;
    @FXML
    private Label errorImageUrl;
    @FXML
    private Label hintNom;
    @FXML
    private Label hintPrix;
    @FXML
    private Label hintDescription;
    @FXML
    private Label hintImageUrl;
    @FXML
    private Label hintTempsPrep;
    @FXML
    private VBox errorContainer;
    @FXML
    private Label globalErrorMessage;
    @FXML
    private ToggleButton iaToggleInForm;
    @FXML
    private HBox allergenBox;
    @FXML
    private Label statusMessage;
    @FXML
    private Label iaInsightLabel;
    @FXML
    private Label mockCalories;
    @FXML
    private Label mockProteins;
    @FXML
    private FlowPane ingredientChips;
    @FXML
    private ComboBox<Ingredient> comboAddIngredient;

    private static final int MAX_NOM = 120;
    private static final int MAX_DESCRIPTION = 1000;
    private static final int MAX_IMAGE_URL = 255;

    private final com.gestion.controllers.RepasController repasController = new com.gestion.controllers.RepasController();
    private IngredientServiceImpl ingredientService = new IngredientServiceImpl();
    private ObservableList<Restaurant> restaurants;
    private ObservableList<Menu> menus;
    private Repas repas;
    private boolean isEditMode = false;
    private List<String> selectedIngredients = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupComboBoxes();
        checkDisponible.setSelected(true);
        setupValidationListeners();
        setupTooltips();
        updateHintNom();
        updateHintPrix();
        updateHintDescription();
        updateHintImageUrl();
        updateHintTempsPrep();
        loadAvailableIngredients();
    }

    private void loadAvailableIngredients() {
        if (comboAddIngredient != null) {
            comboAddIngredient
                    .setItems(javafx.collections.FXCollections.observableArrayList(ingredientService.findAll()));
        }
    }

    private void renderIngredientChips() {
        if (ingredientChips == null)
            return;
        ingredientChips.getChildren().clear();
        for (String ing : selectedIngredients) {
            HBox chip = new HBox(5);
            chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            chip.setStyle(
                    "-fx-background-color: rgba(34, 197, 94, 0.15); -fx-padding: 3 8; -fx-background-radius: 15; -fx-border-color: rgba(34, 197, 94, 0.3); -fx-border-radius: 15;");

            Label name = new Label(ing);
            name.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");

            Label x = new Label("✕");
            x.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: 900;");
            x.setOnMouseClicked(e -> {
                selectedIngredients.remove(ing);
                renderIngredientChips();
            });

            chip.getChildren().addAll(name, x);
            ingredientChips.getChildren().add(chip);
        }
    }

    @FXML
    private void onAddIngredient() {
        Ingredient selected = comboAddIngredient.getValue();
        if (selected != null && !selectedIngredients.contains(selected.getNom())) {
            selectedIngredients.add(selected.getNom());
            renderIngredientChips();
            comboAddIngredient.setValue(null);
            statusMessage.setText("Ingrédient " + selected.getNom() + " ajouté");
        }
    }

    private void setupTooltips() {
        if (inputNom != null)
            inputNom.setTooltip(
                    new Tooltip("Nom du plat (obligatoire). Ex. : Mloukhia, Pizza 4 fromages. Max 120 caractères."));
        if (inputPrix != null)
            inputPrix.setTooltip(new Tooltip("Prix en euros. Ex. : 12.50 ou 10. Laissez vide si non renseigné."));
        if (inputTempsPreparation != null)
            inputTempsPreparation.setTooltip(new Tooltip("Temps de préparation en minutes (0 à 1440)."));
    }

    private void setupComboBoxes() {
        comboCategorie.setItems(javafx.collections.FXCollections.observableArrayList(Repas.Categorie.values()));
        comboTypePlat.setItems(javafx.collections.FXCollections.observableArrayList(Repas.TypePlat.values()));
    }

    private void setupValidationListeners() {
        inputNom.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorNom);
            updateHintNom();
        });
        inputPrix.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorPrix);
            updateHintPrix();
        });
        inputTempsPreparation.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorTempsPreparation);
            updateHintTempsPrep();
        });
        comboRestaurant.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorRestaurantMenu));
        comboMenu.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorRestaurantMenu));
        if (comboCategorie != null)
            comboCategorie.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorCategorie));
        if (comboTypePlat != null)
            comboTypePlat.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorTypePlat));
        if (inputDescription != null)
            inputDescription.textProperty().addListener((obs, oldVal, newVal) -> {
                clearError(errorDescription);
                updateHintDescription();
                detectAllergens(newVal);
            });
        if (inputImageUrl != null)
            inputImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
                clearError(errorImageUrl);
                updateHintImageUrl();
            });

        inputNom.textProperty().addListener((obs, oldVal, newVal) -> {
            updateIAInsights(newVal);
        });
    }

    private void updateIAInsights(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            iaInsightLabel.setText("Saisissez un nom pour analyser le marché local.");
            return;
        }
        if (nom.toLowerCase().contains("poke") || nom.toLowerCase().contains("bowl")) {
            iaInsightLabel.setText("💡 Tendance forte : Les clients de votre zone recherchent des options saines.");
        } else if (nom.toLowerCase().contains("burger") || nom.toLowerCase().contains("pizza")) {
            iaInsightLabel.setText("📉 Marché saturé : Différenciez-vous avec des ingrédients premium.");
        } else {
            iaInsightLabel.setText("✨ Potentiel détecté : Ce plat pourrait plaire à 78% de vos abonnés.");
        }
    }

    private void detectAllergens(String desc) {
        if (desc == null || desc.isEmpty()) {
            allergenBox.setVisible(false);
            allergenBox.setManaged(false);
            return;
        }

        allergenBox.getChildren().clear();
        Label head = new Label("Allergènes détectés :");
        head.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        allergenBox.getChildren().add(head);

        boolean found = false;
        String lower = desc.toLowerCase();

        if (lower.contains("farine") || lower.contains("pain") || lower.contains("gluten")) {
            allergenBox.getChildren().add(createAllergenBadge("GLUTEN", "#EF4444"));
            found = true;
        }
        if (lower.contains("lait") || lower.contains("fromage") || lower.contains("crème")) {
            allergenBox.getChildren().add(createAllergenBadge("LACTOSE", "#F59E0B"));
            found = true;
        }
        if (lower.contains("œuf") || lower.contains("oeuf")) {
            allergenBox.getChildren().add(createAllergenBadge("OEUFS", "#3B82F6"));
            found = true;
        }

        allergenBox.setVisible(found);
        allergenBox.setManaged(found);
    }

    private Label createAllergenBadge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + color
                + "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 9px; -fx-font-weight: bold;");
        return l;
    }

    private void updateHintNom() {
        if (hintNom == null)
            return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM + " car."
                + (len == 0 ? " — Obligatoire" : len > MAX_NOM ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintPrix() {
        if (hintPrix == null)
            return;
        String s = inputPrix.getText();
        if (s == null || s.trim().isEmpty()) {
            hintPrix.setText("Nombre décimal (ex. 12.50). Optionnel.");
            return;
        }
        try {
            new BigDecimal(s.trim().replace(",", "."));
            hintPrix.setText("Format valide.");
            hintPrix.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
        } catch (NumberFormatException e) {
            hintPrix.setText("Saisir un nombre (ex. 12.50).");
            hintPrix.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
        }
    }

    private void updateHintDescription() {
        if (hintDescription == null || inputDescription == null)
            return;
        String s = inputDescription.getText();
        int len = s == null ? 0 : s.length();
        hintDescription.setText(len + " / " + MAX_DESCRIPTION + " car. max");
        hintDescription.setStyle(len > MAX_DESCRIPTION ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintImageUrl() {
        if (hintImageUrl == null || inputImageUrl == null)
            return;
        String s = inputImageUrl.getText();
        int len = s == null ? 0 : s.length();
        hintImageUrl.setText(len + " / " + MAX_IMAGE_URL + " car. max. Optionnel.");
        hintImageUrl.setStyle(len > MAX_IMAGE_URL ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintTempsPrep() {
        if (hintTempsPrep == null)
            return;
        String s = inputTempsPreparation.getText();
        if (s == null || s.trim().isEmpty()) {
            hintTempsPrep.setText("Minutes (0 à 1440). Optionnel.");
            hintTempsPrep.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            return;
        }
        try {
            int v = Integer.parseInt(s.trim());
            if (v < 0 || v > 1440) {
                hintTempsPrep.setText("Entre 0 et 1440 minutes.");
                hintTempsPrep.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
            } else {
                hintTempsPrep.setText("Valide.");
                hintTempsPrep.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
            }
        } catch (NumberFormatException e) {
            hintTempsPrep.setText("Nombre entier (ex. 15).");
            hintTempsPrep.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
        }
    }

    public void setRepas(Repas repas) {
        this.repas = repas;
        this.isEditMode = (repas != null);

        if (isEditMode) {
            formTitle.setText("Modifier Plat");
            populateFields();
        } else {
            formTitle.setText("Nouveau Plat");
        }
    }

    /**
     * Utilise le même contrôleur que la liste pour que la sauvegarde soit visible
     * dans la liste (même stockage).
     */
    public void setController(com.gestion.controllers.RepasController controller) {
        // Method kept for compatibility, we use repasController field
    }

    public void setRestaurants(ObservableList<Restaurant> restaurants) {
        this.restaurants = restaurants;
        comboRestaurant.setItems(restaurants);
        comboRestaurant.setPromptText("Sélectionnez un restaurant (optionnel)");
    }

    public void setMenus(ObservableList<Menu> menus) {
        this.menus = menus;
        comboMenu.setItems(menus);
        comboMenu.setPromptText("Sélectionnez un menu (optionnel)");
    }

    private void populateFields() {
        if (repas == null)
            return;

        inputNom.setText(repas.getNom());
        inputPrix.setText(repas.getPrix() != null ? repas.getPrix().toString() : "");
        inputDescription.setText(repas.getDescription());
        inputTempsPreparation
                .setText(repas.getTempsPreparation() != null ? repas.getTempsPreparation().toString() : "");
        inputImageUrl.setText(repas.getImageUrl());
        checkDisponible.setSelected(repas.isDisponible());

        comboCategorie.setValue(repas.getCategorie());
        comboTypePlat.setValue(repas.getTypePlat());

        // Set restaurant selection
        if (restaurants != null && repas.getRestaurantId() != null) {
            for (Restaurant r : restaurants) {
                if (r.getId().equals(repas.getRestaurantId())) {
                    comboRestaurant.setValue(r);
                    break;
                }
            }
        }

        // Set menu selection
        if (menus != null && repas.getMenuId() != null) {
            for (Menu m : menus) {
                if (m.getId().equals(repas.getMenuId())) {
                    comboMenu.setValue(m);
                    break;
                }
            }
        }

        // Set ingredients
        if (repas.getIngredients() != null && !repas.getIngredients().isEmpty()) {
            selectedIngredients = new ArrayList<>(Arrays.asList(repas.getIngredients().split(",")));
            renderIngredientChips();
        }
    }

    @FXML
    private void onEnregistrer() {
        clearAllErrors();

        Repas r = isEditMode ? repas : new Repas();
        r.setNom(inputNom.getText().trim());

        Restaurant selectedRestaurant = comboRestaurant.getValue();
        if (selectedRestaurant != null) {
            r.setRestaurantId(selectedRestaurant.getId());
            r.setRestaurantNom(selectedRestaurant.getNom());
        } else {
            r.setRestaurantId(null);
        }

        Menu selectedMenu = comboMenu.getValue();
        if (selectedMenu != null) {
            r.setMenuId(selectedMenu.getId());
            r.setMenuNom(selectedMenu.getNom());
        } else {
            r.setMenuId(null);
        }

        // Parse prix
        String prixText = inputPrix.getText().trim();
        if (!prixText.isEmpty()) {
            try {
                r.setPrix(new BigDecimal(prixText.replace(",", ".")));
            } catch (NumberFormatException e) {
                showError(errorPrix, "Le prix doit être un nombre valide");
                return;
            }
        } else {
            r.setPrix(null);
        }

        // Parse temps de préparation
        String tempsText = inputTempsPreparation.getText().trim();
        if (!tempsText.isEmpty()) {
            try {
                r.setTempsPreparation(Integer.parseInt(tempsText));
            } catch (NumberFormatException e) {
                showError(errorTempsPreparation, "Le temps doit être un nombre entier");
                return;
            }
        } else {
            r.setTempsPreparation(null);
        }

        r.setCategorie(comboCategorie.getValue());
        r.setTypePlat(comboTypePlat.getValue());
        r.setDescription(inputDescription.getText().trim());
        r.setImageUrl(inputImageUrl.getText().trim());
        r.setDisponible(checkDisponible.isSelected());
        r.setIngredients(String.join(",", selectedIngredients));

        ValidationResult validation = r.validate();
        if (validation.hasErrors()) {
            displayValidationErrors(validation);
            return;
        }

        try {
            if (isEditMode) {
                repasController.updateRepas(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Plat mis à jour",
                        "Le plat a été modifié avec succès.");
            } else {
                repasController.createRepas(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Plat créé",
                        "Le plat a été créé avec succès.");
            }

            closeForm();

        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    @FXML
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File selectedFile = fileChooser.showOpenDialog(formTitle.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Créer le dossier uploads s'il n'existe pas
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Générer un nom de fichier unique
                String fileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                Path targetPath = uploadDir.resolve(fileName);

                // Copier le fichier
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Mettre à jour le champ URL avec le chemin relatif
                inputImageUrl.setText("uploads/" + fileName);

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Image importée",
                        "L'image a été copiée avec succès : " + fileName);
            } catch (IOException e) {
                showError("Erreur lors de l'upload de l'image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void onIAGenerate() {
        String nom = inputNom.getText();
        if (nom == null || nom.trim().isEmpty()) {
            showError("Veuillez d'abord saisir un nom de plat.");
            return;
        }

        statusMessage.setText("Analyse du plat...");

        // Mocking IA process with a small pause if possible, but here we just update
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            inputDescription.setText("Savourez notre délicieux " + nom
                    + ", préparé avec des ingrédients frais sélectionnés par nos chefs. Une expérience culinaire unique qui allie goût et bien-être.");
            mockCalories.setText("450 kcal");
            mockProteins.setText("22g");
            comboCategorie.setValue(Repas.Categorie.PLAT_PRINCIPAL);
            statusMessage.setText("Génération terminée ✨");
        });
        pause.play();
    }

    @FXML
    private void onTrendPokeBowl() {
        inputNom.setText("Island Style Poke Bowl");
        inputDescription.setText(
                "Bol frais composé de thon mariné, riz vinaigré, avocat, edamame et une sauce soja-gingembre maison.");
        comboCategorie.setValue(Repas.Categorie.ENTREE);
        comboTypePlat.setValue(Repas.TypePlat.VEGETARIEN);
        inputPrix.setText("14.50");
        inputTempsPreparation.setText("12");
        mockCalories.setText("380 kcal");
        mockProteins.setText("18g");
        statusMessage.setText("Modèle Poke Bowl appliqué !");
    }

    @FXML
    private void onTrendTaco() {
        inputNom.setText("Duo de Tacos Al Pastor");
        inputDescription
                .setText("Tortillas de maïs garnies de porc mariné à l'ananas, oignons, coriandre et citron vert.");
        comboCategorie.setValue(Repas.Categorie.PLAT_PRINCIPAL);
        comboTypePlat.setValue(Repas.TypePlat.AUTRE);
        inputPrix.setText("12.00");
        inputTempsPreparation.setText("15");
        mockCalories.setText("520 kcal");
        mockProteins.setText("28g");
        statusMessage.setText("Modèle Taco appliqué !");
    }

    @FXML
    private void onAnnuler() {
        closeForm();
    }

    private void displayValidationErrors(ValidationResult validation) {
        if (validation.getFieldErrors("nom") != null && !validation.getFieldErrors("nom").isEmpty()) {
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        }
        StringBuilder restMenuErr = new StringBuilder();
        if (validation.getFieldErrors("restaurant") != null && !validation.getFieldErrors("restaurant").isEmpty()) {
            restMenuErr.append(validation.getFieldErrors("restaurant").get(0));
        }
        if (validation.getFieldErrors("menu") != null && !validation.getFieldErrors("menu").isEmpty()) {
            if (restMenuErr.length() > 0)
                restMenuErr.append("\n");
            restMenuErr.append(validation.getFieldErrors("menu").get(0));
        }
        if (restMenuErr.length() > 0) {
            showError(errorRestaurantMenu, restMenuErr.toString());
        }
        if (validation.getFieldErrors("prix") != null && !validation.getFieldErrors("prix").isEmpty()) {
            showError(errorPrix, validation.getFieldErrors("prix").get(0));
        }
        if (validation.getFieldErrors("categorie") != null && !validation.getFieldErrors("categorie").isEmpty()
                && errorCategorie != null) {
            showError(errorCategorie, validation.getFieldErrors("categorie").get(0));
        }
        if (validation.getFieldErrors("typePlat") != null && !validation.getFieldErrors("typePlat").isEmpty()
                && errorTypePlat != null) {
            showError(errorTypePlat, validation.getFieldErrors("typePlat").get(0));
        }
        if (validation.getFieldErrors("tempsPreparation") != null
                && !validation.getFieldErrors("tempsPreparation").isEmpty()) {
            showError(errorTempsPreparation, validation.getFieldErrors("tempsPreparation").get(0));
        }
        if (validation.getFieldErrors("description") != null && !validation.getFieldErrors("description").isEmpty()
                && errorDescription != null) {
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        }
        if (validation.getFieldErrors("imageUrl") != null && !validation.getFieldErrors("imageUrl").isEmpty()
                && errorImageUrl != null) {
            showError(errorImageUrl, validation.getFieldErrors("imageUrl").get(0));
        }
        if (validation.hasErrors()) {
            showGlobalError("Veuillez corriger les erreurs indiquées ci-dessous avant d'enregistrer.");
        }
    }

    private void clearAllErrors() {
        clearError(errorNom);
        clearError(errorRestaurantMenu);
        clearError(errorPrix);
        if (errorCategorie != null)
            clearError(errorCategorie);
        if (errorTypePlat != null)
            clearError(errorTypePlat);
        clearError(errorTempsPreparation);
        if (errorDescription != null)
            clearError(errorDescription);
        if (errorImageUrl != null)
            clearError(errorImageUrl);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
    }

    private void clearError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText("⚠ " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            System.err.println("Error label is null while displaying: " + message);
        }
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
