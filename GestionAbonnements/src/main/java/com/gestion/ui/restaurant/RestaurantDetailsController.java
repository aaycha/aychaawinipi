package com.gestion.ui.restaurant;

import com.gestion.entities.Restaurant;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de détail d'un restaurant.
 */
public class RestaurantDetailsController implements Initializable {

        @FXML
        private Label detailNom;
        @FXML
        private Label detailAdresse;
        @FXML
        private Label detailTelephone;
        @FXML
        private Label detailEmail;
        @FXML
        private Label detailDescription;
        @FXML
        private Label detailStatut;
        @FXML
        private Label detailDateCreation;
        @FXML
        private Label detailImageUrl;
        @FXML
        private Label detailCapacite;
        @FXML
        private Label detailPlacesRestantes;
        @FXML
        private javafx.scene.layout.VBox vboxDishes;
        @FXML
        private javafx.scene.layout.VBox vboxMenus;
        @FXML
        private javafx.scene.layout.VBox vboxPlanning;

        private Restaurant restaurant;
        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        @Override
        public void initialize(URL url, ResourceBundle resourceBundle) {
                // Rien à initialiser sans données
        }

        public void setRestaurant(Restaurant restaurant) {
                this.restaurant = restaurant;
                loadDetails();
                loadExpandedData();
        }

        private void loadDetails() {
                if (restaurant == null)
                        return;

                detailNom.setText(restaurant.getNom() != null ? restaurant.getNom() : "—");
                detailAdresse.setText(restaurant.getAdresse() != null && !restaurant.getAdresse().isEmpty()
                                ? restaurant.getAdresse()
                                : "Non renseignée");
                detailTelephone.setText(restaurant.getTelephone() != null && !restaurant.getTelephone().isEmpty()
                                ? restaurant.getTelephone()
                                : "Non renseigné");
                detailEmail.setText(restaurant.getEmail() != null && !restaurant.getEmail().isEmpty()
                                ? restaurant.getEmail()
                                : "Non renseigné");
                detailDescription.setText(restaurant.getDescription() != null && !restaurant.getDescription().isEmpty()
                                ? restaurant.getDescription()
                                : "Aucune description");
                detailStatut.setText(restaurant.isActif() ? "Actif" : "Inactif");
                detailDateCreation.setText(restaurant.getDateCreation() != null
                                ? restaurant.getDateCreation().format(DATE_FORMAT)
                                : "—");
                detailImageUrl.setText(restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()
                                ? restaurant.getImageUrl()
                                : "Aucune image");

                detailCapacite.setText(String.valueOf(restaurant.getNombrePlaces()));

                com.gestion.interfaces.RestaurantService restService = new com.gestion.services.RestaurantServiceImpl();
                int rest = restService.getPlacesRestantes(restaurant.getId(), null);
                detailPlacesRestantes.setText(rest + " places");
        }

        private void loadExpandedData() {
                if (restaurant == null)
                        return;

                vboxDishes.getChildren().clear();
                vboxMenus.getChildren().clear();
                vboxPlanning.getChildren().clear();

                // Dishes
                com.gestion.interfaces.RepasService repasService = new com.gestion.services.RepasServiceImpl();
                java.util.List<com.gestion.entities.Repas> dishes = repasService.findByRestaurantId(restaurant.getId());
                if (dishes.isEmpty()) {
                        Label empty = new Label("No specific rations discovered at this station.");
                        empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                        vboxDishes.getChildren().add(empty);
                } else {
                        for (com.gestion.entities.Repas r : dishes) {
                                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                                Label name = new Label("• " + r.getNom());
                                name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                                Label price = new Label(String.format("%.2f €", r.getPrix()));
                                price.setStyle("-fx-text-fill: #22c55e;");
                                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                                row.getChildren().addAll(name, spacer, price);
                                vboxDishes.getChildren().add(row);
                        }
                }

                // Menus
                com.gestion.interfaces.MenuService menuService = new com.gestion.services.MenuServiceImpl();
                java.util.List<com.gestion.entities.Menu> menus = menuService.findByRestaurantId(restaurant.getId());
                if (menus.isEmpty()) {
                        Label empty = new Label("No curated mission packs scheduled.");
                        empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                        vboxMenus.getChildren().add(empty);
                } else {
                        for (com.gestion.entities.Menu m : menus) {
                                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2);
                                Label name = new Label("◈ " + m.getNom());
                                name.setStyle("-fx-text-fill: white; -fx-font-weight: 800;");
                                Label desc = new Label(m.getDescription());
                                desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                                box.getChildren().addAll(name, desc);
                                vboxMenus.getChildren().add(box);
                        }
                }

                // Planning (Mock)
                vboxPlanning.getChildren().add(createPlanningRow("Mon - Fri", "06:00 - 22:00"));
                vboxPlanning.getChildren().add(createPlanningRow("Sat - Sun", "08:00 - 00:00"));
                Label warning = new Label("⚠️ High-altitude supply drops on Tuesdays.");
                warning.setStyle("-fx-text-fill: #F97316; -fx-font-size: 11px; -fx-font-weight: bold;");
                vboxPlanning.getChildren().add(warning);
        }

        private javafx.scene.layout.HBox createPlanningRow(String day, String time) {
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                Label l1 = new Label(day);
                l1.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 80;");
                Label l2 = new Label(time);
                l2.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                row.getChildren().addAll(l1, l2);
                return row;
        }

        @FXML
        private void onFermer() {
                Stage stage = (Stage) detailNom.getScene().getWindow();
                stage.close();
        }
}
