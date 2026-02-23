package com.gestion.ui.admin;

import com.gestion.entities.Ingredient;
import com.gestion.services.IngredientServiceImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.util.Optional;

public class IngredientManagementController {

    @FXML
    private TableView<Ingredient> tableView;
    @FXML
    private TableColumn<Ingredient, String> colNom;
    @FXML
    private TableColumn<Ingredient, String> colCategorie;
    @FXML
    private TableColumn<Ingredient, Integer> colStock;
    @FXML
    private TableColumn<Ingredient, Integer> colSeuil;
    @FXML
    private TableColumn<Ingredient, Boolean> colActif;

    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<Ingredient.Categorie> inputCategorie;
    @FXML
    private TextField inputStock;
    @FXML
    private TextField inputSeuil;
    @FXML
    private TextField inputPrix;
    @FXML
    private CheckBox inputActif;

    private final IngredientServiceImpl service = new IngredientServiceImpl();
    private final ObservableList<Ingredient> data = FXCollections.observableArrayList();
    private Ingredient selectedIngredient = null;

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getNom()));
        colCategorie.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCategorie().name()));
        colStock.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getStockQuantite()));
        colSeuil.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getStockSeuilAlerte()));
        colActif.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().isActif()));

        inputCategorie.getItems().addAll(Ingredient.Categorie.values());

        tableView.setItems(data);
        onActualiser();

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedIngredient = newVal;
            if (newVal != null)
                loadInForm(newVal);
        });

        // Cell coloring for low stock
        colStock.setCellFactory(column -> new TableCell<Ingredient, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    Ingredient ing = getTableView().getItems().get(getIndex());
                    if (item <= ing.getStockSeuilAlerte()) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    @FXML
    public void onActualiser() {
        data.setAll(service.findAll());
    }

    @FXML
    void onEnregistrer() {
        if (inputNom.getText().isEmpty() || inputCategorie.getValue() == null)
            return;

        try {
            Ingredient i = (selectedIngredient != null) ? selectedIngredient : new Ingredient();
            i.setNom(inputNom.getText().trim());
            i.setCategorie(inputCategorie.getValue());
            i.setStockQuantite(Integer.parseInt(inputStock.getText().trim()));
            i.setStockSeuilAlerte(Integer.parseInt(inputSeuil.getText().trim()));
            i.setPrixSupplement(new BigDecimal(inputPrix.getText().trim()));
            i.setActif(inputActif.isSelected());

            if (selectedIngredient == null) {
                service.create(i);
            } else {
                service.update(i);
            }
            onActualiser();
            onNouveau();
        } catch (Exception e) {
            showError("Erreur d'enregistrement", e.getMessage());
        }
    }

    @FXML
    void onSupprimer() {
        if (selectedIngredient == null)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'ingrédient " + selectedIngredient.getNom() + " ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.delete(selectedIngredient.getId());
            onActualiser();
            onNouveau();
        }
    }

    @FXML
    void onNouveau() {
        selectedIngredient = null;
        inputNom.clear();
        inputCategorie.getSelectionModel().clearSelection();
        inputStock.setText("100");
        inputSeuil.setText("10");
        inputPrix.setText("0.00");
        inputActif.setSelected(true);
        tableView.getSelectionModel().clearSelection();
    }

    private void loadInForm(Ingredient i) {
        inputNom.setText(i.getNom());
        inputCategorie.setValue(i.getCategorie());
        inputStock.setText(String.valueOf(i.getStockQuantite()));
        inputSeuil.setText(String.valueOf(i.getStockSeuilAlerte()));
        inputPrix.setText(i.getPrixSupplement().toString());
        inputActif.setSelected(i.isActif());
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
