/*package com.gestion.controllers;

import javafx.animation.ParallelTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur principal - Dashboard avec liste des gestions.
 * Clic sur un module = chargement de l'interface correspondante.
 */
/*public class MainController {

    public void loadView(String s) {
    }

    public ParallelTransition getMainContent() {
    }

    public enum Role {
        ADMIN,
        UTILISATEUR
    }

    private static Role currentRole = Role.ADMIN;

    @FXML private StackPane contentPane;
    @FXML private ScrollPane dashboardScroll;
    @FXML private FlowPane cardsContainer;
    @FXML private BorderPane moduleContentPane;
    @FXML private StackPane moduleStackPane;
    @FXML private Button backButton;
    @FXML private Label moduleTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private javafx.scene.control.ComboBox<String> roleCombo;

    private record ModuleGestion(String id, String titre, String description, String fxmlPath, String icon) {}

    private final List<ModuleGestion> modules = new ArrayList<>();

    @FXML
    public void initialize() {
        modules.add(new ModuleGestion("abonnement", "Abonnements",
                "Gerer les abonnements mensuels, annuels et premium",
                "/views/abonnement/abonnement.fxml", "\uD83D\uDCCB"));
        modules.add(new ModuleGestion("participation", "Participations",
                "Inscriptions aux evenements et activites",
                "/views/participation/participation.fxml", "\uD83D\uDC65"));
        modules.add(new ModuleGestion("restauration", "Restauration",
                "Menus, repas, restrictions et presences",
                "/views/restauration/restauration-main.fxml", "\uD83C\uDF74"));
        modules.add(new ModuleGestion("recommandations", "Recommandations IA",
                "Suggestions personnalisees pour vos aventures",
                "/views/recommandations/recommandations.fxml", "\uD83E\uDD16"));
        modules.add(new ModuleGestion("analytics", "Analytics",
                "Statistiques et tableaux de bord",
                "/views/analytics/analytics.fxml", "\uD83D\uDCC8"));

        for (ModuleGestion m : modules) {
            VBox card = createCard(m);
            cardsContainer.getChildren().add(card);
        }

        if (roleCombo != null) {
            roleCombo.getSelectionModel().select("Admin");
            roleCombo.setOnAction(e -> {
                String value = roleCombo.getValue();
                if ("Utilisateur".equalsIgnoreCase(value)) {
                    setCurrentRole(Role.UTILISATEUR);
                    userLabel.setText("Espace utilisateur LAMMA");
                } else {
                    setCurrentRole(Role.ADMIN);
                    userLabel.setText("Espace administrateur LAMMA");
                }
            });
        }

        if (userLabel != null) {
            userLabel.setText("Espace administrateur LAMMA");
        }
    }

    private VBox createCard(ModuleGestion m) {
        VBox card = new VBox(12);
        card.getStyleClass().add("voyage-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(300);
        card.setMinHeight(140);
        card.setOnMouseClicked(e -> chargerModule(m));

        Label icon = new Label(m.icon());
        icon.getStyleClass().add("voyage-card-icon");
        icon.setStyle("-fx-font-size: 36px;");

        Label titre = new Label(m.titre());
        titre.getStyleClass().add("voyage-card-titre");
        titre.setWrapText(true);

        Label desc = new Label(m.description());
        desc.getStyleClass().add("voyage-card-desc");
        desc.setWrapText(true);
        desc.setMaxWidth(260);

        Button btn = new Button("Acceder");
        btn.getStyleClass().add("voyage-card-btn");
        btn.setOnAction(e -> chargerModule(m));

        card.getChildren().addAll(icon, titre, desc, btn);
        return card;
    }

    private void chargerModule(ModuleGestion m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(m.fxmlPath()));
            Parent root = loader.load();

            moduleStackPane.getChildren().clear();
            moduleStackPane.getChildren().add(root);

            moduleTitleLabel.setText(m.titre());
            statusLabel.setText("Module " + m.titre() + " charge");

            dashboardScroll.setVisible(false);
            dashboardScroll.setManaged(false);
            moduleContentPane.setVisible(true);
            moduleContentPane.setManaged(true);

            // Forcer l'actualisation des données après que le FXML soit ajouté à la scène
            javafx.application.Platform.runLater(() -> {
                try {
                    Object controller = loader.getController();
                    if (controller != null) {
                        // Appeler onActualiser() si la méthode existe
                        java.lang.reflect.Method actualiserMethod = null;
                        try {
                            actualiserMethod = controller.getClass().getMethod("onActualiser");
                        } catch (NoSuchMethodException e) {
                            // Méthode n'existe pas, ce n'est pas grave
                        }
                        
                        if (actualiserMethod != null) {
                            actualiserMethod.invoke(controller);
                        }
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de réflexion
                }
            });

        } catch (IOException e) {
            statusLabel.setText("Erreur: " + m.titre() + " - Fichier non trouve: " + m.fxmlPath());
            dashboardScroll.setVisible(true);
            dashboardScroll.setManaged(true);
            moduleContentPane.setVisible(false);
            moduleContentPane.setManaged(false);
        }
    }

    public static Role getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(Role role) {
        if (role != null) {
            currentRole = role;
        }
    }

    @FXML
    void retourDashboard() {
        dashboardScroll.setVisible(true);
        dashboardScroll.setManaged(true);
        moduleContentPane.setVisible(false);
        moduleContentPane.setManaged(false);
        statusLabel.setText("Pret - Selectionnez un module");
    }
}*/


