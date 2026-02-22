package com.gestion.ui.evenement;

import com.gestion.controllers.EvenementDAO;
import com.gestion.entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EvenementFormController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField locationField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Label mainTitle;

    private Evenement event;
    private EvenementViewController parent;
    private final EvenementDAO dao = new EvenementDAO();

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
        typeCombo.setValue("SOIREE");
    }

    public void setEvenement(Evenement e) {
        this.event = e;
        if (e != null) {
            mainTitle.setText("Modifier l'événement");
            titleField.setText(e.getTitre());
            descField.setText(e.getDescription());
            typeCombo.setValue(e.getType());
            locationField.setText(e.getLieu());
            if (e.getDateDebut() != null)
                startDatePicker.setValue(e.getDateDebut().toLocalDate());
            if (e.getDateFin() != null)
                endDatePicker.setValue(e.getDateFin().toLocalDate());
        }
    }

    public void setParentController(EvenementViewController p) {
        this.parent = p;
    }

    @FXML
    public void onSave() {
        if (titleField.getText().isEmpty())
            return;

        if (event == null)
            event = new Evenement();
        event.setTitre(titleField.getText());
        event.setDescription(descField.getText());
        event.setType(typeCombo.getValue());
        event.setLieu(locationField.getText());

        if (startDatePicker.getValue() != null) {
            event.setDateDebut(LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(8, 0)));
        }
        if (endDatePicker.getValue() != null) {
            event.setDateFin(LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(18, 0)));
        }

        try {
            if (event.getIdEvent() > 0) {
                dao.update(event);
            } else {
                dao.add(event);
            }
            if (parent != null)
                parent.onActualiser();
            onCancel();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onCancel() {
        ((Stage) titleField.getScene().getWindow()).close();
    }
}
