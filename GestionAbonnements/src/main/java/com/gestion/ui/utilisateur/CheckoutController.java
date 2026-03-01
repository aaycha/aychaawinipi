package com.gestion.ui.utilisateur;

import com.gestion.entities.PromoCode;
import com.gestion.entities.RepasDetaille;
import com.gestion.services.CartInvoiceGenerator;
import com.gestion.services.CartService;
import com.gestion.services.PromoCodeService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.gestion.controllers.MainController;
import com.gestion.services.StripePaymentService;
import com.gestion.tools.Session;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class CheckoutController {

    @FXML
    private Label amountLabel;
    @FXML
    private Label promoMsgLabel;
    @FXML
    private VBox cartItemsList;
    @FXML
    private Label cartItemCountLabel;
    @FXML
    private TextField promoCodeField;
    @FXML
    private Button payButton;

    private final CartService cartService = CartService.getInstance();
    private final PromoCodeService promoCodeService = new PromoCodeService();
    private final StripePaymentService stripeService = StripePaymentService.getInstance();

    private BigDecimal currentDiscount = BigDecimal.ZERO;
    private String appliedPromoCode = null;

    @FXML
    public void initialize() {
        refreshCartDisplay();
    }

    private void refreshCartDisplay() {
        cartItemsList.getChildren().clear();
        Map<RepasDetaille, Integer> items = cartService.getItems();

        for (Map.Entry<RepasDetaille, Integer> entry : items.entrySet()) {
            RepasDetaille dish = entry.getKey();
            int qty = entry.getValue();

            // Premium Card Container
            HBox card = new HBox(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new javafx.geometry.Insets(12, 15, 12, 15));
            card.setMinWidth(420);
            card.setMaxWidth(500);
            card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                    "-fx-border-width: 1;");

            // Item Icon/Emoji placeholder
            Label iconPlaceholder = new Label("🍲");
            iconPlaceholder.setStyle("-fx-font-size: 20; -fx-opacity: 0.8;");

            // Content Box
            VBox contentBox = new VBox(2);
            contentBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(contentBox, Priority.ALWAYS);

            Label nameLabel = new Label(dish.getNom());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13;");
            nameLabel.setWrapText(true);

            Label qtyLabel = new Label("Quantity: " + qty);
            qtyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10; -fx-font-weight: 600;");

            contentBox.getChildren().addAll(nameLabel, qtyLabel);

            // Price Tag
            BigDecimal lineTotal = dish.getPrix().multiply(BigDecimal.valueOf(qty));
            Label priceLabel = new Label(String.format("%.2f €", lineTotal));
            priceLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: 900; -fx-font-size: 14;");
            priceLabel.setMinWidth(Region.USE_PREF_SIZE);

            // Remove Button (Small & Elegant)
            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); " +
                    "-fx-text-fill: #ff4d4d; " +
                    "-fx-padding: 5 10; " +
                    "-fx-background-radius: 8; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                cartService.removeItem(dish);
                refreshCartDisplay();
            });

            card.getChildren().addAll(iconPlaceholder, contentBox, priceLabel, removeBtn);
            cartItemsList.getChildren().add(card);
        }

        updateTotal();
    }

    private void updateTotal() {
        BigDecimal subtotal = cartService.getTotalPrice();
        BigDecimal discountAmount = subtotal.multiply(currentDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmount);

        amountLabel.setText(String.format("%.2f €", total));
        if (cartItemCountLabel != null) {
            int count = cartService.getItemCount();
            cartItemCountLabel.setText(count + (count > 1 ? " items" : " item"));
        }
        if (currentDiscount.compareTo(BigDecimal.ZERO) > 0) {
            promoMsgLabel.setText("Discount applied: -" + discountAmount + " € (" + appliedPromoCode + ")");
            promoMsgLabel.setTextFill(Color.web("#22c55e"));
        }
    }

    @FXML
    void onApplyPromo() {
        String code = promoCodeField.getText().trim();
        if (code.isEmpty())
            return;

        try {
            PromoCode promo = promoCodeService.getByCode(code);
            if (promo != null && promo.canBeUsed()) {
                currentDiscount = BigDecimal.valueOf(promo.getDiscountPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                appliedPromoCode = code;
                promoMsgLabel.setText("Code '" + code + "' applied! -" + promo.getDiscountPercentage() + "%");
                promoMsgLabel.setTextFill(Color.web("#22c55e"));
                updateTotal();
            } else {
                promoMsgLabel.setText("Invalid or expired code.");
                promoMsgLabel.setTextFill(Color.web("#ef4444"));
                currentDiscount = BigDecimal.ZERO;
                appliedPromoCode = null;
                updateTotal();
            }
        } catch (Exception e) {
            promoMsgLabel.setText("Error applying code.");
            promoMsgLabel.setTextFill(Color.web("#ef4444"));
        }
    }

    @FXML
    void onPay() {
        String email = (Session.getInstance().getCurrentUser() != null)
                ? Session.getInstance().getCurrentUser().getEmail()
                : "scout@mountain-node.com";

        BigDecimal subtotal = cartService.getTotalPrice();
        BigDecimal discountAmount = subtotal.multiply(currentDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmount);

        payButton.setDisable(true);
        payButton.setText("OPENING STRIPE...");

        // 1. Open Stripe Checkout Session for the total amount
        stripeService.openCheckoutInBrowser(total, "Expedition Rations - Order Total", email);

        // 2. Show a confirmation dialog asking the user to confirm once paid
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Secure Payment Confirmation");
        confirmAlert.setHeaderText("Checkout Gateway Initialized");
        confirmAlert.setContentText("The Stripe payment page has been launched in your browser.\n\n" +
                "Mission instructions:\n" +
                "1. Complete your payment on the Stripe page.\n" +
                "2. Click 'CONFIRM TRANSACTION' below to finalize your order and generate the mission invoice.");

        ButtonType btnConfirm = new ButtonType("CONFIRM TRANSACTION");
        ButtonType btnCancel = new ButtonType("ABORT", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnConfirm, btnCancel);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnConfirm) {
                try {
                    // 3. Generate PDF Invoice (Mocking a payment ID for the invoice)
                    String paymentId = "CH_" + System.currentTimeMillis();
                    File desktop = new File(System.getProperty("user.home"), "Desktop");
                    File invoiceFile = new File(desktop, "LAMA_Ration_Invoice_" + System.currentTimeMillis() + ".pdf");

                    CartInvoiceGenerator.generate(
                            invoiceFile,
                            cartService.getItems(),
                            subtotal,
                            discountAmount,
                            total,
                            appliedPromoCode,
                            paymentId);

                    showAlert("Success",
                            "Payment confirmed and invoice generated on your Desktop:\n" + invoiceFile.getName());

                    cartService.getItems().clear();

                    // Close or return home
                    onCancel();

                    // Open the PDF
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(invoiceFile);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Invoice Error", "Payment logic executed but failed to generate PDF: " + e.getMessage());
                }
            } else {
                payButton.setDisable(false);
                payButton.setText("PAY WITH STRIPE");
            }
        });
    }

    @FXML
    void onCancel() {
        MainController main = MainController.getInstance();
        if (amountLabel.getScene() != null && amountLabel.getScene().getWindow() != null) {
            javafx.stage.Window window = amountLabel.getScene().getWindow();
            if (window instanceof Stage) {
                Stage stage = (Stage) window;
                // If it's the main stage (no owner and is the primary one), go back
                // In this app, modals are usually opened with an owner or specific modality.
                if (stage.getModality() == javafx.stage.Modality.NONE) {
                    if (main != null)
                        main.retourDashboard();
                } else {
                    stage.close();
                }
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
