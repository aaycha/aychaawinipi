package com.gestion.ui.restaurant;

import com.gestion.controllers.RestaurantController;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
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

    @FXML private Label formTitle;
    @FXML private TextField inputNom;
    @FXML private TextArea inputAdresse;
    @FXML private TextField inputTelephone;
    @FXML private TextField inputEmail;
    @FXML private TextArea inputDescription;
    @FXML private TextField inputImageUrl;
    @FXML private CheckBox checkActif;
    @FXML private Label errorNom;
    @FXML private Label hintNom;
    @FXML private Label errorTelephone;
    @FXML private Label errorEmail;
    @FXML private Label errorAdresse;
    @FXML private Label errorDescription;
    @FXML private Label errorImageUrl;
    @FXML private VBox errorContainer;
    @FXML private Label globalErrorMessage;

    private RestaurantController controller = new RestaurantController();
    private RestaurantListeController listeController;
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
        inputNom.textProperty().addListener((obs, oldVal, newVal) -> { clearError(errorNom); updateHintNom(); });
        inputTelephone.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorTelephone));
        inputEmail.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorEmail));
        inputAdresse.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorAdresse));
        inputDescription.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorDescription));
        if (inputImageUrl != null) inputImageUrl.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorImageUrl));
    }

    private void updateHintNom() {
        if (hintNom == null) return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM_RESTAURANT + " car." + (len == 0 ? " — Obligatoire" : len > MAX_NOM_RESTAURANT ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM_RESTAURANT ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
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

    public void setListeController(RestaurantListeController listeController) {
        this.listeController = listeController;
    }

    /** Utilise le même contrôleur que la liste pour que la sauvegarde soit visible dans la liste (même stockage). */
    public void setController(RestaurantController controller) {
        if (controller != null) {
            this.controller = controller;
        }
    }

    private void populateFields() {
        if (restaurant == null) return;

        inputNom.setText(restaurant.getNom());
        inputAdresse.setText(restaurant.getAdresse());
        inputTelephone.setText(restaurant.getTelephone());
        inputEmail.setText(restaurant.getEmail());
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
        if (errorAdresse != null && validation.getFieldErrors("adresse") != null && !validation.getFieldErrors("adresse").isEmpty()) {
            showError(errorAdresse, validation.getFieldErrors("adresse").get(0));
        }
        if (errorDescription != null && validation.getFieldErrors("description") != null && !validation.getFieldErrors("description").isEmpty()) {
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        }
        if (errorImageUrl != null && validation.getFieldErrors("imageUrl") != null && !validation.getFieldErrors("imageUrl").isEmpty()) {
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
