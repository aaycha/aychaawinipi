package com.gestion.controllers;

import com.gestion.entities.equipementwael;
import com.gestion.entities.EquipementAttribut;
import com.gestion.services.EquipementAttributService;
import com.gestion.services.equipemementservicewael;
import com.gestion.services.MailService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EquipementFormController implements Initializable {

    @FXML
    private Label formTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<String> inputCategorie;
    @FXML
    private ComboBox<String> inputType;
    @FXML
    private VBox attributsSection;
    @FXML
    private VBox attributsMatelasSection;
    @FXML
    private VBox attributsLunettesSection;
    @FXML
    private ComboBox<String> inputPlaces;
    @FXML
    private ComboBox<String> inputTaille;
    @FXML
    private TextField inputMatelasEpaisseur;
    @FXML
    private TextField inputMatelasDimension;
    @FXML
    private ComboBox<String> inputMatelasMatiere;
    @FXML
    private ComboBox<String> inputMatelasGonflable;
    @FXML
    private TextField inputLunettesUV;
    @FXML
    private ComboBox<String> inputLunettesCouleur;
    @FXML
    private ComboBox<String> inputLunettesPolarisees;
    @FXML
    private TextField inputVille;
    @FXML
    private TextField inputPrix;
    @FXML
    private TextArea inputDescription;
    @FXML
    private VBox errorContainer;
    @FXML
    private Label globalErrorMessage;
    @FXML
    private TextField inputMail;

    private equipementwael equipement;
    private equipemementservicewael service;
    private EquipementAttributService attributService;
    private Runnable onSaved;
    private MailService mailService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new equipemementservicewael();
        attributService = new EquipementAttributService();
        mailService = new MailService();
        inputCategorie.getItems().addAll("Tente", "Matelas", "Lunettes", "Sac de couchage", "Lampe", "Réchaud",
                "Chaise", "Glacière", "Autre");
        inputType.getItems().addAll("VENTE", "LOCATION");
        inputPlaces.getItems().addAll("2", "4", "6", "8");
        inputTaille.getItems().addAll("Petite", "Moyenne", "Grande");
        inputMatelasMatiere.getItems().addAll("Mousse", "Latex", "Gonflable", "Autre");
        inputMatelasGonflable.getItems().addAll("Oui", "Non");
        inputLunettesCouleur.getItems().addAll("Noir", "Bleu", "Vert", "Gris", "Brun", "Autre");
        inputLunettesPolarisees.getItems().addAll("Oui", "Non");
        inputCategorie.valueProperty().addListener((o, ov, nv) -> {
            boolean tente = "Tente".equalsIgnoreCase(nv);
            boolean matelas = "Matelas".equalsIgnoreCase(nv);
            boolean lunettes = "Lunettes".equalsIgnoreCase(nv);
            if (attributsSection != null) {
                attributsSection.setVisible(tente);
                attributsSection.setManaged(tente);
            }
            if (attributsMatelasSection != null) {
                attributsMatelasSection.setVisible(matelas);
                attributsMatelasSection.setManaged(matelas);
            }
            if (attributsLunettesSection != null) {
                attributsLunettesSection.setVisible(lunettes);
                attributsLunettesSection.setManaged(lunettes);
            }
        });
        if (attributsSection != null)
            attributsSection.setVisible(false);
        if (attributsMatelasSection != null)
            attributsMatelasSection.setVisible(false);
        if (attributsLunettesSection != null)
            attributsLunettesSection.setVisible(false);
    }

    public void setEquipement(equipementwael e) {
        this.equipement = e;
        if (e != null) {
            formTitle.setText("Modifier équipement");
            inputNom.setText(e.getNom());
            inputCategorie.setValue(e.getCategorie());
            inputType.setValue(e.getType());
            inputVille.setText(e.getVille());
            inputPrix.setText(e.getPrix() != null ? e.getPrix().toString() : "");
            inputDescription.setText(e.getDescription());

            if (e.getCaracteristiques() != null && e.getCaracteristiques().contains("=")) {
                for (String part : e.getCaracteristiques().split(";")) {
                    if (part.startsWith("places="))
                        inputPlaces.setValue(part.substring(7).trim());
                    if (part.startsWith("taille="))
                        inputTaille.setValue(part.substring(7).trim());
                }
            }
            if ("Tente".equalsIgnoreCase(e.getCategorie()) && attributsSection != null) {
                attributsSection.setVisible(true);
                attributsSection.setManaged(true);
            }
            if ("Matelas".equalsIgnoreCase(e.getCategorie()) && attributsMatelasSection != null) {
                attributsMatelasSection.setVisible(true);
                attributsMatelasSection.setManaged(true);
                List<EquipementAttribut> attrs = attributService.getByEquipementId(e.getId());
                for (EquipementAttribut a : attrs) {
                    if ("epaisseur".equalsIgnoreCase(a.getNomAttribut()))
                        inputMatelasEpaisseur.setText(a.getValeur());
                    else if ("dimension".equalsIgnoreCase(a.getNomAttribut()))
                        inputMatelasDimension.setText(a.getValeur());
                    else if ("matiere".equalsIgnoreCase(a.getNomAttribut()))
                        inputMatelasMatiere.setValue(a.getValeur());
                    else if ("gonflable".equalsIgnoreCase(a.getNomAttribut()))
                        inputMatelasGonflable.setValue(a.getValeur());
                }
            }
            if ("Lunettes".equalsIgnoreCase(e.getCategorie()) && attributsLunettesSection != null) {
                attributsLunettesSection.setVisible(true);
                attributsLunettesSection.setManaged(true);
                List<EquipementAttribut> attrs = attributService.getByEquipementId(e.getId());
                for (EquipementAttribut a : attrs) {
                    if ("protection_uv".equalsIgnoreCase(a.getNomAttribut()))
                        inputLunettesUV.setText(a.getValeur());
                    else if ("couleur_verre".equalsIgnoreCase(a.getNomAttribut()))
                        inputLunettesCouleur.setValue(a.getValeur());
                    else if ("polarisees".equalsIgnoreCase(a.getNomAttribut()))
                        inputLunettesPolarisees.setValue(a.getValeur());
                }
            }
            if (inputMail != null) {
                List<EquipementAttribut> attrs = attributService.getByEquipementId(e.getId());
                for (EquipementAttribut a : attrs) {
                    if ("mail".equalsIgnoreCase(a.getNomAttribut())) {
                        inputMail.setText(a.getValeur() != null ? a.getValeur() : "");
                        break;
                    }
                }
            }
        } else {
            formTitle.setText("Nouvel équipement");
            inputCategorie.setValue(null);
        }
    }

    public void setOnSaved(Runnable r) {
        this.onSaved = r;
    }

    @FXML
    private void onEnregistrer() {
        hideError();
        if (!valider())
            return;

        try {
            String nom = inputNom.getText().trim();
            String desc = inputDescription.getText() != null ? inputDescription.getText().trim() : "";
            String cat = inputCategorie.getValue() != null ? inputCategorie.getValue() : "";
            String type = inputType.getValue();
            String ville = inputVille.getText() != null ? inputVille.getText().trim() : "";
            BigDecimal prix = new BigDecimal(inputPrix.getText().trim());

            if (equipement == null) {
                equipementwael nouveau = new equipementwael(nom, desc, cat, type, prix, ville);
                String car = buildCaracteristiques();
                if (car != null)
                    nouveau.setCaracteristiques(car);
                Long id = service.ajouterRetourId(nouveau);
                if (id != null) {
                    nouveau.setId(id);
                    sauvegarderAttributs(nouveau.getId());
                } else {
                    service.ajouter(nouveau);
                }
                // Email de notification (même si l'ID n'a pas pu être récupéré)
                envoyerEmailNotificationSiMail(nouveau);
                new Alert(Alert.AlertType.INFORMATION, "Équipement ajouté !").showAndWait();
            } else {
                equipement.setNom(nom);
                equipement.setDescription(desc);
                equipement.setCategorie(cat);
                String car = buildCaracteristiques();
                if (car != null)
                    equipement.setCaracteristiques(car);
                equipement.setType(type);
                equipement.setVille(ville);
                equipement.setPrix(prix);
                service.modifier(equipement);
                attributService.supprimerParEquipement(equipement.getId());
                sauvegarderAttributs(equipement.getId());
                new Alert(Alert.AlertType.INFORMATION, "Équipement modifié !").showAndWait();
            }
            if (onSaved != null)
                onSaved.run();
            fermer();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        fermer();
    }

    private boolean valider() {
        if (inputNom.getText() == null || inputNom.getText().trim().isEmpty()) {
            showError("Le nom est obligatoire.");
            return false;
        }
        if (inputType.getValue() == null || inputType.getValue().trim().isEmpty()) {
            showError("Le type (Vente/Location) est obligatoire.");
            return false;
        }
        if (inputPrix.getText() == null || inputPrix.getText().trim().isEmpty()) {
            showError("Le prix est obligatoire.");
            return false;
        }
        if (inputDescription.getText() == null || inputDescription.getText().trim().isEmpty()) {
            showError("La description est obligatoire.");
            return false;
        }
        try {
            BigDecimal p = new BigDecimal(inputPrix.getText().trim());
            if (p.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Le prix doit être supérieur à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Le prix doit être un nombre valide (TND).");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
        globalErrorMessage.setText(msg);
    }

    private void hideError() {
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
    }

    private String buildCaracteristiques() {
        String cat = inputCategorie.getValue();
        if (cat == null || !cat.equalsIgnoreCase("Tente"))
            return null;
        String places = inputPlaces.getValue();
        String taille = inputTaille.getValue();
        if ((places == null || places.isEmpty()) && (taille == null || taille.isEmpty()))
            return null;
        return "places=" + (places != null ? places : "") + ";taille=" + (taille != null ? taille : "");
    }

    private void sauvegarderAttributs(long equipementId) {
        String cat = inputCategorie.getValue();
        if ("Matelas".equalsIgnoreCase(cat) && inputMatelasEpaisseur != null) {
            if (inputMatelasEpaisseur.getText() != null && !inputMatelasEpaisseur.getText().trim().isEmpty())
                attributService.ajouter(
                        new EquipementAttribut(equipementId, "epaisseur", inputMatelasEpaisseur.getText().trim()));
            if (inputMatelasDimension.getText() != null && !inputMatelasDimension.getText().trim().isEmpty())
                attributService.ajouter(
                        new EquipementAttribut(equipementId, "dimension", inputMatelasDimension.getText().trim()));
            if (inputMatelasMatiere.getValue() != null)
                attributService
                        .ajouter(new EquipementAttribut(equipementId, "matiere", inputMatelasMatiere.getValue()));
            if (inputMatelasGonflable.getValue() != null)
                attributService
                        .ajouter(new EquipementAttribut(equipementId, "gonflable", inputMatelasGonflable.getValue()));
        }
        if ("Lunettes".equalsIgnoreCase(cat) && inputLunettesUV != null) {
            if (inputLunettesUV.getText() != null && !inputLunettesUV.getText().trim().isEmpty())
                attributService.ajouter(
                        new EquipementAttribut(equipementId, "protection_uv", inputLunettesUV.getText().trim()));
            if (inputLunettesCouleur.getValue() != null)
                attributService.ajouter(
                        new EquipementAttribut(equipementId, "couleur_verre", inputLunettesCouleur.getValue()));
            if (inputLunettesPolarisees.getValue() != null)
                attributService.ajouter(
                        new EquipementAttribut(equipementId, "polarisees", inputLunettesPolarisees.getValue()));
        }
        if (inputMail != null && inputMail.getText() != null && !inputMail.getText().trim().isEmpty())
            attributService.ajouter(new EquipementAttribut(equipementId, "mail", inputMail.getText().trim()));
    }

    /**
     * Envoie un email de notification à l'adresse indiquée dans le champ, avec un
     * expéditeur fixe SendGrid.
     */
    private void envoyerEmailNotificationSiMail(equipementwael e) {
        if (inputMail == null || inputMail.getText() == null || inputMail.getText().trim().isEmpty())
            return;
        String to = inputMail.getText().trim();
        if (mailService == null || !mailService.isConfigured()) {
            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING,
                    "Email non envoyé : SendGrid non configuré (config/mail.properties avec sendgrid.api.key).")
                    .showAndWait());
            return;
        }
        // Expéditeur : adresse FIXE définie dans config/mail.properties (sendgrid.from)
        // et vérifiée dans SendGrid.
        String from = mailService.getFromEmail();
        if (from == null || from.isBlank()) {
            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING,
                    "Email non envoyé : expéditeur SendGrid non configuré.\n" +
                            "Définir sendgrid.from dans config/mail.properties avec l'email vérifié dans SendGrid (Single Sender Verification).")
                    .showAndWait());
            return;
        }
        System.out.println("[MAIL] FROM=" + from + " TO=" + to);
        String type = e.getType() != null ? e.getType() : "";
        String prix = e.getPrix() != null ? e.getPrix().toString() : "";
        String sujet = "Équipement ajouté avec succès – LAMMA";
        String corps = String.format(
                "L'équipement (%s) avec le prix %s TND a été ajouté avec succès.%n%nDétails :%n- Nom : %s%n- Catégorie : %s%n- Type : %s%n- Prix : %s TND%n- Ville : %s%n- Description : %s",
                type, prix,
                e.getNom(), e.getCategorie(), e.getType(), e.getPrix(), e.getVille(),
                e.getDescription() != null ? e.getDescription() : "-");
        boolean ok = mailService.sendText(from, to, sujet, corps);
        if (!ok) {
            String err = mailService.getLastError();
            javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING,
                    "L'email de notification n'a pas pu être envoyé.\n" +
                            (err != null && !err.isBlank() ? err : "Vérifiez la clé API.") +
                            "\n\nPour que ça marche : l'adresse du champ (ou sendgrid.from) doit être vérifiée dans SendGrid → Settings → Sender Authentication → Single Sender Verification.")
                    .showAndWait());
        }
    }

    private void fermer() {
        ((Stage) inputNom.getScene().getWindow()).close();
    }
}
