package com.gestion.ui.utilisateur;

import com.gestion.entities.Participation;
import com.gestion.interfaces.ParticipationService;
import com.gestion.services.ParticipationServiceImpl;
import com.gestion.ui.participation.ParticipationFormController;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Vue simplifiée "Mes participations" pour l'espace utilisateur.
 * Affiche les participations filtrées par userId courant.
 */
public class MesParticipationsController {

    @FXML private TableView<Participation> table;
    @FXML private TableColumn<Participation, Long> colId;
    @FXML private TableColumn<Participation, Long> colEventId;
    @FXML private TableColumn<Participation, String> colDateInscription;
    @FXML private TableColumn<Participation, String> colStatut;
    @FXML private TableColumn<Participation, Integer> colNbAdultes;
    @FXML private TableColumn<Participation, Integer> colNbEnfants;
    @FXML private TableColumn<Participation, Integer> colNbChiens;
    @FXML private TableColumn<Participation, String> colTypeAbonnement;
    @FXML private TableColumn<Participation, String> colMontant;
    @FXML private Label statusLabel;

    private final ParticipationService participationService = new ParticipationServiceImpl();
    // TODO: à remplacer par l'utilisateur connecté
    private Long currentUserId = 1L;

    @FXML
    public void initialize() {
        setupColumns();
        onActualiser();
    }

    private void setupColumns() {
        if (table == null) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (colId != null) {
            colId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()).asObject());
        }
        if (colEventId != null) {
            colEventId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getEvenementId()).asObject());
        }
        if (colDateInscription != null) {
            colDateInscription.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getDateInscription() != null
                            ? cd.getValue().getDateInscription().format(dtf)
                            : ""));
        }
        if (colStatut != null) {
            colStatut.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getStatut() != null ? cd.getValue().getStatut().getLabel() : ""));
        }
        if (colNbAdultes != null) {
            colNbAdultes.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getNbAdultes()).asObject());
        }
        if (colNbEnfants != null) {
            colNbEnfants.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getNbEnfants()).asObject());
        }
        if (colNbChiens != null) {
            colNbChiens.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getNbChiens()).asObject());
        }
        if (colTypeAbonnement != null) {
            colTypeAbonnement.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getTypeAbonnementChoisi() != null ? cd.getValue().getTypeAbonnementChoisi() : ""));
        }
        if (colMontant != null) {
            colMontant.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getMontantCalcule() != null
                            ? cd.getValue().getMontantCalcule().toPlainString()
                            : ""));
        }
    }

    @FXML
    public void onActualiser() {
        if (table == null) return;
        Platform.runLater(() -> {
            try {
                var participations = participationService.findByUserId(currentUserId);
                table.getItems().setAll(participations);
                if (statusLabel != null) {
                    statusLabel.setText(participations.isEmpty()
                            ? "Aucune participation trouvée"
                            : participations.size() + " participation(s) chargée(s)");
                }
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("Erreur chargement : " + e.getMessage());
                }
            }
        });
    }

    /**
     * Ouverture du formulaire dédié pour permettre à l'utilisateur
     * de créer lui‑même une participation.
     */
    @FXML
    public void onCreerParticipation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation/participation-form.fxml"));
            Parent root = loader.load();

            ParticipationFormController formController = loader.getController();
            formController.setAdminMode(false);
            formController.setCurrentUserId(currentUserId);
            formController.setOnSaved(this::onActualiser);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Créer une participation");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Impossible d'ouvrir le formulaire de participation");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }
}