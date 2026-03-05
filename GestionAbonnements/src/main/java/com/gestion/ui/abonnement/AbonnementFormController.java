package com.gestion.ui.abonnement;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AbonnementFormController {

    @FXML
    private Label mainTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<String> comboRestriction;
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
    private Label labelEvent;
    @FXML
    private ComboBox<com.gestion.entities.Evenement> comboEvent;
    @FXML
    private Label validationMessage;
    @FXML
    private VBox eventSelectionContainer;
    @FXML
    private Label summaryType;
    @FXML
    private Button btnSave;
    @FXML
    private VBox validationContainer;
    @FXML
    private Label statusMessage;
    @FXML
    private ToggleButton iaToggleInForm;
    @FXML
    private Label iaInsightLabel;

    private Abonnement selectedAbonnement;
    private boolean isEditMode = false;
    private boolean isReadOnly = false;
    private Runnable onSaveCallback;
    private final AbonnementController controller = new AbonnementController();

    @FXML
    public void initialize() {
        inputType.getItems().setAll(Abonnement.TypeAbonnement.values());
        inputStatut.getItems().setAll(Abonnement.StatutAbonnement.values());

        // Load event types for restrictions
        comboRestriction.getItems().setAll("", "SOIREE", "RANDONNEE", "CAMPING", "TREK", "AUTRE");

        // Listen for type changes to show/hide event selection and update summary
        inputType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPass = newVal == Abonnement.TypeAbonnement.EVENEMENT_PASS;
            if (eventSelectionContainer != null) {
                eventSelectionContainer.setVisible(isPass);
                eventSelectionContainer.setManaged(isPass);
            }

            updateSummary(newVal);

            if (isPass && comboEvent.getItems().isEmpty()) {
                loadEvents();
            }
        });

        // Defaults
        inputDateDebut.setValue(LocalDate.now());
        inputType.getSelectionModel().select(Abonnement.TypeAbonnement.MENSUEL);
        updateSummary(Abonnement.TypeAbonnement.MENSUEL);
        inputStatut.getSelectionModel().select(Abonnement.StatutAbonnement.ACTIF);
        inputAutoRenew.setSelected(true);
    }

    private void loadEvents() {
        try {
            com.gestion.services.EvenementService evService = new com.gestion.services.EvenementService();
            comboEvent.getItems().setAll(evService.getAll());
            // Custom cell factory for display
            comboEvent.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(com.gestion.entities.Evenement item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });
            comboEvent.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(com.gestion.entities.Evenement item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            showError("Erreur de chargement des événements: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAbonnement(Abonnement abonnement) {
        this.selectedAbonnement = abonnement;
        this.isEditMode = (abonnement != null && abonnement.getId() != null);

        if (isEditMode) {
            mainTitle.setText("Modifier l'Expédition");

            inputNom.setText(abonnement.getNom());
            comboRestriction.setValue(abonnement.getRestrictionType());

            inputType.setValue(abonnement.getType());
            inputDateDebut.setValue(abonnement.getDateDebut());
            inputDateFin.setValue(abonnement.getDateFin());
            inputPrix.setText(abonnement.getPrix() != null ? abonnement.getPrix().toString() : "0.00");
            inputStatut.setValue(abonnement.getStatut());
            inputAutoRenew.setSelected(abonnement.isAutoRenew());

            if (abonnement.getType() == Abonnement.TypeAbonnement.EVENEMENT_PASS
                    && abonnement.getEvenementId() != null) {
                loadEvents();
                comboEvent.getItems().stream()
                        .filter(ev -> ev.getIdEvent() == abonnement.getEvenementId().intValue())
                        .findFirst()
                        .ifPresent(ev -> comboEvent.setValue(ev));
            }
        } else if (abonnement != null) {
            // Pre-filling for NEW subscription
            if (abonnement.getType() != null) {
                inputType.setValue(abonnement.getType());
                updateSummary(abonnement.getType());
            }
            if (abonnement.getPrix() != null) {
                inputPrix.setText(abonnement.getPrix().toString());
            }
            if (abonnement.getEvenementId() != null) {
                loadEvents();
                comboEvent.getItems().stream()
                        .filter(ev -> ev.getIdEvent() == (long) abonnement.getEvenementId())
                        .findFirst()
                        .ifPresent(ev -> comboEvent.setValue(ev));
            }
        }
    }

    private void updateSummary(Abonnement.TypeAbonnement type) {
        if (type == null || summaryType == null)
            return;

        switch (type) {
            case EVENEMENT_PASS:
                summaryType.setText("Pass Expédition Unique");
                summaryType
                        .setStyle("-fx-text-fill: -fx-nature-accent-orange; -fx-font-size: 18; -fx-font-weight: bold;");
                break;
            case MENSUEL:
                summaryType.setText("Accès Mensuel Global");
                summaryType.setStyle("-fx-text-fill: -fx-nature-success; -fx-font-size: 18; -fx-font-weight: bold;");
                break;
            case ANNUEL:
                summaryType.setText("Engagement Annuel");
                summaryType.setStyle("-fx-text-fill: -fx-nature-success; -fx-font-size: 18; -fx-font-weight: bold;");
                break;
            case PREMIUM:
                summaryType.setText("Privilège PREMIUM");
                summaryType.setStyle("-fx-text-fill: #c084fc; -fx-font-size: 18; -fx-font-weight: bold;");
                break;
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSave() {
        if (validationContainer != null)
            validationContainer.setVisible(false);
        try {
            // Default to Admin User (ID 1) if creating a template/admin subscription
            Long userId = 1L;

            if (selectedAbonnement != null && selectedAbonnement.getUserId() != null) {
                userId = selectedAbonnement.getUserId();
            }

            if (inputPrix.getText().isBlank()) {
                showError("Le prix est requis");
                return;
            }
            BigDecimal prix;
            try {
                prix = new BigDecimal(inputPrix.getText().trim());
            } catch (Exception e) {
                showError("Prix invalide");
                return;
            }
            Abonnement.TypeAbonnement type = inputType.getValue();
            LocalDate debut = inputDateDebut.getValue();
            LocalDate fin = inputDateFin.getValue();
            Abonnement.StatutAbonnement statut = inputStatut.getValue();
            boolean auto = inputAutoRenew.isSelected();
            String restriction = comboRestriction.getValue();

            Long evId = null;
            if (type == Abonnement.TypeAbonnement.EVENEMENT_PASS) {
                com.gestion.entities.Evenement ev = comboEvent.getValue();
                if (ev == null) {
                    showError("Veuillez sélectionner un événement pour ce pass");
                    return;
                }
                evId = (long) ev.getIdEvent();
            }

            // Payment logic removed as per Admin simplification request

            if (isEditMode) {
                selectedAbonnement.setUserId(userId);
                selectedAbonnement.setNom(inputNom.getText());
                selectedAbonnement.setRestrictionType(restriction);
                selectedAbonnement.setEvenementId(evId);
                selectedAbonnement.setType(type);
                selectedAbonnement.setDateDebut(debut);
                selectedAbonnement.setDateFin(fin);
                selectedAbonnement.setPrix(prix);
                selectedAbonnement.setStatut(statut);
                selectedAbonnement.setAutoRenew(auto);
                controller.update(selectedAbonnement);
            } else {
                Abonnement a = new Abonnement(userId, evId, type, debut, prix, auto);
                a.setNom(inputNom.getText());
                a.setRestrictionType(restriction);
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

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        if (readOnly) {
            inputNom.setDisable(true);
            comboRestriction.setDisable(true);
            inputType.setDisable(true);
            inputDateDebut.setDisable(true);
            inputDateFin.setDisable(true);
            inputPrix.setDisable(true);
            inputStatut.setDisable(true);
            inputAutoRenew.setDisable(true);
            if (comboEvent != null)
                comboEvent.setDisable(true);
            if (btnSave != null)
                btnSave.setText("VALIDER & FACTURER");
            if (mainTitle != null)
                mainTitle.setText("Validation de l'Accès");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void showError(String message) {
        if (validationContainer != null) {
            validationContainer.setVisible(true);
            validationContainer.setManaged(true);
            validationMessage.setText(message);
        }
    }

    private void hideError() {
        if (validationContainer != null) {
            validationContainer.setVisible(false);
            validationContainer.setManaged(false);
        }
    }

    private void close() {
        if (mainTitle != null && mainTitle.getScene() != null) {
            mainTitle.getScene().getWindow().hide();
        }
    }
}
