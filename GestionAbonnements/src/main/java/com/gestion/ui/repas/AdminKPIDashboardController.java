package com.gestion.ui.repas;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le tableau de bord KPI Admin - Version 2026 (Enhanced)
 */
public class AdminKPIDashboardController implements Initializable {

    @FXML
    private LineChart<String, Number> orderChart;
    @FXML
    private PieChart categoryChart;
    @FXML
    private BarChart<String, Number> restaurantChart;
    @FXML
    private AreaChart<String, Number> activityChart;

    @FXML
    private Label todayOrdersLabel;
    @FXML
    private Label estimatedRevenueLabel;
    @FXML
    private Label satisfactionLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCharts();
        animateChartsEntry();
    }

    @FXML
    private void onRefresh() {
        setupCharts();
        animateChartsEntry();
    }

    private void setupCharts() {
        // Reset and populate Today's Stats
        if (todayOrdersLabel != null)
            todayOrdersLabel.setText("142");
        if (estimatedRevenueLabel != null)
            estimatedRevenueLabel.setText("2,180 €");
        if (satisfactionLabel != null)
            satisfactionLabel.setText("4.9/5");

        // 1. LineChart: Activity Trends
        XYChart.Series<String, Number> subsSeries = new XYChart.Series<>();
        subsSeries.setName("Abonnements");
        String[] days = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };
        int[] subData = { 5, 12, 8, 15, 22, 18, 10 };
        for (int i = 0; i < days.length; i++)
            subsSeries.getData().add(new XYChart.Data<>(days[i], subData[i]));

        XYChart.Series<String, Number> partSeries = new XYChart.Series<>();
        partSeries.setName("Participations");
        int[] partData = { 20, 25, 15, 30, 45, 55, 40 };
        for (int i = 0; i < days.length; i++)
            partSeries.getData().add(new XYChart.Data<>(days[i], partData[i]));

        if (orderChart != null) {
            orderChart.getData().clear();
            orderChart.getData().addAll(subsSeries, partSeries);
        }

        // 2. PieChart: System Distribution
        if (categoryChart != null) {
            categoryChart.getData().clear();
            categoryChart.getData().add(new PieChart.Data("Utilisateurs", 124));
            categoryChart.getData().add(new PieChart.Data("Abonnements", 85));
            categoryChart.getData().add(new PieChart.Data("Restaurants", 14));
            categoryChart.getData().add(new PieChart.Data("Plats", 52));
        }

        // 3. BarChart: Top Restaurants (NEW)
        XYChart.Series<String, Number> restoSeries = new XYChart.Series<>();
        restoSeries.setName("Ventes");
        restoSeries.getData().add(new XYChart.Data<>("Le Gourmet", 150));
        restoSeries.getData().add(new XYChart.Data<>("Saveurs d'Asie", 120));
        restoSeries.getData().add(new XYChart.Data<>("Pasta Palace", 95));
        restoSeries.getData().add(new XYChart.Data<>("Burger Bloom", 135));
        restoSeries.getData().add(new XYChart.Data<>("Green Garden", 80));

        if (restaurantChart != null) {
            restaurantChart.getData().clear();
            restaurantChart.getData().add(restoSeries);
        }

        // 4. AreaChart: Hourly Peaks (NEW)
        XYChart.Series<String, Number> peakSeries = new XYChart.Series<>();
        peakSeries.setName("Trafic");
        String[] hours = { "11h", "12h", "13h", "14h", "18h", "19h", "20h", "21h" };
        int[] peakData = { 30, 110, 145, 70, 40, 95, 130, 85 };
        for (int i = 0; i < hours.length; i++)
            peakSeries.getData().add(new XYChart.Data<>(hours[i], peakData[i]));

        if (activityChart != null) {
            activityChart.getData().clear();
            activityChart.getData().add(peakSeries);
        }
    }

    private void animateChartsEntry() {
        Node[] charts = { orderChart, categoryChart, restaurantChart, activityChart };
        for (int i = 0; i < charts.length; i++) {
            if (charts[i] != null) {
                charts[i].setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(800), charts[i]);
                ft.setFromValue(0);
                ft.setToValue(1);

                PauseTransition pt = new PauseTransition(Duration.millis(i * 200));
                SequentialTransition st = new SequentialTransition(pt, ft);
                st.play();
            }
        }
    }

    @FXML
    private void onBack() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        }
    }
}
