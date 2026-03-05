package com.gestion.controllers;

import com.gestion.services.EventSponsorDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import com.gestion.entities.EventStats;
import com.gestion.tools.CurrencyService;

import java.net.URL;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class dashbordmouheb implements Initializable {

    @FXML
    private Label lblLastUpdate;
    @FXML
    private Label lblTotalMontant;
    @FXML
    private Label lblNbAssociations;
    @FXML
    private Label lblTopEvent;
    @FXML
    private PieChart pieParEvenement;
    @FXML
    private javafx.scene.control.TextField txtMontantTnd;
    @FXML
    private javafx.scene.control.ComboBox<String> cboDevise;
    @FXML
    private Label lblResultConversion;

    private final EventSponsorDAO eventSponsorDAO = new EventSponsorDAO();
    private java.util.Map<String, BigDecimal> ratesFromTnd = java.util.Collections.emptyMap();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chargerStats();
    }

    /**
     * Appelé à chaque affichage du Dashboard pour rafraîchir les données (ex. après
     * ajout d'une association).
     */
    public void actualiser() {
        chargerStats();
    }

    private void chargerStats() {
        try {
            List<EventStats> stats = eventSponsorDAO.getTotalsParEvenement();
            if (stats.isEmpty()) {
                lblTotalMontant.setText("Aucune donnée d'association.");
                lblNbAssociations.setText("");
                lblTopEvent.setText("");
                return;
            }

            double totalGlobal = stats.stream().mapToDouble(EventStats::getTotalMontant).sum();
            int nbAssociations = stats.stream().mapToInt(EventStats::getNbAssociations).sum();

            lblTotalMontant.setText(String.format("Total global collecté : %.2f DT", totalGlobal));
            lblNbAssociations.setText(String.format("Nombre total d'associations : %d", nbAssociations));

            EventStats top = stats.get(0);
            lblTopEvent.setText(String.format("Top événement : %s (%.2f DT, %d associations)",
                    top.getNomEvenement(), top.getTotalMontant(), top.getNbAssociations()));

            // PieChart
            pieParEvenement.getData().clear();
            for (EventStats es : stats) {
                PieChart.Data slice = new PieChart.Data(es.getNomEvenement(), es.getTotalMontant());
                pieParEvenement.getData().add(slice);
            }

            // Interaction : afficher détail au survol du camembert
            for (PieChart.Data data : pieParEvenement.getData()) {
                data.getNode().setOnMouseEntered(e -> {
                    double value = data.getPieValue();
                    double pourcentage = (value / totalGlobal) * 100.0;
                    lblTopEvent.setText(String.format("%s : %.2f DT (%.1f%% du total)",
                            data.getName(), value, pourcentage));
                });
            }

            lblLastUpdate.setText("Dernière mise à jour : " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            chargerConvertisseur();

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur Dashboard");
            alert.setHeaderText("Impossible de charger les statistiques");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void chargerConvertisseur() {
        if (txtMontantTnd == null || cboDevise == null || lblResultConversion == null) {
            return;
        }
        try {
            ratesFromTnd = CurrencyService.getRatesFromTnd();
            cboDevise.getItems().setAll(ratesFromTnd.keySet());
            if (cboDevise.getValue() == null && !cboDevise.getItems().isEmpty()) {
                // Devise par défaut
                cboDevise.setValue("EUR");
            }
            if (txtMontantTnd.getText() == null || txtMontantTnd.getText().isBlank()) {
                txtMontantTnd.setText("1");
            }

            // Listeners (ajoutés une seule fois)
            if (cboDevise.getUserData() == null) {
                cboDevise.valueProperty().addListener((obs, o, n) -> mettreAJourConversion());
                txtMontantTnd.textProperty().addListener((obs, o, n) -> mettreAJourConversion());
                cboDevise.setUserData(Boolean.TRUE);
            }

            mettreAJourConversion();
        } catch (IOException | InterruptedException e) {
            lblResultConversion.setText("Erreur API devises");
            e.printStackTrace();
        }
    }

    private void mettreAJourConversion() {
        if (ratesFromTnd == null || ratesFromTnd.isEmpty())
            return;
        String code = cboDevise.getValue();
        if (code == null || !ratesFromTnd.containsKey(code))
            return;

        try {
            double montantTnd = Double.parseDouble(txtMontantTnd.getText().trim());
            BigDecimal rate = ratesFromTnd.get(code);
            BigDecimal result = rate.multiply(BigDecimal.valueOf(montantTnd));
            lblResultConversion.setText(
                    String.format("%.2f TND = %.4f %s", montantTnd, result, code));
        } catch (NumberFormatException e) {
            lblResultConversion.setText("Montant TND invalide");
        }
    }
}
