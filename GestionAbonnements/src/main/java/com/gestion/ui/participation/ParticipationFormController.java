package com.gestion.ui.participation;

import com.gestion.controllers.ParticipationController;
import com.gestion.entities.Participation;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulaire dédié pour la création / modification d'une participation,
 * avec saisie de nbAdultes / nbEnfants / nbChiens et pré‑visualisation du tarif.
 */
public class ParticipationFormController {

    @FXML private Label formTitle;

    @FXML private TextField inputUserId;
    @FXML private TextField inputEvenementId;
    @FXML private ComboBox<Participation.TypeParticipation> inputType;
    @FXML private ComboBox<Participation.ContexteSocial> inputContexte;
    @FXML private CheckBox inputHebergement;
    @FXML private TextField inputHebergementNuits;

    @FXML private TextField inputNbAdultes;
    @FXML private TextField inputNbEnfants;
    @FXML private TextField inputNbChiens;

    @FXML private TextArea inputCommentaire;
    @FXML private TextArea inputBesoinsSpeciaux;

    @FXML private Label labelTotalParticipants;
    @FXML private Label labelTypeAbonnement;
    @FXML private Label labelMontant;

    @FXML private Label errorUserId;
    @FXML private Label errorEvenementId;
    @FXML private Label errorGroupe;
    @FXML private Label errorGlobal;

    @FXML private Button btnEnregistrer;

    private ParticipationController participationController;
    private Participation participation;
    private boolean editMode = false;
    private boolean adminMode = true;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        if (inputType != null) {
            inputType.getItems().addAll(Participation.TypeParticipation.values());
        }
        if (inputContexte != null) {
            inputContexte.getItems().addAll(Participation.ContexteSocial.values());
        }

        ChangeListener<String> recomputeListener = (obs, oldVal, newVal) -> updatePreviewFromFields();
        if (inputNbAdultes != null) inputNbAdultes.textProperty().addListener(recomputeListener);
        if (inputNbEnfants != null) inputNbEnfants.textProperty().addListener(recomputeListener);
        if (inputNbChiens != null) inputNbChiens.textProperty().addListener(recomputeListener);

        // Valeurs par défaut
        if (inputNbAdultes != null) inputNbAdultes.setText("1");
        if (inputNbEnfants != null) inputNbEnfants.setText("0");
        if (inputNbChiens != null) inputNbChiens.setText("0");
        if (inputHebergementNuits != null) inputHebergementNuits.setText("0");

