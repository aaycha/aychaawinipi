package com.gestion.ui.restauration;

import com.gestion.controllers.RestaurationController;
import com.gestion.entities.Restauration;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulaire des options de restauration avec contrôles de saisie et messages de validation riches.
 */
public class RestaurationOptionsFormController {

    @FXML private Label formTitle;
    @FXML private TextField inputLibelle;
    @FXML private TextField inputTypeEvenement;
    @FXML private CheckBox inputActif;
    @FXML private Label errorLibelle;
    @FXML private Label errorTypeEvenement;
    @FXML private VBox errorContainer;
    @FXML private Label globalErrorMessage;

    private final RestaurationController controller = new RestaurationController();
    private Restauration option;
    private boolean isEditMode;
    private Runnable onSaved;

    public void setOption(Restauration option) {
        this.option = option;
        this.isEditMode = (option != null);
        if (formTitle != null) {
            formTitle.setText(isEditMode ? "Modifier l'option de restauration" : "Nouvelle option de restauration");
        }
        if (isEditMode && option != null) {
            inputLibelle.setText(option.getLibelle() != null ? option.getLibelle() : "");
            inputTypeEvenement.setText(option.getTypeEvenement() != null ? option.getTypeEvenement() : "");
            inputActif.setSelected(option.isActif());
        }
        clearAllErrors();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    public void initialize() {
        if (inputLibelle != null) inputLibelle.textProperty().addListener((o, a, b) -> clearError(errorLibelle));
        if (inputTypeEvenement != null) inputTypeEvenement.textProperty().addListener((o, a, b) -> clearError(errorTypeEvenement));
    }

    @FXML
    void onEnregistrer() {
        clearAllErrors();
        List<String> errors = new ArrayList<>();

        String libelle = inputLibelle.getText();
        if (libelle == null || libelle.trim().isEmpty()) {
            errors.add("Le libellé est obligatoire.");
            showError(errorLibelle, "Veuillez saisir un libellé pour l'option (ex: Petit-déjeuner, Déjeuner buffet).");
        } else if (libelle.length() > 120) {
            errors.add("Le libellé ne doit pas dépasser 120 caractères.");
            showError(errorLibelle, "Le libellé est trop long. Maximum 120 caractères.");
        }

        String typeEvt = inputTypeEvenement.getText();
        if (typeEvt != null && !typeEvt.trim().isEmpty()) {
            String trimmed = typeEvt.trim().toUpperCase();
            if (trimmed.length() > 50) {
                errors.add("Le type d'événement ne doit pas dépasser 50 caractères.");
                showError(errorTypeEvenement, "Type d'événement trop long. Ex: SOIREE, RANDONNEE, SEMINAIRE.");
            } else if (!trimmed.matches("^[A-Z0-9_]+$")) {
                showError(errorTypeEvenement, "Utilisez des lettres majuscules, chiffres et underscores (ex: SOIREE, RANDONNEE).");
            }
        }

        if (!errors.isEmpty()) {
            showGlobalError("Veuillez corriger les erreurs suivantes avant d'enregistrer.");
            if (globalErrorMessage != null) globalErrorMessage.setText(String.join("\n", errors));
            return;
        }

        try {
            String typeFinal = (typeEvt == null || typeEvt.isBlank()) ? "SOIREE" : typeEvt.trim().toUpperCase();
            if (isEditMode && option != null) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "La modification nécessite une implémentation dans le service.");
            } else {
                controller.createOption(libelle.trim(), typeFinal, inputActif.isSelected());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Option de restauration créée avec succès.");
            }
            if (onSaved != null) onSaved.run();
            closeForm();
        } catch (Exception e) {
            showGlobalError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    void onAnnuler() {
        closeForm();
    }

    private void clearAllErrors() {
        clearError(errorLibelle);
        clearError(errorTypeEvenement);
        if (errorContainer != null) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        }
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
        }
    }

    private void showGlobalError(String message) {
        if (globalErrorMessage != null) globalErrorMessage.setText(message);
        if (errorContainer != null) {
            errorContainer.setVisible(true);
            errorContainer.setManaged(true);
        }
    }

    private void closeForm() {
        Stage stage = (Stage) (formTitle != null ? formTitle.getScene().getWindow() : inputLibelle.getScene().getWindow());
        if (stage != null) stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
