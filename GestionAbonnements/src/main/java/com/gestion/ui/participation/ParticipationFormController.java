package com.gestion.ui.participation;

import com.gestion.controllers.ParticipationController;
import com.gestion.entities.Evenement;
import com.gestion.entities.User;
import com.gestion.entities.Participation;
import com.gestion.services.EvenementService;
import com.gestion.services.UserService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.gestion.entities.Abonnement;
import com.gestion.ui.abonnement.AbonnementFormController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;

/**
 * Formulaire dédié pour la création / modification d'une participation,
 * avec saisie de nbAdultes / nbEnfants / nbChiens et pré‑visualisation du
 * tarif.
 */
public class ParticipationFormController {

    @FXML
    private Label formTitle;

    @FXML
    private TextField inputUserName;
    @FXML
    private TextField inputUserId;
    @FXML
    private ComboBox<Evenement> comboEvenement;
    @FXML
    private TextField inputPromoCode;
    @FXML
    private ComboBox<Participation.MealOption> comboRepas;
    @FXML
    private VBox containerRestaurant;
    @FXML
    private ComboBox<com.gestion.entities.Restaurant> comboRestaurant;
    @FXML
    private ComboBox<Participation.TypeParticipation> inputType;
    @FXML
    private ComboBox<Participation.ContexteSocial> inputContexte;
    @FXML
    private CheckBox inputHebergement;
    @FXML
    private TextField inputHebergementNuits;

    @FXML
    private TextField inputNbAdultes;
    @FXML
    private TextField inputNbEnfants;
    @FXML
    private TextField inputNbChiens;

    @FXML
    private TextArea inputCommentaire;
    @FXML
    private TextArea inputBesoinsSpeciaux;

    @FXML
    private Label labelTotalParticipants;
    @FXML
    private Label labelTypeAbonnement;
    @FXML
    private Label labelMontant;

    @FXML
    private Label errorUserId;
    @FXML
    private Label errorEvenementId;
    @FXML
    private Label errorGroupe;
    @FXML
    private Label errorGlobal;
    @FXML
    private ScrollPane errorScroll;

    @FXML
    private Button btnEnregistrer;

    private ParticipationController participationController;
    private Participation participation;
    private boolean editMode = false;
    private java.util.function.Consumer<Participation> onSaved;
    private BigDecimal currentPromoDiscount = BigDecimal.ZERO;
    private boolean isSubscriptionCheckBypassed = false;

    private boolean isFilteringEvent = false;

    private final UserService userService = new UserService();
    private final EvenementService eventService = new EvenementService();

