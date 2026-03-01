package com.gestion.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service d'intégration Stripe pour les paiements.
 * Utilise l'API PaymentIntent et Checkout Session de Stripe.
 */
public class StripePaymentService {

    private static final Logger LOGGER = Logger.getLogger(StripePaymentService.class.getName());
    private static final String STRIPE_CHECKOUT_URL = "https://buy.stripe.com/test_6oUaEYgm7drA6A3192abK00";
    private static StripePaymentService instance;
    private boolean configured = false;

    private StripePaymentService() {
        loadConfig();
    }

    public static synchronized StripePaymentService getInstance() {
        if (instance == null) {
            instance = new StripePaymentService();
        }
        return instance;
    }

    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("afilnet.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                String key = props.getProperty("stripe.secret_key", "");
                if (!key.isEmpty() && !key.contains("VOTRE_CLE")) {
                    Stripe.apiKey = key;
                    configured = true;
                    LOGGER.info("Stripe configuré avec succès via afilnet.properties.");
                } else {
                    LOGGER.warning("Clé Stripe non configurée (contient 'VOTRE_CLE') — mode simulation activé.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger la config Stripe", e);
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * Résultat d'un paiement (réel ou simulé).
     */
    public static class PaymentResult {
        public final boolean success;
        public final String paymentId;
        public final String message;

        public PaymentResult(boolean success, String paymentId, String message) {
            this.success = success;
            this.paymentId = paymentId;
            this.message = message;
        }
    }

    /**
     * Crée une session de paiement Stripe Checkout avec un montant dynamique.
     * 
     * @param amount      Montant en EUR
     * @param description Nom du produit/service
     * @param email       Email du client (pour pré-remplissage)
     * @return URL de la page de paiement Stripe
     */
    public String createCheckoutSession(BigDecimal amount, String description, String email) {
        if (!configured) {
            LOGGER.warning("Stripe non configuré, retour à l'URL statique.");
            return STRIPE_CHECKOUT_URL;
        }

        try {
            long amountCents = amount.multiply(new BigDecimal(100)).longValue();

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://lamma.com/success")
                    .setCancelUrl("https://lamma.com/cancel")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount(amountCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(description)
                                            .build())
                                    .build())
                            .build());

            if (email != null && !email.isEmpty()) {
                paramsBuilder.setCustomerEmail(email);
            }

            Session session = Session.create(paramsBuilder.build());
            LOGGER.info("Session Stripe Checkout créée: " + session.getId());
            return session.getUrl();

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Erreur création session Stripe: " + e.getMessage(), e);
            return STRIPE_CHECKOUT_URL;
        }
    }

    /**
     * Crée et confirme un paiement.
     * Si Stripe n'est pas configuré, simule le paiement.
     *
     * @param amount   Montant en euros (ex: 24.50)
     * @param currency Devise (ex: "eur")
     * @param email    Email du client
     * @return PaymentResult avec le statut
     */
    public PaymentResult processPayment(BigDecimal amount, String currency, String email) {
        if (!configured) {
            return simulatePayment(amount, email);
        }

        try {
            // Stripe attend le montant en centimes
            long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency != null ? currency : "eur")
                    .setReceiptEmail(email)
                    .setDescription("LAMA EXPEDITION - Commande repas")
                    .addPaymentMethodType("card")
                    .setConfirm(false)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            LOGGER.info("PaymentIntent créé: " + intent.getId() + " — status: " + intent.getStatus());

            return new PaymentResult(true, intent.getId(),
                    "Paiement initié — ID: " + intent.getId());

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Erreur Stripe: " + e.getMessage(), e);
            return new PaymentResult(false, null, "Erreur Stripe: " + e.getMessage());
        }
    }

    /**
     * Confirme un PaymentIntent déjà créé (avec les données de carte côté client).
     */
    public PaymentResult confirmPayment(String paymentIntentId, String paymentMethodId) {
        if (!configured) {
            return new PaymentResult(true, "SIM-" + System.currentTimeMillis(), "Paiement simulé confirmé ✅");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent = intent.confirm(
                    com.stripe.param.PaymentIntentConfirmParams.builder()
                            .setPaymentMethod(paymentMethodId)
                            .build());

            boolean success = "succeeded".equals(intent.getStatus());
            return new PaymentResult(success, intent.getId(),
                    success ? "Paiement confirmé ✅" : "Statut: " + intent.getStatus());

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Erreur confirmation Stripe", e);
            return new PaymentResult(false, null, "Erreur: " + e.getMessage());
        }
    }

    private PaymentResult simulatePayment(BigDecimal amount, String email) {
        LOGGER.info("MODE SIMULATION — Paiement de " + amount + "€ pour " + email);
        String fakeId = "SIM-" + System.currentTimeMillis();
        return new PaymentResult(true, fakeId, "Paiement simulé réussi ✅ (ID: " + fakeId + ")");
    }

    /**
     * Returns the Stripe Checkout URL.
     */
    public String getCheckoutUrl() {
        return STRIPE_CHECKOUT_URL;
    }

    /**
     * Opens the Stripe Checkout hosted page in the user's default browser.
     * This is the simplest way to handle real payments without a backend server.
     *
     * @param description Description for logging
     * @return true if the browser was opened successfully
     */
    public boolean openCheckoutInBrowser(String description) {
        return openCheckoutInBrowser(new BigDecimal("0.0"), description); // Legacy call
    }

    public boolean openCheckoutInBrowser(BigDecimal amount, String description) {
        return openCheckoutInBrowser(amount, description, null);
    }

    public boolean openCheckoutInBrowser(BigDecimal amount, String description, String email) {
        try {
            String url = (amount.compareTo(BigDecimal.ZERO) > 0)
                    ? createCheckoutSession(amount, description, email)
                    : STRIPE_CHECKOUT_URL;

            LOGGER.info("Opening Stripe Checkout: " + url);

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                LOGGER.info("Stripe Checkout opened in browser successfully.");
                return true;
            } else {
                Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", url });
                LOGGER.info("Stripe Checkout opened via Runtime.");
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open Stripe Checkout: " + e.getMessage(), e);
            return false;
        }
    }
}
