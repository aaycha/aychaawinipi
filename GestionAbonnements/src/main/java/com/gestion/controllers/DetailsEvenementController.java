package com.gestion.controllers;

import com.gestion.entities.Evenement;
import com.gestion.entities.Programme;
import com.gestion.interfaces.DataReceiver;
import com.gestion.services.EquipmentService;
import com.gestion.services.EvenementService;
import com.gestion.services.EventImageApi;
import com.gestion.services.GeoCodingService;
import com.gestion.services.ImageAiService;
import com.gestion.services.ProgrammeService;
import com.gestion.services.QuoteService;
import com.gestion.services.ReservationMaquillageService;
import com.gestion.services.SendGridMailService;

import com.gestion.services.WeatherferService;
import com.gestion.tools.Session;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// HTTP

public class DetailsEvenementController implements DataReceiver<Integer> {

    private final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter H = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblType;
    @FXML
    private Label lblLieu;
    @FXML
    private Label lblDebut;
    @FXML
    private Label lblFin;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblProgCount;

    @FXML
    private ImageView bgImage;
    @FXML
    private ImageView imgLogo;
    @FXML
    private ImageView imgEvent;
    @FXML
    private Button btnGenererImageIa;
    @FXML
    private Label lblImageIaMsg;

    @FXML
    private VBox progContainer;

    @FXML
    private VBox boxEquipmentBanner;
    @FXML
    private Label lblEquipmentTitle;
    @FXML
    private FlowPane flowEquipment;

    @FXML
    private VBox boxReservationMaquillage;
    @FXML
    private Label lblReservationMaquillage;
    @FXML
    private Button btnReserverMaquillage;

    // ✅ QR
    @FXML
    private ImageView imgQr;
    @FXML
    private Label lblQrInfo;
    @FXML
    private Button btnRegenQr;

    // ✅ Meteo (✅ corrigé: WeatherferService)
    @FXML
    private Label lblMeteo;
    private final WeatherferService weatherService = new WeatherferService();

    // ✅ Citation
    @FXML
    private Label lblQuote;
    @FXML
    private Label lblQuoteAuthor;
    private final QuoteService quoteService = new QuoteService();

    // ✅ Localisation
    @FXML
    private Label lblLatitude;
    @FXML
    private Label lblLongitude;
    @FXML
    private Button btnOpenMap;

    private final GeoCodingService geoService = new GeoCodingService();
    private GeoCodingService.GeoPoint currentGeoPoint;

    // ✅ Spotify
    @FXML
    private VBox spotifyBox;
    @FXML
    private Label lblSpotifyMsg;
    @FXML
    private ImageView imgSpotifyCover;
    @FXML
    private Label lblSpotifyTitle;
    @FXML
    private Label lblSpotifyProvider;
    @FXML
    private Button btnOpenSpotify;

    // ✅ Admin buttons
    @FXML
    private HBox adminButtons;

    // ✅ USER buttons
    @FXML
    private HBox userButtons;
    @FXML
    private Button btnAddToGoogleCalendar;
    @FXML
    private Button btnToggleNotif;

    @FXML
    private void onGoBack() {
        SceneUtil.switchTo("/Feryel/ListeEvenements.fxml", "Liste des Événements");
    }

    @FXML
    private void onGoAjouterProg() {
        SceneUtil.switchToWithData("/Feryel/AjouterProgramme.fxml", "Ajouter Programme", eventId);
    }

    @FXML
    private void onGoModifier() {
        SceneUtil.switchToWithData("/Feryel/ModifierEvenement.fxml", "Modifier Événement", eventId);
    }

    private boolean notificationsEnabled = false;

    private final EvenementService evenementService = new EvenementService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final ReservationMaquillageService reservationMaquillageService = new ReservationMaquillageService();

    // ✅ SendGrid API
    private final SendGridMailService mailService = new SendGridMailService();

    private final ProgrammeService programmeService = new ProgrammeService();

