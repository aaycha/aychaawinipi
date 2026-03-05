package com.gestion.controllers;

import com.gestion.entities.Evenement;
import com.gestion.services.EvenementService;
import com.gestion.tools.Session;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ListeEvenementsController {

    @FXML
    private StackPane root;

    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnClearSearch;

    @FXML
    private ComboBox<String> cbType;
    @FXML
    private ComboBox<String> cbSort;
    @FXML
    private DatePicker dpFrom;
    @FXML
    private DatePicker dpTo;

    @FXML
    private FlowPane flowEvents;
    @FXML
    private Label lblMsg;
    @FXML
    private HBox bottomDock;

    @FXML
    private Button btnFiltrer;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnAjouterBottom;

    @FXML
    private ImageView bgImage;
    @FXML
    private ImageView imgLogo;

    @FXML
    private ChoiceBox<String> cbRole;
    @FXML
    private ToggleButton btnTheme;

    private final EvenementService service = new EvenementService();

    private List<Evenement> all = new ArrayList<>();
    private List<Evenement> lastDisplayedList = new ArrayList<>();
    private Evenement selected;

    private EventCardController lastSelectedCard;

    // Theme
    private static final String CSS_BASE = "/Feryel/css/events-base.css";
    private static final String CSS_DARK = "/Feryel/css/theme-dark.css";
    private static final String CSS_LIGHT = "/Feryel/css/theme-light.css";
    private boolean darkMode = true;

    // debounce for live search
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);

        // appliquer le thème quand la scene existe
        Platform.runLater(() -> {
            if (root != null && root.getScene() != null) {
                applyTheme(root.getScene(), true); // start dark
            }
        });

        initRoleChoiceBox();
        applyRoleUi();

        initFilters();
        initLiveSearch();

        if (btnFiltrer != null)
            btnFiltrer.setOnAction(e -> onFiltrer());
        if (btnSupprimer != null)
            btnSupprimer.setOnAction(e -> onSupprimer());
        if (btnAjouterBottom != null)
            btnAjouterBottom.setOnAction(e -> onGoAjouter());

        loadFromDB();
    }

    // ===================== THEME =====================

    @FXML
    private void toggleTheme() {
        darkMode = !darkMode;
        if (root != null && root.getScene() != null) {
            applyTheme(root.getScene(), darkMode);
        }
    }

    private void applyTheme(Scene scene, boolean dark) {
        if (scene == null)
            return;

        scene.getStylesheets().removeIf(s -> s.endsWith("theme-dark.css") ||
                s.endsWith("theme-light.css") ||
                s.endsWith("events-base.css"));

        URL themeUrl = getClass().getResource(dark ? CSS_DARK : CSS_LIGHT);
        URL baseUrl = getClass().getResource(CSS_BASE);

        if (themeUrl == null)
            throw new IllegalStateException("CSS introuvable: " + (dark ? CSS_DARK : CSS_LIGHT));
        if (baseUrl == null)
            throw new IllegalStateException("CSS introuvable: " + CSS_BASE);

        scene.getStylesheets().add(themeUrl.toExternalForm());
        scene.getStylesheets().add(baseUrl.toExternalForm());

        darkMode = dark;

        if (btnTheme != null) {
            btnTheme.setText(darkMode ? "🌙" : "☀️");
            btnTheme.setSelected(darkMode);
        }
    }

    @FXML
    private void onGoBack() {
        // ✅ Corrigé: Utilisation de SceneUtil dans le même package
        SceneUtil.switchTo("/views/utilisateur/espace-utilisateur.fxml", "Espace Utilisateur");
    }

    // ===================== ROLE =====================

    private void initRoleChoiceBox() {
        if (cbRole == null)
            return;

        cbRole.getItems().setAll("USER", "ADMIN");
        cbRole.setValue(isAdmin() ? "ADMIN" : "USER");

        cbRole.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null)
                return;

            setRole(newV);
            applyRoleUi();
        });
    }

    private void applyRoleUi() {
        boolean admin = isAdmin();

        if (btnSupprimer != null) {
            btnSupprimer.setVisible(admin);
            btnSupprimer.setManaged(admin);
        }
        if (btnAjouterBottom != null) {
            btnAjouterBottom.setVisible(admin);
            btnAjouterBottom.setManaged(admin);
        }
        if (bottomDock != null) {
            bottomDock.setVisible(admin);
            bottomDock.setManaged(admin);
        }
    }

    private boolean isAdmin() {
        if (Session.getInstance() == null)
            return false;
        if (Session.getInstance().getCurrentUser() == null)
            return false;
        String role = Session.getInstance().getCurrentUser().getRole();
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    private void setRole(String role) {
        if (Session.getInstance() != null && Session.getInstance().getCurrentUser() != null) {
            Session.getInstance().getCurrentUser().setRole(role);
        }
    }

    // ===================== FILTERS + SEARCH =====================

    private void initFilters() {
        if (cbType != null) {
            cbType.getItems().setAll("TOUS", "SOIREE", "RANDONNEE", "CAMPING", "SEJOUR");
            cbType.setValue("TOUS");
        }

        if (cbSort != null) {
            cbSort.getItems().setAll("Titre", "Date début", "Type", "Le plus vu");
            cbSort.setValue("Titre");
            cbSort.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                if (lastDisplayedList != null && !lastDisplayedList.isEmpty()) {
                    refreshCards(applySort(lastDisplayedList));
                }
            });
        }

        if (dpFrom != null)
            dpFrom.setValue(null);
        if (dpTo != null)
            dpTo.setValue(null);
    }

    private void initLiveSearch() {
        if (txtSearch == null)
            return;

        if (btnClearSearch != null) {
            btnClearSearch.setVisible(false);
            btnClearSearch.setManaged(false);
        }

        searchDebounce.setOnFinished(e -> onSearch());

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            if (btnClearSearch != null) {
                btnClearSearch.setVisible(hasText);
                btnClearSearch.setManaged(hasText);
            }
            searchDebounce.playFromStart();
        });
    }

    @FXML
    private void onClearSearch() {
        if (txtSearch != null)
            txtSearch.clear();
        onSearch();
    }

    // ===================== DB =====================

    private void loadFromDB() {
        try {
            all = service.getAll();
            refreshCards(applySort(all));
            clearSelection();
            updateRecoAfterLoad();
            if (lblMsg != null)
                lblMsg.setText("✓ " + all.size() + " événement(s)");
        } catch (Exception ex) {
            if (lblMsg != null)
                lblMsg.setText("❌ Erreur chargement DB");
            ex.printStackTrace();
        }
    }

    // ===================== RENDER =====================

    private void refreshCards(List<Evenement> events) {
        if (flowEvents == null)
            return;

        lastDisplayedList = events == null ? new ArrayList<>() : new ArrayList<>(events);
        flowEvents.getChildren().clear();
        clearSelection();

        if (events == null || events.isEmpty()) {
            if (lblMsg != null)
                lblMsg.setText("ℹ️ Aucun événement à afficher");
            return;
        }

        URL url = getClass().getResource("/Feryel/EventCard.fxml");
        if (url == null)
            throw new IllegalStateException("EventCard.fxml introuvable. Chemin attendu: /Feryel/EventCard.fxml");

        for (Evenement ev : events) {
            try {
                FXMLLoader loader = new FXMLLoader(url);
                Node card = loader.load();

                EventCardController c = loader.getController();
                c.setSelected(false);

                c.setData(ev,
                        e -> {
                            selected = e;
                            if (lastSelectedCard != null)
                                lastSelectedCard.setSelected(false);
                            lastSelectedCard = c;
                            lastSelectedCard.setSelected(true);

                            if (lblMsg != null)
                                lblMsg.setText("✓ Sélectionné: " + safe(e.getTitre()));
                        },
                        e -> SceneUtil.switchToWithData("/Feryel/DetailsEvenement.fxml", "Détails Événement",
                                e.getIdEvent()));

                flowEvents.getChildren().add(card);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void clearSelection() {
        selected = null;
        if (lastSelectedCard != null) {
            lastSelectedCard.setSelected(false);
            lastSelectedCard = null;
        }
    }

    private void updateRecoAfterLoad() {
    }

    @FXML
    private void onGoAjouter() {
        SceneUtil.switchTo("/Feryel/AjouterEvenement.fxml", "Ajouter Événement");
    }

    @FXML
    private void onSearch() {
        String q = safe(txtSearch != null ? txtSearch.getText() : "").toLowerCase(Locale.ROOT);

        List<Evenement> filtered;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(all);
        } else {
            filtered = all.stream()
                    .filter(e -> safe(e.getTitre()).toLowerCase(Locale.ROOT).contains(q)
                            || safe(e.getLieu()).toLowerCase(Locale.ROOT).contains(q)
                            || safe(e.getDescription()).toLowerCase(Locale.ROOT).contains(q))
                    .collect(Collectors.toList());
        }

        filtered = applyCurrentTypeAndDateFilters(filtered);

        refreshCards(applySort(filtered));
        if (lblMsg != null)
            lblMsg.setText(q.isEmpty()
                    ? "✓ " + filtered.size() + " événement(s)"
                    : "✓ Résultats: " + filtered.size());
    }

    @FXML
    private void onFiltrer() {
        onSearch();
    }

    @FXML
    private void onSupprimer() {
        if (!isAdmin()) {
            if (lblMsg != null)
                lblMsg.setText("⛔ Action réservée à l'admin.");
            return;
        }

        if (selected == null) {
            if (lblMsg != null)
                lblMsg.setText("⚠️ Sélectionne un événement d’abord.");
            return;
        }

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer cet événement ?");
        a.setContentText(safe(selected.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    service.delete(selected.getIdEvent());
                    if (lblMsg != null)
                        lblMsg.setText("✅ Supprimé");
                    loadFromDB();
                } catch (Exception ex) {
                    if (lblMsg != null)
                        lblMsg.setText("❌ Erreur suppression");
                    ex.printStackTrace();
                }
            }
        });
    }

    private List<Evenement> applyCurrentTypeAndDateFilters(List<Evenement> base) {
        List<Evenement> tmp = new ArrayList<>(base);

        String type = cbType == null ? "TOUS" : cbType.getValue();
        if (type != null && !"TOUS".equalsIgnoreCase(type)) {
            tmp = tmp.stream()
                    .filter(e -> type.equalsIgnoreCase(safe(e.getType())))
                    .collect(Collectors.toList());
        }

        LocalDate from = dpFrom == null ? null : dpFrom.getValue();
        LocalDate to = dpTo == null ? null : dpTo.getValue();

        if (from != null) {
            tmp = tmp.stream()
                    .filter(e -> e.getDateDebut() != null && !e.getDateDebut().toLocalDate().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (to != null) {
            tmp = tmp.stream()
                    .filter(e -> e.getDateDebut() != null && !e.getDateDebut().toLocalDate().isAfter(to))
                    .collect(Collectors.toList());
        }

        return tmp;
    }

    private List<Evenement> applySort(List<Evenement> list) {
        if (list == null)
            return Collections.emptyList();

        String sort = cbSort == null ? "Titre" : cbSort.getValue();
        if (sort == null)
            sort = "Titre";

        List<Evenement> sorted = new ArrayList<>(list);

        switch (sort) {
            case "Date début":
                sorted.sort(Comparator.comparing(e -> e.getDateDebut() == null ? LocalDateTime.MIN : e.getDateDebut()));
                break;

            case "Type":
                sorted.sort(Comparator.comparing(e -> safe(e.getType()).toLowerCase(Locale.ROOT)));
                break;

            case "Le plus vu":
                sorted.sort(Comparator.comparingInt(Evenement::getNbVues).reversed());
                break;

            default:
                sorted.sort(Comparator.comparing(e -> safe(e.getTitre()).toLowerCase(Locale.ROOT)));
                break;
        }

        return sorted;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}