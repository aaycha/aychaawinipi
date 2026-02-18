package com.gestion.ui.repas;

import com.gestion.controllers.RepasController;
import com.gestion.entities.Menu;
import com.gestion.entities.Repas;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le formulaire de repas (plat)
 */
public class RepasFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField inputNom;
    @FXML private ComboBox<Restaurant> comboRestaurant;
    @FXML private ComboBox<Menu> comboMenu;
    @FXML private TextField inputPrix;
    @FXML private ComboBox<Repas.Categorie> comboCategorie;
    @FXML private ComboBox<Repas.TypePlat> comboTypePlat;
    @FXML private TextField inputTempsPreparation;
    @FXML private TextArea inputDescription;
    @FXML private TextField inputImageUrl;
    @FXML private CheckBox checkDisponible;
    @FXML private Label errorNom;
    @FXML private Label errorRestaurantMenu;
    @FXML private Label errorPrix;
    @FXML private Label errorTempsPreparation;
    @FXML private Label errorCategorie;
    @FXML private Label errorTypePlat;
    @FXML private Label errorDescription;
    @FXML private Label errorImageUrl;
    @FXML private Label hintNom;
    @FXML private Label hintPrix;
    @FXML private Label hintDescription;
    @FXML private Label hintImageUrl;
    @FXML private Label hintTempsPrep;
    @FXML private VBox errorContainer;
    @FXML private Label globalErrorMessage;

    private static final int MAX_NOM = 120;
    private static final int MAX_DESCRIPTION = 1000;
    private static final int MAX_IMAGE_URL = 255;

    private RepasController controller = new RepasController();
    private RepasListeController listeController;
    private ObservableList<Restaurant> restaurants;
    private ObservableList<Menu> menus;
    private Repas repas;
    private boolean isEditMode = false;

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
    }

    private void setupTooltips() {
        if (inputNom != null) inputNom.setTooltip(new Tooltip("Nom du plat (obligatoire). Ex. : Mloukhia, Pizza 4 fromages. Max 120 caractères."));
        if (inputPrix != null) inputPrix.setTooltip(new Tooltip("Prix en euros. Ex. : 12.50 ou 10. Laissez vide si non renseigné."));
        if (inputTempsPreparation != null) inputTempsPreparation.setTooltip(new Tooltip("Temps de préparation en minutes (0 à 1440)."));
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
        if (comboCategorie != null) comboCategorie.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorCategorie));
        if (comboTypePlat != null) comboTypePlat.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorTypePlat));
        if (inputDescription != null) inputDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorDescription);
            updateHintDescription();
        });
        if (inputImageUrl != null) inputImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorImageUrl);
            updateHintImageUrl();
        });
    }

    private void updateHintNom() {
        if (hintNom == null) return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM + " car." + (len == 0 ? " — Obligatoire" : len > MAX_NOM ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintPrix() {
        if (hintPrix == null) return;
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
        if (hintDescription == null || inputDescription == null) return;
        String s = inputDescription.getText();
        int len = s == null ? 0 : s.length();
        hintDescription.setText(len + " / " + MAX_DESCRIPTION + " car. max");
        hintDescription.setStyle(len > MAX_DESCRIPTION ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintImageUrl() {
        if (hintImageUrl == null || inputImageUrl == null) return;
        String s = inputImageUrl.getText();
        int len = s == null ? 0 : s.length();
        hintImageUrl.setText(len + " / " + MAX_IMAGE_URL + " car. max. Optionnel.");
        hintImageUrl.setStyle(len > MAX_IMAGE_URL ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintTempsPrep() {
        if (hintTempsPrep == null) return;
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

    public void setListeController(RepasListeController controller) {
        this.listeController = controller;
    }

    /** Utilise le même contrôleur que la liste pour que la sauvegarde soit visible dans la liste (même stockage). */
    public void setController(RepasController controller) {
        if (controller != null) {
            this.controller = controller;
        }
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
        if (repas == null) return;

        inputNom.setText(repas.getNom());
        inputPrix.setText(repas.getPrix() != null ? repas.getPrix().toString() : "");
        inputDescription.setText(repas.getDescription());
        inputTempsPreparation.setText(repas.getTempsPreparation() != null ? repas.getTempsPreparation().toString() : "");
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

        ValidationResult validation = r.validate();
        if (validation.hasErrors()) {
            displayValidationErrors(validation);
            return;
        }

        try {
            if (isEditMode) {
                controller.updateRepas(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Plat mis à jour",
                        "Le plat a été modifié avec succès.");
            } else {
                controller.createRepas(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Plat créé",
                        "Le plat a été créé avec succès.");
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

    private void displayValidationErrors(ValidationResult validation) {
        if (validation.getFieldErrors("nom") != null && !validation.getFieldErrors("nom").isEmpty()) {
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        }
        StringBuilder restMenuErr = new StringBuilder();
        if (validation.getFieldErrors("restaurant") != null && !validation.getFieldErrors("restaurant").isEmpty()) {
            restMenuErr.append(validation.getFieldErrors("restaurant").get(0));
        }
        if (validation.getFieldErrors("menu") != null && !validation.getFieldErrors("menu").isEmpty()) {
            if (restMenuErr.length() > 0) restMenuErr.append("\n");
            restMenuErr.append(validation.getFieldErrors("menu").get(0));
        }
        if (restMenuErr.length() > 0) {
            showError(errorRestaurantMenu, restMenuErr.toString());
        }
        if (validation.getFieldErrors("prix") != null && !validation.getFieldErrors("prix").isEmpty()) {
            showError(errorPrix, validation.getFieldErrors("prix").get(0));
        }
        if (validation.getFieldErrors("categorie") != null && !validation.getFieldErrors("categorie").isEmpty() && errorCategorie != null) {
            showError(errorCategorie, validation.getFieldErrors("categorie").get(0));
        }
        if (validation.getFieldErrors("typePlat") != null && !validation.getFieldErrors("typePlat").isEmpty() && errorTypePlat != null) {
            showError(errorTypePlat, validation.getFieldErrors("typePlat").get(0));
        }
        if (validation.getFieldErrors("tempsPreparation") != null && !validation.getFieldErrors("tempsPreparation").isEmpty()) {
            showError(errorTempsPreparation, validation.getFieldErrors("tempsPreparation").get(0));
        }
        if (validation.getFieldErrors("description") != null && !validation.getFieldErrors("description").isEmpty() && errorDescription != null) {
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        }
        if (validation.getFieldErrors("imageUrl") != null && !validation.getFieldErrors("imageUrl").isEmpty() && errorImageUrl != null) {
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
        if (errorCategorie != null) clearError(errorCategorie);
        if (errorTypePlat != null) clearError(errorTypePlat);
        clearError(errorTempsPreparation);
        if (errorDescription != null) clearError(errorDescription);
        if (errorImageUrl != null) clearError(errorImageUrl);
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
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
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
