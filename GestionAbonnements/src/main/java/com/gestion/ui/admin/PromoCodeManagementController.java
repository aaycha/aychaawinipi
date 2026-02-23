package com.gestion.ui.admin;

import com.gestion.entities.PromoCode;
import com.gestion.services.PromoCodeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class PromoCodeManagementController {

    @FXML
    private TableView<PromoCode> tableView;
    @FXML
    private TableColumn<PromoCode, String> colCode;
    @FXML
    private TableColumn<PromoCode, Integer> colDiscount;
    @FXML
    private TableColumn<PromoCode, LocalDate> colExpiry;
    @FXML
    private TableColumn<PromoCode, Boolean> colActive;
    @FXML
    private TableColumn<PromoCode, Integer> colUsage;

    @FXML
    private TextField inputCode;
    @FXML
    private TextField inputDiscount;
    @FXML
    private DatePicker inputExpiry;
    @FXML
    private TextField inputUsageLimit;
    @FXML
    private CheckBox inputActive;

    private final PromoCodeService service = new PromoCodeService();
    private final ObservableList<PromoCode> data = FXCollections.observableArrayList();
    private PromoCode selectedPromo = null;

    @FXML
    public void initialize() {
        colCode.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCode()));
        colDiscount.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getDiscountPercentage()));
        colExpiry.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getExpirationDate()));
        colActive.setCellValueFactory(
                cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().isActive()));
        colUsage.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(
                cell.getValue().getCurrentUsage()));

        tableView.setItems(data);
        onActualiser();

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPromo = newVal;
            if (newVal != null)
                loadPromoInForm(newVal);
        });
    }

    @FXML
    public void onActualiser() {
        try {
            data.setAll(service.getAll());
        } catch (SQLException e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    void onEnregistrer() {
        if (inputCode.getText().isEmpty() || inputDiscount.getText().isEmpty())
            return;

        try {
            PromoCode p = (selectedPromo != null) ? selectedPromo : new PromoCode();
            p.setCode(inputCode.getText().trim());
            p.setDiscountPercentage(Integer.parseInt(inputDiscount.getText().trim()));
            p.setExpirationDate(inputExpiry.getValue());
            p.setUsageLimit(Integer.parseInt(inputUsageLimit.getText().trim()));
            p.setActive(inputActive.isSelected());

            if (selectedPromo == null) {
                service.create(p);
            } else {
                service.update(p);
            }
            onActualiser();
            onNouveau();
        } catch (Exception e) {
            showError("Erreur d'enregistrement", e.getMessage());
        }
    }

    @FXML
    void onSupprimer() {
        if (selectedPromo == null)
            return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le code " + selectedPromo.getCode() + " ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.delete(selectedPromo.getId());
                onActualiser();
                onNouveau();
            } catch (SQLException e) {
                showError("Erreur de suppression", e.getMessage());
            }
        }
    }

    @FXML
    void onNouveau() {
        selectedPromo = null;
        inputCode.clear();
        inputDiscount.clear();
        inputExpiry.setValue(LocalDate.now().plusMonths(1));
        inputUsageLimit.setText("100");
        inputActive.setSelected(true);
        tableView.getSelectionModel().clearSelection();
    }

    private void loadPromoInForm(PromoCode p) {
        inputCode.setText(p.getCode());
        inputDiscount.setText(String.valueOf(p.getDiscountPercentage()));
        inputExpiry.setValue(p.getExpirationDate());
        inputUsageLimit.setText(String.valueOf(p.getUsageLimit()));
        inputActive.setSelected(p.isActive());
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
