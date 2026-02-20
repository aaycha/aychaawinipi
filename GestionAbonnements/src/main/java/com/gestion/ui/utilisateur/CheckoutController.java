package com.gestion.ui.utilisateur;

import com.gestion.services.CartService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CheckoutController {

    @FXML
    private Label amountLabel;
    @FXML
    private TextField emailField;
    @FXML
    private TextField cardNumberField;
    @FXML
    private TextField expiryField;
    @FXML
    private TextField cvcField;
    @FXML
    private ComboBox<String> countryCombo;
    @FXML
    private Button payButton;

    private final CartService cartService = CartService.getInstance();

    @FXML
    public void initialize() {
        amountLabel.setText(String.format("Total: %.2f €", cartService.getTotalPrice()));
        if (countryCombo != null) {
            countryCombo.getItems().addAll("France", "Belgique", "Suisse", "Canada", "Maroc", "Tunisie");
            countryCombo.setValue("France");
        }
    }

    @FXML
    void onPay() {
        if (validateFields()) {
            payButton.setDisable(true);
            payButton.setText("Traitement...");

            // Simuler un appel API Stripe
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                payButton.setStyle("-fx-background-color: #28a745;");
                payButton.setText("Paiement Réussi ! ✅");

                PauseTransition close = new PauseTransition(Duration.seconds(1));
                close.setOnFinished(ev -> {
                    cartService.clear();
                    onCancel(); // Fermer la fenêtre
                });
                close.play();
            });
            pause.play();
        }
    }

    private boolean validateFields() {
        // Validation basique pour la simulation
        return !emailField.getText().isEmpty() && !cardNumberField.getText().isEmpty();
    }

    @FXML
    void onCancel() {
        ((Stage) payButton.getScene().getWindow()).close();
    }
}
