package com.gestion.ui.restauration;

import com.gestion.controllers.RestaurationController;
import com.gestion.entities.Restauration;
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
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.List;

/**
 * Contrôleur de la page liste des options de restauration uniquement.
 * Le formulaire CRUD s'ouvre dans une fenêtre séparée (pas sur la même page).
 */
public class RestaurationOptionsListeController {

    @FXML
    private ListView<Restauration> listView;
    @FXML
    private TextField filterTypeField;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Label countLabel;

    private final RestaurationController controller = new RestaurationController();
    private final ObservableList<Restauration> optionsData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedOption = null;

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
                    setGraphic(createOptionCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createOptionCard(Restauration item) {
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
        Label libelleLabel = new Label(item.getLibelle() != null ? item.getLibelle() : "Sans libellé");
        libelleLabel.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadge.getStyleClass().addAll("status-badge", item.isActif() ? "ACTIF" : "ANNULE");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(libelleLabel, spacer, statusBadge);

        HBox details = new HBox(15);
        Label idLabel = new Label("ID: " + item.getId());
        idLabel.getStyleClass().add("card-label");

        Label typeLabel = new Label("Type: " + (item.getTypeEvenement() != null ? item.getTypeEvenement() : "N/A"));
        typeLabel.getStyleClass().add("card-value");

        details.getChildren().addAll(idLabel, typeLabel);

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(icon, content);

        return card;
    }

    @FXML
    public void initialize() {
        setupListView();

        filteredData = new FilteredList<>(optionsData, p -> true);
        listView.setItems(filteredData);

        onActualiser(); // appel initial
        updateCount();
        updateButtons();
    }

    @FXML
    void onNouvelleOption() {
        openForm(null);
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
                updateCount();
                listView.refresh();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du filtrage : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String typeFilter = filterTypeField.getText().trim().toLowerCase();
        filteredData.setPredicate(option -> {
            if (typeFilter.isEmpty())
                return true;
            String optionType = option.getTypeEvenement() != null ? option.getTypeEvenement().toLowerCase() : "";
            return optionType.contains(typeFilter);
        });
        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedOption = listView.getSelectionModel().getSelectedItem();
        updateButtons();
    }

    @FXML
    void onModifier() {
        if (selectedOption != null) {
            openForm(selectedOption);
        } else {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une option à modifier.");
        }
    }

    @FXML
    void onSupprimer() {
        if (selectedOption == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une option à supprimer.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer cette option ?");
        confirmAlert.setContentText("ID: " + selectedOption.getId() + "\nLibellé: " + selectedOption.getLibelle());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO : implémenter la vraie suppression
                // controller.deleteOption(selectedOption.getId());
                showAlert(Alert.AlertType.INFORMATION, "Info",
                        "La suppression nécessite une implémentation dans le service.");
                onActualiser();
                selectedOption = null;
                updateButtons();
            }
        });
    }

    private void updateButtons() {
        boolean hasSelection = selectedOption != null;
        if (btnModifier != null)
            btnModifier.setDisable(!hasSelection);
        if (btnSupprimer != null)
            btnSupprimer.setDisable(!hasSelection);
    }

    private void updateCount() {
        int count = (filteredData != null) ? filteredData.size() : optionsData.size();
        if (countLabel != null) {
            countLabel.setText(count + " option(s)");
        }
    }

    private void openForm(Restauration option) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/restauration/restauration-options-form.fxml"));
            Parent root = loader.load();
            RestaurationOptionsFormController formController = loader.getController();

            formController.setOption(option);
            formController.setOnSaved(this::onActualiser);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(option == null ? "Nouvelle option de restauration" : "Modifier l'option");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Rafraîchissement après fermeture du formulaire
            onActualiser();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}