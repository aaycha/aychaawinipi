package com.gestion.ui.restauration;

import com.gestion.controllers.MainController;
import com.gestion.controllers.RestaurationController;
import com.gestion.entities.Restauration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class RestaurationRepasController {

    @FXML
    private ListView<Restauration> listView;

    @FXML
    private TextField filterParticipantField;
    @FXML
    private DatePicker filterDatePicker;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortPrixCombo;

    @FXML
    private TextField inputNomRepas;
    @FXML
    private TextField inputPrix;
    @FXML
    private DatePicker inputDate;
    @FXML
    private TextField inputParticipantId;

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
    private final ObservableList<Restauration> repasData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedRepas = null;
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
                    setGraphic(createRepasCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createRepasCard(Restauration item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Text icon = new Text("🍱");
        icon.setFont(Font.font("Segoe UI Emoji", 24));

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(item.getNomRepas() != null ? item.getNomRepas() : "Sans nom");
        title.getStyleClass().add("card-title");

        BigDecimal prix = item.getPrix() != null ? item.getPrix() : BigDecimal.ZERO;
        Label priceBadge = new Label(String.format("%.2f €", prix));
        priceBadge.getStyleClass().add("status-badge");
        priceBadge.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, priceBadge);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        Label dateLabel = new Label("Date: " + (item.getDate() != null ? item.getDate().format(dateFormatter) : "N/A"));
        dateLabel.getStyleClass().add("card-label");

        Label partLabel = new Label("Participant: " + item.getParticipantId());
        partLabel.getStyleClass().add("card-value");

        details.add(dateLabel, 0, 0);
        details.add(partLabel, 1, 0);

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(icon, content);

        return card;
    }

    @FXML
    public void initialize() {
        try {
            setupListView();

            listView.setItems(repasData);
            listView.setVisible(true);
            listView.setManaged(true);

            filteredData = new FilteredList<>(repasData, p -> true);
            listView.setItems(filteredData);

            if (sortPrixCombo != null) {
                sortPrixCombo.getItems().setAll(
                        "Aucun",
                        "Prix ↑",
                        "Prix ↓");
                sortPrixCombo.getSelectionModel().selectFirst();
            }

            inputDate.setValue(LocalDate.now());
            inputNomRepas.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
            inputPrix.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

            onActualiser();
            updateCount();
            updateButtons();
        } catch (Exception e) {
            System.err.println("Erreur initialisation RestaurationRepasController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onNouveauRepas() {
        selectedRepas = null;
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
            List<Restauration> list;
            Long partId = parseLong(filterParticipantField.getText());
            LocalDate date = filterDatePicker.getValue();
            if (partId != null) {
                list = controller.getRepasByParticipant(partId);
            } else if (date != null) {
                list = controller.getRepasByDate(date);
            } else {
                list = controller.getRepasByDate(LocalDate.now());
                if (list.isEmpty()) {
                    list = controller.getRepasByParticipant(1L);
                }
            }

            List<Restauration> finalList = list;
            Platform.runLater(() -> {
                repasData.clear();
                repasData.addAll(finalList);
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

    @FXML
    void onReinitialiserFiltres() {
        filterParticipantField.clear();
        filterDatePicker.setValue(null);
        searchField.clear();
        onFiltrer();
    }

    @FXML
    void onTrierPrix() {
        if (sortPrixCombo == null)
            return;
        String value = sortPrixCombo.getValue();
        if (value == null || value.equals("Aucun")) {
            // listView doesn't support automatic column sorting like TableView,
            // but we can sort the underlying data list
            repasData.sort((r1, r2) -> r1.getId().compareTo(r2.getId()));
            return;
        }
        if (value.equals("Prix ↑")) {
            repasData.sort((r1, r2) -> {
                BigDecimal p1 = r1.getPrix() != null ? r1.getPrix() : BigDecimal.ZERO;
                BigDecimal p2 = r2.getPrix() != null ? r2.getPrix() : BigDecimal.ZERO;
                return p1.compareTo(p2);
            });
        } else if (value.equals("Prix ↓")) {
            repasData.sort((r1, r2) -> {
                BigDecimal p1 = r1.getPrix() != null ? r1.getPrix() : BigDecimal.ZERO;
                BigDecimal p2 = r2.getPrix() != null ? r2.getPrix() : BigDecimal.ZERO;
                return p2.compareTo(p1);
            });
        }
        // repasTable.getSortOrder().setAll(colPrix);
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();

        filteredData.setPredicate(repas -> {
            if (searchText == null || searchText.isEmpty())
                return true;
            String searchable = repas.getId() + " " +
                    (repas.getNomRepas() != null ? repas.getNomRepas() : "") + " " +
                    repas.getParticipantId();
            return searchable.toLowerCase().contains(searchText);
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedRepas = listView.getSelectionModel().getSelectedItem();
        if (selectedRepas != null) {
            loadRepasInForm(selectedRepas);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm())
            return;

        try {
            String nom = inputNomRepas.getText();
            BigDecimal prix = parseBigDecimal(inputPrix.getText());
            LocalDate date = inputDate.getValue() != null ? inputDate.getValue() : LocalDate.now();
            Long partId = parseLong(inputParticipantId.getText());

            if (partId == null)
                partId = 1L;

            if (isEditMode && selectedRepas != null) {
                selectedRepas.setNomRepas(nom);
                selectedRepas.setPrix(prix);
                selectedRepas.setDate(date);
                selectedRepas.setParticipantId(partId);
                controller.updateRepas(selectedRepas);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Repas modifié avec succès");
            } else {
                controller.createRepas(nom, prix, date, partId);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Repas créé avec succès");
            }

            onActualiser();
            onNouveauRepas();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedRepas == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un repas à modifier");
            return;
        }
        isEditMode = true;
        loadRepasInForm(selectedRepas);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedRepas == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un repas à supprimer");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer ce repas ?");
        confirmAlert.setContentText("ID: " + selectedRepas.getId() + "\nNom: " + selectedRepas.getNomRepas());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = controller.deleteRepas(selectedRepas.getId());
                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Repas supprimé avec succès");
                    onActualiser();
                    onNouveauRepas();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer ce repas");
                }
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouveauRepas();
    }

    @FXML
    void onStatistiques() {
        try {
            int total = repasData.size();
            BigDecimal totalPrix = repasData.stream()
                    .map(r -> r.getPrix() != null ? r.getPrix() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText("📊 Statistiques des Repas\n\nTotal: " + total + "\nTotal prix: "
                    + String.format("%.2f", totalPrix) + " €");
            statsAlert.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void loadRepasInForm(Restauration repas) {
        inputNomRepas.setText(repas.getNomRepas() != null ? repas.getNomRepas() : "");
        inputPrix.setText(repas.getPrix() != null ? repas.getPrix().toString() : "");
        inputDate.setValue(repas.getDate());
        inputParticipantId.setText(repas.getParticipantId() != null ? String.valueOf(repas.getParticipantId()) : "");
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (inputNomRepas.getText() == null || inputNomRepas.getText().trim().isEmpty()) {
            errors.append("• Nom est requis\n");
        }
        if (inputPrix.getText() == null || inputPrix.getText().trim().isEmpty()) {
            errors.append("• Prix est requis\n");
        } else {
            try {
                BigDecimal prix = new BigDecimal(inputPrix.getText().trim());
                if (prix.compareTo(BigDecimal.ZERO) < 0) {
                    errors.append("• Le prix doit être positif\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Prix doit être un nombre valide\n");
            }
        }
        if (inputParticipantId.getText() == null || inputParticipantId.getText().trim().isEmpty()) {
            errors.append("• Participant ID est requis\n");
        }
        if (errors.length() > 0) {
            showValidationMessage(errors.toString(), "error");
            return false;
        }
        hideValidationMessage();
        return true;
    }

    private void clearForm() {
        inputNomRepas.clear();
        inputPrix.clear();
        inputDate.setValue(LocalDate.now());
        inputParticipantId.clear();
    }

    private void updateButtons() {
        boolean hasSelection = selectedRepas != null;
        boolean admin = isAdmin();
        btnModifier.setDisable(!admin || !hasSelection);
        btnSupprimer.setDisable(!admin || !hasSelection);
        btnModifierForm.setDisable(!admin || !hasSelection);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : repasData.size();
        countLabel.setText(count + " repas");
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

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank())
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private boolean isAdmin() {
        return MainController.getCurrentRole() == MainController.Role.ADMIN;
    }
}
