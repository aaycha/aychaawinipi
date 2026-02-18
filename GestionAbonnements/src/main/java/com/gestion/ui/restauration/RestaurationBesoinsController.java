package com.gestion.ui.restauration;

import com.gestion.controllers.RestaurationController;
import com.gestion.entities.ParticipantRestauration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class RestaurationBesoinsController {

    @FXML private TableView<ParticipantRestauration> besoinsTable;
    @FXML private TableColumn<ParticipantRestauration, Long> colBesoinId;
    @FXML private TableColumn<ParticipantRestauration, Long> colParticipantId;
    @FXML private TableColumn<ParticipantRestauration, Long> colEvenementId;
    @FXML private TableColumn<ParticipantRestauration, String> colBesoinLibelle;
    @FXML private TableColumn<ParticipantRestauration, String> colRestrictionLibelle;
    @FXML private TableColumn<ParticipantRestauration, String> colNiveauGravite;
    @FXML private TableColumn<ParticipantRestauration, Boolean> colAnnule;
    @FXML private TextField filterParticipantField;
    @FXML private TextField inputParticipantId;
    @FXML private TextField inputEvenementId;
    @FXML private TextField inputBesoinLibelle;
    @FXML private TextField inputRestrictionLibelle;
    @FXML private TextField inputNiveauGravite;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<ParticipantRestauration> besoinsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colBesoinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colParticipantId.setCellValueFactory(new PropertyValueFactory<>("participantId"));
        colEvenementId.setCellValueFactory(new PropertyValueFactory<>("evenementId"));
        colBesoinLibelle.setCellValueFactory(new PropertyValueFactory<>("besoinLibelle"));
        colRestrictionLibelle.setCellValueFactory(new PropertyValueFactory<>("restrictionLibelle"));
        colNiveauGravite.setCellValueFactory(new PropertyValueFactory<>("niveauGravite"));
        colAnnule.setCellValueFactory(cell -> new javafx.beans.property.SimpleBooleanProperty(cell.getValue().isAnnule()));
        besoinsTable.setItems(besoinsData);
        onActualiser();
    }

    @FXML
    void onNouveauBesoin() {
        inputParticipantId.clear();
        inputEvenementId.clear();
        inputBesoinLibelle.clear();
        inputRestrictionLibelle.clear();
        inputNiveauGravite.clear();
    }

    @FXML
    void onActualiser() {
        onFiltrer();
    }

    @FXML
    void onFiltrer() {
        Long partId = parseLong(filterParticipantField.getText());
        List<ParticipantRestauration> list;
        if (partId != null) {
            list = controller.getBesoinsByParticipantId(partId);
        } else {
            list = List.of();
        }
        besoinsData.clear();
        besoinsData.addAll(list);
    }

    @FXML
    void onAjouterBesoin() {
        Long partId = parseLong(inputParticipantId.getText());
        Long evtId = parseLong(inputEvenementId.getText());
        String besoin = inputBesoinLibelle.getText();
        String restriction = inputRestrictionLibelle.getText();
        String gravite = inputNiveauGravite.getText();
        if (partId == null || evtId == null) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Participant ID et Événement ID sont requis.");
            return;
        }
        ParticipantRestauration p = new ParticipantRestauration();
        p.setParticipantId(partId);
        p.setEvenementId(evtId);
        p.setBesoinLibelle(besoin);
        p.setRestrictionLibelle(restriction);
        p.setNiveauGravite(gravite != null && !gravite.isBlank() ? gravite : "LEGERE");
        try {
            controller.createBesoin(p);
            onActualiser();
            onNouveauBesoin();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Besoin créé.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
