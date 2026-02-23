package com.gestion.ui.abonnement;

import com.gestion.entities.Abonnement;
import com.gestion.entities.User;
import com.gestion.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Random;

public class FactureController {

    @FXML
    private Label labelFactureId;
    @FXML
    private Label labelDate;
    @FXML
    private Label labelClientName;
    @FXML
    private Label labelClientId;
    @FXML
    private Label labelItemTitle;
    @FXML
    private Label labelItemDesc;
    @FXML
    private Label labelItemPrice;
    @FXML
    private Label labelSubtotal;
    @FXML
    private Label labelTotal;

    public void setData(Abonnement abonnement) {
        Random random = new Random();
        labelFactureId.setText("INV-2026-" + String.format("%04d", random.nextInt(10000)));
        labelDate.setText(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        try {
            User user = new UserService().getUserById(abonnement.getUserId().intValue());
            if (user != null) {
                labelClientName.setText(user.getName());
                labelClientId.setText("ID: " + user.getId());
            }
        } catch (Exception e) {
            labelClientName.setText("Identifiant: " + abonnement.getUserId());
        }

        String title = "";
        String desc = "";
        switch (abonnement.getType()) {
            case EVENEMENT_PASS:
                title = "Pass Expédition Unique";
                desc = "Accès spécial réservé pour une aventure unique.";
                break;
            case MENSUEL:
                title = "Abonnement Mensuel Global";
                desc = "Accès illimité à toutes les activités pour 30 jours.";
                break;
            case ANNUEL:
                title = "Abonnement Annuel Explorateur";
                desc = "Le pack complet pour une année d'évasions.";
                break;
            case PREMIUM:
                title = "Privilège VIP PREMIUM";
                desc = "L'expérience ultime avec avantages exclusifs.";
                break;
        }

        labelItemTitle.setText(title);
        labelItemDesc.setText(desc);
        String priceStr = String.format("%.2f €", abonnement.getPrix());
        labelItemPrice.setText(priceStr);
        labelSubtotal.setText(priceStr);
        labelTotal.setText(priceStr);
    }

    @FXML
    private void onClose() {
        ((Stage) labelFactureId.getScene().getWindow()).close();
    }
}