    private int eventId = 0;
    private Evenement event;

    private final EventImageApi imageApi = new EventImageApi();

    @FXML
    public void initialize() {
        SceneUtil.loadBackgroundImage(bgImage);
        SceneUtil.loadLogoImage(imgLogo);

        if (lblProgCount != null)
            lblProgCount.setText("");
        if (lblQrInfo != null)
            lblQrInfo.setText("");
        if (lblMeteo != null)
            lblMeteo.setText("");
        if (lblImageIaMsg != null)
            lblImageIaMsg.setText("");

        updateNotifButtonText();
        applyRoleUI();

        loadQuoteAsync();
    }

    @Override
    public void setData(Integer id) {
        if (id == null || id <= 0) {
            showError("Aucun ID reçu / ID invalide");
            clearDetails();
            return;
        }

        this.eventId = id;

        applyRoleUI();
        loadDetails();
        loadProgrammes();
    }

    // ===================== ROLE UI =====================
    private void applyRoleUI() {
        boolean admin = isAdmin();

        if (adminButtons != null) {
            adminButtons.setVisible(admin);
            adminButtons.setManaged(admin);
        }

        if (btnRegenQr != null) {
            btnRegenQr.setVisible(admin);
            btnRegenQr.setManaged(admin);
        }

        if (btnGenererImageIa != null) {
            btnGenererImageIa.setVisible(admin);
            btnGenererImageIa.setManaged(admin);
        }

        System.out.println("DetailsEvenement ROLE = " + getRole());
    }

