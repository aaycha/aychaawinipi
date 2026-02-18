package com.gestion.ui.abonnement;

import com.gestion.controllers.AbonnementController;
import com.gestion.controllers.MainController;
import com.gestion.entities.Abonnement;
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
import java.util.stream.Collectors;

public class AbonnementViewController {

    @FXML private TableView<Abonnement> table;
    @FXML private TableColumn<Abonnement, Long> colId;
    @FXML private TableColumn<Abonnement, Long> colUserId;
    @FXML private TableColumn<Abonnement, String> colType;
    @FXML private TableColumn<Abonnement, String> colDateDebut;
    @FXML private TableColumn<Abonnement, String> colDateFin;
    @FXML private TableColumn<Abonnement, String> colPrix;
    @FXML private TableColumn<Abonnement, String> colStatut;
    @FXML private TableColumn<Abonnement, Number> colPoints;
    @FXML private TableColumn<Abonnement, Boolean> colAutoRenew;
    
    // Filtres et recherche
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterType;
    @FXML private TextField filterUserId;
    @FXML private TextField searchField;
    
    // Formulaire
    @FXML private TextField inputUserId;
    @FXML private ComboBox<Abonnement.TypeAbonnement> inputType;
    @FXML private DatePicker inputDateDebut;
    @FXML private DatePicker inputDateFin;
    @FXML private TextField inputPrix;
    @FXML private ComboBox<Abonnement.StatutAbonnement> inputStatut;
    @FXML private CheckBox inputAutoRenew;
    
    // Boutons
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnEnregistrer;
    @FXML private Button btnModifierForm;
    
    // Labels
    @FXML private Label statusInfoLabel;
    @FXML private Label countLabel;
    @FXML private Label validationMessage;

    private final AbonnementController controller = new AbonnementController();
    private final ObservableList<Abonnement> data = FXCollections.observableArrayList();
    private FilteredList<Abonnement> filteredData;
    private Abonnement selectedAbonnement = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            setupFilters();
            setupForm();
            
            // Configuration du TableView
            table.setItems(data);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setVisible(true);
            table.setManaged(true);
            
            // Initialiser le filtered list
            filteredData = new FilteredList<>(data, p -> true);
            table.setItems(filteredData);
            
            // Charger les données
            onActualiser();
            
            // Mise à jour du compteur
            updateCount();

            applyRolePermissions();
            
