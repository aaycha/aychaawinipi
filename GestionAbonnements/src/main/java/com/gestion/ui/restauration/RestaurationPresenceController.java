package com.gestion.ui.restauration;

import com.gestion.controllers.RestaurationController;
import com.gestion.entities.Restauration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class RestaurationPresenceController {

    @FXML
    private ListView<Restauration> listView;

    @FXML
    private TextField filterParticipantField;
    @FXML
    private TextField searchField;

    @FXML
    private TextField inputParticipantId;
    @FXML
    private DatePicker inputDate;
    @FXML
    private CheckBox inputAbonnementActif;

    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnEnregistrer;
    @FXML
    private Button btnModifierForm;
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Label countLabel;
    @FXML
    private Label validationMessage;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<Restauration> presenceData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedPresence = null;
    private boolean isEditMode = false;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Restauration>() {
            @Override
            protected void updateItem(Restauration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createPresenceCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createPresenceCard(Restauration item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Text icon = new Text("✅");
        icon.setFont(Font.font("Segoe UI Emoji", 24));

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Participant #" + item.getParticipantId());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.isAbonnementActif() ? "ABONNEMENT ACTIF" : "PAS D'ABONNEMENT");
        statusBadge.getStyleClass().addAll("status-badge", item.isAbonnementActif() ? "ACTIF" : "EN_ATTENTE");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        HBox details = new HBox(15);

        Label dateLabel = new Label(
                "Date: " + (item.getDatePresence() != null ? item.getDatePresence().format(dateFormatter) : "N/A"));
        dateLabel.getStyleClass().add("card-label");

        details.getChildren().add(dateLabel);

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(icon, content);

        return card;
    }

    @FXML
    public void initialize() {
        try {
            setupListView();

            listView.setItems(presenceData);
            listView.setVisible(true);
            listView.setManaged(true);

            filteredData = new FilteredList<>(presenceData, p -> true);
            listView.setItems(filteredData);

            inputDate.setValue(LocalDate.now());
            inputParticipantId.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

            onActualiser();
            updateCount();
        } catch (Exception e) {
            System.err.println("Erreur initialisation RestaurationPresenceController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onNouvellePresence() {
        selectedPresence = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
    }

    @FXML
    void onActualiser() {
        onFiltrer();
    }

    @FXML
    void onFiltrer() {
        try {
            Long partId = parseLong(filterParticipantField.getText());
            List<Restauration> list = partId != null
                    ? controller.getPresenceByParticipant(partId)
                    : controller.getPresenceByParticipant(1L);

            Platform.runLater(() -> {
                presenceData.clear();
                presenceData.addAll(list);
                applyFilters();
                updateCount();
                updateCount();
                listView.refresh();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du filtrage: " + e.getMessage());
        }
    }

    @FXML
    void onRechercher() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase()
                : "";

        filteredData.setPredicate(presence -> {
            if (searchText.isEmpty())
                return true;
            String searchable = presence.getId() + " " +
                    presence.getParticipantId();
            return searchable.toLowerCase().contains(searchText);
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedPresence = listView.getSelectionModel().getSelectedItem();
        if (selectedPresence != null) {
            loadPresenceInForm(selectedPresence);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm())
            return;

        try {
            Long partId = parseLong(inputParticipantId.getText());
            LocalDate date = inputDate.getValue() != null ? inputDate.getValue() : LocalDate.now();
            boolean abonnementActif = inputAbonnementActif.isSelected();

            if (partId == null)
                partId = 1L;

            if (isEditMode && selectedPresence != null) {
                showAlert(Alert.AlertType.INFORMATION, "Info",
                        "La modification nécessite une implémentation dans le service");
            } else {
                controller.createPresence(partId, date, abonnementActif);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Présence créée avec succès");
            }

            onActualiser();
            onNouvellePresence();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedPresence == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une présence à modifier");
            return;
        }
        isEditMode = true;
        loadPresenceInForm(selectedPresence);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedPresence == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une présence à supprimer");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette présence ?");
        confirmAlert.setContentText(
                "ID: " + selectedPresence.getId() + "\nParticipant ID: " + selectedPresence.getParticipantId());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert(Alert.AlertType.INFORMATION, "Info",
                        "La suppression nécessite une implémentation dans le service");
                onActualiser();
                onNouvellePresence();
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouvellePresence();
    }

    private void loadPresenceInForm(Restauration presence) {
        inputParticipantId
                .setText(presence.getParticipantId() != null ? String.valueOf(presence.getParticipantId()) : "");
        inputDate.setValue(presence.getDatePresence());
        inputAbonnementActif.setSelected(presence.isAbonnementActif());
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (inputParticipantId.getText() == null || inputParticipantId.getText().trim().isEmpty()) {
            errors.append("• Participant ID est requis\n");
        } else {
            try {
                Long.parseLong(inputParticipantId.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("• Participant ID doit être un nombre valide\n");
            }
        }
        if (inputDate.getValue() == null) {
            errors.append("• Date est requise\n");
        }
        if (errors.length() > 0) {
            showValidationMessage(errors.toString(), "error");
            return false;
        }
        hideValidationMessage();
        return true;
    }

    private void clearForm() {
        inputParticipantId.clear();
        inputDate.setValue(LocalDate.now());
        inputAbonnementActif.setSelected(true);
    }

    private void updateButtons() {
        boolean hasSelection = selectedPresence != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
        btnModifierForm.setDisable(!hasSelection);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : presenceData.size();
        countLabel.setText(count + " présence(s)");
    }

    private void showValidationMessage(String message, String type) {
        if (validationMessage != null) {
            validationMessage.setText(message);
            validationMessage.setVisible(true);
            validationMessage.setManaged(true);
            validationMessage.getStyleClass().removeAll("validation-error", "validation-info", "validation-success");
            validationMessage.getStyleClass().add("validation-" + type);
        }
    }

    private void hideValidationMessage() {
        if (validationMessage != null) {
            validationMessage.setVisible(false);
            validationMessage.setManaged(false);
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