    private boolean isAdmin() {
        if (Session.getInstance() == null || Session.getInstance().getCurrentUser() == null)
            return false;
        String role = Session.getInstance().getCurrentUser().getRole();
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    private String getRole() {
        if (Session.getInstance() == null || Session.getInstance().getCurrentUser() == null)
            return "GUEST";
        String role = Session.getInstance().getCurrentUser().getRole();
        return role == null ? "GUEST" : role;
    }

    private void loadDetails() {
        try {
            event = evenementService.getById(eventId);

            if (event == null) {
                showError("Événement introuvable");
                clearDetails();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL lors du chargement: " + e.getMessage());
            return;
        }

        if (lblTitre != null)
            lblTitre.setText(safe(event.getTitre()));
        if (lblType != null)
            lblType.setText(safe(event.getType()));
        if (lblLieu != null)
            lblLieu.setText(safe(event.getLieu()));
        if (lblDebut != null)
            lblDebut.setText(event.getDateDebut() == null ? "—" : event.getDateDebut().format(F));
        if (lblFin != null)
            lblFin.setText(event.getDateFin() == null ? "—" : event.getDateFin().format(F));
        if (lblDescription != null)
            lblDescription.setText(safe(event.getDescription()));

        loadEventImage(event.getImage());
        if (lblImageIaMsg != null)
            lblImageIaMsg.setText("");

        loadEquipmentBanner();
        loadReservationMaquillageSection();

        onGenererQr();
        loadMeteoAsync();
        loadLocationAsync();
    }

    // ===================== LOCALISATION =====================
    private void loadLocationAsync() {
        if (event == null || event.getLieu() == null || event.getLieu().isBlank())
            return;

        if (lblLatitude != null)
            lblLatitude.setText("⏳");
        if (lblLongitude != null)
            lblLongitude.setText("⏳");
        currentGeoPoint = null;

        Task<GeoCodingService.GeoPoint> task = new Task<>() {
            @Override
            protected GeoCodingService.GeoPoint call() throws Exception {
                return geoService.geocode(event.getLieu());
            }
        };

        task.setOnSucceeded(e -> {
            currentGeoPoint = task.getValue();
            if (currentGeoPoint == null) {
                if (lblLatitude != null)
                    lblLatitude.setText("❌");
                if (lblLongitude != null)
                    lblLongitude.setText("❌");
                return;
            }
            if (lblLatitude != null)
                lblLatitude.setText(String.format(Locale.ROOT, "%.5f", currentGeoPoint.lat));
            if (lblLongitude != null)
                lblLongitude.setText(String.format(Locale.ROOT, "%.5f", currentGeoPoint.lon));
        });

        task.setOnFailed(e -> {
            if (lblLatitude != null)
                lblLatitude.setText("❌");
            if (lblLongitude != null)
                lblLongitude.setText("❌");
        });

        Thread th = new Thread(task, "geo-task");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onOpenMap() {
        if (currentGeoPoint == null) {
            if (lblProgCount != null)
                lblProgCount.setText("❌ Localisation indisponible");
            return;
        }

        try {
            String url = "https://www.google.com/maps?q=" + currentGeoPoint.lat + "," + currentGeoPoint.lon;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (lblProgCount != null)
                lblProgCount.setText("❌ Impossible d'ouvrir Google Maps");
        }
    }

    private void loadEquipmentBanner() {
        if (boxEquipmentBanner == null || flowEquipment == null)
            return;

        List<com.gestion.entities.Equipment> list = equipmentService.getByEventId(eventId);

        if (list == null || list.isEmpty()) {
            boxEquipmentBanner.setVisible(false);
            boxEquipmentBanner.setManaged(false);
            return;
        }

        if (lblEquipmentTitle != null)
            lblEquipmentTitle.setText("📋 À prévoir pour cet événement");
        flowEquipment.getChildren().clear();

        for (com.gestion.entities.Equipment eq : list) {
            Label chip = new Label("✓ " + safe(eq.getLibelle()));
            chip.getStyleClass().add("chip");
            chip.setStyle(
                    "-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.25); -fx-padding: 6 12; -fx-background-radius: 4;");
            flowEquipment.getChildren().add(chip);
        }

        boxEquipmentBanner.setVisible(true);
        boxEquipmentBanner.setManaged(true);
    }

    private boolean eventHasMaquillageSpecial(List<com.gestion.entities.Equipment> list) {
        if (list == null)
            return false;
        for (com.gestion.entities.Equipment eq : list) {
            String lib = safe(eq.getLibelle()).toLowerCase(Locale.ROOT).replace("é", "e");
            if (lib.contains("maquillage") && (lib.contains("special") || lib.contains("spécial")))
                return true;
        }
        return false;
    }

    private void loadReservationMaquillageSection() {
        if (boxReservationMaquillage == null)
            return;
        List<com.gestion.entities.Equipment> list = equipmentService.getByEventId(eventId);
        boolean show = eventHasMaquillageSpecial(list);
        boxReservationMaquillage.setVisible(show);
        boxReservationMaquillage.setManaged(show);
    }

    @FXML
    private void onReserverMaquillage() {
        if (event == null || eventId <= 0)
            return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Réservation coin maquillage");
        dialog.setHeaderText("Réservez votre place au coin maquillage");
        dialog.setContentText("Votre email :");
        dialog.getEditor().setPromptText("exemple@email.com");

        dialog.showAndWait().ifPresent(email -> {
            String em = email == null ? "" : email.trim();
            if (em.isBlank()) {
                if (lblProgCount != null)
                    lblProgCount.setText("❌ Indiquez votre email.");
                return;
            }
            if (!em.contains("@") || !em.contains(".")) {
                if (lblProgCount != null)
                    lblProgCount.setText("❌ Email invalide.");
                return;
            }

            try {
                if (reservationMaquillageService.exists(eventId, em)) {
                    if (lblProgCount != null)
                        lblProgCount.setText("ℹ️ Réservation déjà existante.");
                    return;
                }

                reservationMaquillageService.add(eventId, em);

                if (btnReserverMaquillage != null)
                    btnReserverMaquillage.setDisable(true);
                if (lblProgCount != null)
                    lblProgCount.setText("✅ Réservation enregistrée ! Envoi du mail...");

                mailService.sendReservationMaquillageConfirmationAsync(
                        em,
                        event.getTitre(),
                        event.getLieu(),
                        event.getDateDebut(),
                        (emailSent) -> {
                            if (emailSent) {
                                if (lblProgCount != null)
                                    lblProgCount.setText("✅ Email envoyé à " + em);
                            } else {
                                if (lblProgCount != null)
                                    lblProgCount.setText("✅ Réservation OK. (Email non envoyé)");
                            }
                        });

            } catch (SQLException ex) {
                if (lblProgCount != null)
                    lblProgCount.setText("❌ Erreur réservation.");
                ex.printStackTrace();
                if (btnReserverMaquillage != null)
                    btnReserverMaquillage.setDisable(false);
            }
        });
    }

    private void loadEventImage(String file) {
        if (imgEvent == null)
            return;
        String name = (file == null || file.isBlank()) ? "logo.png" : file.trim();

        try {
            Path p = Path.of("uploads/images").resolve(name);
            if (Files.exists(p)) {
                imgEvent.setImage(new Image(p.toUri().toString(), true));
                return;
            }

            try (InputStream is = getClass().getResourceAsStream("/images/" + name)) {
                if (is != null) {
                    imgEvent.setImage(new Image(is));
                    return;
                }
            }

            try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null)
                    imgEvent.setImage(new Image(is));
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void onGenererImageIa() {
        if (event == null) {
            showError("Aucun événement chargé.");
            return;
        }

        String titre = safe(event.getTitre());
        String desc = safe(event.getDescription());
        String type = safe(event.getType());
        String lieu = safe(event.getLieu());

        if (titre.isBlank()) {
            if (lblImageIaMsg != null)
                lblImageIaMsg.setText("❌ Titre requis");
            return;
        }

        if (btnGenererImageIa != null)
            btnGenererImageIa.setDisable(true);
        if (lblImageIaMsg != null)
            lblImageIaMsg.setText("⏳ Génération...");

        Task<ImageAiService.GeneratedImage> task = new Task<>() {
            @Override
            protected ImageAiService.GeneratedImage call() throws Exception {
                return imageApi.generateForEvent(titre, desc, type, lieu);
            }
        };

        task.setOnSucceeded(e -> {
            if (btnGenererImageIa != null)
                btnGenererImageIa.setDisable(false);
            ImageAiService.GeneratedImage gen = task.getValue();
            if (gen == null) {
                if (lblImageIaMsg != null)
                    lblImageIaMsg.setText("❌ Échec");
                return;
            }

            event.setImage(gen.fileName);
            try {
                evenementService.update(event);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            loadEventImage(gen.fileName);
            if (imgEvent != null && gen.bytes != null) {
                imgEvent.setImage(new Image(new ByteArrayInputStream(gen.bytes)));
            }
            if (lblImageIaMsg != null)
                lblImageIaMsg.setText("✅ Image générée");
        });

        task.setOnFailed(e -> {
            if (btnGenererImageIa != null)
                btnGenererImageIa.setDisable(false);
            Throwable ex = task.getException();
            if (lblImageIaMsg != null)
                lblImageIaMsg.setText("❌ " + (ex == null ? "Erreur" : ex.getMessage()));
            if (ex != null)
                ex.printStackTrace();
        });

        Thread th = new Thread(task, "ai-image-details");
        th.setDaemon(true);
        th.start();
    }

    private void clearDetails() {
        if (lblTitre != null)
            lblTitre.setText("—");
        if (lblType != null)
            lblType.setText("—");
        if (lblLieu != null)
            lblLieu.setText("—");
        if (lblDebut != null)
            lblDebut.setText("—");
        if (lblFin != null)
            lblFin.setText("—");
        if (lblDescription != null)
            lblDescription.setText("");
        if (lblMeteo != null)
            lblMeteo.setText("");

        if (lblLatitude != null)
            lblLatitude.setText("");
        if (lblLongitude != null)
            lblLongitude.setText("");
        currentGeoPoint = null;

        if (imgEvent != null)
            imgEvent.setImage(null);
        if (imgQr != null)
            imgQr.setImage(null);
        if (lblQrInfo != null)
            lblQrInfo.setText("");
        if (lblImageIaMsg != null)
            lblImageIaMsg.setText("");

        if (progContainer != null)
            progContainer.getChildren().clear();

        if (boxEquipmentBanner != null) {
            boxEquipmentBanner.setVisible(false);
            boxEquipmentBanner.setManaged(false);
        }
        if (flowEquipment != null)
            flowEquipment.getChildren().clear();
        if (boxReservationMaquillage != null) {
            boxReservationMaquillage.setVisible(false);
            boxReservationMaquillage.setManaged(false);
        }

    }

    private void loadProgrammes() {
        if (eventId <= 0) {
            showError("ID événement invalide.");
            return;
        }
        if (progContainer == null)
            return;

        try {
            List<Programme> list = programmeService.getByEventId(eventId);
            list = list.stream()
                    .sorted(Comparator.comparing(p -> p.getDebut() == null ? LocalDateTime.MAX : p.getDebut()))
                    .collect(Collectors.toList());

            progContainer.getChildren().clear();

            if (list.isEmpty()) {
                if (lblProgCount != null)
                    lblProgCount.setText("ℹ️ Aucun programme");
                return;
            }

            for (Programme p : list)
                progContainer.getChildren().add(createProgrammeRow(p));
            if (lblProgCount != null)
                lblProgCount.setText("✅ " + list.size() + " programme(s)");

        } catch (SQLException e) {
            showError("Erreur chargement programmes");
            e.printStackTrace();
        }
    }

    private HBox createProgrammeRow(Programme p) {
        String heure = (p.getDebut() == null) ? "—" : p.getDebut().format(H);

        Label lblTime = new Label(heure);
        lblTime.getStyleClass().add("prog-time");

        VBox timeBox = new VBox(lblTime);
        timeBox.setAlignment(Pos.TOP_CENTER);
        timeBox.getStyleClass().add("prog-time-box");

        Label title = new Label(safe(p.getTitre()));
        title.getStyleClass().add("prog-title");

        String range = "";
        if (p.getDebut() != null && p.getFin() != null)
            range = p.getDebut().format(H) + " - " + p.getFin().format(H);
        else if (p.getDebut() != null)
            range = p.getDebut().format(H);

        Label timeRange = new Label(range);
        timeRange.getStyleClass().add("prog-range");

        Label hint = new Label(isAdmin() ? "Double-clic pour supprimer" : "");
        hint.getStyleClass().add("prog-hint");
        hint.setVisible(isAdmin());
        hint.setManaged(isAdmin());

        VBox details = new VBox(4, title, timeRange, hint);
        details.getStyleClass().add("prog-card-content");
        HBox.setHgrow(details, Priority.ALWAYS);

        Region colorBar = new Region();
        colorBar.getStyleClass().add("prog-color-bar");

        HBox card = new HBox(12, colorBar, details);
        card.getStyleClass().add("prog-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        card.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && isAdmin())
                onDeleteProgramme(p);
        });

        HBox row = new HBox(12, timeBox, card);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("prog-row");
        HBox.setHgrow(card, Priority.ALWAYS);

        return row;
    }

    private void onDeleteProgramme(Programme p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText("Supprimer ce programme ?");
        a.setContentText(safe(p.getTitre()));

        a.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    programmeService.delete(p.getIdProg());
                    if (lblProgCount != null)
                        lblProgCount.setText("✅ Programme supprimé");
                    loadProgrammes();
                } catch (SQLException e) {
                    showError("Erreur suppression programme");
                    e.printStackTrace();
                }
            }
        });
    }