    @FXML
    public void initialize() {
        if (comboRepas != null) {
            comboRepas.getItems().setAll(Participation.MealOption.values());
            comboRepas.setConverter(new StringConverter<Participation.MealOption>() {
                @Override
                public String toString(Participation.MealOption option) {
                    return option == null ? "" : option.getLabel();
                }

                @Override
                public Participation.MealOption fromString(String string) {
                    return null;
                }
            });
            comboRepas.setValue(Participation.MealOption.SANS_REPAS);
        }

        if (comboRestaurant != null) {
            com.gestion.interfaces.RestaurantService restService = new com.gestion.services.RestaurantServiceImpl();
            comboRestaurant.getItems().setAll(restService.findActifs());
            comboRestaurant.setConverter(new StringConverter<com.gestion.entities.Restaurant>() {
                @Override
                public String toString(com.gestion.entities.Restaurant r) {
                    if (r == null)
                        return "";
                    com.gestion.interfaces.RestaurantService localRestService = new com.gestion.services.RestaurantServiceImpl();
                    int rest = localRestService.getPlacesRestantes(r.getId(), editMode ? participation.getId() : null);
                    return r.getNom() + " (" + rest + " places dispo / " + r.getNombrePlaces() + ")";
                }

                @Override
                public com.gestion.entities.Restaurant fromString(String string) {
                    return null;
                }
            });
        }

        // Chargement initial des données
        List<Evenement> events = new ArrayList<>();
        try {
            events = eventService.getAll();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            showError(errorGlobal, "Erreur lors du chargement des événements : " + e.getMessage());
        }

        if (comboEvenement != null) {
            comboEvenement.setEditable(true);
            javafx.collections.ObservableList<Evenement> eventList = javafx.collections.FXCollections
                    .observableArrayList(events);
            javafx.collections.transformation.FilteredList<Evenement> filteredEvents = new javafx.collections.transformation.FilteredList<>(
                    eventList, p -> true);

            // StringConverter pour n'afficher que le TITRE et éviter les ClassCastException
            comboEvenement.setConverter(new javafx.util.StringConverter<Evenement>() {
                @Override
                public String toString(Evenement e) {
                    return e == null ? "" : e.getTitre();
                }

                @Override
                public Evenement fromString(String string) {
                    if (string == null || string.isBlank())
                        return null;
                    return comboEvenement.getItems().stream()
                            .filter(e -> e.getTitre().equalsIgnoreCase(string.trim()))
                            .findFirst().orElse(null);
                }
            });

            comboEvenement.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (isFilteringEvent)
                    return;
                isFilteringEvent = true;

                final String filter = newValue == null ? "" : newValue.trim().toLowerCase();

                javafx.application.Platform.runLater(() -> {
                    try {
                        filteredEvents.setPredicate(e -> {
                            if (filter.isEmpty())
                                return true;
                            return e.getTitre().toLowerCase().contains(filter);
                        });

                        if (!filter.isEmpty() && !comboEvenement.isShowing()) {
                            comboEvenement.show();
                        }
                    } finally {
                        isFilteringEvent = false;
                    }
                });
            });

            comboEvenement.setItems(filteredEvents);

