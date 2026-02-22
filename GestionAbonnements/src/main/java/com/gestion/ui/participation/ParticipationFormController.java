package com.gestion.ui.participation;

import com.gestion.tools.Session;

import com.gestion.controllers.ParticipationController;
import com.gestion.entities.Evenement;
import com.gestion.entities.User;
import com.gestion.entities.Participation;
import com.gestion.services.EvenementService;
import com.gestion.services.UserService;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulaire dédié pour la création / modification d'une participation,
 * avec saisie de nbAdultes / nbEnfants / nbChiens et pré‑visualisation du
 * tarif.
 */
public class ParticipationFormController {

    @FXML
    private Label formTitle;

    @FXML
    private ComboBox<User> comboUser;
    @FXML
    private ComboBox<Evenement> comboEvenement;
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
    private Button btnEnregistrer;

    private ParticipationController participationController;
    private Participation participation;
    private boolean editMode = false;
    private boolean adminMode = true;
    private Runnable onSaved;

    private boolean isFilteringUser = false;
    private boolean isFilteringEvent = false;

    private final UserService userService = new UserService();
    private final EvenementService eventService = new EvenementService();

    @FXML
    public void initialize() {
        // Chargement initial des données
        List<User> users;
        try {
            users = userService.recuperer();
        } catch (java.sql.SQLException e) {
            users = new ArrayList<>();
            e.printStackTrace();
        }
        List<Evenement> events = eventService.findAll();

        if (comboUser != null) {
            javafx.collections.ObservableList<User> userList = javafx.collections.FXCollections
                    .observableArrayList(users);
            javafx.collections.transformation.FilteredList<User> filteredUsers = new javafx.collections.transformation.FilteredList<>(
                    userList, p -> true);

            // Ajout d'un StringConverter pour gérer l'édition textuelle sans
            // ClassCastException
            comboUser.setConverter(new javafx.util.StringConverter<User>() {
                @Override
                public String toString(User u) {
                    return u == null ? "" : u.getName();
                }

                @Override
                public User fromString(String string) {
                    if (string == null || string.isBlank())
                        return null;
                    return comboUser.getItems().stream()
                            .filter(u -> u.getName().equalsIgnoreCase(string.trim()))
                            .findFirst().orElse(null);
                }
            });

            comboUser.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (isFilteringUser)
                    return;
                isFilteringUser = true;

                final String filter = newValue == null ? "" : newValue.trim().toLowerCase();

                javafx.application.Platform.runLater(() -> {
                    try {
                        filteredUsers.setPredicate(u -> {
                            if (filter.isEmpty())
                                return true;
                            return u.getName().toLowerCase().contains(filter);
                        });

                        if (!filter.isEmpty() && !comboUser.isShowing()) {
                            comboUser.show();
                        }
                    } finally {
                        isFilteringUser = false;
                    }
                });
            });

            comboUser.setItems(filteredUsers);

            // Auto-identification pour les nouvelles participations
            if (!editMode) {
                Integer currentId = Session.getInstance().getCurrentUserId();
                if (currentId != null) {
                    userList.stream()
                            .filter(u -> Integer.valueOf(u.getId()).equals(currentId))
                            .findFirst()
                            .ifPresent(u -> {
                                comboUser.setValue(u);
                                // Si on n'est pas admin, on verrouille
                                if (!adminMode) {
                                    comboUser.setDisable(true);
                                }
                            });
                }
            }

