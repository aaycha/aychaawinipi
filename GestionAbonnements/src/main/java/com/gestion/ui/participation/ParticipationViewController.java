package com.gestion.ui.participation;

import com.gestion.controllers.MainController;
import com.gestion.controllers.ParticipationController;
import com.gestion.entities.Participation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;
import java.util.List;

public class ParticipationViewController {

    @FXML
    private ListView<Participation> listView;

    // Filtres et recherche
    @FXML
    private ComboBox<String> filterStatut;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private TextField filterUserId;
    @FXML
    private TextField filterEvenementId;
    @FXML
    private TextField searchField;

    // Formulaire
    @FXML
    private TextField inputUserId;
    @FXML
    private TextField inputEvenementId;
    @FXML
    private ComboBox<Participation.TypeParticipation> inputType;
    @FXML
    private ComboBox<Participation.ContexteSocial> inputContexte;
    @FXML
    private CheckBox inputHebergement;
    @FXML
    private TextField inputHebergementNuits;

    // Boutons
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnConfirmer;
    @FXML
    private Button btnAnnulerPart;
    @FXML
    private Button btnEnregistrer;
    @FXML
    private Button btnModifierForm;

    // Labels
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Label countLabel;
    @FXML
    private Label validationMessage;

    private final ParticipationController controller = new ParticipationController();
    private final ObservableList<Participation> data = FXCollections.observableArrayList();
    private FilteredList<Participation> filteredData;
    private Participation selectedParticipation = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        try {
            setupListView();
            setupFilters();
            setupForm();

            listView.setItems(data);
            listView.setVisible(true);
            listView.setManaged(true);

            filteredData = new FilteredList<>(data, p -> true);
            listView.setItems(filteredData);

            onActualiser();
            updateCount();

            System.out.println("ParticipationViewController initialisé avec succès");

            applyRolePermissions();
        } catch (Exception e) {
            System.err.println("Erreur initialisation ParticipationViewController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Participation>() {
            @Override
            protected void updateItem(Participation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createCard(Participation item) {
        // Main Card Container
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconPane = new StackPane();
        Circle bg = new Circle(20, Color.web("#fff0f6"));
        Text icon = new Text(getItemIcon(item.getType()));
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconPane.getChildren().addAll(bg, icon);

        // Content Area
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("User #" + item.getUserId() + " • Événement #" + item.getEvenementId());
        title.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.getStatut().name());
        statusBadge.getStyleClass().addAll("status-badge", item.getStatut().name());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer, statusBadge);

        // Details Grid
        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        // Info
        details.add(createDetailLabel("🎭 Type:", item.getType().name()), 0, 0);
        details.add(createDetailLabel("🤝 Contexte:", item.getContexteSocial().name()), 1, 0);

        if (item.getHebergementNuits() > 0) {
            details.add(createDetailLabel("🏨 Hébergement:", item.getHebergementNuits() + " nuits"), 0, 1);
        }
        if (item.getBadgeAssocie() != null && !item.getBadgeAssocie().isEmpty()) {
            details.add(createDetailLabel("🏷️ Badge:", item.getBadgeAssocie()), 1, 1);
        }

        content.getChildren().addAll(header, details);

        card.getChildren().addAll(iconPane, content);
        return card;
    }

    private String getItemIcon(Participation.TypeParticipation type) {
        if (type == null)
            return "🎫";
        return switch (type) {
            case GROUPE -> "👥";
            case HEBERGEMENT -> "🏨";
            case SIMPLE -> "👤";
            default -> "🎫";
        };
    }

    private HBox createDetailLabel(String labelText, String valueText) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label(labelText);
        label.getStyleClass().add("card-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("card-value");
        box.getChildren().addAll(label, value);
        return box;
    }

    private void setupFilters() {
        filterStatut.getItems().addAll("", "EN_ATTENTE", "CONFIRME", "ANNULE", "EN_LISTE_ATTENTE");
        filterType.getItems().addAll("", "SIMPLE", "HEBERGEMENT", "GROUPE");
    }

    private void setupForm() {
        inputType.getItems().addAll(Participation.TypeParticipation.values());
        inputContexte.getItems().addAll(Participation.ContexteSocial.values());

        inputUserId.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        inputEvenementId.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
    }

    @FXML
    void onNouvelle() {
        selectedParticipation = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
        openForm(null);
    }

    @FXML
    void onActualiser() {
        try {
            List<Participation> list = controller.getAll();
            Platform.runLater(() -> {
                data.clear();
                data.addAll(list);
                updateCount();
                updateStatusInfo("Données actualisées avec succès");
                listView.refresh();
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
        filterEvenementId.clear();
        searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String statut = filterStatut.getValue();
        String type = filterType.getValue();
        Long userId = parseLong(filterUserId.getText());
        Long evenementId = parseLong(filterEvenementId.getText());

        filteredData.setPredicate(participation -> {
            boolean match = true;

            if (searchText != null && !searchText.isEmpty()) {
                String searchable = participation.getId() + " " +
                        participation.getUserId() + " " +
                        participation.getEvenementId() + " " +
                        participation.getType().name() + " " +
                        participation.getStatut().name();
                match = match && searchable.toLowerCase().contains(searchText);
            }

            if (statut != null && !statut.isEmpty()) {
                match = match && participation.getStatut().name().equals(statut);
            }

            if (type != null && !type.isEmpty()) {
                match = match && participation.getType().name().equals(type);
            }

            if (userId != null) {
                match = match && participation.getUserId().equals(userId);
            }

            if (evenementId != null) {
                match = match && participation.getEvenementId().equals(evenementId);
            }

            return match;
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedParticipation = listView.getSelectionModel().getSelectedItem();
        if (selectedParticipation != null) {
            loadParticipationInForm(selectedParticipation);
            updateButtons();

            // Double‑clic : ouverture d'une "page" de détails dans une boîte de dialogue
            // moderne
            if (event.getClickCount() == 2) {
                showDetailsDialog(selectedParticipation);
            }
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm()) {
            return;
        }

        try {
            Participation participation = buildParticipationFromForm();

            if (isEditMode && selectedParticipation != null) {
                participation.setId(selectedParticipation.getId());
                controller.update(participation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation modifiée avec succès");
            } else {
                controller.create(participation);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation créée avec succès");
            }

            onActualiser();
            onNouvelle();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedParticipation == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une participation à modifier");
            return;
        }
        openForm(selectedParticipation);
    }

    @FXML
    void onSupprimer() {
        if (selectedParticipation == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une participation à supprimer");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer la participation ?");
        confirmAlert.setContentText("ID: " + selectedParticipation.getId() +
                "\nUser ID: " + selectedParticipation.getUserId() +
                "\nÉvénement ID: " + selectedParticipation.getEvenementId());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = controller.delete(selectedParticipation.getId());
                    if (deleted) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation supprimée avec succès");
                        onActualiser();
                        onNouvelle();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer la participation");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    void onConfirmer() {
        if (selectedParticipation == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une participation à confirmer");
            return;
        }

        try {
            controller.confirmer(selectedParticipation.getId());
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation confirmée avec succès");
            onActualiser();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onAnnulerParticipation() {
        if (selectedParticipation == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une participation à annuler");
            return;
        }

        try {
            controller.annuler(selectedParticipation.getId(), "Annulation manuelle");
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation annulée avec succès");
            onActualiser();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onAnnuler() {
        onNouvelle();
    }

    @FXML
    void onStatistiques() {
        try {
            int total = data.size();
            long confirmes = data.stream().filter(p -> p.getStatut() == Participation.StatutParticipation.CONFIRME)
                    .count();
            long enAttente = data.stream().filter(p -> p.getStatut() == Participation.StatutParticipation.EN_ATTENTE)
                    .count();

            StringBuilder stats = new StringBuilder();
            stats.append("📊 Statistiques des Participations\n\n");
            stats.append("Total des participations: ").append(total).append("\n");
            stats.append("Confirmées: ").append(confirmes).append("\n");
            stats.append("En attente: ").append(enAttente);

            Alert statsAlert = new Alert(Alert.AlertType.INFORMATION);
            statsAlert.setTitle("Statistiques");
            statsAlert.setHeaderText(null);
            statsAlert.setContentText(stats.toString());
            statsAlert.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du calcul des statistiques: " + e.getMessage());
        }
    }

    private void loadParticipationInForm(Participation participation) {
        inputUserId.setText(String.valueOf(participation.getUserId()));
        inputEvenementId.setText(String.valueOf(participation.getEvenementId()));
        inputType.setValue(participation.getType());
        inputContexte.setValue(participation.getContexteSocial());
        inputHebergement.setSelected(participation.getHebergementNuits() > 0);
        inputHebergementNuits.setText(String.valueOf(participation.getHebergementNuits()));
    }

    private Participation buildParticipationFromForm() {
        Long userId = parseLong(inputUserId.getText());
        Long evtId = parseLong(inputEvenementId.getText());
        Participation.TypeParticipation type = inputType.getValue();
        Participation.ContexteSocial contexte = inputContexte.getValue();
        int nuits = parseInt(inputHebergementNuits.getText());

        if (inputHebergement.isSelected() && nuits <= 0) {
            nuits = 1;
        }

        Participation participation = new Participation(userId, evtId, type, contexte);
        participation.setHebergementNuits(nuits);

        return participation;
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

        if (inputEvenementId.getText() == null || inputEvenementId.getText().trim().isEmpty()) {
            errors.append("• Événement ID est requis\n");
        } else {
            try {
                Long.parseLong(inputEvenementId.getText().trim());
            } catch (NumberFormatException e) {
                errors.append("• Événement ID doit être un nombre valide\n");
            }
        }

        if (inputType.getValue() == null) {
            errors.append("• Type est requis\n");
        }

        if (inputContexte.getValue() == null) {
            errors.append("• Contexte est requis\n");
        }

        if (inputHebergement.isSelected()) {
            int nuits = parseInt(inputHebergementNuits.getText());
            if (nuits <= 0) {
                errors.append("• Le nombre de nuits doit être supérieur à 0 si hébergement est sélectionné\n");
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
        inputEvenementId.clear();
        inputType.getSelectionModel().selectFirst();
        inputContexte.getSelectionModel().selectFirst();
        inputHebergement.setSelected(false);
        inputHebergementNuits.clear();
    }

    private void updateButtons() {
        boolean hasSelection = selectedParticipation != null;
        boolean admin = isAdmin();
        btnModifier.setDisable(!admin || !hasSelection);
        btnSupprimer.setDisable(!admin || !hasSelection);
        btnConfirmer.setDisable(!admin || !hasSelection);
        btnAnnulerPart.setDisable(!admin || !hasSelection);
        btnModifierForm.setDisable(!admin || !hasSelection);
        // en mode utilisateur, on autorise uniquement la création de nouvelles
        // participations
        btnEnregistrer.setDisable(!admin && isEditMode);
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : data.size();
        countLabel.setText(count + " participation(s)");
    }

    private void updateStatusInfo(String message) {
        if (statusInfoLabel != null) {
            statusInfoLabel.setText(message);
        }
    }

    private boolean isAdmin() {
        return MainController.getCurrentRole() == MainController.Role.ADMIN;
    }

    private void applyRolePermissions() {
        updateButtons();
    }

    private void showDetailsDialog(Participation p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Détails de la participation\n\n");
        sb.append("ID: ").append(p.getId()).append("\n");
        sb.append("User ID: ").append(p.getUserId()).append("\n");
        sb.append("Événement ID: ").append(p.getEvenementId()).append("\n");
        sb.append("Type: ").append(p.getType()).append("\n");
        sb.append("Statut: ").append(p.getStatut()).append("\n");
        sb.append("Contexte: ").append(p.getContexteSocial()).append("\n");
        sb.append("Hébergement (nuits): ").append(p.getHebergementNuits()).append("\n");
        if (p.getBadgeAssocie() != null) {
            sb.append("Badge: ").append(p.getBadgeAssocie()).append("\n");
        }

        Alert detail = new Alert(Alert.AlertType.INFORMATION);
        detail.setTitle("Détails de la participation");
        detail.setHeaderText(null);
        detail.setContentText(sb.toString());
        detail.showAndWait();
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

    private int parseInt(String s) {
        if (s == null || s.isBlank())
            return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    /**
     * Ouvre la fenêtre dédiée de formulaire (création / modification).
     */
    private void openForm(Participation participation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation/participation-form.fxml"));
            Parent root = loader.load();

            ParticipationFormController formController = loader.getController();
            formController.setParticipationController(controller);
            formController.setParticipation(participation);
            formController.setOnSaved(this::onActualiser);
            formController.setAdminMode(isAdmin());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(participation == null ? "Nouvelle participation" : "Modifier la participation");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }
}
