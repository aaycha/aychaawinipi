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

    @FXML private Label detailNom;
    @FXML private Label detailAdresse;
    @FXML private Label detailTelephone;
    @FXML private Label detailEmail;
    @FXML private Label detailDescription;
    @FXML private Label detailStatut;
    @FXML private Label detailDateCreation;
    @FXML private Label detailImageUrl;

    private Restaurant restaurant;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Rien à initialiser sans données
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
        loadDetails();
    }

    private void loadDetails() {
        if (restaurant == null) return;

        detailNom.setText(restaurant.getNom() != null ? restaurant.getNom() : "—");
        detailAdresse.setText(restaurant.getAdresse() != null && !restaurant.getAdresse().isEmpty()
                ? restaurant.getAdresse() : "Non renseignée");
        detailTelephone.setText(restaurant.getTelephone() != null && !restaurant.getTelephone().isEmpty()
                ? restaurant.getTelephone() : "Non renseigné");
        detailEmail.setText(restaurant.getEmail() != null && !restaurant.getEmail().isEmpty()
                ? restaurant.getEmail() : "Non renseigné");
        detailDescription.setText(restaurant.getDescription() != null && !restaurant.getDescription().isEmpty()
                ? restaurant.getDescription() : "Aucune description");
        detailStatut.setText(restaurant.isActif() ? "Actif" : "Inactif");
        detailDateCreation.setText(restaurant.getDateCreation() != null
                ? restaurant.getDateCreation().format(DATE_FORMAT) : "—");
        detailImageUrl.setText(restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()
                ? restaurant.getImageUrl() : "Aucune image");
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) detailNom.getScene().getWindow();
        stage.close();
    }
}
