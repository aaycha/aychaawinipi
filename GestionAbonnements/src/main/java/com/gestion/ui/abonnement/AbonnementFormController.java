package com.gestion.ui.abonnement;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AbonnementFormController {

    @FXML
    private Label mainTitle;
    @FXML
    private TextField inputUserId;
    @FXML
    private ComboBox<Abonnement.TypeAbonnement> inputType;
    @FXML
    private DatePicker inputDateDebut;
    @FXML
    private DatePicker inputDateFin;
    @FXML
    private TextField inputPrix;
    @FXML
    private ComboBox<Abonnement.StatutAbonnement> inputStatut;
    @FXML
    private CheckBox inputAutoRenew;
    @FXML
    private Label validationMessage;

    private Abonnement selectedAbonnement;
    private boolean isEditMode = false;
    private Runnable onSaveCallback;
    private final AbonnementController controller = new AbonnementController();

    @FXML
    public void initialize() {
        inputType.getItems().setAll(Abonnement.TypeAbonnement.values());
        inputStatut.getItems().setAll(Abonnement.StatutAbonnement.values());

        // Defaults
        inputDateDebut.setValue(LocalDate.now());
        inputType.getSelectionModel().select(Abonnement.TypeAbonnement.MENSUEL);
        inputStatut.getSelectionModel().select(Abonnement.StatutAbonnement.ACTIF);
        inputAutoRenew.setSelected(true);
    }

    public void setAbonnement(Abonnement abonnement) {
        this.selectedAbonnement = abonnement;
        this.isEditMode = (abonnement != null && abonnement.getId() != null);

        if (isEditMode) {
            mainTitle.setText("Modifier l'Expédition");
            inputUserId.setText(String.valueOf(abonnement.getUserId()));
            inputType.setValue(abonnement.getType());
            inputDateDebut.setValue(abonnement.getDateDebut());
            inputDateFin.setValue(abonnement.getDateFin());
            inputPrix.setText(abonnement.getPrix().toString());
            inputStatut.setValue(abonnement.getStatut());
            inputAutoRenew.setSelected(abonnement.isAutoRenew());
        } else if (abonnement != null) {
            // New with pre-filled fields (e.g. userId)
            if (abonnement.getUserId() != null) {
                inputUserId.setText(String.valueOf(abonnement.getUserId()));
            }
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSave() {
        if (!validate())
            return;

        try {
            Long userId = Long.parseLong(inputUserId.getText().trim());
            Abonnement.TypeAbonnement type = inputType.getValue();
            LocalDate debut = inputDateDebut.getValue();
            LocalDate fin = inputDateFin.getValue();
            BigDecimal prix = new BigDecimal(inputPrix.getText().trim());
            Abonnement.StatutAbonnement statut = inputStatut.getValue();
            boolean auto = inputAutoRenew.isSelected();

            if (isEditMode) {
                selectedAbonnement.setUserId(userId);
                selectedAbonnement.setType(type);
                selectedAbonnement.setDateDebut(debut);
                selectedAbonnement.setDateFin(fin);
                selectedAbonnement.setPrix(prix);
                selectedAbonnement.setStatut(statut);
                selectedAbonnement.setAutoRenew(auto);
                controller.update(selectedAbonnement);
            } else {
                Abonnement a = new Abonnement(userId, type, debut, prix, auto);
                if (fin != null)
                    a.setDateFin(fin);
                if (statut != null)
                    a.setStatut(statut);
                controller.create(a);
            }

            if (onSaveCallback != null)
                onSaveCallback.run();
            close();
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private boolean validate() {
        if (inputUserId.getText().isBlank()) {
            showError("L'ID Utilisateur est requis");
            return false;
        }
        if (inputPrix.getText().isBlank()) {
            showError("Le prix est requis");
            return false;
        }
        try {
            new BigDecimal(inputPrix.getText().trim());
        } catch (Exception e) {
            showError("Prix invalide");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        if (validationMessage != null) {
            validationMessage.setText(msg);
            validationMessage.setVisible(true);
            validationMessage.setManaged(true);
        }
    }

    private void close() {
        ((Stage) inputUserId.getScene().getWindow()).close();
    }
}