        updatePreviewFromFields();
    }

    public void setParticipationController(ParticipationController controller) {
        this.participationController = controller;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
        this.editMode = (participation != null);

        if (formTitle != null) {
            formTitle.setText(editMode ? "Modifier la participation" : "Nouvelle participation");
        }

        if (participation != null) {
            if (inputUserId != null && participation.getUserId() != null) {
                inputUserId.setText(String.valueOf(participation.getUserId()));
            }
            if (inputEvenementId != null && participation.getEvenementId() != null) {
                inputEvenementId.setText(String.valueOf(participation.getEvenementId()));
            }
            if (inputType != null) inputType.setValue(participation.getType());
            if (inputContexte != null) inputContexte.setValue(participation.getContexteSocial());
            if (inputHebergement != null) inputHebergement.setSelected(participation.getHebergementNuits() > 0);
            if (inputHebergementNuits != null) {
                inputHebergementNuits.setText(String.valueOf(participation.getHebergementNuits()));
            }

            if (inputNbAdultes != null) inputNbAdultes.setText(String.valueOf(participation.getNbAdultes()));
            if (inputNbEnfants != null) inputNbEnfants.setText(String.valueOf(participation.getNbEnfants()));
            if (inputNbChiens != null) inputNbChiens.setText(String.valueOf(participation.getNbChiens()));

            if (inputCommentaire != null) inputCommentaire.setText(participation.getCommentaire());
            if (inputBesoinsSpeciaux != null) inputBesoinsSpeciaux.setText(participation.getBesoinsSpeciaux());

            if (labelTypeAbonnement != null && participation.getTypeAbonnementChoisi() != null) {
                labelTypeAbonnement.setText(participation.getTypeAbonnementChoisi());
            }
            if (labelMontant != null && participation.getMontantCalcule() != null) {
                labelMontant.setText(participation.getMontantCalcule().toPlainString());
            }
            if (labelTotalParticipants != null) {
                labelTotalParticipants.setText("Total participants : " + participation.getTotalParticipants());
            }
        }
        clearErrors();
        updatePreviewFromFields();
    }

    public void setCurrentUserId(Long userId) {
        if (userId != null && inputUserId != null && !adminMode) {
            inputUserId.setText(String.valueOf(userId));
            inputUserId.setDisable(true);
        }
    }

    @FXML
    void onEnregistrer() {
        clearErrors();
        List<String> errors = new ArrayList<>();

        Long userId = parseLong(inputUserId != null ? inputUserId.getText() : null);
        Long evenementId = parseLong(inputEvenementId != null ? inputEvenementId.getText() : null);
        Participation.TypeParticipation type = inputType != null ? inputType.getValue() : null;
        Participation.ContexteSocial contexte = inputContexte != null ? inputContexte.getValue() : null;

        if (userId == null || userId <= 0) {
            errors.add("User ID est obligatoire et doit être un nombre positif.");
            showError(errorUserId, "Veuillez saisir un identifiant utilisateur valide (ex: 1, 2, 3...).");
        }
        if (evenementId == null || evenementId <= 0) {
            errors.add("Événement ID est obligatoire et doit être un nombre positif.");
            showError(errorEvenementId, "Veuillez saisir un identifiant d'événement valide.");
        }
        if (type == null) {
            errors.add("Le type de participation est obligatoire.");
        }
        if (contexte == null) {
            errors.add("Le contexte social est obligatoire.");
        }

        int nbAdultes = parseInt(inputNbAdultes != null ? inputNbAdultes.getText() : null, 1);
        int nbEnfants = parseInt(inputNbEnfants != null ? inputNbEnfants.getText() : null, 0);
        int nbChiens = parseInt(inputNbChiens != null ? inputNbChiens.getText() : null, 0);

        if (nbAdultes < 1) {
            errors.add("Au moins 1 adulte est requis.");
        }
        if (nbEnfants < 0) {
            errors.add("Le nombre d'enfants ne peut pas être négatif.");
        }
        if (nbChiens < 0) {
            errors.add("Le nombre de chiens ne peut pas être négatif.");
        }

        int nuits = 0;
        if (inputHebergement != null && inputHebergement.isSelected()) {
            nuits = parseInt(inputHebergementNuits != null ? inputHebergementNuits.getText() : null, 1);
            if (nuits <= 0) {
                errors.add("Le nombre de nuits doit être supérieur à 0 si hébergement est sélectionné.");
            }
        }

        if (!errors.isEmpty()) {
            showError(errorGroupe, String.join("\n", errors));
            showError(errorGlobal, "Veuillez corriger les champs indiqués avant d'enregistrer.");
            return;
        }

        try {
            Participation target;
            if (editMode && participation != null) {
                target = participation;
            } else {
                target = new Participation(userId, evenementId, type, contexte);
                target.setStatut(Participation.StatutParticipation.EN_ATTENTE);
                target.setDateInscription(LocalDateTime.now());
            }

            target.setUserId(userId);
            target.setEvenementId(evenementId);
            target.setType(type);
            target.setContexteSocial(contexte);
            target.setHebergementNuits(nuits);

            target.setNbAdultes(nbAdultes);
            target.setNbEnfants(nbEnfants);
            target.setNbChiens(nbChiens);
            target.setTotalParticipants(nbAdultes + nbEnfants);

            if (inputCommentaire != null) {
                target.setCommentaire(inputCommentaire.getText());
            }
            if (inputBesoinsSpeciaux != null) {
                target.setBesoinsSpeciaux(inputBesoinsSpeciaux.getText());
            }

            if (participationController == null) {
                participationController = new ParticipationController();
            }

            if (editMode) {
                participationController.update(target);
                showInfo("Participation modifiée avec succès.");
            } else {
                participationController.create(target);
                showInfo("Participation créée avec succès.");
            }

            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (IllegalArgumentException ex) {
            showError(errorGlobal, ex.getMessage());
        } catch (Exception ex) {
            showError(errorGlobal, "Erreur lors de l'enregistrement : " + ex.getMessage());
        }
    }

    @FXML
    void onAnnuler() {
        closeWindow();
    }

    // ──────────────────── Helpers ─────────────────────

    private void updatePreviewFromFields() {
        int nbAdultes = parseInt(inputNbAdultes != null ? inputNbAdultes.getText() : null, 1);
        int nbEnfants = parseInt(inputNbEnfants != null ? inputNbEnfants.getText() : null, 0);
        int nbChiens = parseInt(inputNbChiens != null ? inputNbChiens.getText() : null, 0);

        int total = Math.max(1, nbAdultes) + Math.max(0, nbEnfants);
        if (labelTotalParticipants != null) {
            labelTotalParticipants.setText("Total participants : " + total);
        }

        // Logique de tarification alignée sur le service (valeurs par défaut)
        BigDecimal tarifAdulte = new BigDecimal("25.00");
        BigDecimal tarifEnfant = new BigDecimal("15.00");
        BigDecimal tarifChien = new BigDecimal("8.00");
        BigDecimal forfaitFamille = new BigDecimal("60.00");

        BigDecimal montant;
        String typeAbonnement;

        if (nbAdultes >= 1 && nbEnfants >= 1) {
            montant = forfaitFamille;
            typeAbonnement = "pass_famille";
        } else {
            montant = tarifAdulte.multiply(BigDecimal.valueOf(Math.max(1, nbAdultes)))
                    .add(tarifEnfant.multiply(BigDecimal.valueOf(Math.max(0, nbEnfants))))
                    .add(tarifChien.multiply(BigDecimal.valueOf(Math.max(0, nbChiens))));
            typeAbonnement = "journee";
        }

        montant = montant.setScale(2, RoundingMode.HALF_UP);

        if (labelTypeAbonnement != null) {
            labelTypeAbonnement.setText(typeAbonnement);
        }
        if (labelMontant != null) {
            labelMontant.setText(montant.toPlainString());
        }
    }

    private void clearErrors() {
        hideError(errorUserId);
        hideError(errorEvenementId);
        hideError(errorGroupe);
        hideError(errorGlobal);
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private Long parseLong(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseInt(String text, int defaultValue) {
        if (text == null || text.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void closeWindow() {
        if (formTitle != null && formTitle.getScene() != null) {
            Stage stage = (Stage) formTitle.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}

