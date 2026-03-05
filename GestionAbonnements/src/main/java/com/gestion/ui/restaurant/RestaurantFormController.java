package com.gestion.ui.restaurant;

import com.gestion.controllers.RestaurantController;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.services.GeminiVisionService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le formulaire de restaurant
 */
public class RestaurantFormController implements Initializable {

    @FXML
    private Label formTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private TextArea inputAdresse;
    @FXML
    private TextField inputTelephone;
    @FXML
    private TextField inputEmail;
    @FXML
    private TextField inputNombrePlaces;
    @FXML
    private TextArea inputDescription;
    @FXML
    private TextField inputImageUrl;
    @FXML
    private CheckBox checkActif;
    @FXML
    private Label errorNom;
    @FXML
    private Label hintNom;
    @FXML
    private Label errorTelephone;
    @FXML
    private Label errorEmail;
    @FXML
    private Label errorNombrePlaces;
    @FXML
    private Label errorAdresse;
    @FXML
    private Label errorDescription;
    @FXML
    private Label errorImageUrl;
    @FXML
    private VBox errorContainer;
    @FXML
    private Label globalErrorMessage;
    @FXML
    private Button btnGenerateAI;

    private final GeminiVisionService geminiService = new GeminiVisionService();

    private RestaurantController controller = new RestaurantController();
    private Restaurant restaurant;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        checkActif.setSelected(true);
        setupValidationListeners();
        updateHintNom();
    }

    private static final int MAX_NOM_RESTAURANT = 100;

    private void setupValidationListeners() {
        inputNom.textProperty().addListener((obs, oldVal, newVal) -> {
            clearError(errorNom);
            updateHintNom();
        });
        inputTelephone.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorTelephone));
        inputEmail.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorEmail));
        inputNombrePlaces.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorNombrePlaces));
        inputAdresse.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorAdresse));
        inputDescription.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorDescription));
        if (inputImageUrl != null)
            inputImageUrl.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorImageUrl));
    }

    private void updateHintNom() {
        if (hintNom == null)
            return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM_RESTAURANT + " car."
                + (len == 0 ? " — Obligatoire" : len > MAX_NOM_RESTAURANT ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM_RESTAURANT ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
        this.isEditMode = (restaurant != null);

        if (isEditMode) {
            formTitle.setText("Modifier Restaurant");
            populateFields();
        } else {
            formTitle.setText("Nouveau Restaurant");
        }
    }

    /**
     * Utilise le même contrôleur que la liste pour que la sauvegarde soit visible
     * dans la liste (même stockage).
     */
    public void setController(RestaurantController controller) {
        if (controller != null) {
            this.controller = controller;
        }
    }

    private void populateFields() {
        if (restaurant == null)
            return;

        inputNom.setText(restaurant.getNom());
        inputAdresse.setText(restaurant.getAdresse());
        inputTelephone.setText(restaurant.getTelephone());
        inputEmail.setText(restaurant.getEmail());
        inputNombrePlaces.setText(String.valueOf(restaurant.getNombrePlaces()));
        inputDescription.setText(restaurant.getDescription());
        inputImageUrl.setText(restaurant.getImageUrl());
        checkActif.setSelected(restaurant.isActif());
    }

    @FXML
    private void onEnregistrer() {
        clearAllErrors();

        Restaurant r = isEditMode ? restaurant : new Restaurant();
        r.setNom(inputNom.getText().trim());
        r.setAdresse(inputAdresse.getText().trim());
        r.setTelephone(inputTelephone.getText().trim());
        r.setEmail(inputEmail.getText().trim());
        try {
            r.setNombrePlaces(Integer.parseInt(inputNombrePlaces.getText().trim()));
        } catch (NumberFormatException e) {
            showError(errorNombrePlaces, "Veuillez entrer un nombre valide.");
            return;
        }
        r.setDescription(inputDescription.getText().trim());
        r.setImageUrl(inputImageUrl.getText().trim());
        r.setActif(checkActif.isSelected());

        ValidationResult validation = r.validate();
        if (validation.hasErrors()) {
            displayValidationErrors(validation);
            return;
        }

        try {
            if (isEditMode) {
                controller.updateRestaurant(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Restaurant mis à jour",
                        "Le restaurant a été modifié avec succès.");
            } else {
                controller.createRestaurant(r);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Restaurant créé",
                        "Le restaurant a été créé avec succès.");
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

    @FXML
    private void onBrowseImage() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        java.io.File file = fileChooser.showOpenDialog(btnGenerateAI.getScene().getWindow());
        if (file != null) {
            inputImageUrl.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onGenerateAI() {
        String input = inputImageUrl.getText();
        if (input == null || input.trim().isEmpty()) {
            showError("Veuillez d'abord saisir une URL d'image ou sélectionner un fichier local.");
            return;
        }

        btnGenerateAI.setDisable(true);
        btnGenerateAI.setText("✨ ANALYSE EN COURS...");

        new Thread(() -> {
            try {
                javafx.application.Platform.runLater(() -> btnGenerateAI.setText("🛰️ GEMINI VISION..."));

                String deepPrompt = "Agis en tant qu'expert critique culinaire international et stratège marketing. " +
                        "Analyse cette image avec une profondeur maximale (Deep Vision). " +
                        "Génère une description de restaurant prestigieuse, captivante et haut de gamme. " +
                        "Inclus des détails sur l'atmosphère, le style culinaire possible et l'expérience client. " +
                        "Rédige en Français élégant (4-5 phrases).";

                // Uses the API key from afilnet.properties (loaded by GeminiVisionService)
                String description = geminiService.analyzeImage(input, deepPrompt);

                if (description != null
                        && !description.startsWith("Error:")
                        && !description.startsWith("Fatal Route Error:")
                        && !description.contains("Universal AI Blockade")
                        && !description.contains("API key")) {
                    javafx.application.Platform.runLater(() -> {
                        inputDescription.setText(description);
                        finalizeAI();
                    });
                } else {
                    final String errorMsg = description;
                    javafx.application.Platform.runLater(() -> {
                        showError("Échec de l'analyse IA.\n" + errorMsg
                                + "\n\nVérifiez votre clé API Gemini dans afilnet.properties.");
                        finalizeAI();
                    });
                }

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur IA: " + e.getMessage());
                    finalizeAI();
                });
            }
        }).start();
    }

    private void finalizeAI() {
        btnGenerateAI.setDisable(false);
        btnGenerateAI.setText("✨ AUTO-DESCRIBE");
    }

    private void displayValidationErrors(ValidationResult validation) {
        if (validation.getFieldErrors("nom") != null && !validation.getFieldErrors("nom").isEmpty()) {
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        }
        if (validation.getFieldErrors("telephone") != null && !validation.getFieldErrors("telephone").isEmpty()) {
            showError(errorTelephone, validation.getFieldErrors("telephone").get(0));
        }
        if (validation.getFieldErrors("email") != null && !validation.getFieldErrors("email").isEmpty()) {
            showError(errorEmail, validation.getFieldErrors("email").get(0));
        }
        if (errorAdresse != null && validation.getFieldErrors("adresse") != null
                && !validation.getFieldErrors("adresse").isEmpty()) {
            showError(errorAdresse, validation.getFieldErrors("adresse").get(0));
        }
        if (errorDescription != null && validation.getFieldErrors("description") != null
                && !validation.getFieldErrors("description").isEmpty()) {
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        }
        if (errorImageUrl != null && validation.getFieldErrors("imageUrl") != null
                && !validation.getFieldErrors("imageUrl").isEmpty()) {
            showError(errorImageUrl, validation.getFieldErrors("imageUrl").get(0));
        }
        if (validation.hasErrors()) {
            showGlobalError("Veuillez corriger les erreurs indiquées avant d'enregistrer.");
        }
    }

    private void clearAllErrors() {
        clearError(errorNom);
        clearError(errorTelephone);
        clearError(errorEmail);
        clearError(errorAdresse);
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
