package com.gestion.ui.repas;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le tableau de bord KPI Admin
 */
public class AdminKPIDashboardController implements Initializable {

    @FXML
    private LineChart<String, Number> orderChart;
    @FXML
    private PieChart categoryChart;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCharts();
    }

    private void setupCharts() {
        // Mock data for LineChart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Commandes");
        series.getData().add(new XYChart.Data<>("Lun", 85));
        series.getData().add(new XYChart.Data<>("Mar", 92));
        series.getData().add(new XYChart.Data<>("Mer", 78));
        series.getData().add(new XYChart.Data<>("Jeu", 110));
        series.getData().add(new XYChart.Data<>("Ven", 145));
        series.getData().add(new XYChart.Data<>("Sam", 120));
        series.getData().add(new XYChart.Data<>("Dim", 95));

        if (orderChart != null) {
            orderChart.getData().add(series);
        }

        // Mock data for PieChart
        if (categoryChart != null) {
            categoryChart.getData().add(new PieChart.Data("Signature", 45));
            categoryChart.getData().add(new PieChart.Data("Bowl Builder", 30));
            categoryChart.getData().add(new PieChart.Data("Accompagnements", 15));
            categoryChart.getData().add(new PieChart.Data("Boissons", 10));
        }
    }

    @FXML
    private void onBack() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        }
    }
}
