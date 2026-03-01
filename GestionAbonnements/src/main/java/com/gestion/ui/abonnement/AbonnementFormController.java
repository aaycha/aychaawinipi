package com.gestion.ui.abonnement;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class AbonnementFormController {

    @FXML
    private Label mainTitle;
    @FXML
    private TextField inputUserName;
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

    private Abonnement selectedAbonnement;
    private boolean isEditMode = false;
    private boolean isReadOnly = false;
    private Runnable onSaveCallback;
    private final AbonnementController controller = new AbonnementController();

    @FXML
    public void initialize() {
        inputType.getItems().setAll(Abonnement.TypeAbonnement.values());
        inputStatut.getItems().setAll(Abonnement.StatutAbonnement.values());

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
            comboEvent.getItems().setAll(evService.findAll());
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAbonnement(Abonnement abonnement) {
        this.selectedAbonnement = abonnement;
        this.isEditMode = (abonnement != null && abonnement.getId() != null);

        if (isEditMode) {
            mainTitle.setText("Modifier l'Expédition");
            inputUserId.setText(String.valueOf(abonnement.getUserId()));
            resolveUserName(abonnement.getUserId().intValue());
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
            if (abonnement.getUserId() != null) {
                inputUserId.setText(String.valueOf(abonnement.getUserId()));
                resolveUserName(abonnement.getUserId().intValue());
            }
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

    private void resolveUserName(int userId) {
        try {
            com.gestion.entities.User user = new com.gestion.services.UserService().getUserById(userId);
            if (user != null) {
                inputUserName.setText(user.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        hideError();
        try {
            Long userId = resolveUserId();
            if (userId == null)
                return;

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

            Long evId = null;
            if (type == Abonnement.TypeAbonnement.EVENEMENT_PASS) {
                com.gestion.entities.Evenement ev = comboEvent.getValue();
                if (ev == null) {
                    showError("Veuillez sélectionner un événement pour ce pass");
                    return;
                }
                evId = (long) ev.getIdEvent();
            }

            // --- Open Stripe Checkout for real payment ---
            com.gestion.services.StripePaymentService stripeService = com.gestion.services.StripePaymentService
                    .getInstance();
            boolean opened = stripeService.openCheckoutInBrowser(
                    "Abonnement " + type.getLabel() + " - " + prix + " EUR");
            if (!opened) {
                showError("Impossible d'ouvrir la page de paiement Stripe. Veuillez réessayer.");
                return;
            }

            // Show confirmation that payment page was opened
            Alert paymentAlert = new Alert(Alert.AlertType.INFORMATION);
            paymentAlert.setTitle("Paiement Stripe");
            paymentAlert.setHeaderText("Page de paiement ouverte !");
            paymentAlert.setContentText(
                    "La page de paiement Stripe a été ouverte dans votre navigateur.\n\n" +
                            "Montant : " + prix + " €\n" +
                            "Type : " + type.getLabel() + "\n\n" +
                            "Cliquez OK une fois le paiement effectué pour finaliser votre abonnement.");
            paymentAlert.showAndWait();

            if (isEditMode) {
                selectedAbonnement.setUserId(userId);
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
                if (fin != null)
                    a.setDateFin(fin);
                if (statut != null)
                    a.setStatut(statut);
                controller.create(a);
            }

            if (onSaveCallback != null)
                onSaveCallback.run();

            showFacture(isEditMode ? selectedAbonnement
                    : controller.getAll().stream().max(Comparator.comparing(Abonnement::getId)).orElse(null));
            close();
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }

    private void showFacture(Abonnement a) {
        if (a == null)
            return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/abonnement/facture.fxml"));
            Parent root = loader.load();
            FactureController ctrl = loader.getController();
            ctrl.setData(a);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Facture LAMMA");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        if (readOnly) {
            inputUserName.setDisable(true);
            inputUserId.setDisable(true);
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

    private Long resolveUserId() {
        String idText = inputUserId.getText().trim();
        String nameText = inputUserName.getText().trim();

        com.gestion.services.UserService userService = new com.gestion.services.UserService();
        try {
            if (!idText.isEmpty()) {
                int id = Integer.parseInt(idText);
                com.gestion.entities.User u = userService.getUserById(id);
                if (u != null)
                    return (long) id;
                showError("Utilisateur avec l'ID " + id + " non trouvé.");
                return null;
            } else if (!nameText.isEmpty()) {
                com.gestion.entities.User u = userService.getUserByName(nameText);
                if (u != null) {
                    inputUserId.setText(String.valueOf(u.getId()));
                    return (long) u.getId();
                }
                showError("Utilisateur avec le nom '" + nameText + "' non trouvé.");
                return null;
            }
        } catch (Exception e) {
            showError("Erreur lors de la recherche de l'utilisateur.");
        }
        showError("Veuillez saisir un ID ou un Nom.");
        return null;
    }

    private void showError(String msg) {
        if (validationMessage != null) {
            validationMessage.setText(msg);
            validationMessage.setVisible(true);
            validationMessage.setManaged(true);
        }
    }

    private void hideError() {
        if (validationMessage != null) {
            validationMessage.setVisible(false);
            validationMessage.setManaged(false);
        }
    }

    private void close() {
        ((Stage) inputUserId.getScene().getWindow()).close();
    }
}
