package com.gestion.controllers;

import com.gestion.entities.Equipment;
import com.gestion.entities.Evenement;
import com.gestion.interfaces.DataReceiver;
import com.gestion.services.EquipmentService;
import com.gestion.services.EvenementService;
import com.gestion.tools.ClockPickerDialog;
import com.gestion.tools.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModifierEvenementController implements DataReceiver<Integer> {

    @FXML
    private ImageView bgImage;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtType;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextArea txtDescription;

    @FXML
    private DatePicker dpDebut;
    @FXML
    private DatePicker dpFin;

    @FXML
    private Label lblMsg;
    @FXML
    private Label lblHeureDebut;
    @FXML
    private Label lblHeureFin;
    @FXML
    private Label lblDateFin;
    @FXML
    private HBox boxDateFin;

    @FXML
    private VBox boxEquipment;
    @FXML
    private TextField txtEquipment;
    @FXML
    private FlowPane flowEquipmentSuggestions;
    @FXML
    private FlowPane flowEquipmentSelected;

    private final EvenementService evenementService = new EvenementService();
    private final EquipmentService equipmentService = new EquipmentService();

    private int eventId;
    private Evenement event;

    private final List<String> equipmentSelected = new ArrayList<>();

    private int heureDebutH = 9, heureDebutM = 0, heureFinH = 22, heureFinM = 0;

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);

        if (txtType != null) {
            txtType.textProperty().addListener((o, oldV, newV) -> {
                updateDateFinVisibility();
                // si type change, suggestions aussi
                refreshEquipmentSuggestions();
            });
        }

        if (lblMsg != null)
            lblMsg.setText("");
        updateDateFinVisibility();
        applyEquipmentVisibility();
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            if (lblMsg != null)
                lblMsg.setText("❌ ID événement invalide");
            return;
        }

        this.eventId = id;
        try {
            event = evenementService.getById(eventId);
            if (event == null) {
                if (lblMsg != null)
                    lblMsg.setText("❌ Événement introuvable");
                return;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (lblMsg != null)
                lblMsg.setText("❌ Erreur SQL: " + e.getMessage());
            return;
        }

        if (txtTitre != null)
            txtTitre.setText(safe(event.getTitre()));
        if (txtType != null)
            txtType.setText(safe(event.getType()));
        if (txtLieu != null)
            txtLieu.setText(safe(event.getLieu()));
        if (txtDescription != null)
            txtDescription.setText(safe(event.getDescription()));

        if (event.getDateDebut() != null) {
            if (dpDebut != null)
                dpDebut.setValue(event.getDateDebut().toLocalDate());
            heureDebutH = event.getDateDebut().getHour();
            heureDebutM = event.getDateDebut().getMinute();
        } else {
            if (dpDebut != null)
                dpDebut.setValue(LocalDate.now());
            heureDebutH = 9;
            heureDebutM = 0;
        }

        if (event.getDateFin() != null) {
            if (dpFin != null)
                dpFin.setValue(event.getDateFin().toLocalDate());
            heureFinH = event.getDateFin().getHour();
            heureFinM = event.getDateFin().getMinute();
        } else {
            if (dpFin != null)
                dpFin.setValue(LocalDate.now());
            heureFinH = 22;
            heureFinM = 0;
        }

        updateHeureLabels();
        updateDateFinVisibility();

        applyEquipmentVisibility();
        loadEquipmentForEdit();

        if (lblMsg != null)
            lblMsg.setText("✓ Modification: " + safe(event.getTitre()));
    }

    @FXML
    private void onEnregistrer() {
        if (event == null) {
            if (lblMsg != null)
                lblMsg.setText("❌ Aucun événement chargé");
            return;
        }

        String titre = safe(txtTitre != null ? txtTitre.getText() : "");
        String type = safe(txtType != null ? txtType.getText() : "");
        String lieu = safe(txtLieu != null ? txtLieu.getText() : "");
        String desc = safe(txtDescription != null ? txtDescription.getText() : "");

        if (titre.isBlank() || type.isBlank() || lieu.isBlank()) {
            if (lblMsg != null)
                lblMsg.setText("❌ Titre, Type et Lieu sont obligatoires");
            return;
        }

        LocalDate d1 = dpDebut != null ? dpDebut.getValue() : null;
        if (d1 == null) {
            if (lblMsg != null)
                lblMsg.setText("❌ Date début obligatoire");
            return;
        }

        LocalDateTime debut = LocalDateTime.of(d1, LocalTime.of(heureDebutH, heureDebutM));

        LocalDateTime fin;
        String typeUpper = type.toUpperCase(Locale.ROOT).trim();
        boolean hideDateFin = "SOIREE".equals(typeUpper) || "RANDONNEE".equals(typeUpper);

        if (!hideDateFin) {
            LocalDate d2 = dpFin != null ? dpFin.getValue() : null;
            if (d2 == null) {
                if (lblMsg != null)
                    lblMsg.setText("❌ Date fin obligatoire");
                return;
            }
            fin = LocalDateTime.of(d2, LocalTime.of(heureFinH, heureFinM));
            if (!fin.isAfter(debut)) {
                if (lblMsg != null)
                    lblMsg.setText("❌ Fin doit être après début");
                return;
            }
        } else {
            fin = debut.plusHours(4); // durée automatique
        }

        event.setTitre(titre);
        event.setType(type);
        event.setLieu(lieu);
        event.setDescription(desc);
        event.setDateDebut(debut);
        event.setDateFin(fin);

        try {
            evenementService.update(event);

            // ✅ seulement admin peut modifier équipements
            if (isAdmin()) {
                equipmentService.deleteByEventId(eventId);
                if (!equipmentSelected.isEmpty()) {
                    equipmentService.addAll(eventId, equipmentSelected);
                }
            }

            if (lblMsg != null)
                lblMsg.setText("✅ Modifié !");
            SceneUtil.switchToWithData("/Feryel/DetailsEvenement.fxml", "Détails Événement", eventId);

        } catch (Exception ex) {
            ex.printStackTrace();
            if (lblMsg != null)
                lblMsg.setText("❌ Erreur: " + ex.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        SceneUtil.switchToWithData("/Feryel/DetailsEvenement.fxml", "Détails Événement", eventId);
    }

    private void updateDateFinVisibility() {
        if (boxDateFin == null || lblDateFin == null)
            return;

        String type = safe(txtType != null ? txtType.getText() : "").toUpperCase(Locale.ROOT).trim();
        boolean hide = "SOIREE".equals(type) || "RANDONNEE".equals(type);

        boxDateFin.setVisible(!hide);
        boxDateFin.setManaged(!hide);
        lblDateFin.setVisible(!hide);
        lblDateFin.setManaged(!hide);

        if (dpFin != null) {
            dpFin.setDisable(hide);
            if (hide)
                dpFin.setValue(null);
        }
    }

    private void updateHeureLabels() {
        if (lblHeureDebut != null)
            lblHeureDebut.setText(String.format(Locale.ROOT, "%02d:%02d", heureDebutH, heureDebutM));
        if (lblHeureFin != null)
            lblHeureFin.setText(String.format(Locale.ROOT, "%02d:%02d", heureFinH, heureFinM));
    }

    @FXML
    private void onChoisirHeureDebut() {
        new ClockPickerDialog(heureDebutH, heureDebutM).showAndWait().ifPresent(t -> {
            heureDebutH = t.getHour();
            heureDebutM = t.getMinute();
            updateHeureLabels();
        });
    }

    @FXML
    private void onChoisirHeureFin() {
        new ClockPickerDialog(heureFinH, heureFinM).showAndWait().ifPresent(t -> {
            heureFinH = t.getHour();
            heureFinM = t.getMinute();
            updateHeureLabels();
        });
    }

    private void applyEquipmentVisibility() {
        if (boxEquipment != null) {
            boolean admin = isAdmin();
            boxEquipment.setVisible(admin);
            boxEquipment.setManaged(admin);
        }
    }

    private void loadEquipmentForEdit() {
        equipmentSelected.clear();

        List<Equipment> list = equipmentService.getByEventId(eventId);
        if (list != null) {
            for (Equipment e : list) {
                equipmentSelected.add(safe(e.getLibelle()));
            }
        }

        refreshEquipmentSuggestions();
        refreshEquipmentSelected();
    }

    private void refreshEquipmentSuggestions() {
        if (flowEquipmentSuggestions == null)
            return;

        flowEquipmentSuggestions.getChildren().clear();
        String type = safe(txtType != null ? txtType.getText() : "");

        for (String s : EquipmentService.getSuggestionsByType(type)) {
            Button btn = new Button("+ " + s);
            btn.getStyleClass().add("chip");
            btn.setOnAction(e -> addEquipmentItem(s));
            flowEquipmentSuggestions.getChildren().add(btn);
        }
    }

    private void refreshEquipmentSelected() {
        if (flowEquipmentSelected == null)
            return;

        flowEquipmentSelected.getChildren().clear();
        for (String lib : equipmentSelected) {
            Button chip = new Button("✕ " + lib);
            chip.getStyleClass().add("chip");
            chip.setOnAction(e -> removeEquipmentItem(lib));
            flowEquipmentSelected.getChildren().add(chip);
        }
    }

    @FXML
    private void onAddEquipment() {
        String custom = txtEquipment != null ? safe(txtEquipment.getText()) : "";
        if (!custom.isBlank()) {
            addEquipmentItem(custom);
            if (txtEquipment != null)
                txtEquipment.clear();
        }
    }

    private void addEquipmentItem(String libelle) {
        if (libelle == null || libelle.trim().isBlank())
            return;
        String lib = libelle.trim();
        if (equipmentSelected.contains(lib))
            return;
        equipmentSelected.add(lib);
        refreshEquipmentSelected();
    }

    private void removeEquipmentItem(String libelle) {
        equipmentSelected.remove(libelle);
        refreshEquipmentSelected();
    }

    // ✅ Remplace Session.isAdmin() (qui n’existe pas)
    private boolean isAdmin() {
        if (Session.getInstance() == null)
            return false;
        if (Session.getInstance().getCurrentUser() == null)
            return false;
        String role = Session.getInstance().getCurrentUser().getRole();
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}