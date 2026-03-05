package com.gestion.controllers;

import com.gestion.entities.equipementwael;
import com.gestion.services.equipemementservicewael;
import com.gestion.services.EquipementVueService;
import com.gestion.tools.ClientIdUtil;
import com.gestion.tools.UserSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class EquipementStoreController implements Initializable {

    @FXML
    private FlowPane cardsPane;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterCategorie;
    @FXML
    private Label countLabel;
    @FXML
    private Button btnAjouter;
    @FXML
    private Label weatherLabel;

    private equipemementservicewael service;
    private EquipementVueService vueService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Flag pour éviter que le listener de la ComboBox ne réagisse lors des mises à
    // jour programmatiques
    private boolean suspendCategoryListener = false;

    // Coordonnées de Tunis, Tunisie
    private static final double LATITUDE = 36.80;
    private static final double LONGITUDE = 10.18;

    // Météo actuelle
    private static final String WEATHER_CURRENT_URL = "https://api.open-meteo.com/v1/forecast?latitude=" + LATITUDE +
            "&longitude=" + LONGITUDE +
            "&hourly=temperature_2m&current=rain,precipitation,wind_speed_10m,temperature_2m&timezone=auto";

    // Prévisions quotidiennes sur 14 jours (2 semaines) avec codes météo
    private static final String WEATHER_DAILY_14_URL = "https://api.open-meteo.com/v1/forecast?latitude=" + LATITUDE +
            "&longitude=" + LONGITUDE +
            "&daily=temperature_2m_max,temperature_2m_min,weathercode&forecast_days=14&timezone=auto";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new equipemementservicewael();
        vueService = new EquipementVueService();
        // si aucun userId présent, demander une saisie simple (popup) pour l'identifier
        String uid = UserSession.getUserId();
        if (uid == null || uid.isEmpty()) {
            Platform.runLater(() -> {
                TextInputDialog d = new TextInputDialog();
                d.setTitle("Identifiant utilisateur");
                d.setHeaderText("Entrez votre userId pour que vos visites soient comptées (ex: email ou login)");
                d.setContentText("UserId : ");
                d.showAndWait().ifPresent(v -> {
                    if (v != null && !v.trim().isEmpty()) {
                        UserSession.saveUserId(v.trim());
                    }
                });
            });
        }
        searchField.textProperty().addListener((o, ov, nv) -> charger());
        filterCategorie.valueProperty().addListener((o, ov, nv) -> {
            if (!suspendCategoryListener)
                charger();
        });
        charger();
        chargerMeteo();
        if (weatherLabel != null) {
            weatherLabel.setOnMouseClicked(e -> chargerPrevisions14Jours());
            weatherLabel.setTooltip(new Tooltip("Clique pour voir la météo sur 14 jours"));
        }
    }

    private void charger() {
        // Conserver la sélection courante avant de rafraîchir la liste
        String previousSelection = null;
        try {
            previousSelection = filterCategorie.getValue();
        } catch (Exception ex) {
            /* ignore */ }

        List<equipementwael> all = service.afficherDisponibles();

        // Construire la liste des catégories triées
        List<String> categories = all.stream()
                .map(equipementwael::getCategorie)
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        // Préfixer par "Toutes catégories"
        categories.add(0, "Toutes catégories");

        // Mettre à jour les items sans déclencher le listener
        suspendCategoryListener = true;
        try {
            filterCategorie.getItems().setAll(categories);
            if (previousSelection != null && filterCategorie.getItems().contains(previousSelection)) {
                filterCategorie.setValue(previousSelection);
            } else {
                filterCategorie.setValue("Toutes catégories");
            }
        } finally {
            suspendCategoryListener = false;
        }

        String search = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String cat = filterCategorie.getValue();
        final String catFilter = "Toutes catégories".equals(cat) ? null : cat;

        List<equipementwael> filtered = all.stream()
                .filter(e -> {
                    boolean matchSearch = search.isEmpty() ||
                            (e.getNom() != null && e.getNom().toLowerCase().contains(search)) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(search)) ||
                            (e.getCategorie() != null && e.getCategorie().toLowerCase().contains(search));
                    boolean matchCat = catFilter == null
                            || (e.getCategorie() != null && e.getCategorie().trim().equalsIgnoreCase(catFilter.trim()));
                    return matchSearch && matchCat;
                })
                .collect(Collectors.toList());

        cardsPane.getChildren().clear();
        for (equipementwael e : filtered) {
            cardsPane.getChildren().add(creerCarte(e));
        }
        countLabel.setText(filtered.size() + " équipement(s)");
    }

    /** Charge la météo courante via l'API Open‑Meteo (sans clé). */
    private void chargerMeteo() {
        if (weatherLabel == null)
            return;
        weatherLabel.setText("Météo: chargement...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEATHER_CURRENT_URL))
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).thenAccept(json -> Platform.runLater(() -> {
            if (json == null) {
                weatherLabel.setText("Météo: indisponible");
                return;
            }
            String resume = parseCurrentWeather(json);
            if (resume == null || resume.isEmpty()) {
                weatherLabel.setText("Météo: indisponible");
            } else {
                weatherLabel.setText(resume);
            }
        }));
    }

    /**
     * Parse grossier de la section \"current\" du JSON Open‑Meteo (sans dépendance
     * JSON).
     */
    private String parseCurrentWeather(String json) {
        try {
            int idxCurrent = json.indexOf("\"current\"");
            if (idxCurrent < 0)
                return null;
            int start = json.indexOf('{', idxCurrent);
            if (start < 0)
                return null;
            int end = json.indexOf("}", start);
            if (end < 0)
                return null;
            String current = json.substring(start + 1, end);

            Double temp = extractDouble(current, "\"temperature_2m\"");
            Double wind = extractDouble(current, "\"wind_speed_10m\"");
            Double rain = extractDouble(current, "\"rain\"");

            StringBuilder sb = new StringBuilder("Météo: ");
            if (temp != null) {
                sb.append(String.format("%.1f°C", temp));
            }
            if (wind != null) {
                if (sb.length() > 8)
                    sb.append(" · ");
                sb.append(String.format("Vent %.0f km/h", wind));
            }
            if (rain != null) {
                if (sb.length() > 8)
                    sb.append(" · ");
                sb.append(String.format("Pluie %.1f mm", rain));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0)
            return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0)
            return null;
        int end = colon + 1;
        while (end < json.length() && Character.isWhitespace(json.charAt(end)))
            end++;
        int stop = end;
        while (stop < json.length() && "0123456789+-.eE".indexOf(json.charAt(stop)) >= 0)
            stop++;
        String num = json.substring(end, stop);
        if (num.isEmpty())
            return null;
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Charge les prévisions quotidiennes sur 14 jours et les affiche dans une boîte
     * de dialogue.
     */
    private void chargerPrevisions14Jours() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEATHER_DAILY_14_URL))
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).thenAccept(json -> Platform.runLater(() -> {
            if (json == null) {
                new Alert(Alert.AlertType.INFORMATION, "Prévisions météo indisponibles.").showAndWait();
                return;
            }
            // Extraire le bloc \"daily\" pour construire un affichage visuel (cartes +
            // courbe)
            try {
                int idxDaily = json.indexOf("\"daily\"");
                if (idxDaily < 0) {
                    new Alert(Alert.AlertType.INFORMATION, "Prévisions météo indisponibles.").showAndWait();
                    return;
                }
                int start = json.indexOf('{', idxDaily);
                if (start < 0) {
                    new Alert(Alert.AlertType.INFORMATION, "Prévisions météo indisponibles.").showAndWait();
                    return;
                }
                int brace = 1;
                int i = start + 1;
                for (; i < json.length() && brace > 0; i++) {
                    char c = json.charAt(i);
                    if (c == '{')
                        brace++;
                    else if (c == '}')
                        brace--;
                }
                if (brace != 0) {
                    new Alert(Alert.AlertType.INFORMATION, "Prévisions météo indisponibles.").showAndWait();
                    return;
                }
                String daily = json.substring(start + 1, i - 1);

                String[] dates = extractStringArray(daily, "\"time\"");
                double[] tMax = extractDoubleArray(daily, "\"temperature_2m_max\"");
                double[] tMin = extractDoubleArray(daily, "\"temperature_2m_min\"");
                double[] codes = extractDoubleArray(daily, "\"weathercode\"");
                if (dates == null || tMax == null || tMin == null) {
                    new Alert(Alert.AlertType.INFORMATION, "Prévisions météo indisponibles.").showAndWait();
                    return;
                }

                int n = Math.min(14, Math.min(dates.length, Math.min(tMax.length, tMin.length)));
                DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outDay = DateTimeFormatter.ofPattern("EEE");
                DateTimeFormatter outDate = DateTimeFormatter.ofPattern("dd/MM");

                // Prépare les catégories pour le graphique
                CategoryAxis xAxis = new CategoryAxis();
                NumberAxis yAxis = new NumberAxis();
                yAxis.setLabel("Température moyenne (°C)");
                LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
                chart.setLegendVisible(true);
                chart.setAnimated(false);
                chart.setCreateSymbols(true);
                chart.setHorizontalGridLinesVisible(false);
                chart.setVerticalGridLinesVisible(false);
                chart.setStyle("-fx-background-color: transparent;");

                XYChart.Series<String, Number> serieMoy = new XYChart.Series<>();
                serieMoy.setName("Moyenne");

                javafx.scene.layout.HBox cards = new javafx.scene.layout.HBox(8);
                cards.setStyle("-fx-padding: 8 0 4 0;");

                for (int k = 0; k < n; k++) {
                    String d = dates[k];
                    if (d.length() >= 10)
                        d = d.substring(0, 10);
                    LocalDate ld = LocalDate.parse(d, inFmt);
                    String label = outDay.format(ld);
                    String dateLabel = outDate.format(ld);

                    xAxis.getCategories().add(label);
                    double moy = (tMax[k] + tMin[k]) / 2.0;
                    serieMoy.getData().add(new XYChart.Data<>(label, moy));

                    // Petite carte pour le jour (icône + températures)
                    javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(4);
                    card.setStyle(
                            "-fx-background-color: rgba(15,23,42,0.9); -fx-background-radius: 10; -fx-padding: 6 8;");

                    Label dayLbl = new Label(label);
                    dayLbl.setStyle("-fx-text-fill: #e5e7eb; -fx-font-weight: bold; -fx-font-size: 11;");
                    Label dateLbl = new Label(dateLabel);
                    dateLbl.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 10;");

                    String icon = "☀"; // par défaut
                    if (codes != null && k < codes.length) {
                        icon = iconForWeatherCode((int) Math.round(codes[k]));
                    }
                    Label iconLbl = new Label(icon);
                    iconLbl.setStyle("-fx-font-size: 14;");

                    String tempText = Math.round(tMax[k]) + "° / " + Math.round(tMin[k]) + "°";
                    Label tempLbl = new Label(tempText);
                    tempLbl.setStyle("-fx-text-fill: #f97316; -fx-font-size: 11; -fx-font-weight: 600;");

                    card.getChildren().addAll(dayLbl, dateLbl, iconLbl, tempLbl);
                    cards.getChildren().add(card);
                }

                chart.getData().addAll(serieMoy);

                javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(cards);
                scroll.setFitToHeight(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

                javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
                root.setStyle("-fx-background-color: #0f172a; -fx-padding: 12;");
                Label title = new Label("Tunis – Prévisions sur 14 jours");
                title.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");

                root.getChildren().addAll(title, scroll, chart);

                Stage stage = new Stage();
                stage.setTitle("Météo – 14 jours (Tunis)");
                stage.setScene(new Scene(root, 640, 420));
                stage.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                String texte = parseDailyForecast14Jours(json);
                new Alert(Alert.AlertType.INFORMATION,
                        texte != null ? texte : "Prévisions météo indisponibles.").showAndWait();
            }
        }));
    }

    /** Parse la section \"daily\" pour construire un résumé sur 14 jours. */
    private String parseDailyForecast14Jours(String json) {
        try {
            int idxDaily = json.indexOf("\"daily\"");
            if (idxDaily < 0)
                return null;
            int start = json.indexOf('{', idxDaily);
            if (start < 0)
                return null;
            int brace = 1;
            int i = start + 1;
            for (; i < json.length() && brace > 0; i++) {
                char c = json.charAt(i);
                if (c == '{')
                    brace++;
                else if (c == '}')
                    brace--;
            }
            if (brace != 0)
                return null;
            String daily = json.substring(start + 1, i - 1);

            String[] dates = extractStringArray(daily, "\"time\"");
            double[] tMax = extractDoubleArray(daily, "\"temperature_2m_max\"");
            double[] tMin = extractDoubleArray(daily, "\"temperature_2m_min\"");
            if (dates == null || tMax == null || tMin == null)
                return null;

            int n = Math.min(14, Math.min(dates.length, Math.min(tMax.length, tMin.length)));
            DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("EEE dd/MM");

            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < n; k++) {
                String d = dates[k];
                if (d.length() >= 10)
                    d = d.substring(0, 10); // garde la partie date
                LocalDate ld = LocalDate.parse(d, inFmt);
                if (k > 0)
                    sb.append('\n');
                sb.append(outFmt.format(ld))
                        .append(" : min ")
                        .append(Math.round(tMin[k]))
                        .append("°C  / max ")
                        .append(Math.round(tMax[k]))
                        .append("°C");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String[] extractStringArray(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0)
            return null;
        int bracket = json.indexOf('[', idx);
        if (bracket < 0)
            return null;
        int end = json.indexOf(']', bracket);
        if (end < 0)
            return null;
        String inside = json.substring(bracket + 1, end);
        String[] parts = inside.split(",");
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
                s = s.substring(1, s.length() - 1);
            }
            parts[i] = s;
        }
        return parts;
    }

    private double[] extractDoubleArray(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0)
            return null;
        int bracket = json.indexOf('[', idx);
        if (bracket < 0)
            return null;
        int end = json.indexOf(']', bracket);
        if (end < 0)
            return null;
        String inside = json.substring(bracket + 1, end);
        String[] parts = inside.split(",");
        double[] res = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (s.isEmpty()) {
                res[i] = Double.NaN;
            } else {
                try {
                    res[i] = Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    res[i] = Double.NaN;
                }
            }
        }
        return res;
    }

    /** Map des codes météo Open‑Meteo vers une icône simple. */
    private String iconForWeatherCode(int code) {
        // Codes open‑meteo / WMO simplifiés
        if (code == 0)
            return "☀"; // ciel dégagé
        if (code == 1 || code == 2)
            return "🌤"; // peu nuageux
        if (code == 3)
            return "☁"; // nuageux
        if (code == 45 || code == 48)
            return "🌫"; // brouillard
        if (code >= 51 && code <= 57)
            return "🌦"; // bruine
        if (code >= 61 && code <= 67)
            return "🌧"; // pluie
        if (code >= 71 && code <= 77)
            return "🌨"; // neige
        if (code >= 80 && code <= 82)
            return "🌦"; // averses
        if (code >= 95 && code <= 99)
            return "⛈"; // orage
        return "☀";
    }

    private VBox creerCarte(equipementwael e) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-padding: 15; -fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 12; -fx-min-width: 200; -fx-max-width: 240;");
        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2)
                afficherDetails(e);
        });

        Label nom = new Label(e.getNom() != null ? e.getNom() : "");
        nom.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-wrap-text: true;");
        nom.setMaxWidth(200);

        Label cat = new Label(e.getCategorie() != null ? e.getCategorie() : "-");
        cat.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");

        Label prix = new Label(
                e.getPrix() != null ? new DecimalFormat("#,##0.00").format(e.getPrix()) + " TND" : "0 TND");
        prix.setStyle("-fx-text-fill: #F97316; -fx-font-weight: bold; -fx-font-size: 13;");

        // Remplacer le statut par le compteur de vues
        int vues = 0;
        try {
            vues = vueService.getViewsCount(e.getId());
        } catch (Exception ignored) {
        }
        Label vuesLbl = new Label("👁 " + vues);
        vuesLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: bold;");

        HBox boutons = new HBox(6);
        boutons.setStyle("-fx-padding: 6 0 0 0;");
        Button btnModifier = new Button("✏");
        btnModifier.setStyle(
                "-fx-background-color: rgba(249,115,22,0.8); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");
        btnModifier.setOnAction(ev -> onModifierEquipement(e));
        Button btnSupprimer = new Button("🗑");
        btnSupprimer.setStyle(
                "-fx-background-color: rgba(239,68,68,0.8); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12;");
        btnSupprimer.setOnAction(ev -> onSupprimerEquipement(e));
        boutons.getChildren().addAll(btnModifier, btnSupprimer);

        card.getChildren().addAll(nom, cat, prix, vuesLbl, boutons);
        return card;
    }

    private void onModifierEquipement(equipementwael e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/wael/EquipementFormView.fxml"));
            Parent root = loader.load();
            EquipementFormController ctrl = loader.getController();
            ctrl.setEquipement(e);
            ctrl.setOnSaved(this::charger);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("Modifier équipement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }

    private void onSupprimerEquipement(equipementwael e) {
        new Alert(Alert.AlertType.CONFIRMATION, "Supprimer \"" + e.getNom() + "\" ?", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                    service.supprimer(e.getId());
                    charger();
                });
    }

    private void afficherDetails(equipementwael e) {
        // récupérer userId (auth) et enregistrer la vue par user
        String userId = UserSession.getUserId();
        if (userId != null && !userId.isBlank()) {
            try {
                vueService.registerView(e.getId(), userId);
            } catch (Exception ignored) {
            }
        }

        // Charger les détails (texte simplifié)
        String msg = String.format("Nom: %s\nCatégorie: %s\nType: %s\nPrix: %s TND\nVille: %s\n\n%s",
                e.getNom(), e.getCategorie() != null ? e.getCategorie() : "-",
                e.getType() != null ? e.getType() : "-",
                e.getPrix() != null ? new DecimalFormat("#,##0.00").format(e.getPrix()) : "0",
                e.getVille() != null ? e.getVille() : "-",
                e.getDescription() != null ? e.getDescription() : "");

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Détails");
        a.setHeaderText(e.getNom());
        a.setContentText(msg);
        a.showAndWait();
        charger();
    }

    /**
     * Appelé depuis MainController à chaque fois que l’onglet Boutique est affiché.
     * Permet de garder cette vue synchronisée avec le dashboard (ajout /
     * modification / suppression).
     */
    public void rafraichirDepuisMain() {
        charger();
    }

    @FXML
    private void onAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/wael/EquipementFormView.fxml"));
            Parent root = loader.load();
            EquipementFormController ctrl = loader.getController();
            // Création d’un nouvel équipement depuis la boutique
            ctrl.setEquipement(null);
            ctrl.setOnSaved(this::charger);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle("Nouvel équipement (Boutique)");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }
}
