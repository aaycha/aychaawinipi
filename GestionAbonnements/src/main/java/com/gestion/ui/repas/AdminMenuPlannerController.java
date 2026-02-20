package com.gestion.ui.repas;

import com.gestion.controllers.RepasController;
import com.gestion.entities.Repas;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le planificateur de menus admin
 */
public class AdminMenuPlannerController implements Initializable {

    @FXML
    private GridPane plannerGrid;
    @FXML
    private Label currentWeekLabel;
    @FXML
    private VBox dishPalette;
    @FXML
    private ListView<Repas> availableDishesList;

    private final RepasController controller = new RepasController();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupPlannerGrid();
        loadAvailableDishes();
    }

    private void setupPlannerGrid() {
        // Logic to populate the grid with slots (Breakfast, Lunch, Dinner)
        for (int row = 1; row <= 3; row++) {
            for (int col = 0; col < 7; col++) {
                VBox slot = new VBox(5);
                slot.getStyleClass().add("glass-panel-light");
                slot.setStyle("-fx-min-height: 100; -fx-padding: 10; -fx-background-color: rgba(255,255,255,0.5);");

                String timeLabel = (row == 1) ? "Petit-Déjeuner" : (row == 2) ? "Déjeuner" : "Dîner";
                Label label = new Label(timeLabel);
                label.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
                slot.getChildren().add(label);

                plannerGrid.add(slot, col, row);
            }
        }
    }

    private void loadAvailableDishes() {
        if (availableDishesList != null) {
            availableDishesList.getItems().setAll(controller.getAllRepas());
        }
    }

    @FXML
    private void onBack() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        }
    }
}