package com.gestion.controllers;

import com.gestion.MainApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur principal du dashboard LAMMA
 * - Affiche les cartes des modules de gestion
 * - Gère le basculement Admin / Utilisateur
 * - Charge dynamiquement les modules sélectionnés dans un StackPane
 */
public class MainController {

    public enum Role {
        ADMIN,
        UTILISATEUR
    }

    private static Role currentRole = Role.ADMIN;

    // ─── Panneaux principaux ────────────────────────────────────────
    @FXML private ScrollPane dashboardScroll;
    @FXML private ScrollPane userDashboardScroll;
    @FXML private FlowPane cardsContainer;
    @FXML private BorderPane moduleContentPane;
    @FXML
    public StackPane moduleStackPane;

    private static MainController instance;

    // ─── Éléments d'en-tête / statut ────────────────────────────────
    @FXML private Label moduleTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button backButton;

    // ─── Structure des modules ──────────────────────────────────────
    private record ModuleGestion(
            String id,
            String titre,
            String description,
            String fxmlPath,
            String icon
    ) {}

    private final List<ModuleGestion> modules = new ArrayList<>();

    @FXML
    public void initialize() {
        instance = this;
        initModules();
        createDashboardCards();
        setupRoleSelector();
        updateUIForRole();
        refreshDashboardVisibility();

        if (statusLabel != null) {
            statusLabel.setText("Prêt – Sélectionnez un module");
        }
        if (backButton != null) {
            backButton.setVisible(false);
        }
    }