            // Listener pour la sélection
            comboUser.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isFilteringUser)
                    return; // Ignorer les changements pendant le filtrage
                if (newVal != null) {
                    clearError(errorUserId);
                }
            });
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
                if (isFilteringEvent)
                    return; // Ignorer les changements pendant le filtrage
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
    }

    private void clearError(Label label) {
        hideError(label);
    }

    public void setParticipationController(ParticipationController controller) {
        this.participationController = controller;
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
        this.editMode = (participation != null);

        if (formTitle != null) {
            formTitle.setText(editMode ? "Modifier la participation" : "Nouvelle participation");
        }

        if (participation != null) {
            if (comboUser != null && participation.getUserId() != null) {
                comboUser.getItems().stream()
                        .filter(u -> Integer.valueOf(u.getId()).equals(participation.getUserId().intValue()))
                        .findFirst()
                        .ifPresent(u -> comboUser.setValue(u));
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
        if (userId != null && comboUser != null && !adminMode) {
            comboUser.getItems().stream()
                    .filter(u -> Long.valueOf(u.getId()).equals(userId))
                    .findFirst()
                    .ifPresent(u -> {
                        comboUser.setValue(u);
                        comboUser.setDisable(true);
                    });
        }
    }

    @FXML
    void onEnregistrer() {
        clearErrors();
        List<String> errors = new ArrayList<>();

        Object userVal = comboUser != null ? comboUser.getValue() : null;
        User selectedUser = null;
        if (userVal instanceof User) {
            selectedUser = (User) userVal;
        } else if (userVal instanceof String) {
            String str = ((String) userVal).trim();
            if (!str.isEmpty()) {
                // Tentative de résolution intelligente par nom
                selectedUser = comboUser.getItems().stream()
                        .filter(u -> u.getName().equalsIgnoreCase(str))
                        .findFirst()
                        .orElseGet(() ->
                        // Deuxième tentative : contient (si unique ou premier match)
                        comboUser.getItems().stream()
                                .filter(u -> u.getName().toLowerCase().contains(str.toLowerCase()))
                                .findFirst().orElse(null));

                // Si trouvé via String, on met à jour la valeur de la combo pour éviter les
                // ambiguïtés
                if (selectedUser != null) {
                    comboUser.setValue(selectedUser);
                }
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

        Long userId = selectedUser != null ? Long.valueOf(selectedUser.getId()) : null;
        Long evenementId = selectedEvent != null ? Long.valueOf(selectedEvent.getIdEvent()) : null;

        Participation.TypeParticipation type = inputType != null ? inputType.getValue() : null;
        Participation.ContexteSocial contexte = inputContexte != null ? inputContexte.getValue() : null;

        if (userId == null) {
            errors.add("Sélection de l'utilisateur obligatoire.");
            showError(errorUserId, "Veuillez sélectionner un utilisateur dans la liste.");
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

        if (!errors.isEmpty()) {
            showError(errorGroupe, String.join("\n", errors));
            showError(errorGlobal, "Veuillez corriger les champs indiqués avant d'enregistrer.");
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
            if (inputBesoinsSpeciaux != null) {
                target.setBesoinsSpeciaux(inputBesoinsSpeciaux.getText());
            }

            // Persistence de l'abonnement utilisé
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

            if (editMode) {
                participationController.update(target);
                showInfo("Participation modifiée avec succès.");
            } else {
                participationController.create(target);
                showInfo("Participation créée avec succès.");
            }

            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (IllegalArgumentException ex) {
            showError(errorGlobal, ex.getMessage());
        } catch (Exception ex) {
            showError(errorGlobal, "Erreur lors de l'enregistrement : " + ex.getMessage());
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

        int total = Math.max(1, nbAdultes) + Math.max(0, nbEnfants);
        if (labelTotalParticipants != null) {
            labelTotalParticipants.setText("Total participants : " + total);
        }

        // 1. Calcul de base
        BigDecimal tarifAdulte = new BigDecimal("25.00");
        BigDecimal tarifEnfant = new BigDecimal("15.00");
        BigDecimal tarifChien = new BigDecimal("8.00");
        BigDecimal forfaitFamille = new BigDecimal("60.00");

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

        // 2. Application de la réduction Abonnement
        Object userVal = comboUser != null ? comboUser.getValue() : null;
        if (userVal instanceof User) {
            User selectedUser = (User) userVal;
            try {
                com.gestion.controllers.AbonnementController abonnementController = new com.gestion.controllers.AbonnementController();
                List<com.gestion.entities.Abonnement> plans = abonnementController.getAll().stream()
                        .filter(a -> a.getUserId().equals(Long.valueOf(selectedUser.getId())) && a.estActif())
                        .collect(java.util.stream.Collectors.toList());

                if (!plans.isEmpty()) {
                    com.gestion.entities.Abonnement activePlan = plans.get(0);
                    typeLabel = activePlan.getType().getLabel() + " (Actif)";

                    // Récupération du taux de réduction (defaut: 10%)
                    int discountPercent = 10;
                    if (activePlan.getAvantages() != null && activePlan.getAvantages().containsKey("discounts")) {
                        Object disc = activePlan.getAvantages().get("discounts");
                        if (disc instanceof Number)
                            discountPercent = ((Number) disc).intValue();
                    }

                    BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                            BigDecimal.valueOf(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                    montant = montant.multiply(discountMultiplier);
                }
            } catch (Exception e) {
                System.err.println("Erreur check abonnement: " + e.getMessage());
            }
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
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
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

    private void closeWindow() {
        if (formTitle != null && formTitle.getScene() != null) {
            Stage stage = (Stage) formTitle.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}
