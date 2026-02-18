package com.gestion.ui.repas;

import com.gestion.entities.Repas;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de détail d'un plat (Repas).
 */
public class RepasDetailsController implements Initializable {

    @FXML private Label detailNom;
    @FXML private Label detailRestaurant;
    @FXML private Label detailMenu;
    @FXML private Label detailCategorie;
    @FXML private Label detailTypePlat;
    @FXML private Label detailPrix;
    @FXML private Label detailTempsPreparation;
    @FXML private Label detailDisponible;
    @FXML private Label detailImageUrl;
    @FXML private Label detailDescription;

    private Repas repas;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void setRepas(Repas repas) {
        this.repas = repas;
        loadDetails();
    }

    private void loadDetails() {
        if (repas == null) return;

        detailNom.setText(repas.getNom() != null ? repas.getNom() : "—");
        detailRestaurant.setText(repas.getRestaurantNom() != null && !repas.getRestaurantNom().isEmpty()
                ? repas.getRestaurantNom() : "Non renseigné");
        detailMenu.setText(repas.getMenuNom() != null && !repas.getMenuNom().isEmpty()
                ? repas.getMenuNom() : "Aucun menu");
        detailCategorie.setText(repas.getCategorie() != null ? repas.getCategorie().getLabel() : "—");
        detailTypePlat.setText(repas.getTypePlat() != null ? repas.getTypePlat().getLabel() : "—");
        detailPrix.setText(repas.getPrix() != null ? String.format("%.2f €", repas.getPrix()) : "—");
        detailTempsPreparation.setText(repas.getTempsPreparation() != null
                ? repas.getTempsPreparation() + " min" : "—");
        detailDisponible.setText(repas.isDisponible() ? "Oui" : "Non");
        detailImageUrl.setText(repas.getImageUrl() != null && !repas.getImageUrl().isEmpty()
                ? repas.getImageUrl() : "Aucune image");
        detailDescription.setText(repas.getDescription() != null && !repas.getDescription().isEmpty()
                ? repas.getDescription() : "Aucune description");
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) detailNom.getScene().getWindow();
        stage.close();
    }
}
