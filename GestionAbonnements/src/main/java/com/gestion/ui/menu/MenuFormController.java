package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;

/**
 * Contrôleur pour le formulaire de menu
 */
public class MenuFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField inputNom;
    @FXML private ComboBox<Restaurant> comboRestaurant;
    @FXML private TextField inputPrix;
    @FXML private TextArea inputDescription;
    @FXML private DatePicker inputDateDebut;
    @FXML private DatePicker inputDateFin;
    @FXML private CheckBox checkActif;
    @FXML private Label errorNom;
    @FXML private Label errorRestaurant;
    @FXML private Label errorPrix;
    @FXML private Label errorDates;
    @FXML private Label errorDescription;
    @FXML private Label hintNom;
    @FXML private Label hintPrix;
    @FXML private VBox errorContainer;
    @FXML private Label globalErrorMessage;
    private static final int MAX_NOM_MENU = 100;

    private final MenuController controller = new MenuController();
    private MenuListeController listeController;
    private ObservableList<Restaurant> restaurants;
    private Menu menu;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        checkActif.setSelected(true);
        setupValidationListeners();
        updateHintNom();
        updateHintPrix();
    }

    private void setupValidationListeners() {
        inputNom.textProperty().addListener((obs, oldVal, newVal) -> { clearError(errorNom); updateHintNom(); });
        inputPrix.textProperty().addListener((obs, oldVal, newVal) -> { clearError(errorPrix); updateHintPrix(); });
        comboRestaurant.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorRestaurant));
        inputDateDebut.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorDates));
        inputDateFin.valueProperty().addListener((obs, oldVal, newVal) -> clearError(errorDates));
        if (inputDescription != null) inputDescription.textProperty().addListener((obs, oldVal, newVal) -> clearError(errorDescription));
    }

    private void updateHintNom() {
        if (hintNom == null) return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM_MENU + " car." + (len == 0 ? " — Obligatoire" : len > MAX_NOM_MENU ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM_MENU ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintPrix() {
        if (hintPrix == null) return;
        String s = inputPrix.getText();
        if (s == null || s.trim().isEmpty()) {
            hintPrix.setText("Obligatoire. Ex: 15.00");
            hintPrix.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
            return;
        }
        try {
            BigDecimal v = new BigDecimal(s.trim().replace(",", "."));
            hintPrix.setText(v.compareTo(BigDecimal.ZERO) < 0 ? "Le prix ne peut pas être négatif." : "Format valide.");
            hintPrix.setStyle(v.compareTo(BigDecimal.ZERO) < 0 ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;" : "-fx-text-fill: #27ae60; -fx-font-size: 11px;");
        } catch (NumberFormatException e) {
            hintPrix.setText("Saisir un nombre (ex. 15.00).");
            hintPrix.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
        }
    }

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
        this.listeController = controller;
    }

    public void setRestaurants(ObservableList<Restaurant> restaurants) {
        this.restaurants = restaurants;
        comboRestaurant.setItems(restaurants);
        comboRestaurant.setPromptText("Sélectionnez un restaurant");
    }

    private void populateFields() {
        if (menu == null) return;

        inputNom.setText(menu.getNom());
        inputPrix.setText(menu.getPrix() != null ? menu.getPrix().toString() : "");
        inputDescription.setText(menu.getDescription());
        inputDateDebut.setValue(menu.getDateDebut());
        inputDateFin.setValue(menu.getDateFin());
        checkActif.setSelected(menu.isActif());

        // Set restaurant selection
        if (restaurants != null && menu.getRestaurantId() != null) {
            for (Restaurant r : restaurants) {
                if (r.getId().equals(menu.getRestaurantId())) {
                    comboRestaurant.setValue(r);
                    break;
                }
            }
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

        // Parse prix
        String prixText = inputPrix.getText().trim();
        if (!prixText.isEmpty()) {
            try {
                m.setPrix(new BigDecimal(prixText.replace(",", ".")));
            } catch (NumberFormatException e) {
                showError(errorPrix, "Le prix doit être un nombre valide");
                return;
            }
        } else {
            m.setPrix(null);
        }

        m.setDescription(inputDescription.getText().trim());
        m.setDateDebut(inputDateDebut.getValue());
        m.setDateFin(inputDateFin.getValue());
        m.setActif(checkActif.isSelected());

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

    private void displayValidationErrors(ValidationResult validation) {
        if (validation.getFieldErrors("nom") != null && !validation.getFieldErrors("nom").isEmpty()) {
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        }
        if (validation.getFieldErrors("restaurant") != null && !validation.getFieldErrors("restaurant").isEmpty()) {
            showError(errorRestaurant, validation.getFieldErrors("restaurant").get(0));
        }
        if (validation.getFieldErrors("prix") != null && !validation.getFieldErrors("prix").isEmpty()) {
            showError(errorPrix, validation.getFieldErrors("prix").get(0));
        }
        if (validation.getFieldErrors("dates") != null && !validation.getFieldErrors("dates").isEmpty()) {
            showError(errorDates, validation.getFieldErrors("dates").get(0));
        }
        if (validation.getFieldErrors("description") != null && !validation.getFieldErrors("description").isEmpty() && errorDescription != null) {
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        }
        if (validation.hasErrors()) {
            showGlobalError("Veuillez corriger les erreurs indiquées avant d'enregistrer.");
        }
    }

    private void clearAllErrors() {
        clearError(errorNom);
        clearError(errorRestaurant);
        clearError(errorPrix);
        clearError(errorDates);
        if (errorDescription != null) clearError(errorDescription);
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
