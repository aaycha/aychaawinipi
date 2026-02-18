package com.gestion.ui.restauration;

import com.gestion.controllers.RestaurationController;
import com.gestion.entities.ParticipantRestauration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class RestaurationBesoinsController {

    @FXML
    private ListView<ParticipantRestauration> listView;
    @FXML
    private TextField filterParticipantField;
    @FXML
    private TextField inputParticipantId;
    @FXML
    private TextField inputEvenementId;
    @FXML
    private TextField inputBesoinLibelle;
    @FXML
    private TextField inputRestrictionLibelle;
    @FXML
    private TextField inputNiveauGravite;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<ParticipantRestauration> besoinsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupListView();
        listView.setItems(besoinsData);
        onActualiser();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<ParticipantRestauration>() {
            @Override
            protected void updateItem(ParticipantRestauration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createBesoinCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createBesoinCard(ParticipantRestauration item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Text icon = new Text(item.isAnnule() ? "❌" : "🍽️");
        icon.setFont(Font.font("Segoe UI Emoji", 24));

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Besoin: " + (item.getBesoinLibelle() != null ? item.getBesoinLibelle() : "N/A"));
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.getNiveauGravite() != null ? item.getNiveauGravite() : "N/A");
        statusBadge.getStyleClass().add("status-badge");
        if ("ELEVEE".equalsIgnoreCase(item.getNiveauGravite())) {
            statusBadge.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828;");
        } else {
            statusBadge.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        HBox details = new HBox(15);
        Label partLabel = new Label("Participant #" + item.getParticipantId());
        partLabel.getStyleClass().add("card-label");

        Label evtLabel = new Label("Event #" + item.getEvenementId());
        evtLabel.getStyleClass().add("card-label");

        Label restrLabel = new Label(
                "Restriction: " + (item.getRestrictionLibelle() != null ? item.getRestrictionLibelle() : "-"));
        restrLabel.getStyleClass().add("card-label");

        details.getChildren().addAll(partLabel, evtLabel, restrLabel);

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(icon, content);

        return card;
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
        if (s == null || s.isBlank())
            return null;
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
