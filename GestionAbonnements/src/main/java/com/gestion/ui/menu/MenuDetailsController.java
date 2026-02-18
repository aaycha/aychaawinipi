package com.gestion.ui.menu;

import com.gestion.entities.Menu;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de détail d'un menu.
 */
public class MenuDetailsController implements Initializable {

    @FXML private Label detailNom;
    @FXML private Label detailRestaurant;
    @FXML private Label detailPrix;
    @FXML private Label detailDescription;
    @FXML private Label detailPeriode;
    @FXML private Label detailStatut;
    @FXML private Label detailCreatedAt;
    @FXML private Label detailUpdatedAt;

    private Menu menu;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
        loadDetails();
    }

    private void loadDetails() {
        if (menu == null) return;

        detailNom.setText(menu.getNom() != null ? menu.getNom() : "—");
        detailRestaurant.setText(menu.getRestaurantNom() != null && !menu.getRestaurantNom().isEmpty()
                ? menu.getRestaurantNom() : "Non renseigné");
        detailPrix.setText(menu.getPrix() != null ? String.format("%.2f €", menu.getPrix()) : "—");
        detailDescription.setText(menu.getDescription() != null && !menu.getDescription().isEmpty()
                ? menu.getDescription() : "Aucune description");

        String periode;
        if (menu.getDateDebut() != null && menu.getDateFin() != null) {
            periode = menu.getDateDebut() + " → " + menu.getDateFin();
        } else if (menu.getDateDebut() != null) {
            periode = "À partir du " + menu.getDateDebut();
        } else if (menu.getDateFin() != null) {
            periode = "Jusqu'au " + menu.getDateFin();
        } else {
            periode = "Permanent";
        }
        detailPeriode.setText(periode);

        detailStatut.setText(menu.isActif() ? "Actif" : "Inactif");
        detailCreatedAt.setText(menu.getCreatedAt() != null ? menu.getCreatedAt().format(DATE_FORMAT) : "—");
        detailUpdatedAt.setText(menu.getUpdatedAt() != null ? menu.getUpdatedAt().format(DATE_FORMAT) : "—");
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) detailNom.getScene().getWindow();
        stage.close();
    }
}
