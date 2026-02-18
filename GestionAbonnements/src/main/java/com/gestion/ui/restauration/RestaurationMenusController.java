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
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

import java.util.List;
import java.util.stream.Collectors;

public class RestaurationMenusController {

    @FXML
    private ListView<Restauration> listView;

    @FXML
    private ComboBox<Long> filterOptionCombo;
    @FXML
    private TextField searchField;

    @FXML
    private TextField inputNom;
    @FXML
    private TextField inputOptionId;
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
    private final ObservableList<Restauration> menusData = FXCollections.observableArrayList();
    private FilteredList<Restauration> filteredData;
    private Restauration selectedMenu = null;
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
                    setGraphic(createMenuCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });
    }

    private javafx.scene.Node createMenuCard(Restauration item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon based on activity
        Text icon = new Text(item.isActif() ? "🍽️" : "🌑");
        icon.setFont(Font.font("Segoe UI Emoji", 24));

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(item.getNom() != null ? item.getNom() : "Sans nom");
        nameLabel.getStyleClass().add("card-title");

        Label statusBadge = new Label(item.isActif() ? "ACTIF" : "INACTIF");
        statusBadge.getStyleClass().addAll("status-badge", item.isActif() ? "ACTIF" : "ANNULE"); // ACTIF/ANNULE mapping
                                                                                                 // to existing CSS
                                                                                                 // styles

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(nameLabel, spacer, statusBadge);

        HBox details = new HBox(15);
        Label idLabel = new Label("ID: " + item.getId());
        idLabel.getStyleClass().add("card-label");

        Label optLabel = new Label(
                "Option ID: " + (item.getOptionRestaurationId() != null ? item.getOptionRestaurationId() : "N/A"));
        optLabel.getStyleClass().add("card-value");

        details.getChildren().addAll(idLabel, optLabel);

        content.getChildren().addAll(header, details);
        card.getChildren().addAll(icon, content);

        return card;
    }

    @FXML
    public void initialize() {
        try {
            setupListView();

            listView.setItems(menusData);
            listView.setVisible(true);
            listView.setManaged(true);

            filteredData = new FilteredList<>(menusData, p -> true);
            listView.setItems(filteredData);

            inputNom.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

            onActualiser();
            updateCount();
            updateButtons();
        } catch (Exception e) {
            System.err.println("Erreur initialisation RestaurationMenusController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onNouveauMenu() {
        selectedMenu = null;
        isEditMode = false;
        clearForm();
        updateButtons();
        hideValidationMessage();
    }

    @FXML
    void onActualiser() {
        try {
            List<Restauration> menus = controller.getMenusActifs();
            Platform.runLater(() -> {
                menusData.clear();
                menusData.addAll(menus);

                filterOptionCombo.getItems().clear();
                filterOptionCombo.getItems().addAll(menus.stream()
                        .map(Restauration::getOptionRestaurationId)
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .collect(Collectors.toList()));

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
    void onFiltrerOption() {
        applyFilters();
    }

    @FXML
    void onRechercher() {
        applyFilters();
    }

    private void applyFilters() {
        Long optId = filterOptionCombo.getValue();
        String searchText = searchField != null && searchField.getText() != null ? searchField.getText().toLowerCase()
                : "";

        filteredData.setPredicate(menu -> {
            boolean match = true;

            if (optId != null) {
                match = match && optId.equals(menu.getOptionRestaurationId());
            }

            if (!searchText.isEmpty()) {
                String searchable = menu.getId() + " " +
                        (menu.getNom() != null ? menu.getNom() : "");
                match = match && searchable.toLowerCase().contains(searchText);
            }

            return match;
        });

        updateCount();
    }

    @FXML
    void onListClick(MouseEvent event) {
        selectedMenu = listView.getSelectionModel().getSelectedItem();
        if (selectedMenu != null) {
            loadMenuInForm(selectedMenu);
            updateButtons();
        }
    }

    @FXML
    void onEnregistrer() {
        if (!validateForm())
            return;

        try {
            String nom = inputNom.getText();
            Long optId = parseLong(inputOptionId.getText());
            boolean actif = inputActif.isSelected();

            if (isEditMode && selectedMenu != null) {
                selectedMenu.setNom(nom);
                selectedMenu.setOptionRestaurationId(optId);
                selectedMenu.setActif(actif);
                controller.updateMenu(selectedMenu);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu modifié avec succès");
            } else {
                controller.createMenu(nom, optId, actif);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu créé avec succès");
            }

            onActualiser();
            onNouveauMenu();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    void onModifier() {
        if (selectedMenu == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un menu à modifier");
            return;
        }
        isEditMode = true;
        loadMenuInForm(selectedMenu);
        updateButtons();
        showValidationMessage("Mode édition activé", "info");
    }

    @FXML
    void onSupprimer() {
        if (selectedMenu == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner un menu à supprimer");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer ce menu ?");
        confirmAlert.setContentText("ID: " + selectedMenu.getId() + "\nNom: " + selectedMenu.getNom());
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = controller.deleteMenu(selectedMenu.getId());
                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu supprimé avec succès");
                    onActualiser();
                    onNouveauMenu();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer ce menu");
                }
            }
        });
    }

    @FXML
    void onAnnuler() {
        onNouveauMenu();
    }

    private void loadMenuInForm(Restauration menu) {
        inputNom.setText(menu.getNom() != null ? menu.getNom() : "");
        inputOptionId
                .setText(menu.getOptionRestaurationId() != null ? String.valueOf(menu.getOptionRestaurationId()) : "");
        inputActif.setSelected(menu.isActif());
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (inputNom.getText() == null || inputNom.getText().trim().isEmpty()) {
            errors.append("• Nom est requis\n");
        }
        if (errors.length() > 0) {
            showValidationMessage(errors.toString(), "error");
            return false;
        }
        hideValidationMessage();
        return true;
    }

    private void clearForm() {
        inputNom.clear();
        inputOptionId.clear();
        inputActif.setSelected(true);
    }

    private void updateButtons() {
        boolean hasSelection = selectedMenu != null;
        boolean admin = isAdmin();
        btnModifier.setDisable(!admin || !hasSelection);
        btnSupprimer.setDisable(!admin || !hasSelection);
        btnModifierForm.setDisable(!admin || !hasSelection);
    }

    private boolean isAdmin() {
        return MainController.getCurrentRole() == MainController.Role.ADMIN;
    }

    private void updateCount() {
        int count = filteredData != null ? filteredData.size() : menusData.size();
        countLabel.setText(count + " menu(s)");
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