    // ===================== ✅ METEO (✅ corrigé WeatherferService)
    // =====================
    private void loadMeteoAsync() {
        if (lblMeteo == null || event == null || event.getDateDebut() == null)
            return;

        lblMeteo.setText("⏳ Chargement météo...");

        String city = safe(event.getLieu());
        String dt = event.getDateDebut().toString();

        Task<WeatherferService.WeatherInfo> task = new Task<>() {
            @Override
            protected WeatherferService.WeatherInfo call() throws Exception {
                return weatherService.getWeatherForCityAtHour(city, dt);
            }
        };

        task.setOnSucceeded(e -> {
            WeatherferService.WeatherInfo w = task.getValue();
            if (w == null) {
                lblMeteo.setText("❌ Météo indisponible");
                return;
            }
            String text = String.format(Locale.ROOT,
                    "🌡 %.0f°C  ☔ %.1fmm  💨 %.0f km/h (%s)",
                    w.temperatureC, w.precipitationMm, w.windKmh, w.locationName);
            lblMeteo.setText(text);
        });

        task.setOnFailed(e -> lblMeteo.setText("❌ Météo indisponible"));

        Thread th = new Thread(task, "meteo-task");
        th.setDaemon(true);
        th.start();
    }

    // ===================== Citation =====================
    private void loadQuoteAsync() {
        if (lblQuote == null)
            return;

        lblQuote.setText("⏳ Chargement citation...");
        if (lblQuoteAuthor != null)
            lblQuoteAuthor.setText("");

        Task<QuoteService.Quote> task = new Task<>() {
            @Override
            protected QuoteService.Quote call() throws Exception {
                return quoteService.getRandomQuote();
            }
        };

        task.setOnSucceeded(e -> {
            QuoteService.Quote q = task.getValue();
            if (q == null) {
                lblQuote.setText("❌ Citation indisponible");
                if (lblQuoteAuthor != null)
                    lblQuoteAuthor.setText("");
                return;
            }
            lblQuote.setText("“ " + q.text + " ”");
            if (lblQuoteAuthor != null) {
                lblQuoteAuthor.setText(q.author == null || q.author.isBlank() ? "" : "— " + q.author);
            }
        });

        task.setOnFailed(e -> {
            lblQuote.setText("❌ Citation indisponible");
            if (lblQuoteAuthor != null)
                lblQuoteAuthor.setText("");
        });

        Thread th = new Thread(task, "quote-task");
        th.setDaemon(true);
        th.start();
    }

