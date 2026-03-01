package com.gestion.ui.menu;

import com.gestion.entities.Menu;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de détail d'un menu.
 */
public class MenuDetailsController implements Initializable {

    @FXML
    private Label restoName;
    @FXML
    private Label menuTitle;
    @FXML
    private Label menuDescription;
    @FXML
    private VBox dishesContainer;
    @FXML
    private Label validityPeriod;
    @FXML
    private ImageView qrCodeImage;

    private Menu menu;
    private final com.gestion.interfaces.RepasDetailleService dishService = new com.gestion.services.RepasDetailleServiceImpl();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
        loadDetails();
    }

    private void loadDetails() {
        if (menu == null)
            return;

        restoName.setText((menu.getRestaurantNom() != null ? menu.getRestaurantNom() : "RESTAU").toUpperCase());
        menuTitle.setText((menu.getNom() != null ? menu.getNom() : "—").toUpperCase());
        menuDescription.setText(menu.getDescription() != null && !menu.getDescription().isEmpty()
                ? menu.getDescription()
                : "Aucune description");

        String periode;
        if (menu.getDateDebut() != null && menu.getDateFin() != null) {
            periode = "VABLE DU " + menu.getDateDebut() + " AU " + menu.getDateFin();
        } else if (menu.getDateDebut() != null) {
            periode = "À PARTIR DU " + menu.getDateDebut();
        } else if (menu.getDateFin() != null) {
            periode = "JUSQU'AU " + menu.getDateFin();
        } else {
            periode = "MENU PERMANENT";
        }
        validityPeriod.setText(periode.toUpperCase());

        // Load Dishes with Categories
        dishesContainer.getChildren().clear();
        if (menu.getDishesIds() != null && !menu.getDishesIds().isEmpty()) {
            java.util.List<com.gestion.entities.RepasDetaille> allDishes = dishService.findAll();
            java.util.List<com.gestion.entities.RepasDetaille> selectedDishes = allDishes.stream()
                    .filter(d -> menu.getDishesIds().contains(d.getId()))
                    .collect(java.util.stream.Collectors.toList());

            if (selectedDishes.isEmpty()) {
                Label noDishes = new Label("AUCUN PLAT SÉLECTIONNÉ");
                noDishes.setStyle(
                        "-fx-font-family: 'Arial'; -fx-font-size: 11; -fx-text-fill: #1d3c34; -fx-opacity: 0.5;");
                dishesContainer.getChildren().add(noDishes);
            } else {
                // Group by Category
                java.util.Map<String, java.util.List<com.gestion.entities.RepasDetaille>> grouped = selectedDishes
                        .stream()
                        .collect(java.util.stream.Collectors
                                .groupingBy(d -> d.getTypeRepas() != null ? d.getTypeRepas() : "AUTRES"));

                for (java.util.Map.Entry<String, java.util.List<com.gestion.entities.RepasDetaille>> entry : grouped
                        .entrySet()) {
                    // Category Header
                    Label catHeader = new Label("— " + entry.getKey().replace("_", " ").toUpperCase() + " —");
                    catHeader.setStyle(
                            "-fx-font-family: 'Arial'; -fx-font-size: 11; -fx-text-fill: #1d3c34; -fx-font-weight: bold; -fx-letter-spacing: 2; -fx-opacity: 0.6;");
                    javafx.scene.layout.VBox.setMargin(catHeader, new javafx.geometry.Insets(10, 0, 5, 0));
                    dishesContainer.getChildren().add(catHeader);

                    for (com.gestion.entities.RepasDetaille dish : entry.getValue()) {
                        Label dishLabel = new Label(dish.getNom().toUpperCase());
                        dishLabel.setStyle(
                                "-fx-font-family: 'Georgia'; -fx-font-size: 14; -fx-text-fill: #1d3c34; -fx-font-weight: bold;");
                        dishesContainer.getChildren().add(dishLabel);
                    }
                }
            }
        }

        // Generate a extremely low-density text QR for perfect scannability
        String resto = menu.getRestaurantNom() != null ? menu.getRestaurantNom() : "CAFE RIMBERIO";
        String title = menu.getNom() != null ? menu.getNom() : "MENU";

        StringBuilder simpleMenu = new StringBuilder();
        simpleMenu.append("{ ").append(resto.toUpperCase()).append(" }\n");
        simpleMenu.append(title.toUpperCase()).append("\n---\n");

        if (menu.getDishesIds() != null && !menu.getDishesIds().isEmpty()) {
            java.util.List<com.gestion.entities.RepasDetaille> allDishes = dishService.findAll();
            // Distinct dish names only
            java.util.List<String> uniqueDishNames = allDishes.stream()
                    .filter(d -> menu.getDishesIds().contains(d.getId()))
                    .map(d -> d.getNom().toUpperCase())
                    .distinct()
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());

            for (String dishName : uniqueDishNames) {
                simpleMenu.append("• ").append(dishName).append("\n");
            }
        }

        // Exact 160x160 size (2x the 80x80 container) for pixel-perfect blocks
        javafx.scene.image.Image qrImage = com.gestion.tools.QRCodeGenerator.generateQRCode(simpleMenu.toString(), 160,
                160);
        if (qrImage != null) {
            qrCodeImage.setImage(qrImage);
        }
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) restoName.getScene().getWindow();
        stage.close();
    }
}
