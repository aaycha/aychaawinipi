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

public class RestaurationRepasController {

    @FXML private TableView<Restauration> repasTable;
    @FXML private TableColumn<Restauration, Long> colRepasId;
    @FXML private TableColumn<Restauration, String> colNomRepas;
    @FXML private TableColumn<Restauration, String> colPrix;
    @FXML private TableColumn<Restauration, String> colDate;
    @FXML private TableColumn<Restauration, Long> colParticipantId;
    
    @FXML private TextField filterParticipantField;
    @FXML private DatePicker filterDatePicker;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortPrixCombo;
    
    @FXML private TextField inputNomRepas;
    @FXML private TextField inputPrix;
    @FXML private DatePicker inputDate;
    @FXML private TextField inputParticipantId;
    
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnModifierForm;
    @FXML private Label statusInfoLabel;
    @FXML private Label countLabel;
    @FXML private Label validationMessage;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<Restauration> repasData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedRepas = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            colRepasId.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
            
            colNomRepas.setCellValueFactory(cellData -> {
                String nom = cellData.getValue().getNomRepas();
                return new javafx.beans.property.SimpleStringProperty(nom != null ? nom : "");
            });
            
            colPrix.setCellValueFactory(cellData -> {
                BigDecimal prix = cellData.getValue().getPrix();
                return new javafx.beans.property.SimpleStringProperty(
                    prix != null ? String.format("%.2f", prix) : "0.00");
            });
            
            colDate.setCellValueFactory(cellData -> {
                LocalDate date = cellData.getValue().getDate();
                return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(formatter) : "");
            });
            
            colParticipantId.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getParticipantId()));
            
            repasTable.setItems(repasData);
            repasTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            repasTable.setVisible(true);
            repasTable.setManaged(true);
            
            filteredData = new FilteredList<>(repasData, p -> true);
            repasTable.setItems(filteredData);

            if (sortPrixCombo != null) {
                sortPrixCombo.getItems().setAll(
                        "Aucun",
                        "Prix ↑",
                        "Prix ↓"
                );
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
                repasTable.refresh();
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
        if (sortPrixCombo == null) return;
        String value = sortPrixCombo.getValue();
        if (value == null || value.equals("Aucun")) {
            repasTable.getSortOrder().clear();
            return;
        }
        if (value.equals("Prix ↑")) {
            colPrix.setSortType(TableColumn.SortType.ASCENDING);
        } else if (value.equals("Prix ↓")) {
            colPrix.setSortType(TableColumn.SortType.DESCENDING);
        }
        repasTable.getSortOrder().setAll(colPrix);
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        
        filteredData.setPredicate(repas -> {
            if (searchText == null || searchText.isEmpty()) return true;
            String searchable = repas.getId() + " " + 
                              (repas.getNomRepas() != null ? repas.getNomRepas() : "") + " " +
                              repas.getParticipantId();
            return searchable.toLowerCase().contains(searchText);
        });
        
        updateCount();
    }

    @FXML
    void onTableClick(MouseEvent event) {
        selectedRepas = repasTable.getSelectionModel().getSelectedItem();
        if (selectedRepas != null) {
            loadRepasInForm(selectedRepas);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm()) return;

        try {
            String nom = inputNomRepas.getText();
            BigDecimal prix = parseBigDecimal(inputPrix.getText());
            LocalDate date = inputDate.getValue() != null ? inputDate.getValue() : LocalDate.now();
            Long partId = parseLong(inputParticipantId.getText());
            
            if (partId == null) partId = 1L;
            
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
            statsAlert.setContentText("📊 Statistiques des Repas\n\nTotal: " + total + "\nTotal prix: " + String.format("%.2f", totalPrix) + " €");
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
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
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