    /** Permet à l'espace utilisateur de charger une section (Mes Participations, Mes Repas, etc.) */
    public void loadUserSection(String fxmlPath, String title) {
        if (statusLabel != null) statusLabel.setText("Chargement de " + title + "...");
        if (backButton != null) backButton.setVisible(true);
        if (moduleStackPane != null) moduleStackPane.setCursor(Cursor.WAIT);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                if (loader.getLocation() == null) {
                    throw new IOException("FXML introuvable : " + fxmlPath);
                }
                Parent root = loader.load();
                if (moduleStackPane != null) moduleStackPane.getChildren().setAll(root);
                if (moduleTitleLabel != null) moduleTitleLabel.setText(title);
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try {
                        Method refresh = ctrl.getClass().getMethod("onActualiser");
                        refresh.invoke(ctrl);
                    } catch (NoSuchMethodException ignored) {}
                    catch (Exception ex) {
                        System.err.println("Erreur onActualiser : " + ex.getMessage());
                    }
                }
                showModuleContent(true);
                if (userDashboardScroll != null) {
                    userDashboardScroll.setVisible(false);
                    userDashboardScroll.setManaged(false);
                }
                if (statusLabel != null) statusLabel.setText("Module " + title + " chargé");
            } catch (IOException e) {
                if (statusLabel != null) statusLabel.setText("Erreur chargement " + title);
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger " + title, e.getMessage());
            } finally {
                if (moduleStackPane != null) moduleStackPane.setCursor(Cursor.DEFAULT);
            }
        });
    }

    public static MainController getInstance() {
        return instance;
    }

    private void refreshDashboardVisibility() {
        boolean isUser = currentRole == Role.UTILISATEUR;
        if (dashboardScroll != null) {
            dashboardScroll.setVisible(!isUser);
            dashboardScroll.setManaged(!isUser);
        }
        if (userDashboardScroll != null) {
            userDashboardScroll.setVisible(isUser);
            userDashboardScroll.setManaged(isUser);
        }
    }

    private void initModules() {
        modules.clear();

        modules.add(new ModuleGestion(
                "abonnement", "Abonnements",
                "Gérer les abonnements mensuels, annuels et premium",
                "/views/abonnement/abonnement.fxml", "\uD83D\uDCCB"
        ));

        modules.add(new ModuleGestion(
                "participation", "Participations",
                "Inscriptions aux événements et activités",
                "/views/participation/participation.fxml", "\uD83D\uDC65"
        ));

        modules.add(new ModuleGestion(
                "restaurants", "Restaurants",
                "Gérer les restaurants partenaires",
                "/views/restaurant/restaurant-liste.fxml", "\uD83C\uDF74"
        ));

        modules.add(new ModuleGestion(
                "menus", "Menus",
                "Gérer les menus des restaurants",
                "/views/menu/menu-liste.fxml", "\uD83D\uDCD6"
        ));

        modules.add(new ModuleGestion(
                "repas", "Plats",
                "Gérer les plats et leur composition",
                "/views/repas/repas-liste.fxml", "\uD83C\uDF55"
        ));

        modules.add(new ModuleGestion(
                "restauration", "Restauration (Legacy)",
                "Menus, repas, restrictions et présences (Ancienne version)",
                "/views/restauration/restauration-main.fxml", "\uD83C\uDF74"
        ));

        modules.add(new ModuleGestion(
                "recommandations", "Recommandations IA",
                "Suggestions personnalisées pour vos aventures",
                "/views/recommandations/recommandations.fxml", "\uD83E\uDD16"
        ));

        modules.add(new ModuleGestion(
                "analytics", "Analytics",
                "Statistiques et tableaux de bord",
                "/views/analytics/analytics.fxml", "\uD83D\uDCC8"
        ));
    }

    private void createDashboardCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        for (ModuleGestion module : modules) {
            VBox card = createModuleCard(module);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createModuleCard(ModuleGestion m) {
        VBox card = new VBox(12);
        card.getStyleClass().add("voyage-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(24));
        card.setPrefWidth(300);
        card.setMinHeight(160);
        card.setMaxWidth(320);

        card.setOnMouseEntered(e -> card.setCursor(Cursor.HAND));
        card.setOnMouseClicked(e -> chargerModule(m));

        Label icon = new Label(m.icon());
        icon.getStyleClass().add("voyage-card-icon");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #1890ff;");

        Label titre = new Label(m.titre());
        titre.getStyleClass().add("voyage-card-titre");
        titre.setWrapText(true);

        Label desc = new Label(m.description());
        desc.getStyleClass().add("voyage-card-desc");
        desc.setWrapText(true);
        desc.setMaxWidth(260);

        Button btn = new Button("Accéder →");
        btn.getStyleClass().addAll("voyage-card-btn", "btn-primary");
        btn.setOnAction(e -> chargerModule(m));

        card.getChildren().addAll(icon, titre, desc, btn);
        return card;
    }

    private void setupRoleSelector() {
        if (roleCombo == null) return;

        roleCombo.getItems().setAll("Admin", "Utilisateur");
        roleCombo.getSelectionModel().select("Admin");

        roleCombo.setOnAction(e -> {
            String selected = roleCombo.getValue();
            Role newRole = "Utilisateur".equalsIgnoreCase(selected) ? Role.UTILISATEUR : Role.ADMIN;
            setCurrentRole(newRole);

            if (userLabel != null) {
                userLabel.setText(newRole == Role.ADMIN ?
                        "Espace administrateur LAMMA" :
                        "Espace utilisateur LAMMA");
            }

            if (statusLabel != null) {
                statusLabel.setText("Mode changé : " + selected);
            }

            refreshDashboardVisibility();
        });
    }

    private void updateUIForRole() {
        if (userLabel != null) {
            userLabel.setText(currentRole == Role.ADMIN ?
                    "Espace administrateur LAMMA" :
                    "Espace utilisateur LAMMA");
        }
    }

    private void chargerModule(ModuleGestion module) {
        if (statusLabel != null) {
            statusLabel.setText("Chargement du module " + module.titre() + "...");
        }
        if (backButton != null) {
            backButton.setVisible(true);
        }
        if (moduleStackPane != null) {
            moduleStackPane.setCursor(Cursor.WAIT);
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(module.fxmlPath()));
                if (loader.getLocation() == null) {
                    throw new IOException("FXML introuvable : " + module.fxmlPath());
                }

                Parent root = loader.load();

                if (moduleStackPane != null) {
                    moduleStackPane.getChildren().setAll(root);
                }

                if (moduleTitleLabel != null) {
                    moduleTitleLabel.setText(module.titre());
                }

                // Appel automatique à onActualiser() si la méthode existe
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try {
                        Method refresh = ctrl.getClass().getMethod("onActualiser");
                        refresh.invoke(ctrl);
                    } catch (NoSuchMethodException ignored) {
                        // Méthode absente → normal
                    } catch (Exception ex) {
                        System.err.println("Erreur lors de l'appel à onActualiser() : " + ex.getMessage());
                    }
                }

                showModuleContent(true);

                if (statusLabel != null) {
                    statusLabel.setText("Module " + module.titre() + " chargé avec succès");
                }

            } catch (IOException e) {
                if (statusLabel != null) {
                    statusLabel.setText("Erreur chargement " + module.titre());
                }
                showAlert(
                        Alert.AlertType.ERROR,
                        "Erreur de chargement",
                        "Impossible de charger le module " + module.titre(),
                        "Chemin FXML : " + module.fxmlPath() + "\n\n" + e.getMessage()
                );
                showModuleContent(false);

            } finally {
                if (moduleStackPane != null) {
                    moduleStackPane.setCursor(Cursor.DEFAULT);
                }
            }
        });
    }

    private void showModuleContent(boolean showModule) {
        if (dashboardScroll != null) {
            dashboardScroll.setVisible(!showModule);
            dashboardScroll.setManaged(!showModule);
        }
        if (moduleContentPane != null) {
            moduleContentPane.setVisible(showModule);
            moduleContentPane.setManaged(showModule);
        }
    }

    @FXML
    public void retourDashboard() {
        showModuleContent(false);
        if (statusLabel != null) {
            statusLabel.setText("Prêt – Sélectionnez un module");
        }
        if (moduleTitleLabel != null) {
            moduleTitleLabel.setText("Dashboard LAMMA");
        }
        if (backButton != null) {
            backButton.setVisible(false);
        }
        refreshDashboardVisibility();
    }

    // ────────────────────────────────────────────────
    // Gestion statique du rôle (accessible depuis n'importe quel contrôleur)
    // ────────────────────────────────────────────────

    public static Role getCurrentRole() {
        return currentRole != null ? currentRole : Role.ADMIN;
    }

    public static void setCurrentRole(Role role) {
        if (role != null) {
            currentRole = role;
        }
    }
    @FXML
    private void loadAbonnements() {
        MainApplication.loadView("/views/abonnements/abonnement-list.fxml");
    }

    // ────────────────────────────────────────────────
    // Utilitaires
    // ────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}