            System.out.println("AbonnementViewController initialisé avec succès");
        } catch (Exception e) {
            System.err.println("Erreur initialisation AbonnementViewController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        colId.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        
        colUserId.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getUserId()));
        
        colType.setCellValueFactory(cellData -> {
            Abonnement.TypeAbonnement type = cellData.getValue().getType();
            return new javafx.beans.property.SimpleStringProperty(type != null ? type.name() : "");
        });
        
        colDateDebut.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateDebut();
            return new javafx.beans.property.SimpleStringProperty(
                date != null ? date.format(formatter) : "");
        });
        
        colDateFin.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateFin();
            return new javafx.beans.property.SimpleStringProperty(
                date != null ? date.format(formatter) : "");
        });
        
        colPrix.setCellValueFactory(cellData -> {
            BigDecimal prix = cellData.getValue().getPrix();
            return new javafx.beans.property.SimpleStringProperty(
                prix != null ? String.format("%.2f", prix) : "0.00");
        });
        
        colStatut.setCellValueFactory(cellData -> {
            Abonnement.StatutAbonnement statut = cellData.getValue().getStatut();
            return new javafx.beans.property.SimpleStringProperty(statut != null ? statut.name() : "");
        });
        
        colPoints.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPointsAccumules()));
        
        colAutoRenew.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isAutoRenew()));
    }

    private void setupFilters() {
        filterStatut.getItems().addAll("", "ACTIF", "EXPIRE", "SUSPENDU", "EN_ATTENTE");
        filterType.getItems().addAll("", "MENSUEL", "ANNUEL", "PREMIUM");
    }

    private void setupForm() {
        inputType.getItems().addAll(Abonnement.TypeAbonnement.values());
        inputStatut.getItems().addAll(Abonnement.StatutAbonnement.values());
        
        // Validation en temps réel
        inputUserId.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        inputPrix.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        inputDateDebut.valueProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    @FXML
    void onNouveau() {
        selectedAbonnement = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
    }

    @FXML
    void onActualiser() {
        try {
            List<Abonnement> list = controller.getAll();
            Platform.runLater(() -> {
                data.clear();
                data.addAll(list);
                updateCount();
                updateStatusInfo("Données actualisées avec succès");
                table.refresh();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    @FXML
    void onFiltrer() {
        applyFilters();
    }

    @FXML
    void onRechercher() {
        applyFilters();
    }

    @FXML
    void onReinitialiserFiltres() {
        filterStatut.getSelectionModel().clearSelection();
        filterType.getSelectionModel().clearSelection();
        filterUserId.clear();
        searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statut = filterStatut.getValue();
        String type = filterType.getValue();
        Long userId = parseLong(filterUserId.getText());

        filteredData.setPredicate(abonnement -> {
            boolean match = true;
            
            // Recherche textuelle
            if (searchText != null && !searchText.isEmpty()) {
                String searchable = abonnement.getId() + " " + 
                                  abonnement.getUserId() + " " +
                                  abonnement.getType().name() + " " +
                                  abonnement.getStatut().name();
                match = match && searchable.toLowerCase().contains(searchText);
            }
            
            // Filtre statut
            if (statut != null && !statut.isEmpty()) {
                match = match && abonnement.getStatut().name().equals(statut);
            }
            
            // Filtre type
            if (type != null && !type.isEmpty()) {
                match = match && abonnement.getType().name().equals(type);
            }
            
            // Filtre userId
            if (userId != null) {
                match = match && abonnement.getUserId().equals(userId);
            }
            
            return match;
        });
        
        updateCount();
    }

    @FXML
    void onTableClick(MouseEvent event) {
        selectedAbonnement = table.getSelectionModel().getSelectedItem();
        if (selectedAbonnement != null) {
            loadAbonnementInForm(selectedAbonnement);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm()) {
            return;
        }

        try {
            Abonnement abonnement = buildAbonnementFromForm();
            
            if (isEditMode && selectedAbonnement != null) {
                abonnement.setId(selectedAbonnement.getId());
                controller.update(abonnement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Abonnement modifié avec succès");
            } else {
                controller.create(abonnement);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Abonnement créé avec succès");
            }
            
            onActualiser();
            onNouveau();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedAbonnement == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un abonnement à modifier");
            return;
        }
        
        isEditMode = true;
        loadAbonnementInForm(selectedAbonnement);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedAbonnement == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un abonnement à supprimer");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer l'abonnement ?");
        confirmAlert.setContentText("ID: " + selectedAbonnement.getId() + 
                                   "\nUser ID: " + selectedAbonnement.getUserId() +
                                   "\nType: " + selectedAbonnement.getType().name());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = controller.delete(selectedAbonnement.getId());
                    if (deleted) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Abonnement supprimé avec succès");
                        onActualiser();
                        onNouveau();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'abonnement");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouveau();
    }

    @FXML
    void onStatistiques() {
        try {
            BigDecimal totalPoints = controller.getTotalPointsAccumules();
            List<Abonnement> prochesExpiration = controller.findProchesExpiration(30);
            
            StringBuilder stats = new StringBuilder();
            stats.append("📊 Statistiques des Abonnements\n\n");
            stats.append("Total des points accumulés: ").append(totalPoints).append("\n");
            stats.append("Abonnements proches expiration (30 jours): ").append(prochesExpiration.size()).append("\n");
            stats.append("Total des abonnements: ").append(data.size());
            
            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText(stats.toString());
            statsAlert.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du calcul des statistiques: " + e.getMessage());
        }
    }

    private void loadAbonnementInForm(Abonnement abonnement) {
        inputUserId.setText(String.valueOf(abonnement.getUserId()));
        inputType.setValue(abonnement.getType());
        inputDateDebut.setValue(abonnement.getDateDebut());
        inputDateFin.setValue(abonnement.getDateFin());
        inputPrix.setText(abonnement.getPrix().toString());
        inputStatut.setValue(abonnement.getStatut());
        inputAutoRenew.setSelected(abonnement.isAutoRenew());
    }

    private Abonnement buildAbonnementFromForm() {
        Long userId = parseLong(inputUserId.getText());
        Abonnement.TypeAbonnement type = inputType.getValue();
        LocalDate dateDebut = inputDateDebut.getValue();
        LocalDate dateFin = inputDateFin.getValue();
        BigDecimal prix = parseBigDecimal(inputPrix.getText());
        Abonnement.StatutAbonnement statut = inputStatut.getValue();
        boolean autoRenew = inputAutoRenew.isSelected();

        Abonnement abonnement = new Abonnement(userId, type, dateDebut, prix, autoRenew);
        if (dateFin != null) {
            abonnement.setDateFin(dateFin);
        }
        if (statut != null) {
            abonnement.setStatut(statut);
        }
        
        return abonnement;
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        if (inputUserId.getText() == null || inputUserId.getText().trim().isEmpty()) {
            errors.append("• User ID est requis\n");
        } else {
            try {
                Long.parseLong(inputUserId.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("• User ID doit être un nombre valide\n");
            }
        }
        
        if (inputType.getValue() == null) {
            errors.append("• Type est requis\n");
        }
        
        if (inputDateDebut.getValue() == null) {
            errors.append("• Date de début est requise\n");
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
        
        if (inputDateFin.getValue() != null && inputDateDebut.getValue() != null) {
            if (inputDateFin.getValue().isBefore(inputDateDebut.getValue())) {
                errors.append("• La date de fin doit être après la date de début\n");
            }
        }
        
        if (errors.length() > 0) {
            showValidationMessage(errors.toString(), "error");
            return false;
        } else {
            hideValidationMessage();
            return true;
        }
    }

    private void clearForm() {
        inputUserId.clear();
        inputType.getSelectionModel().selectFirst();
        inputDateDebut.setValue(LocalDate.now());
        inputDateFin.setValue(null);
        inputPrix.clear();
        inputStatut.getSelectionModel().selectFirst();
        inputAutoRenew.setSelected(true);
    }

    private void updateButtons() {
        boolean hasSelection = selectedAbonnement != null;
        boolean admin = isAdmin();
        btnModifier.setDisable(!admin || !hasSelection);
        btnSupprimer.setDisable(!admin || !hasSelection);
        btnModifierForm.setDisable(!admin || !hasSelection);
        // en mode utilisateur, on autorise uniquement la création de nouveaux abonnements
        btnEnregistrer.setDisable(!admin && isEditMode);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : data.size();
        countLabel.setText(count + " abonnement(s)");
    }

    private void updateStatusInfo(String message) {
        if (statusInfoLabel != null) {
            statusInfoLabel.setText(message);
        }
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

    private boolean isAdmin() {
        return MainController.getCurrentRole() == MainController.Role.ADMIN;
    }

    private void applyRolePermissions() {
        updateButtons();
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
}