            // Listener pour la sélection
            comboEvenement.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    clearError(errorEvenementId);
                    updatePreviewFromFields();
                }
            });
        }

        // Initialisation des autres combos
        if (inputType != null) {
            inputType.getItems().addAll(Participation.TypeParticipation.values());
        }
        if (inputContexte != null) {
            inputContexte.getItems().addAll(Participation.ContexteSocial.values());
        }

        // Listeners pour calcul automatique
        addCalculationListeners();

        updatePreviewFromFields();
    }

    private void addCalculationListeners() {
        ChangeListener<Object> recalc = (obs, oldVal, newVal) -> updatePreviewFromFields();

        if (inputNbAdultes != null)
            inputNbAdultes.textProperty().addListener((obs, old, val) -> updatePreviewFromFields());
        if (inputNbEnfants != null)
            inputNbEnfants.textProperty().addListener((obs, old, val) -> updatePreviewFromFields());
        if (inputNbChiens != null)
            inputNbChiens.textProperty().addListener((obs, old, val) -> updatePreviewFromFields());
        if (inputHebergement != null)
            inputHebergement.selectedProperty().addListener((obs, old, val) -> updatePreviewFromFields());
        if (inputHebergementNuits != null)
            inputHebergementNuits.textProperty().addListener((obs, old, val) -> updatePreviewFromFields());
        if (inputType != null)
            inputType.valueProperty().addListener(recalc);
        if (inputContexte != null)
            inputContexte.valueProperty().addListener(recalc);
        if (comboRepas != null)
            comboRepas.valueProperty().addListener((obs, oldVal, newVal) -> {
                updatePreviewFromFields();
                if (containerRestaurant != null) {
                    boolean isResto = newVal == Participation.MealOption.AU_RESTAURANT;
                    containerRestaurant.setVisible(isResto);
                    containerRestaurant.setManaged(isResto);
                }
            });
    }

    private void clearError(Label label) {
        hideError(label);
    }

    public void setParticipationController(ParticipationController controller) {
        this.participationController = controller;
    }

    public void setOnSaved(java.util.function.Consumer<Participation> onSaved) {
        this.onSaved = onSaved;
    }

    public void setAdminMode(boolean adminMode) {
        // Method kept for compatibility with other controllers
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
        this.editMode = (participation != null);

        if (formTitle != null) {
            formTitle.setText(editMode ? "Modifier la participation" : "Nouvelle participation");
        }

        if (participation != null) {
            if (participation.getUserId() != null) {
                inputUserId.setText(String.valueOf(participation.getUserId()));
                resolveUserName(participation.getUserId().intValue());
            }
            if (comboEvenement != null && participation.getEvenementId() != null) {
                comboEvenement.getItems().stream()
                        .filter(e -> Long.valueOf(e.getIdEvent()).equals(participation.getEvenementId()))
                        .findFirst()
                        .ifPresent(e -> comboEvenement.setValue(e));
            }
            if (inputType != null)
                inputType.setValue(participation.getType());
            if (inputContexte != null)
                inputContexte.setValue(participation.getContexteSocial());
            if (comboRepas != null) {
                comboRepas.setValue(participation.getMealOption());
                if (participation.getMealOption() == Participation.MealOption.AU_RESTAURANT
                        && participation.getRestaurantId() != null) {
                    comboRestaurant.getItems().stream()
                            .filter(r -> r.getId().equals(participation.getRestaurantId()))
                            .findFirst().ifPresent(comboRestaurant::setValue);
                }
            }
            if (inputHebergement != null)
                inputHebergement.setSelected(participation.getHebergementNuits() > 0);
            if (inputHebergementNuits != null) {
                inputHebergementNuits.setText(String.valueOf(participation.getHebergementNuits()));
            }

            if (inputNbAdultes != null)
                inputNbAdultes.setText(String.valueOf(participation.getNbAdultes()));
            if (inputNbEnfants != null)
                inputNbEnfants.setText(String.valueOf(participation.getNbEnfants()));
            if (inputNbChiens != null)
                inputNbChiens.setText(String.valueOf(participation.getNbChiens()));

            if (inputCommentaire != null)
                inputCommentaire.setText(participation.getCommentaire());
            if (inputBesoinsSpeciaux != null)
                inputBesoinsSpeciaux.setText(participation.getBesoinsSpeciaux());

            if (labelTypeAbonnement != null && participation.getTypeAbonnementChoisi() != null) {
                labelTypeAbonnement.setText(participation.getTypeAbonnementChoisi());
            }
            if (labelMontant != null && participation.getMontantCalcule() != null) {
                labelMontant.setText(participation.getMontantCalcule().toPlainString());
            }
            if (labelTotalParticipants != null) {
                labelTotalParticipants.setText("Total participants : " + participation.getTotalParticipants());
            }
        }
        clearErrors();
        updatePreviewFromFields();
    }

    public void setCurrentUserId(Long userId) {
        if (userId != null) {
            inputUserId.setText(String.valueOf(userId));
            inputUserId.setDisable(true);
            resolveUserName(userId.intValue());
        }
    }

    private void resolveUserName(int userId) {
        try {
            User u = userService.getUserById(userId);
            if (u != null)
                inputUserName.setText(u.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onEnregistrer() {
        clearErrors();
        List<String> errors = new ArrayList<>();

        Long userId = resolveUserId();
        if (userId == null)
            return;

        // --- CHECK PARTICIPATION LIMIT (Max 3) ---
        if (!editMode) {
            if (participationController == null)
                participationController = new ParticipationController();
            List<Participation> existing = participationController.getByUserId(userId);
            if (existing.size() >= 3) {
                showError(errorGlobal, "Limite atteinte : Chaque utilisateur est limité à 3 participations.");
                return;
            }
        }

        Object eventVal = comboEvenement != null ? comboEvenement.getValue() : null;
        Evenement selectedEvent = null;
        if (eventVal instanceof Evenement) {
            selectedEvent = (Evenement) eventVal;
        } else if (eventVal instanceof String) {
            String str = (String) eventVal;
            if (!str.isBlank()) {
                selectedEvent = comboEvenement.getItems().stream()
                        .filter(e -> e.getTitre().equalsIgnoreCase(str.trim()))
                        .findFirst().orElse(null);
            }
        }

        Long evenementId = selectedEvent != null ? Long.valueOf(selectedEvent.getIdEvent()) : null;

        Participation.TypeParticipation type = inputType != null ? inputType.getValue() : null;
        Participation.ContexteSocial contexte = inputContexte != null ? inputContexte.getValue() : null;
        Participation.MealOption mealOption = comboRepas != null ? comboRepas.getValue()
                : Participation.MealOption.SANS_REPAS;

        Long restaurantId = null;
        if (mealOption == Participation.MealOption.AU_RESTAURANT) {
            com.gestion.entities.Restaurant selResto = comboRestaurant.getValue();
            if (selResto == null) {
                errors.add("Veuillez sélectionner un restaurant.");
            } else {
                com.gestion.interfaces.RestaurantService restService = new com.gestion.services.RestaurantServiceImpl();
                int available = restService.getPlacesRestantes(selResto.getId(),
                        editMode ? participation.getId() : null);
                if (available <= 0) {
                    errors.add("Désolé, ce restaurant est complet.");
                } else {
                    restaurantId = selResto.getId();
                }
            }
        }

        if (evenementId == null) {
            errors.add("Sélection de l'événement obligatoire.");
            showError(errorEvenementId, "Veuillez sélectionner un événement dans la liste.");
        }
        if (type == null) {
            errors.add("Le type de participation est obligatoire.");
        }
        if (contexte == null) {
            errors.add("Le contexte social est obligatoire.");
        }

        int nbAdultes = parseInt(inputNbAdultes != null ? inputNbAdultes.getText() : null, 1);
        int nbEnfants = parseInt(inputNbEnfants != null ? inputNbEnfants.getText() : null, 0);
        int nbChiens = parseInt(inputNbChiens != null ? inputNbChiens.getText() : null, 0);

        if (nbAdultes < 1) {
            errors.add("Au moins 1 adulte est requis.");
        }
        if (nbEnfants < 0) {
            errors.add("Le nombre d'enfants ne peut pas être négatif.");
        }
        if (nbChiens < 0) {
            errors.add("Le nombre de chiens ne peut pas être négatif.");
        }

        int nuits = 0;
        if (inputHebergement != null && inputHebergement.isSelected()) {
            nuits = parseInt(inputHebergementNuits != null ? inputHebergementNuits.getText() : null, 1);
            if (nuits <= 0) {
                errors.add("Le nombre de nuits doit être supérieur à 0 si hébergement est sélectionné.");
            }
        }

        String rawNuits = inputHebergementNuits != null ? inputHebergementNuits.getText() : "null";
        System.out.println("[DEBUG] onEnregistrer - rawNuits='" + rawNuits + "', parsed nuits=" + nuits);
        System.out.println("[DEBUG] onEnregistrer - userId=" + userId + ", evenementId=" + evenementId);
        System.out.println("[DEBUG] onEnregistrer - Type=" + type + ", Contexte=" + contexte + ", Meal=" + mealOption);
        System.out.println("[DEBUG] onEnregistrer - Hebergement Selected="
                + (inputHebergement != null && inputHebergement.isSelected())
                + ", Nuits=" + nuits);

        if (!errors.isEmpty()) {
            String combinedErrors = String.join("\n• ", errors);
            System.out.println("[DEBUG] onEnregistrer - Validation Errors:\n" + combinedErrors);
            showError(errorGlobal, "Erreurs de validation :\n• " + combinedErrors);
            return;
        }

        try {
            Participation target;
            if (editMode && participation != null) {
                target = participation;
            } else {
                target = new Participation(userId, evenementId, type, contexte);
                target.setStatut(Participation.StatutParticipation.EN_ATTENTE);
                target.setDateInscription(LocalDateTime.now());
            }

            target.setUserId(userId);
            target.setEvenementId(evenementId);
            target.setType(type);
            target.setContexteSocial(contexte);
            target.setHebergementNuits(nuits);

            target.setNbAdultes(nbAdultes);
            target.setNbEnfants(nbEnfants);
            target.setNbChiens(nbChiens);
            target.setTotalParticipants(nbAdultes + nbEnfants);

            if (inputCommentaire != null) {
                target.setCommentaire(inputCommentaire.getText());
            }
            target.setBesoinsSpeciaux(inputBesoinsSpeciaux.getText());
            target.setMealOption(mealOption);
            target.setRestaurantId(restaurantId);

            // --- LOYALTY POINTS ---
            int points = 10; // Base points
            if (mealOption != Participation.MealOption.SANS_REPAS)
                points += 5;
            target.setPointsEarned(points);

            // Persistence de l'abonnement utilisé
            Long activeAbonnementId = null;
            try {
                com.gestion.controllers.AbonnementController abCont = new com.gestion.controllers.AbonnementController();
                List<com.gestion.entities.Abonnement> activePlans = abCont.getAll().stream()
                        .filter(a -> a.getUserId().equals(userId) && a.estActif())
                        .collect(java.util.stream.Collectors.toList());
                if (!activePlans.isEmpty()) {
                    activeAbonnementId = activePlans.get(0).getId();
                }
            } catch (Exception e) {
            }

            target.setAbonnementId(activeAbonnementId);

            if (labelTypeAbonnement != null) {
                target.setTypeAbonnementChoisi(labelTypeAbonnement.getText());
            }
            if (labelMontant != null) {
                try {
                    target.setMontantCalcule(new java.math.BigDecimal(labelMontant.getText()));
                } catch (Exception ignored) {
                }
            }

            if (participationController == null) {
                participationController = new ParticipationController();
            }

            // --- MANDATORY SUBSCRIPTION CHOICE FLOW ---
            if (!editMode && !isSubscriptionCheckBypassed && !hasActiveSubscription(userId, evenementId)) {
                showSubscriptionChoicePopup(userId, evenementId);
                return;
            }

            if (editMode) {
                participationController.update(target);
                showInfo("Participation modifiée avec succès.");
            } else {
                participationController.create(target);
                showInfo("Participation créée avec succès.");
            }

            if (onSaved != null) {
                onSaved.accept(target);
            }
            closeWindow();
        } catch (IllegalArgumentException ex) {
            System.err.println("[ERROR] Validation Service Fail: " + ex.getMessage());
            showError(errorGlobal, ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError(errorGlobal, "Problème technique : " + ex.getMessage());
        }
    }

    private boolean hasActiveSubscription(Long userId, Long eventId) {
        try {
            com.gestion.controllers.AbonnementController abCont = new com.gestion.controllers.AbonnementController();
            List<com.gestion.entities.Abonnement> fullList = abCont.getAll();
            System.out.println("[DEBUG] hasActiveSubscription - Total in DB: " + fullList.size());

            List<com.gestion.entities.Abonnement> list = fullList.stream()
                    .filter(a -> a.getUserId().equals(userId) && a.estActif())
                    .collect(java.util.stream.Collectors.toList());

            System.out.println("[DEBUG] hasActiveSubscription - Active for User " + userId + ": " + list.size());
            if (list.isEmpty())
                return false;

            // Check if any sub is global or matches this event
            boolean hasAccess = list.stream().anyMatch(a -> a.getType() != Abonnement.TypeAbonnement.EVENEMENT_PASS
                    || (a.getEvenementId() != null && a.getEvenementId().equals(eventId)));

            System.out.println("[DEBUG] hasActiveSubscription - Access granted: " + hasAccess);
            return hasAccess;
        } catch (Exception e) {
            System.err.println("[DEBUG] hasActiveSubscription - Exception: " + e.getMessage());
            return false;
        }
    }

    private void showSubscriptionChoicePopup(Long userId, Long eventId) {
        // Calculate the base amount from the UI labels
        BigDecimal baseAmount = BigDecimal.ZERO;
        try {
            if (labelMontant != null && !labelMontant.getText().isEmpty()) {
                baseAmount = new BigDecimal(labelMontant.getText());
            }
        } catch (Exception e) {
            baseAmount = new BigDecimal("25.00"); // Fallback
        }

        BigDecimal passPrice = baseAmount;
        BigDecimal abonnementPrice = new BigDecimal("14.99"); // Standard Monthly Price

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Choix de l'Accès - LAMMA");
        alert.setHeaderText("Comment souhaitez-vous valider votre participation ?");
        alert.setContentText("Choisissez une option pour continuer :\n\n" +
                "• Pass Unique : " + passPrice + " € (Accès pour cet événement uniquement)\n" +
                "• Abonnement Global : à partir de " + abonnementPrice + " € (Accès à tous les événements)");

        ButtonType btnPass = new ButtonType("Prendre un Pass Unique");
        ButtonType btnAbonnement = new ButtonType("S'abonner (Global)");
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnPass, btnAbonnement, btnCancel);

        final BigDecimal finalPassPrice = passPrice;

        alert.showAndWait().ifPresent(response -> {
            if (response == btnPass) {
                // Open Stripe Checkout for Pass payment
                com.gestion.services.StripePaymentService stripeService = com.gestion.services.StripePaymentService
                        .getInstance();
                stripeService.openCheckoutInBrowser(finalPassPrice, "Pass Unique LAMMA");

                Alert payConfirm = new Alert(Alert.AlertType.INFORMATION);
                payConfirm.setTitle("Paiement Stripe");
                payConfirm.setHeaderText("Page de paiement ouverte !");
                payConfirm.setContentText(
                        "La page de paiement Stripe a été ouverte dans votre navigateur.\n\n" +
                                "Montant : " + finalPassPrice + " €\n" +
                                "Type : Pass Unique\n\n" +
                                "Cliquez OK une fois le paiement effectué.");
                payConfirm.showAndWait();

                openAbonnementForm(new Abonnement(userId, eventId, Abonnement.TypeAbonnement.EVENEMENT_PASS,
                        java.time.LocalDate.now(), finalPassPrice, false), true);
            } else if (response == btnAbonnement) {
                // Open Stripe Checkout for Abonnement payment
                com.gestion.services.StripePaymentService stripeService = com.gestion.services.StripePaymentService
                        .getInstance();
                stripeService.openCheckoutInBrowser(abonnementPrice, "Abonnement Global LAMMA");

                Alert payConfirm = new Alert(Alert.AlertType.INFORMATION);
                payConfirm.setTitle("Paiement Stripe");
                payConfirm.setHeaderText("Page de paiement ouverte !");
                payConfirm.setContentText(
                        "La page de paiement Stripe a été ouverte dans votre navigateur.\n\n" +
                                "Montant : à partir de " + abonnementPrice + " €\n" +
                                "Type : Abonnement Global\n\n" +
                                "Cliquez OK une fois le paiement effectué.");
                payConfirm.showAndWait();

                openAbonnementChoix();
            }
        });
    }

    private void openAbonnementChoix() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/utilisateur/abonnement-choix.fxml"));
            Parent root = loader.load();
            com.gestion.ui.utilisateur.AbonnementChoixController ctrl = loader.getController();

            // Setup a callback for when an appointment is confirmed in the choice view
            ctrl.setOnSuccess(() -> {
                Platform.runLater(() -> {
                    showInfo("Abonnement activé ! Nous confirmons votre participation...");
                    isSubscriptionCheckBypassed = true;
                    onEnregistrer(); // Auto-confirm
                });
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Choisir un Abonnement");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError(errorGlobal, "Impossible d'ouvrir la liste des abonnements.");
        }
    }

    private void openAbonnementForm(Abonnement template, boolean readOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/abonnement/abonnement-form.fxml"));
            Parent root = loader.load();
            AbonnementFormController ctrl = loader.getController();

            ctrl.setAbonnement(template);
            ctrl.setReadOnly(readOnly);
            ctrl.setOnSave(() -> {
                Platform.runLater(() -> {
                    showInfo("Accès obtenu ! Confirmation de votre participation en cours...");
                    isSubscriptionCheckBypassed = true;
                    onEnregistrer(); // Auto-confirm
                });
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(readOnly ? "Facturation Pass Unique" : "Obtention d'Accès");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError(errorGlobal, "Impossible d'ouvrir le formulaire d'abonnement.");
        }
    }

    @FXML
    void onApplyPromo() {
        String code = inputPromoCode.getText().trim();
        if (code.isEmpty())
            return;
        try {
            com.gestion.services.PromoCodeService service = new com.gestion.services.PromoCodeService();
            com.gestion.entities.PromoCode promo = service.getByCode(code);
            if (promo != null && promo.canBeUsed()) {
                currentPromoDiscount = BigDecimal.valueOf(promo.getDiscountPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                showInfo("Code promo '" + code + "' appliqué : -" + promo.getDiscountPercentage() + "%");
                updatePreviewFromFields();
            } else {
                showError(errorGlobal, "Code promo invalide ou expiré.");
                currentPromoDiscount = BigDecimal.ZERO;
                updatePreviewFromFields();
            }
        } catch (Exception e) {
            showError(errorGlobal, "Erreur lors de l'application du code promo.");
        }
    }

    @FXML
    void onAnnuler() {
        closeWindow();
    }

    // ──────────────────── Helpers ─────────────────────

    private void updatePreviewFromFields() {
        int nbAdultes = parseInt(inputNbAdultes != null ? inputNbAdultes.getText() : null, 1);
        int nbEnfants = parseInt(inputNbEnfants != null ? inputNbEnfants.getText() : null, 0);
        int nbChiens = parseInt(inputNbChiens != null ? inputNbChiens.getText() : null, 0);
        Participation.MealOption mealOption = comboRepas != null ? comboRepas.getValue()
                : Participation.MealOption.SANS_REPAS;

        int total = Math.max(1, nbAdultes) + Math.max(0, nbEnfants);
        if (labelTotalParticipants != null) {
            labelTotalParticipants.setText("Total participants : " + total);
        }

        // 1. Calcul de base
        BigDecimal tarifAdulte = new BigDecimal("25.00");
        BigDecimal tarifEnfant = new BigDecimal("15.00");
        BigDecimal tarifChien = new BigDecimal("8.00");
        BigDecimal forfaitFamille = new BigDecimal("60.00");
        BigDecimal tarifRepas = new BigDecimal("12.00");

        BigDecimal montant;
        String typeLabel = "Standard";

        if (nbAdultes >= 1 && nbEnfants >= 1) {
            montant = forfaitFamille;
            typeLabel = "Pass Famille";
        } else {
            montant = tarifAdulte.multiply(BigDecimal.valueOf(Math.max(1, nbAdultes)))
                    .add(tarifEnfant.multiply(BigDecimal.valueOf(Math.max(0, nbEnfants))))
                    .add(tarifChien.multiply(BigDecimal.valueOf(Math.max(0, nbChiens))));
            typeLabel = "Individuel";
        }

        // Ajout du coût du repas
        if (mealOption != Participation.MealOption.SANS_REPAS) {
            montant = montant.add(tarifRepas.multiply(BigDecimal.valueOf(total)));
        }

        // 2. Application des réductions
        Long userId = resolveUserIdSilent();
        if (userId != null) {
            // Check for second participation discount (25%)
            try {
                if (participationController == null)
                    participationController = new ParticipationController();
                List<Participation> list = participationController.getByUserId(userId);
                // If this is the second participation for the user and not in edit mode
                if (list.size() == 1 && !editMode) {
                    montant = montant.multiply(new BigDecimal("0.75"));
                    typeLabel = "Early Booking (-25%)";
                }
            } catch (Exception ignored) {
            }

            // Check for official subscription discount (e.g. 10%)
            try {
                com.gestion.controllers.AbonnementController abonnementController = new com.gestion.controllers.AbonnementController();
                List<com.gestion.entities.Abonnement> plans = abonnementController.getAll().stream()
                        .filter(a -> a.getUserId().equals(userId) && a.estActif())
                        .collect(java.util.stream.Collectors.toList());

                if (!plans.isEmpty()) {
                    com.gestion.entities.Abonnement activePlan = plans.get(0);
                    if (!typeLabel.contains("%")) { // Prefer Early Booking if both apply or stack? User said
                                                    // "regardless of type".
                        typeLabel = activePlan.getType().getLabel() + " (Actif)";
                        // Récupération du taux de réduction (defaut: 10%)
                        int discountPercent = 10;
                        if (activePlan.getAvantages() != null && activePlan.getAvantages().containsKey("discounts")) {
                            Object disc = activePlan.getAvantages().get("discounts");
                            if (disc instanceof Number)
                                discountPercent = ((Number) disc).intValue();
                        }

                        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                                BigDecimal.valueOf(discountPercent).divide(new BigDecimal("100"), 2,
                                        RoundingMode.HALF_UP));
                        montant = montant.multiply(discountMultiplier);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur check abonnement: " + e.getMessage());
            }
        }

        // 3. Application du code promo
        if (currentPromoDiscount.compareTo(BigDecimal.ZERO) > 0) {
            montant = montant.multiply(BigDecimal.ONE.subtract(currentPromoDiscount));
        }

        montant = montant.setScale(2, RoundingMode.HALF_UP);

        if (labelTypeAbonnement != null) {
            labelTypeAbonnement.setText(typeLabel);
        }
        if (labelMontant != null) {
            labelMontant.setText(montant.toPlainString());
        }
    }

    private void clearErrors() {
        hideError(errorUserId);
        hideError(errorEvenementId);
        hideError(errorGroupe);
        hideError(errorGlobal);
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);

            // If it's the global error, also show the scroll container
            if (label == errorGlobal && errorScroll != null) {
                errorScroll.setVisible(true);
                errorScroll.setManaged(true);
            }
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);

            // If it's the global error, also hide the scroll container
            if (label == errorGlobal && errorScroll != null) {
                errorScroll.setVisible(false);
                errorScroll.setManaged(false);
            }
        }
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private int parseInt(String text, int defaultValue) {
        if (text == null || text.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long resolveUserId() {
        String idText = inputUserId != null ? inputUserId.getText().trim() : "";
        String nameText = inputUserName != null ? inputUserName.getText().trim() : "";

        try {
            if (!idText.isEmpty()) {
                int id = Integer.parseInt(idText);
                User u = userService.getUserById(id);
                if (u != null)
                    return (long) id;
                showError(errorUserId, "Utilisateur introuvable.");
                return null;
            } else if (!nameText.isEmpty()) {
                User u = userService.getUserByName(nameText);
                if (u != null) {
                    inputUserId.setText(String.valueOf(u.getId()));
                    return (long) u.getId();
                }
                showError(errorUserId, "Utilisateur introuvable.");
                return null;
            }
        } catch (Exception e) {
        }
        showError(errorUserId, "ID ou Nom requis.");
        return null;
    }

    private Long resolveUserIdSilent() {
        String idText = inputUserId != null ? inputUserId.getText().trim() : "";
        String nameText = inputUserName != null ? inputUserName.getText().trim() : "";
        try {
            if (!idText.isEmpty()) {
                return (long) Integer.parseInt(idText);
            } else if (!nameText.isEmpty()) {
                User u = userService.getUserByName(nameText);
                if (u != null)
                    return (long) u.getId();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private void closeWindow() {
        if (formTitle != null && formTitle.getScene() != null) {
            Stage stage = (Stage) formTitle.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}