    // Help and other methods

    // ===================== HELPERS =====================
    private void showError(String msg) {
        if (lblProgCount != null)
            lblProgCount.setText("❌ " + msg);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void updateNotifButtonText() {
        if (btnToggleNotif == null)
            return;
        btnToggleNotif.setText(notificationsEnabled ? "Désactiver notification" : "Activer notification");
    }

    @FXML
    private void onToggleNotification() {
        notificationsEnabled = !notificationsEnabled;
        updateNotifButtonText();

        Alert alert = new Alert(notificationsEnabled ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("Rappel d'Événement");
        alert.setHeaderText(notificationsEnabled ? "Notifications Activées" : "Notifications Désactivées");
        alert.setContentText(notificationsEnabled
                ? "Vous recevrez un rappel dans l'application 24h avant le début de cet événement, ainsi que les mises à jour !"
                : "Vous avez désactivé les rappels et alertes pour cet événement.");
        alert.show();

        if (lblProgCount != null)
            lblProgCount.setText(notificationsEnabled ? "🔔 Notifications activées." : "🔕 Notifications désactivées.");
    }

    @FXML
    private void onAddToGoogleCalendar() {
        if (event == null)
            return;

        try {
            String title = java.net.URLEncoder.encode(safe(event.getTitre()), java.nio.charset.StandardCharsets.UTF_8);
            String details = java.net.URLEncoder.encode(safe(event.getDescription()),
                    java.nio.charset.StandardCharsets.UTF_8);
            String location = java.net.URLEncoder.encode(safe(event.getLieu()),
                    java.nio.charset.StandardCharsets.UTF_8);

            java.time.format.DateTimeFormatter gcalFormatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String startStr = (event.getDateDebut() != null) ? event.getDateDebut().format(gcalFormatter) : "";
            String endStr = (event.getDateFin() != null) ? event.getDateFin().format(gcalFormatter)
                    : (event.getDateDebut() != null ? event.getDateDebut().plusHours(2).format(gcalFormatter) : "");

            String datesStr = "";
            if (!startStr.isEmpty()) {
                datesStr = "&dates=" + startStr + "/" + (endStr.isEmpty() ? startStr : endStr);
            }

            String url = "https://calendar.google.com/calendar/r/eventedit?text=" + title + datesStr + "&details="
                    + details + "&location=" + location;

            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                if (lblProgCount != null)
                    lblProgCount.setText("❌ Ouverture du navigateur non supportée.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (lblProgCount != null)
                lblProgCount.setText("❌ Erreur Google Calendar.");
        }
    }

    @FXML
    private void onGenererQr() {
        if (event == null)
            return;

        String titre = safe(event.getTitre());
        String type = safe(event.getType());
        String lieu = safe(event.getLieu());
        String debut = (event.getDateDebut() == null) ? "—" : event.getDateDebut().format(F);

        String desc = safe(event.getDescription());
        if (desc.length() > 180)
            desc = desc.substring(0, 180) + "...";

        String message = "LAMMA EVENT\n" +
                "━━━━━━━━━━━━━━━━━━\n" +
                "Titre : " + titre + "\n" +
                "Type  : " + type + "\n" +
                "Lieu  : " + lieu + "\n" +
                "Début : " + debut + "\n" +
                "━━━━━━━━━━━━━━━━━━\n" +
                "Description :\n" +
                desc + "\n" +
                "━━━━━━━━━━━━━━━━━━\n" +
                "Plus de détails : Ouvrir l'app LAMMA";

        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String url = "https://api.qrserver.com/v1/create-qr-code/?size=320x320&margin=12&data=" + encoded;

        if (imgQr != null)
            imgQr.setImage(new Image(url, true));
        if (lblQrInfo != null)
            lblQrInfo.setText("✓ QR généré");
    }

    @FXML
    private void onModifierEvenement() {
        if (event == null)
            return;
        SceneUtil.switchToWithData("/Feryel/ModifierEvenement.fxml", "Modifier l'événement", event.getIdEvent());
    }

    @FXML
    private void onOpenSpotify() {
        if (event == null || lblSpotifyProvider == null || lblSpotifyProvider.getText().isEmpty())
            return;

        try {
            // Assume the Spotify URL might be saved or can be opened generically.
            // For now, we will just open Spotify home as a demo or use a predefined link.
            String url = "https://open.spotify.com";
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                if (lblProgCount != null)
                    lblProgCount.setText("❌ Ouverture du navigateur non supportée.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (lblProgCount != null)
                lblProgCount.setText("❌ Erreur ouverture Spotify.");
        }
    }

    @FXML
    private void onAjouterProgramme() {
        if (event == null)
            return;
        SceneUtil.switchToWithData("/Feryel/AjouterProgramme.fxml", "Ajouter un Programme", event.getIdEvent());
    }

    @FXML
    private void onRetour() {
        if (Session.getInstance() != null && Session.getInstance().getCurrentUser() != null) {
            String role = Session.getInstance().getCurrentUser().getRole();
            if (role != null && role.equalsIgnoreCase("ADMIN")) {
                SceneUtil.switchTo("/usersaif/User.fxml", "Tableau de Bord Administrateur");
            } else {
                SceneUtil.switchTo("/Feryel/ListeEvenements.fxml", "Liste des Événements");
            }
        } else {
            SceneUtil.switchTo("/Feryel/ListeEvenements.fxml", "Liste des Événements");
        }
    }
}