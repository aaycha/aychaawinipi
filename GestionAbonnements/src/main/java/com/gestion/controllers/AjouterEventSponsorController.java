package com.gestion.controllers;

import com.gestion.entities.Evenement;
import com.gestion.entities.EventSponsor;
import com.gestion.entities.Sponsor;
import com.gestion.interfaces.DataReceiver;
import com.gestion.services.EvenementService;
import com.gestion.services.EventSponsorDAO;
import com.gestion.tools.MyConnection;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AjouterEventSponsorController implements DataReceiver<EventSponsor> {

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblMessage;
    @FXML
    private ComboBox<Evenement> cboEvenement;
    @FXML
    private ComboBox<Sponsor> cboSponsor;
    @FXML
    private ComboBox<String> cboNiveau;
    @FXML
    private TextField txtMontant;
    @FXML
    private Button btnSubmit;

    private final EvenementService evenementService = new EvenementService();
    private final EventSponsorDAO eventSponsorDAO = new EventSponsorDAO();

    private EventSponsor editingAssociation = null;

    @FXML
    public void initialize() {
        // No background image adjustment needed for the new layout

        cboNiveau.setItems(FXCollections.observableArrayList("GOLD", "SILVER", "BRONZE", "PARTENAIRE"));

        setupComboBoxes();
        loadData();
    }

    private void setupComboBoxes() {
        cboEvenement.setConverter(new StringConverter<Evenement>() {
            @Override
            public String toString(Evenement e) {
                return (e == null) ? "" : e.getTitre();
            }

            @Override
            public Evenement fromString(String string) {
                return null;
            }
        });

        cboSponsor.setConverter(new StringConverter<Sponsor>() {
            @Override
            public String toString(Sponsor s) {
                return (s == null) ? "" : s.getNom();
            }

            @Override
            public Sponsor fromString(String string) {
                return null;
            }
        });
    }

    private void loadData() {
        try {
            // Load Events
            List<Evenement> events = evenementService.getAll();
            cboEvenement.setItems(FXCollections.observableArrayList(events));

            // Load Sponsors
            List<Sponsor> sponsors = new ArrayList<>();
            String query = "SELECT * FROM Sponsor WHERE statut = true ORDER BY nom";
            try (Connection conn = MyConnection.getConnectionStatic();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    sponsors.add(new Sponsor(rs.getInt("id"), rs.getString("nom"), rs.getString("telephone"),
                            rs.getString("email"), rs.getString("logo"), rs.getBoolean("statut")));
                }
            }
            cboSponsor.setItems(FXCollections.observableArrayList(sponsors));

        } catch (SQLException e) {
            showError("❌ Erreur lors du chargement des données.");
            e.printStackTrace();
        }
    }

    @Override
    public void setData(EventSponsor data) {
        this.editingAssociation = data;
        if (data != null) {
            lblTitle.setText("Modifier l'Association");
            btnSubmit.setText("Modifier");

            // Select corresponding event
            for (Evenement e : cboEvenement.getItems()) {
                if (e.getTitre().equals(data.getNomEvenement())) {
                    cboEvenement.getSelectionModel().select(e);
                    break;
                }
            }
            // Select corresponding sponsor
            for (Sponsor s : cboSponsor.getItems()) {
                if (s.getNom().equals(data.getNomSponsor())) {
                    cboSponsor.getSelectionModel().select(s);
                    break;
                }
            }

            cboNiveau.getSelectionModel().select(data.getNiveau());
            txtMontant.setText(String.valueOf(data.getMontant()));

            // In edit mode, usually we don't allow changing event/sponsor if it's a primary
            // key in some DB designs,
            // but here we have an 'id' for the association, so we can allow it or disable
            // it.
            // For safety, let's disable them if you want to be strict, but I'll leave them
            // enabled.
        }
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        Evenement ev = cboEvenement.getValue();
        Sponsor sp = cboSponsor.getValue();
        String niveau = cboNiveau.getValue();
        String montantStr = safe(txtMontant.getText());

        if (ev == null || sp == null || niveau == null || montantStr.isEmpty()) {
            showError("❌ Veuillez remplir tous les champs.");
            return;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantStr);
        } catch (NumberFormatException e) {
            showError("❌ Montant invalide.");
            return;
        }

        try {
            if (editingAssociation == null) {
                // Check if already exists
                if (eventSponsorDAO.existe(ev.getIdEvent(), sp.getId())) {
                    showError("❌ Ce sponsor est déjà associé à cet événement.");
                    return;
                }

                EventSponsor es = new EventSponsor();
                es.setEventId(ev.getIdEvent());
                es.setSponsorId(sp.getId());
                es.setNiveau(niveau);
                es.setMontant(montant);

                eventSponsorDAO.ajouter(es);
                showSuccess("✅ Association créée avec succès !");
            } else {
                editingAssociation.setEventId(ev.getIdEvent());
                editingAssociation.setSponsorId(sp.getId());
                editingAssociation.setNiveau(niveau);
                editingAssociation.setMontant(montant);

                eventSponsorDAO.modifier(editingAssociation);
                showSuccess("✅ Association modifiée avec succès !");
            }

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(this::onRetour);
            }).start();

        } catch (SQLException e) {
            showError("❌ Erreur base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        DashboardController.getInstance().loadModule("/views/EventSponsorView.fxml");
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("error", "success");
        lblMessage.getStyleClass().add("success");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("success", "error");
        lblMessage.getStyleClass().add("error");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
