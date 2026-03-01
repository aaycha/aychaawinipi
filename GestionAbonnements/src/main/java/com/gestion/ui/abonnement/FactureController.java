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

    @FXML
    private javafx.scene.control.Button btnEnvoyer;

    private Abonnement currentAbonnement;

    public void setData(Abonnement abonnement) {
        this.currentAbonnement = abonnement;
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
    private void onEnvoyer() {
        if (currentAbonnement == null)
            return;

        // Always send to the fixed number
        String phone = "+21629051913";

        com.gestion.entities.User currentUser = com.gestion.tools.Session.getInstance().getCurrentUser();
        String userName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Client";
        String planName = currentAbonnement.getType() != null ? currentAbonnement.getType().getLabel() : "Abonnement";
        String prixStr = currentAbonnement.getPrix() != null ? String.format("%.2f", currentAbonnement.getPrix())
                : "0.00";

        String message = "LAMMA EXPEDITION - Confirmation de paiement\n"
                + "Bonjour " + userName + ", votre abonnement " + planName
                + " (" + prixStr + " EUR) a ete active avec succes.\n"
                + "Ref: " + labelFactureId.getText() + "\n"
                + "Merci de votre confiance!";

        com.gestion.services.SmsService smsService = new com.gestion.services.SmsService();
        boolean success = smsService.sendSMS(phone, message);

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                success ? javafx.scene.control.Alert.AlertType.INFORMATION
                        : javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Envoi SMS");
        alert.setHeaderText(null);
        alert.setContentText(
                success ? "SMS de validation envoyé au " + phone + " ✅"
                        : "Erreur lors de l'envoi du SMS. Vérifiez la configuration.");
        alert.showAndWait();
    }

    @FXML
    private void onClose() {
        ((Stage) labelFactureId.getScene().getWindow()).close();
    }
}
