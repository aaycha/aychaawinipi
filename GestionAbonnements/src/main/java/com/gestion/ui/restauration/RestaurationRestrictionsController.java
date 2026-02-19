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

import java.util.List;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class RestaurationRestrictionsController {

    @FXML
    private ListView<Restauration> listView;

    @FXML
    private TextField searchField;

    @FXML
    private TextField inputLibelle;
    @FXML
    private TextField inputDescription;
    @FXML
    private CheckBox inputActif;

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
    private final ObservableList<Restauration> restrictionsData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedRestriction = null;
    private boolean isEditMode = false;

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
                    setGraphic(createRestrictionCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createRestrictionCard(Restauration item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconPane = new StackPane();
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.web("#fff5f5"));
        Text icon = new Text("⚠️");
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Participant #" + item.getParticipantId());
        title.getStyleClass().add("card-title");

        Label libelleBadge = new Label(item.getRestrictionLibelle() != null ? item.getRestrictionLibelle() : "N/A");
        libelleBadge.getStyleClass().add("status-badge");
        libelleBadge.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, libelleBadge, spacer);

        VBox details = new VBox(2);
        Label descLabel = new Label(
                item.getRestrictionDescription() != null ? item.getRestrictionDescription() : "Aucune description");
        descLabel.getStyleClass().add("card-label");
        descLabel.setWrapText(true);

        details.getChildren().add(descLabel);

        content.getChildren().addAll(header, details);

        // Right side: Active status
        VBox rightSide = new VBox();
        rightSide.setAlignment(Pos.CENTER_RIGHT);
        Label statusBadgeLabel = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadgeLabel.getStyleClass().add("status-badge");
        if (item.isActif()) {
            statusBadgeLabel.setStyle("-fx-background-color: #28a745;");
        } else {
            statusBadgeLabel.setStyle("-fx-background-color: #6c757d;");
        }
        rightSide.getChildren().add(statusBadgeLabel);

        card.getChildren().addAll(iconPane, content, rightSide);

        return card;
    }

    @FXML
    public void initialize() {
        try {
            setupListView();

            listView.setItems(restrictionsData);
            listView.setVisible(true);
            listView.setManaged(true);

            filteredData = new FilteredList<>(restrictionsData, p -> true);
            listView.setItems(filteredData);

            inputLibelle.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

            onActualiser();
            updateCount();
        } catch (Exception e) {
            System.err.println("Erreur initialisation RestaurationRestrictionsController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onNouvelleRestriction() {
        selectedRestriction = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
    }

    @FXML
    void onActualiser() {
        try {
            List<Restauration> list = controller.getRestrictionsActives();
            Platform.runLater(() -> {
                restrictionsData.clear();
                restrictionsData.addAll(list);
                applyFilters();
                updateCount();
                updateCount();
                listView.refresh();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    @FXML
    void onRechercher() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase()
                : "";

        filteredData.setPredicate(restriction -> {
            if (searchText.isEmpty())
                return true;
            String searchable = restriction.getId() + " " +
                    (restriction.getRestrictionLibelle() != null ? restriction.getRestrictionLibelle() : "") + " " +
                    (restriction.getRestrictionDescription() != null ? restriction.getRestrictionDescription() : "");
            return searchable.toLowerCase().contains(searchText);
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedRestriction = listView.getSelectionModel().getSelectedItem();
        if (selectedRestriction != null) {
            loadRestrictionInForm(selectedRestriction);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm())
            return;

        try {
            String libelle = inputLibelle.getText();
            String desc = inputDescription.getText() != null ? inputDescription.getText() : "";

            if (isEditMode && selectedRestriction != null) {
                showAlert(Alert.AlertType.INFORMATION, "Info",
                        "La modification nécessite une implémentation dans le service");
            } else {
                controller.createRestriction(libelle, desc, inputActif.isSelected());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Restriction créée avec succès");
            }

            onActualiser();
            onNouvelleRestriction();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedRestriction == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une restriction à modifier");
            return;
        }
        isEditMode = true;
        loadRestrictionInForm(selectedRestriction);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedRestriction == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une restriction à supprimer");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette restriction ?");
        confirmAlert.setContentText(
                "ID: " + selectedRestriction.getId() + "\nLibellé: " + selectedRestriction.getRestrictionLibelle());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert(Alert.AlertType.INFORMATION, "Info",
                        "La suppression nécessite une implémentation dans le service");
                onActualiser();
                onNouvelleRestriction();
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouvelleRestriction();
    }

    private void loadRestrictionInForm(Restauration restriction) {
        inputLibelle.setText(restriction.getRestrictionLibelle() != null ? restriction.getRestrictionLibelle() : "");
        inputDescription.setText(
                restriction.getRestrictionDescription() != null ? restriction.getRestrictionDescription() : "");
        inputActif.setSelected(restriction.isActif());
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (inputLibelle.getText() == null || inputLibelle.getText().trim().isEmpty()) {
            errors.append("• Libellé est requis\n");
        }
        if (errors.length() > 0) {
            showValidationMessage(errors.toString(), "error");
            return false;
        }
        hideValidationMessage();
        return true;
    }

    private void clearForm() {
        inputLibelle.clear();
        inputDescription.clear();
        inputActif.setSelected(true);
    }

    private void updateButtons() {
        boolean hasSelection = selectedRestriction != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
        btnModifierForm.setDisable(!hasSelection);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : restrictionsData.size();
        countLabel.setText(count + " restriction(s)");
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
