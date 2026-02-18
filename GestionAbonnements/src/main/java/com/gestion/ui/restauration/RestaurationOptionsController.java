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

public class RestaurationOptionsController {

    @FXML private TableView<Restauration> optionsTable;
    @FXML private TableColumn<Restauration, Long> colOptId;
    @FXML private TableColumn<Restauration, String> colOptLibelle;
    @FXML private TableColumn<Restauration, String> colOptType;
    @FXML private TableColumn<Restauration, Boolean> colOptActif;
    
    @FXML private TextField filterTypeField;
    @FXML private TextField searchField;
    
    @FXML private TextField inputLibelle;
    @FXML private TextField inputTypeEvenement;
    @FXML private CheckBox inputActif;
    
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnModifierForm;
    @FXML private Label statusInfoLabel;
    @FXML private Label countLabel;
    @FXML private Label validationMessage;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<Restauration> optionsData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedOption = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        try {
            colOptId.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
            
            colOptLibelle.setCellValueFactory(cellData -> {
                String libelle = cellData.getValue().getLibelle();
                return new javafx.beans.property.SimpleStringProperty(libelle != null ? libelle : "");
            });
            
            colOptType.setCellValueFactory(cellData -> {
                String type = cellData.getValue().getTypeEvenement();
                return new javafx.beans.property.SimpleStringProperty(type != null ? type : "");
            });
            
            colOptActif.setCellValueFactory(cellData -> 
                new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isActif()));
            
            optionsTable.setItems(optionsData);
            optionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            optionsTable.setVisible(true);
            optionsTable.setManaged(true);
            
            filteredData = new FilteredList<>(optionsData, p -> true);
            optionsTable.setItems(filteredData);
            
            inputLibelle.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
            
            onActualiser();
            updateCount();
        } catch (Exception e) {
            System.err.println("Erreur initialisation RestaurationOptionsController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onNouvelleOption() {
        selectedOption = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
    }

    @FXML
    void onActualiser() {
        filterTypeField.clear();
        onFiltrer();
    }

    @FXML
    void onFiltrer() {
        try {
            String type = filterTypeField.getText();
            List<Restauration> list;
            if (type == null || type.isBlank()) {
                list = controller.getAllOptions();
            } else {
                list = controller.getOptionsByType(type.trim().toUpperCase());
                if (list.isEmpty()) {
                    list = controller.getAllOptions();
                }
            }

            List<Restauration> finalList = list;
            Platform.runLater(() -> {
                optionsData.clear();
                optionsData.addAll(finalList);
                applyFilters();
                updateCount();
                optionsTable.refresh();
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
        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String typeFilter = filterTypeField.getText().toLowerCase();
        
        filteredData.setPredicate(option -> {
            boolean match = true;
            
            if (!typeFilter.isEmpty()) {
                String optionType = option.getTypeEvenement() != null ? option.getTypeEvenement().toLowerCase() : "";
                match = match && optionType.contains(typeFilter);
            }
            
            if (!searchText.isEmpty()) {
                String searchable = option.getId() + " " + 
                                  (option.getLibelle() != null ? option.getLibelle() : "") + " " +
                                  (option.getTypeEvenement() != null ? option.getTypeEvenement() : "");
                match = match && searchable.toLowerCase().contains(searchText);
            }
            
            return match;
        });
        
        updateCount();
    }

    @FXML
    void onTableClick(MouseEvent event) {
        selectedOption = optionsTable.getSelectionModel().getSelectedItem();
        if (selectedOption != null) {
            loadOptionInForm(selectedOption);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm()) return;

        try {
            String libelle = inputLibelle.getText();
            String typeEvt = inputTypeEvenement.getText();
            if (typeEvt == null || typeEvt.isBlank()) typeEvt = "SOIREE";
            
            if (isEditMode && selectedOption != null) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "La modification nécessite une implémentation dans le service");
            } else {
                controller.createOption(libelle, typeEvt.toUpperCase(), inputActif.isSelected());
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Option créée avec succès");
            }
            
            onActualiser();
            onNouvelleOption();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedOption == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une option à modifier");
            return;
        }
        isEditMode = true;
        loadOptionInForm(selectedOption);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedOption == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une option à supprimer");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette option ?");
        confirmAlert.setContentText("ID: " + selectedOption.getId() + "\nLibellé: " + selectedOption.getLibelle());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert(Alert.AlertType.INFORMATION, "Info", "La suppression nécessite une implémentation dans le service");
                onActualiser();
                onNouvelleOption();
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouvelleOption();
    }

    private void loadOptionInForm(Restauration option) {
        inputLibelle.setText(option.getLibelle() != null ? option.getLibelle() : "");
        inputTypeEvenement.setText(option.getTypeEvenement() != null ? option.getTypeEvenement() : "");
        inputActif.setSelected(option.isActif());
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
        inputTypeEvenement.clear();
        inputActif.setSelected(true);
    }

    private void updateButtons() {
        boolean hasSelection = selectedOption != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
        btnModifierForm.setDisable(!hasSelection);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : optionsData.size();
        countLabel.setText(count + " option(s)");
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
