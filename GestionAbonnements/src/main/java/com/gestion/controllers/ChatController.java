package com.gestion.controllers;

import com.gestion.entities.GroupeChat;
import com.gestion.entities.MessageChat;
import com.gestion.services.BannedWordsService;
import com.gestion.services.GroupeChatService;
import com.gestion.services.MessageChatService;
import com.gestion.services.UserService;
import com.gestion.entities.User;
import com.gestion.tools.Session;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.scene.effect.DropShadow;
import javafx.stage.Popup;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class ChatController implements Initializable {

    @FXML
    private ListView<GroupeChat> groupsList;
    @FXML
    private Label activeGroupName;
    @FXML
    private Label groupInfo;
    @FXML
    private VBox messagesBox;
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private TextField messageField;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnNewGroup;
    @FXML
    private Button btnSend;
    @FXML
    private Button btnMenu;
    @FXML
    private Button btnAttach;
    @FXML
    private Button btnVocal;
    @FXML
    private Button btnGif;
    @FXML
    private Button btnLocation;
    private GroupeChatService groupeService;
    private MessageChatService messageService;
    private BannedWordsService bannedWordsService;
    private com.gestion.services.OptionSondageService optionSondageService;
    private com.gestion.services.MessageReactionService reactionService;
    private UserService utilisateurService;
    private String pendingFichierPath;
    private String pendingTypeMessage;
    private Double pendingLatitude;
    private Double pendingLongitude;
    private ObservableList<GroupeChat> groupesObservableList;
    private GroupeChat selectedGroupe;
    private Timer refreshTimer;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        groupeService = new GroupeChatService();
        messageService = new MessageChatService();
        bannedWordsService = new BannedWordsService();
        optionSondageService = new com.gestion.services.OptionSondageService();
        reactionService = new com.gestion.services.MessageReactionService();
        utilisateurService = new UserService();
        groupesObservableList = FXCollections.observableArrayList();
        pendingTypeMessage = "TEXT";

        // Configuration de la ListView pour afficher les groupes
        groupsList.setCellFactory(param -> new ListCell<GroupeChat>() {
            @Override
            protected void updateItem(GroupeChat groupe, boolean empty) {
                super.updateItem(groupe, empty);
                if (empty || groupe == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(5);
                    vbox.setPadding(new Insets(10, 15, 10, 15));

                    Label nomLabel = new Label(groupe.getNom());
                    nomLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                    // Utiliser couleur sombre pour lisibilité sur fond clair
                    nomLabel.setTextFill(Color.web("#0f172a"));

                    Label descLabel = new Label(groupe.getDescription() != null && !groupe.getDescription().isEmpty()
                            ? groupe.getDescription()
                            : "Aucune description");
                    descLabel.setFont(Font.font("System", 12));
                    descLabel.setTextFill(Color.web("#6b7280")); // Gris moyen pour lisibilité
                    descLabel.setWrapText(true);
                    descLabel.setMaxWidth(300);

                    vbox.getChildren().addAll(nomLabel, descLabel);
                    setGraphic(vbox);
                }
            }
        });

        // Sélection d'un groupe
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedGroupe = newVal;
                activeGroupName.setText(newVal.getNom());
                groupInfo.setText(newVal.getType() + " • Groupe de discussion");
                checkPermissions(newVal);
                loadMessages(newVal.getId());
            } else {
                selectedGroupe = null;
                activeGroupName.setText("Sélectionnez un groupe");
                groupInfo.setText("Discutez avec les amis sur les équipements");
                checkPermissions(null);
                afficherMessageAccueil();
            }
        });

        // Recherche en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                loadGroupes();
            } else {
                List<GroupeChat> filtered = groupeService.rechercherParNom(newVal.trim());
                groupesObservableList.clear();
                groupesObservableList.addAll(filtered);
            }
        });

        // Enter = send
        messageField.setOnAction(e -> onSend());

        // Charger les groupes
        loadGroupes();

        // Message d'accueil quand aucun groupe n'est sélectionné
        afficherMessageAccueil();

        // Démarrer le rafraîchissement automatique des messages (temps réel)
        startAutoRefresh();
    }

    private void afficherMessageAccueil() {
        if (messagesBox == null)
            return;
        messagesBox.getChildren().clear();
        Label welcome = new Label(
                "Sélectionnez un groupe à gauche pour voir les messages et discuter avec les amis sur les équipements.");
        welcome.setWrapText(true);
        welcome.setMaxWidth(500);
        welcome.setFont(Font.font("System", 14));
        welcome.setTextFill(Color.web("#666666"));
        VBox box = new VBox(welcome);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        messagesBox.getChildren().add(box);
    }

    private void loadGroupes() {
        if (groupeService == null || groupsList == null)
            return;

        try {
            List<GroupeChat> groupes = groupeService.afficher();
            if (groupes == null)
                return;

            groupesObservableList.clear();
            groupesObservableList.addAll(groupes);
            groupsList.setItems(groupesObservableList);
        } catch (Exception e) {
            System.err.println("Chargement groupes: " + e.getMessage());
        }
    }

    private void loadMessages(int idGroupe) {
        if (messagesBox == null)
            return;

        messagesBox.getChildren().clear();
        List<MessageChat> messages = messageService.afficherParGroupe(idGroupe);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        // Stream : filtrer les messages valides, grouper par jour, trier les jours puis
        // afficher
        List<MessageChat> validMessages = messages.stream()
                .filter(msg -> msg != null && msg.getDateEnvoi() != null)
                .collect(Collectors.toList());

        validMessages.stream()
                .map(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate())
                .distinct()
                .sorted()
                .forEach(date -> {
                    Timestamp firstOfDay = validMessages.stream()
                            .filter(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate().equals(date))
                            .min(Comparator.comparing(MessageChat::getDateEnvoi,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(MessageChat::getDateEnvoi)
                            .orElse(null);
                    if (firstOfDay != null) {
                        addDateSeparator(firstOfDay);
                    }
                    validMessages.stream()
                            .filter(msg -> msg.getDateEnvoi().toLocalDateTime().toLocalDate().equals(date))
                            .sorted(Comparator.comparing(MessageChat::getDateEnvoi,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .forEach(this::addMessageBubble);
                });

        // Scroll vers le bas
        Platform.runLater(() -> {
            if (messagesScroll != null) {
                messagesScroll.setVvalue(1.0);
            }
        });
    }

    private void checkPermissions(GroupeChat groupe) {
        if (groupe == null) {
            messageField.setDisable(true);
            btnSend.setDisable(true);
            btnAttach.setDisable(true);
            btnLocation.setDisable(true);
            messageField.setPromptText("Sélectionnez un groupe...");
            return;
        }

        User u = com.gestion.tools.Session.getInstance().getCurrentUser();
        int loggedUserId = u != null ? u.getId() : 1;
        boolean isAdmin = u != null && "ADMIN".equals(u.getRole());
        boolean isCreator = groupe.getIdCreateur() == loggedUserId;
        boolean isPrivate = "PRIVATE".equalsIgnoreCase(groupe.getType());

        if (isPrivate && !isCreator && !isAdmin) {
            messageField.setDisable(true);
            btnSend.setDisable(true);
            btnAttach.setDisable(true);
            btnLocation.setDisable(true);
            messageField.setPromptText("Ce groupe est privé. En lecture seule.");
        } else {
            messageField.setDisable(false);
            btnSend.setDisable(false);
            btnAttach.setDisable(false);
            btnLocation.setDisable(false);
            messageField.setPromptText("Tapez votre message...");
        }
    }

    private boolean isSameDay(LocalDateTime date1, Timestamp timestamp) {
        if (date1 == null || timestamp == null)
            return false;
        LocalDateTime date2 = timestamp.toLocalDateTime();
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    private void addDateSeparator(Timestamp timestamp) {
        Label dateLabel = new Label(timestamp.toLocalDateTime().format(dateFmt));
        dateLabel.getStyleClass().add("date-separator");
        HBox separatorBox = new HBox(dateLabel);
        separatorBox.setAlignment(Pos.CENTER);
        separatorBox.setPadding(new Insets(10, 0, 10, 0));
        messagesBox.getChildren().add(separatorBox);
    }

    private void addMessageBubble(MessageChat msg) {
        if (msg == null || messagesBox == null)
            return;

        User u = Session.getInstance().getCurrentUser();
        int loggedUserId = u != null ? u.getId() : 1;
        boolean isMe = msg.getIdUser() == loggedUserId;

        // Debug: log message basics to help diagnose UI issues
        System.out.println("[Chat] render msg id=" + msg.getId() + " type=" + msg.getTypeMessage() + " path="
                + msg.getFichierPath() + " contenu='" + msg.getContenu() + "'");

        String type = msg.getTypeMessage() != null ? msg.getTypeMessage() : "TEXT";
        String contenu = msg.getContenu() != null ? msg.getContenu() : "";
        String path = msg.getFichierPath();
        Double lat = msg.getLatitude();
        Double lon = msg.getLongitude();

        // Si le type n'est pas renseigné mais qu'il y a un fichier, inférer le type
        // depuis l'extension
        if ((type == null || "TEXT".equalsIgnoreCase(type)) && path != null && !path.isBlank()) {
            type = inferTypeFromPath(path);
        }

        VBox wrapper = new VBox(2);
        wrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (!isMe) {
            User sender = null;
            try {
                sender = utilisateurService.getUserById(msg.getIdUser());
            } catch (java.sql.SQLException e) {
                System.err.println("Error fetching sender: " + e.getMessage());
            }
            String senderName = sender != null ? sender.getName() : "Utilisateur " + msg.getIdUser();
            Label lblName = new Label(senderName);
            lblName.setFont(Font.font("System", FontWeight.BOLD, 11));
            lblName.getStyleClass().add("message-sender");
            lblName.setStyle(textColorStyle(false));
            wrapper.getChildren().add(lblName);
        }

        VBox bubble = new VBox(6);
        bubble.getStyleClass().addAll("message-bubble", "message-bubble-admin", isMe ? "bubble-me" : "bubble-other");
        bubble.setMaxWidth(400);
        bubble.setPadding(new Insets(10, 15, 10, 15));

        // Traitement GIF
        if ("GIF".equalsIgnoreCase(type) && path != null && !path.isBlank()) {
            // Handle both local files and remote URLs (http/https)
            try {
                if (path.startsWith("http")) {
                    // remote GIF: render directly from URL (Image supports animated GIFs)
                    ImageView gifView = new ImageView(new Image(path, 300, 250, true, true));
                    gifView.setPreserveRatio(true);
                    gifView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");

                    HBox controls = new HBox(8);
                    controls.setAlignment(Pos.CENTER);
                    Button openBtn = new Button("Ouvrir GIF");
                    openBtn.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
                    openBtn.setOnAction(e -> {
                        try {
                            Desktop.getDesktop().browse(new URI(path));
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir l'URL: " + ex.getMessage())
                                    .showAndWait();
                        }
                    });
                    controls.getChildren().add(openBtn);
                    bubble.getChildren().addAll(gifView, controls);
                } else {
                    // local file path
                    File fGif = new File(path);
                    final File finalGifFile = fGif;

                    if (finalGifFile.exists()) {
                        try {
                            ImageView gifView = new ImageView(
                                    new Image(finalGifFile.toURI().toString(), 300, 250, true, true));
                            gifView.setPreserveRatio(true);
                            gifView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");

                            HBox controls = new HBox(8);
                            controls.setAlignment(Pos.CENTER);

                            Button openBtn = new Button("Ouvrir GIF");
                            openBtn.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
                            openBtn.setOnAction(e -> ouvrirFichier(finalGifFile));

                            controls.getChildren().add(openBtn);
                            bubble.getChildren().addAll(gifView, controls);
                        } catch (Exception e) {
                            System.err.println("Erreur GIF: " + e.getMessage());
                            Label errLabel = new Label("🎬 GIF");
                            bubble.getChildren().add(errLabel);
                        }
                    } else {
                        // If file missing, fallback to remote URL if 'path' is relative filename in
                        // uploads
                        Path possible = Paths.get("uploads").resolve(path).toAbsolutePath();
                        if (possible.toFile().exists()) {
                            try {
                                ImageView gifView = new ImageView(
                                        new Image(possible.toUri().toString(), 300, 250, true, true));
                                gifView.setPreserveRatio(true);
                                gifView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");
                                bubble.getChildren().add(gifView);
                            } catch (Exception ex) {
                                Label errLabel = new Label("🎬 GIF non trouvé");
                                errLabel.setStyle("-fx-text-fill: #ef4444;");
                                bubble.getChildren().add(errLabel);
                            }
                        } else {
                            Label errLabel = new Label("🎬 GIF non trouvé");
                            errLabel.setStyle("-fx-text-fill: #ef4444;");
                            bubble.getChildren().add(errLabel);
                        }
                    }
                }

                if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                    Label txt = new Label(contenu);
                    txt.setWrapText(true);
                    txt.setFont(Font.font("System", 14));
                    txt.getStyleClass().add("message-text");
                    txt.setStyle(textColorStyle(isMe));
                    bubble.getChildren().add(txt);
                }
            } catch (Exception ex) {
                System.err.println("GIF handling error: " + ex.getMessage());
                Label errLabel = new Label("🎬 GIF");
                bubble.getChildren().add(errLabel);
            }
        } else if ("IMAGE".equalsIgnoreCase(type) && path != null && !path.isBlank()) {
            File fImage = new File(path);
            if (!fImage.exists()) {
                Path p = Paths.get("uploads").resolve(path).toAbsolutePath();
                fImage = p.toFile();
            }
            final File finalImageFile = fImage;
            if (finalImageFile.exists()) {
                try {
                    Image img = new Image("file:" + finalImageFile.getAbsolutePath(), 320, 240, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");
                    iv.setOnMouseClicked(e -> ouvrirFichier(finalImageFile));
                    bubble.getChildren().add(iv);
                } catch (Exception e) {
                    ajouterLignePieceJointe(bubble, "🖼 " + finalImageFile.getName(), finalImageFile);
                }
            } else {
                ajouterLignePieceJointe(bubble, "🖼 " + new File(path).getName() + " (fichier absent)", null);
            }
            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.getStyleClass().add("message-text");
                txt.setStyle(textColorStyle(isMe));
                bubble.getChildren().add(txt);
            }
            if (finalImageFile.exists()) {
                Button btnOuvrir = new Button("Ouvrir");
                btnOuvrir.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
                btnOuvrir.setOnAction(e -> ouvrirFichier(finalImageFile));
                bubble.getChildren().add(btnOuvrir);
            }
        } else if ("PDF".equalsIgnoreCase(type) || "AUDIO".equalsIgnoreCase(type) || "VIDEO".equalsIgnoreCase(type)) {
            File fDoc = path != null ? new File(path) : null;
            if (fDoc != null && !fDoc.exists()) {
                File localAttempt = new File("uploads/" + new File(path).getName());
                if (localAttempt.exists()) {
                    fDoc = localAttempt;
                }
            }
            final File finalDocFile = fDoc;

            System.out.println("[DEBUG AUDIO] Type=" + type + ", Path=" + path + ", Exists="
                    + (finalDocFile != null && finalDocFile.exists()));

            if ("AUDIO".equalsIgnoreCase(type) && finalDocFile != null && finalDocFile.exists()) {
                // Affichage professionnel pour messages vocaux (Messenger-style)
                HBox audioPlayer = new HBox(12);
                audioPlayer.setAlignment(Pos.CENTER_LEFT);
                audioPlayer.setStyle("-fx-background-color: transparent; -fx-padding: 2;");

                // Extraire la durée du contenu si disponible (ex: "🎤 Message vocal (6s)" ->
                // "6")
                String durationStr = contenu.contains("(")
                        ? contenu.substring(contenu.lastIndexOf("(") + 1, contenu.lastIndexOf(")"))
                        : "0s";
                int durationSec = 0;
                try {
                    durationSec = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                }
                if (durationSec == 0)
                    durationSec = 1;

                final com.gestion.services.VoiceMessageService[] voiceService = {
                        new com.gestion.services.VoiceMessageService() };
                final boolean[] isPlayingLocal = { false };

                Button playBtn = new Button("▶");
                playBtn.setStyle(
                        "-fx-font-size: 16; -fx-min-width: 40; -fx-min-height: 40; -fx-background-radius: 50%; -fx-background-color: "
                                + (isMe ? "#ffffff" : "#0084ff") + "; -fx-text-fill: " + (isMe ? "#0084ff" : "#ffffff")
                                + "; -fx-cursor: hand; -fx-padding: 0;");

                ProgressBar audioProgress = new ProgressBar(0.0);
                audioProgress.setPrefWidth(140);
                audioProgress
                        .setStyle("-fx-accent: " + (isMe ? "#ffffff" : "#0084ff") + "; -fx-control-inner-background: "
                                + (isMe ? "rgba(255,255,255,0.3)" : "rgba(0,132,255,0.2)") + ";");

                Label durationLabel = new Label(durationStr.replace("s", " s"));
                durationLabel.setStyle("-fx-font-size: 13; -fx-text-fill: " + (isMe ? "#ffffff" : "#0f172a")
                        + "; -fx-font-weight: bold;");

                final int durationFinal = durationSec;
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(Duration.ZERO,
                                new javafx.animation.KeyValue(audioProgress.progressProperty(), 0.0)),
                        new javafx.animation.KeyFrame(Duration.seconds(durationFinal),
                                new javafx.animation.KeyValue(audioProgress.progressProperty(), 1.0)));
                timeline.setOnFinished(ev -> {
                    isPlayingLocal[0] = false;
                    playBtn.setText("▶");
                    audioProgress.setProgress(0.0);
                });

                playBtn.setOnAction(e -> {
                    if (!isPlayingLocal[0]) {
                        voiceService[0].playVoiceMessage(finalDocFile.getAbsolutePath());
                        playBtn.setText("⏸");
                        isPlayingLocal[0] = true;
                        timeline.playFromStart();
                    } else {
                        voiceService[0].stopPlayback();
                        playBtn.setText("▶");
                        isPlayingLocal[0] = false;
                        timeline.stop();
                        audioProgress.setProgress(0.0);
                    }
                });

                audioPlayer.getChildren().addAll(playBtn, audioProgress, durationLabel);
                bubble.getChildren().add(audioPlayer);
            } else if ("AUDIO".equalsIgnoreCase(type)) {
                // Fallback pour AUDIO : afficher le message texte si fichier n'existe pas
                System.err.println("[DEBUG] AUDIO file not found at: " + path);
                Label audioLabel = new Label(contenu.isEmpty() ? "🎤 Message vocal" : contenu);
                audioLabel.setStyle(textColorStyle(isMe));
                bubble.getChildren().add(audioLabel);
            } else {
                // Affichage standard pour PDF/VIDEO et autres fichiers
                String icon = "PDF".equalsIgnoreCase(type) ? "📄" : "AUDIO".equalsIgnoreCase(type) ? "🎵" : "🎬";
                String nom = finalDocFile != null ? finalDocFile.getName() : "fichier";

                Label labelDoc = new Label(icon + " " + nom);
                labelDoc.setFont(Font.font("System", 13));
                if ("PDF".equalsIgnoreCase(type)) {
                    labelDoc.setTextFill(Color.web("#ef4444")); // Rouge pour PDF
                } else if ("AUDIO".equalsIgnoreCase(type)) {
                    labelDoc.setTextFill(Color.web("#8b5cf6")); // Violet pour audio
                } else {
                    labelDoc.setTextFill(Color.web("#06b6d4")); // Cyan pour vidéo
                }
                labelDoc.setWrapText(true);
                if (finalDocFile != null && finalDocFile.exists()) {
                    labelDoc.setOnMouseClicked(e -> ouvrirFichier(finalDocFile));
                }
                bubble.getChildren().add(labelDoc);
            }

            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.getStyleClass().add("message-text");
                txt.setStyle(textColorStyle(isMe));
                bubble.getChildren().add(txt);
            }
            if (finalDocFile != null && finalDocFile.exists()) {
                Button btnOuvrir = new Button("Ouvrir / Consulter");
                btnOuvrir.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
                btnOuvrir.setOnAction(e -> ouvrirFichier(finalDocFile));
                bubble.getChildren().add(btnOuvrir);
            }
        } else if ("LOCATION".equalsIgnoreCase(type) && lat != null && lon != null) {
            Label titre = new Label("📍 Position partagée");
            titre.setFont(Font.font("System", FontWeight.BOLD, 14));
            titre.setTextFill(Color.web("#ef4444")); // Rouge pour la position
            titre.getStyleClass().add("message-text");
            Label coords = new Label(String.format("%.5f, %.5f", lat, lon));
            coords.setFont(Font.font("System", 12));
            coords.setTextFill(Color.web("#10b981")); // Vert pour les coords
            coords.getStyleClass().add("message-meta");
            Button btnCarte = new Button("Voir sur la carte");
            btnCarte.setStyle("-fx-font-size: 11; -fx-cursor: hand;");
            btnCarte.setOnAction(e -> ouvrirCarteNavigateur(lat, lon));
            bubble.getChildren().addAll(titre, coords, btnCarte);
            if (!contenu.isEmpty() && !"(pièce jointe)".equals(contenu)) {
                Label txt = new Label(contenu);
                txt.setWrapText(true);
                txt.setFont(Font.font("System", 14));
                txt.getStyleClass().add("message-text");
                txt.setStyle(textColorStyle(isMe));
                bubble.getChildren().add(txt);
            }
        } else if ("POLL".equalsIgnoreCase(type)) {
            Label titre = new Label("📊 Sondage");
            titre.setFont(Font.font("System", FontWeight.BOLD, 14));
            titre.setTextFill(Color.web("#fbbf24")); // Jaune/Orange
            titre.getStyleClass().add("message-text");

            Label question = new Label(contenu);
            question.setWrapText(true);
            question.setFont(Font.font("System", 14));
            question.setTextFill(Color.web("#ffffff")); // Blanc pour le texte
            question.getStyleClass().add("message-text");

            bubble.getChildren().addAll(titre, question);

            List<com.gestion.entities.OptionSondage> options = optionSondageService.getOptionsForMessage(msg.getId());
            int totalVotes = options.stream().mapToInt(com.gestion.entities.OptionSondage::getVotes).sum();

            boolean userHasVoted = optionSondageService.hasUserVoted(msg.getId(),
                    Session.getInstance().getCurrentUser().getId());
            int votedOptionId = optionSondageService.getUserVotedOptionId(msg.getId(),
                    Session.getInstance().getCurrentUser().getId());

            for (com.gestion.entities.OptionSondage opt : options) {
                HBox optRow = new HBox(10);
                optRow.setAlignment(Pos.CENTER_LEFT);

                double pct = totalVotes > 0 ? (opt.getVotes() * 100.0 / totalVotes) : 0.0;
                ProgressBar bar = new ProgressBar(pct / 100.0);
                bar.setPrefWidth(160);

                Button btnVote = new Button(opt.getOptionText());
                btnVote.getStyleClass().addAll("poll-button");
                // marquer l'option déjà votée
                if (opt.getId() == votedOptionId) {
                    btnVote.getStyleClass().add("voted");
                }
                btnVote.setDisable(userHasVoted);
                btnVote.setOnAction(e -> {
                    optionSondageService.voter(opt.getId(), u != null ? u.getId() : 1);
                    loadMessages(selectedGroupe.getId());
                });

                Label lblVotes = new Label(opt.getVotes() + " (" + String.format("%.0f", pct) + "%)");
                lblVotes.getStyleClass().add("poll-votes");
                lblVotes.setStyle(textColorStyle(false));

                optRow.getChildren().addAll(btnVote, bar, lblVotes);
                bubble.getChildren().add(optRow);
            }

        } else {
            // Si contenu = (pièce jointe) et qu'il y a un path, tenter d'afficher
            // l'attachment inline
            if (("(pièce jointe)".equals(contenu) || "(message)".equals(contenu)) && path != null && !path.isBlank()) {
                String inferred = inferTypeFromPath(path);
                if ("IMAGE".equalsIgnoreCase(inferred)) {
                    File fInline = new File(path);
                    if (!fInline.exists()) {
                        Path p = Paths.get("uploads").resolve(path).toAbsolutePath();
                        fInline = p.toFile();
                    }
                    final File finalInlineFile = fInline;
                    if (finalInlineFile.exists()) {
                        try {
                            Image img = new Image("file:" + finalInlineFile.getAbsolutePath(), 320, 240, true, true);
                            ImageView iv = new ImageView(img);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);");
                            iv.setOnMouseClicked(e -> ouvrirFichier(finalInlineFile));
                            bubble.getChildren().add(iv);
                        } catch (Exception ex) {
                            // fallback: affiche le texte
                            Label msgLabel = new Label(contenu.isEmpty() ? "(message)" : contenu);
                            msgLabel.setWrapText(true);
                            msgLabel.setFont(Font.font("System", 14));
                            msgLabel.getStyleClass().add("message-text");
                            msgLabel.setStyle(textColorStyle(isMe));
                            bubble.getChildren().add(msgLabel);
                        }
                    } else {
                        Label msgLabel = new Label(contenu.isEmpty() ? "(message)" : contenu);
                        msgLabel.setWrapText(true);
                        msgLabel.setFont(Font.font("System", 14));
                        msgLabel.getStyleClass().add("message-text");
                        msgLabel.setStyle(textColorStyle(isMe));
                        bubble.getChildren().add(msgLabel);
                    }
                } else {
                    Label msgLabel = new Label(contenu.isEmpty() ? "(message)" : contenu);
                    msgLabel.setWrapText(true);
                    msgLabel.setFont(Font.font("System", 14));
                    msgLabel.getStyleClass().add("message-text");
                    msgLabel.setStyle(textColorStyle(isMe));
                    bubble.getChildren().add(msgLabel);
                }
            } else {
                if (type != null && !"TEXT".equalsIgnoreCase(type) && (path != null || (lat != null && lon != null))) {
                    if (path != null)
                        contenu = "[" + type + "] " + (contenu.isEmpty() ? new File(path).getName() : contenu);
                    else
                        contenu = "📍 Position partagée";
                }
                Label msgLabel = new Label(contenu.isEmpty() ? "(message)" : contenu);
                msgLabel.setWrapText(true);
                msgLabel.setFont(Font.font("System", 14));
                msgLabel.getStyleClass().add("message-text");
                msgLabel.setStyle(textColorStyle(isMe));
                bubble.getChildren().add(msgLabel);
            }
        }

        String timeStr = msg.getDateEnvoi() != null
                ? msg.getDateEnvoi().toLocalDateTime().format(timeFmt)
                : "";
        Label timeLabel = new Label(timeStr);
        timeLabel.setFont(Font.font("System", 11));
        timeLabel.getStyleClass().add("message-time");
        timeLabel.setStyle("-fx-text-fill: #94a3b8;");
        bubble.getChildren().add(timeLabel);

        HBox actionsBox = new HBox(5);
        actionsBox.setAlignment(Pos.CENTER);

        if (isMe) {
            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("message-action-icon");
            btnEdit.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #0f172a; -fx-cursor: hand; -fx-font-size: 16;");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> ouvrirEditMessage(msg));

            Button btnDelete = new Button("🗑");
            btnDelete.getStyleClass().addAll("message-action-icon", "message-action-icon-other");
            btnDelete.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 16;");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> {
                new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce message ?", ButtonType.OK, ButtonType.CANCEL)
                        .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                            messageService.supprimer(msg.getId());
                            loadMessages(selectedGroupe.getId());
                        });
            });
            actionsBox.getChildren().addAll(btnEdit, btnDelete);
        }

        // Bouton réactions emoji
        Button btnReact = new Button("😊");
        btnReact.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #f97316; -fx-cursor: hand; -fx-font-size: 14;");
        btnReact.setTooltip(new Tooltip("Ajouter une réaction"));
        btnReact.setOnAction(e -> showEmojiPicker(msg));
        actionsBox.getChildren().add(0, btnReact);

        HBox row = new HBox(5);
        row.setPadding(new Insets(3, 0, 3, 0));

        wrapper.getChildren().add(bubble);

        // Afficher les réactions sous la bulle
        java.util.Map<String, Integer> reactions = reactionService.getReactionsForMessage(msg.getId());
        if (!reactions.isEmpty()) {
            HBox reactionsBox = new HBox(6);
            reactionsBox.setStyle("-fx-padding: 6 0 0 0;");

            // mapping d'emoji -> couleur de fond (hex)
            java.util.Map<String, String> emojiColor = new java.util.HashMap<>();
            emojiColor.put("👍", "#06b6d4");
            emojiColor.put("❤️", "#ef4444");
            emojiColor.put("😂", "#f59e0b");
            emojiColor.put("😮", "#a78bfa");
            emojiColor.put("😢", "#60a5fa");
            emojiColor.put("😡", "#f97316");
            emojiColor.put("🎉", "#10b981");
            emojiColor.put("🔥", "#ef4444");
            emojiColor.put("✨", "#fbbf24");
            emojiColor.put("👏", "#8b5cf6");

            // Trier les emojis par count décroissant pour afficher les plus populaires en
            // premier
            reactions.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        String emoji = entry.getKey();
                        int count = entry.getValue();

                        // obtenir la liste des users qui ont réagi avec cet emoji
                        java.util.List<Integer> userIds = reactionService.getUsersWhoReacted(msg.getId(), emoji);
                        String tooltipText = "";
                        if (userIds != null && !userIds.isEmpty()) {
                            java.util.List<String> names = new java.util.ArrayList<>();
                            for (Integer uid : userIds) {
                                try {
                                    com.gestion.entities.User uName = utilisateurService.getUserById(uid);
                                    if (uName != null)
                                        names.add(uName.getName());
                                    else
                                        names.add("User " + uid);
                                } catch (Exception ex) {
                                    names.add("User " + uid);
                                }
                            }
                            tooltipText = String.join("\n", names);
                        } else {
                            tooltipText = "Aucune réaction";
                        }

                        // afficher pill emoji + count
                        Label reactionPill = new Label(emoji + (count > 1 ? " " + count : ""));
                        reactionPill.setStyle(
                                "-fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 13; -fx-text-fill: white; -fx-cursor: hand;");

                        // couleur de fond (fallback si absent)
                        String bg = emojiColor.containsKey(emoji) ? emojiColor.get(emoji) : "#374151";
                        reactionPill.setStyle(reactionPill.getStyle() + " -fx-background-color: " + bg + ";");

                        // indiquer si l'utilisateur courant a réagi
                        boolean currentReacted = false;
                        if (u != null) {
                            try {
                                currentReacted = reactionService.hasUserReacted(msg.getId(), u.getId(), emoji);
                            } catch (Exception ex) {
                                currentReacted = false;
                            }
                        }
                        if (currentReacted) {
                            // border to indicate user's reaction
                            reactionPill.setStyle(reactionPill.getStyle()
                                    + " -fx-border-color: #ffffff; -fx-border-width: 1; -fx-border-radius: 12;");
                        }

                        // tooltip listant les noms
                        Tooltip tt = new Tooltip(tooltipText);
                        tt.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;");
                        reactionPill.setTooltip(tt);

                        // stacked avatars under the pill (like Messenger) - show up to 3 avatars
                        HBox avatars = new HBox(-6);
                        avatars.setPadding(new Insets(4, 0, 0, 0));
                        if (userIds != null) {
                            int shown = 0;
                            for (Integer uid : userIds) {
                                if (shown >= 3)
                                    break;
                                User uu = null;
                                try {
                                    uu = utilisateurService.getUserById(uid);
                                } catch (Exception ex) {
                                }
                                StackPane av = createInitialsAvatar(uu != null ? uu.getName() : "User", uid, 10);
                                av.setStyle("-fx-border-color: white; -fx-border-width: 1; -fx-border-radius: 10;");
                                avatars.getChildren().add(av);
                                shown++;
                            }
                            if (userIds.size() > 3) {
                                Label more = new Label("+" + (userIds.size() - 3));
                                more.setStyle(
                                        "-fx-font-size:11; -fx-text-fill:#475569; -fx-padding:2 6; -fx-background-radius:10; -fx-background-color:#e2e8f0;");
                                avatars.getChildren().add(more);
                            }
                        }

                        // click handler : toggle reaction pour utilisateur courant
                        reactionPill.setOnMouseClicked(ev -> {
                            User cur = Session.getInstance().getCurrentUser();
                            if (cur == null)
                                return;
                            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                                // animation feedback
                                ScaleTransition st = new ScaleTransition(Duration.millis(120), reactionPill);
                                st.setFromX(1.0);
                                st.setFromY(1.0);
                                st.setToX(1.12);
                                st.setToY(1.12);
                                st.setAutoReverse(true);
                                st.setCycleCount(2);
                                st.play();
                                reactionService.addReaction(msg.getId(), cur.getId(), emoji); // toggle inside service
                                // rafraîchir uniquement les réactions du message
                                loadMessages(selectedGroupe.getId());
                            } else if (ev.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                                // clic-droit -> detailler qui a reagit
                                showReactionDetails(msg.getId(), emoji);
                            }
                        });

                        // group pill + avatars in a VBox
                        VBox pillBox = new VBox(2);
                        pillBox.setAlignment(Pos.CENTER);
                        pillBox.getChildren().addAll(reactionPill, avatars);
                        reactionsBox.getChildren().add(pillBox);
                    });
            wrapper.getChildren().add(reactionsBox);
        }

        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(actionsBox, wrapper);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().add(wrapper);
        }

        messagesBox.getChildren().add(row);
    }

    private void ajouterLignePieceJointe(VBox bubble, String texte, File f) {
        Label l = new Label(texte);
        l.setWrapText(true);
        l.setFont(Font.font("System", 13));
        // Texte plus sombre pour être lisible sur fond clair
        l.setTextFill(Color.web("#0f172a"));
        if (f != null && f.exists())
            l.setOnMouseClicked(e -> ouvrirFichier(f));
        bubble.getChildren().add(l);
    }

    private void ouvrirFichier(File f) {
        if (f == null || !f.exists()) {
            new Alert(Alert.AlertType.WARNING, "Fichier introuvable: " + (f != null ? f.getAbsolutePath() : ""))
                    .showAndWait();
            return;
        }
        try {
            Desktop.getDesktop().open(f);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le fichier: " + e.getMessage()).showAndWait();
        }
    }

    private void ouvrirCarteNavigateur(double lat, double lon) {
        try {
            String url = "https://www.google.com/maps?q=" + lat + "," + lon;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la carte: " + e.getMessage()).showAndWait();
        }
    }

    private void ouvrirEditMessage(MessageChat msg) {
        TextInputDialog d = new TextInputDialog(msg.getContenu());
        d.setTitle("Modifier le message");
        d.setHeaderText("Nouveau contenu");
        d.showAndWait().ifPresent(nouveauContenu -> {
            if (bannedWordsService.containsBannedWord(nouveauContenu)) {
                new Alert(Alert.AlertType.WARNING,
                        "Message bloqué : langage inapproprié. Veuillez modifier votre message.").showAndWait();
                return;
            }
            msg.setContenu(nouveauContenu);
            messageService.modifier(msg);
            loadMessages(selectedGroupe.getId());
        });
    }

    @FXML
    private void onSend() {
        if (selectedGroupe == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucun groupe sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez sélectionner un groupe pour envoyer un message.");
            alert.showAndWait();
            return;
        }

        String text = messageField.getText();
        if (text == null)
            text = "";
        if (text.trim().isEmpty() && (pendingFichierPath == null || pendingFichierPath.isEmpty()))
            return;

        if (bannedWordsService.containsBannedWord(text)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Message bloqué");
            alert.setHeaderText("Langage inapproprié");
            alert.setContentText(
                    "Votre message contient des mots interdits. Veuillez modifier votre message et respecter les règles de la communauté.");
            alert.showAndWait();
            return;
        }

        try {
            User u = Session.getInstance().getCurrentUser();
            int loggedUserId = u != null ? u.getId() : 1;

            MessageChat newMsg = new MessageChat(text.trim().isEmpty() ? "(pièce jointe)" : text.trim(),
                    selectedGroupe.getId(), loggedUserId);
            newMsg.setTypeMessage(pendingTypeMessage != null ? pendingTypeMessage : "TEXT");
            newMsg.setFichierPath(pendingFichierPath);
            newMsg.setLatitude(pendingLatitude);
            newMsg.setLongitude(pendingLongitude);
            messageService.ajouter(newMsg);
            pendingFichierPath = null;
            pendingTypeMessage = "TEXT";
            pendingLatitude = null;
            pendingLongitude = null;
            loadMessages(selectedGroupe.getId());
            messageField.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Erreur lors de l'envoi du message: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onAttach() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Joindre un fichier (image, PDF, audio, vidéo)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("Vidéo", "*.mp4", "*.webm", "*.avi"),
                new FileChooser.ExtensionFilter("Tous", "*.*"));
        java.io.File f = fc.showOpenDialog(messageField.getScene().getWindow());
        if (f != null && f.exists()) {
            try {
                if (selectedGroupe == null) {
                    new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un groupe avant de joindre un fichier.")
                            .showAndWait();
                    return;
                }

                // Créer le dossier uploads s'il n'existe pas
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Générer un nom unique
                String extension = "";
                int i = f.getName().lastIndexOf('.');
                if (i > 0) {
                    extension = f.getName().substring(i);
                }
                String newFileName = UUID.randomUUID().toString() + extension;
                Path targetPath = uploadDir.resolve(newFileName);

                // Copier le fichier
                Files.copy(f.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                pendingFichierPath = targetPath.toString();
                String name = f.getName().toLowerCase();
                if (name.endsWith(".pdf"))
                    pendingTypeMessage = "PDF";
                else if (name.matches(".*\\.(png|jpg|jpeg|gif|bmp)"))
                    pendingTypeMessage = "IMAGE";
                else if (name.matches(".*\\.(mp3|wav|m4a)"))
                    pendingTypeMessage = "AUDIO";
                else if (name.matches(".*\\.(mp4|webm|avi)"))
                    pendingTypeMessage = "VIDEO";
                else
                    pendingTypeMessage = "TEXT";

                // Envoi automatique (comme Messenger) : on envoie immédiatement
                // l'image/ressource sélectionnée
                onSend();

            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur de copie: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onLocation() {
        Dialog<String> d = new Dialog<>();
        d.setTitle("📍 Partager ma position actuelle");
        d.setHeaderText("Comme WhatsApp / Messenger : entrez vos coordonnées (ex. Tunis: 36.8, 10.18)");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        TextField latF = new TextField("36.8");
        latF.setPromptText("Latitude");
        TextField lonF = new TextField("10.18");
        lonF.setPromptText("Longitude");
        grid.add(new Label("Latitude:"), 0, 0);
        grid.add(latF, 1, 0);
        grid.add(new Label("Longitude:"), 0, 1);
        grid.add(lonF, 1, 1);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(btn -> btn == ButtonType.OK ? "ok" : null);
        if (d.showAndWait().isEmpty() || d.getResult() == null)
            return;
        try {
            pendingLatitude = Double.parseDouble(latF.getText().trim());
            pendingLongitude = Double.parseDouble(lonF.getText().trim());
            pendingTypeMessage = "LOCATION";
            messageField.setPromptText("📍 Position actuelle "
                    + String.format("%.2f, %.2f", pendingLatitude, pendingLongitude) + " – message optionnel");
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Latitude et longitude doivent être des nombres (ex. 36.8 et 10.18).")
                    .showAndWait();
        }
    }

    @FXML
    private void onNewGroup() {
        Dialog<GroupeChat> dialog = new Dialog<>();
        dialog.setTitle("Nouveau Groupe");
        dialog.setHeaderText("Créer un nouveau groupe de chat");

        ButtonType createButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField nomField = new TextField();
        nomField.setPromptText("Nom du groupe");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("PUBLIC", "PRIVATE");
        typeCombo.setValue("PUBLIC");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String nom = nomField.getText();
                String desc = descField.getText();
                String type = typeCombo.getValue();
                if (nom != null && !nom.trim().isEmpty()) {
                    User u = Session.getInstance().getCurrentUser();
                    int loggedUserId = u != null ? u.getId() : 1;
                    return new GroupeChat(nom.trim(), desc != null ? desc.trim() : "", type != null ? type : "PUBLIC",
                            loggedUserId);
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(groupe -> {
            if (groupe != null) {
                groupeService.ajouter(groupe);
                loadGroupes();
            }
        });
    }

    @FXML
    private void onPoll() {
        if (selectedGroupe == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un groupe.").showAndWait();
            return;
        }

        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("📊 Créer un sondage");
        dialog.setHeaderText("Posez une question et ajoutez des options");

        ButtonType createButton = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        TextField questionField = new TextField();
        questionField.setPromptText("Exemple : Quel est votre équipement préféré ?");

        VBox optionsBox = new VBox(5);
        TextField opt1 = new TextField();
        opt1.setPromptText("Option 1");
        TextField opt2 = new TextField();
        opt2.setPromptText("Option 2");
        optionsBox.getChildren().addAll(opt1, opt2);

        Button btnAddOption = new Button("+ Ajouter une option");
        btnAddOption.setOnAction(e -> {
            TextField newOpt = new TextField();
            newOpt.setPromptText("Nouvelle option");
            optionsBox.getChildren().add(newOpt);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        content.getChildren().addAll(new Label("Question:"), questionField, new Label("Options:"), optionsBox,
                btnAddOption);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == createButton) {
                List<String> result = new java.util.ArrayList<>();
                result.add(questionField.getText());
                for (javafx.scene.Node node : optionsBox.getChildren()) {
                    if (node instanceof TextField) {
                        String txt = ((TextField) node).getText();
                        if (txt != null && !txt.trim().isEmpty()) {
                            result.add(txt.trim());
                        }
                    }
                }
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result.size() >= 3) {
                String question = result.get(0);
                if (question == null || question.trim().isEmpty())
                    return;

                User u = Session.getInstance().getCurrentUser();
                int loggedUserId = u != null ? u.getId() : 1;

                MessageChat msg = new MessageChat(question, selectedGroupe.getId(), loggedUserId);
                msg.setTypeMessage("POLL");
                int msgId = messageService.ajouter(msg);

                if (msgId != -1) {
                    List<String> options = result.subList(1, result.size());
                    optionSondageService.ajouterList(msgId, options);
                    loadMessages(selectedGroupe.getId());
                }
            } else {
                new Alert(Alert.AlertType.WARNING,
                        "Un sondage nécessite une question et au moins deux options non vides.").showAndWait();
            }
        });
    }

    @FXML
    private void onMenu() {
        if (selectedGroupe == null)
            return;

        ContextMenu menu = new ContextMenu();
        MenuItem infoItem = new MenuItem("Informations du groupe");
        MenuItem deleteItem = new MenuItem("Supprimer le groupe");

        infoItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informations du groupe");
            alert.setHeaderText(selectedGroupe.getNom());
            alert.setContentText("Description: " + selectedGroupe.getDescription() + "\n" +
                    "Type: " + selectedGroupe.getType() + "\n" +
                    "Date de création: " + selectedGroupe.getDateCreation());
            alert.showAndWait();
        });

        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le groupe");
            confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + selectedGroupe.getNom() + " ?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    groupeService.supprimer(selectedGroupe.getId());
                    selectedGroupe = null;
                    activeGroupName.setText("Sélectionnez un groupe");
                    messagesBox.getChildren().clear();
                    loadGroupes();
                }
            });
        });

        menu.getItems().addAll(infoItem, deleteItem);
        if (btnMenu.getScene() != null && btnMenu.getScene().getWindow() != null) {
            menu.show(btnMenu,
                    btnMenu.getScene().getWindow().getX() + btnMenu.getLayoutX(),
                    btnMenu.getScene().getWindow().getY() + btnMenu.getLayoutY() + btnMenu.getHeight());
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (selectedGroupe != null) {
                        loadMessages(selectedGroupe.getId());
                    }
                });
            }
        }, 2000, 2000); // Rafraîchir toutes les 2 secondes
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }

    private String inferTypeFromPath(String path) {
        if (path == null)
            return "TEXT";
        String p = path.toLowerCase();
        if (p.matches(".*\\.(png|jpg|jpeg|gif|bmp)$"))
            return "IMAGE";
        if (p.endsWith(".pdf"))
            return "PDF";
        if (p.matches(".*\\.(mp3|wav|m4a)$"))
            return "AUDIO";
        if (p.matches(".*\\.(mp4|webm|avi)$"))
            return "VIDEO";
        return "FILE";
    }

    private String textColorStyle(boolean isMe) {
        // Pour les messages de l'utilisateur (bulles foncées), texte clair ; sinon
        // texte sombre
        return isMe ? "-fx-text-fill: #e5e7eb;" : "-fx-text-fill: #0f172a;";
    }

    @FXML
    private void onVocal() {
        if (selectedGroupe == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un groupe.").showAndWait();
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("🎤 Enregistrer un message vocal");
        dialog.setHeaderText("Enregistrez un message vocal comme sur Messenger");
        dialog.setWidth(500);
        dialog.setHeight(350);

        ButtonType sendButton = new ButtonType("Envoyer le vocal", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("🎤 Enregistrement vocal");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label infoLabel = new Label("Cliquez sur le bouton pour démarrer/arrêter l'enregistrement (max 60 secondes)");
        infoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b; -fx-wrap-text: true;");
        infoLabel.setMaxWidth(400);

        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 48; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        ToggleButton recordBtn = new ToggleButton("⏺ Démarrer");
        recordBtn.setStyle(
                "-fx-font-size: 14; -fx-padding: 15 30; -fx-cursor: hand; -fx-background-color: #ef4444; -fx-text-fill: white;");

        Label statusLabel = new Label("Prêt à enregistrer");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");

        final String[] recordedFilePath = { null };
        final int[] recordingDuration = { 0 };
        final Timer[] recordingTimer = { null };

        recordBtn.setOnAction(e -> {
            if (recordBtn.isSelected()) {
                // Démarrer l'enregistrement
                recordBtn.setText("⏹ Arrêter");
                recordBtn.setStyle(
                        "-fx-font-size: 14; -fx-padding: 15 30; -fx-cursor: hand; -fx-background-color: #10b981; -fx-text-fill: white;");
                statusLabel.setText("Enregistrement en cours...");
                statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #10b981;");
                recordingDuration[0] = 0;

                // Démarrer le minuteur
                recordingTimer[0] = new Timer();
                recordingTimer[0].scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        recordingDuration[0]++;
                        int minutes = recordingDuration[0] / 60;
                        int seconds = recordingDuration[0] % 60;
                        Platform.runLater(() -> {
                            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
                        });

                        if (recordingDuration[0] >= 60) {
                            Platform.runLater(() -> {
                                recordBtn.setSelected(false);
                                recordBtn.fire(); // Arrêter automatiquement
                            });
                        }
                    }
                }, 0, 1000);

                // Démarrer l'enregistrement en arrière-plan
                new Thread(() -> {
                    try {
                        recordedFilePath[0] = com.gestion.services.VoiceMessageService.recordVoiceMessage(60);
                    } catch (javax.sound.sampled.LineUnavailableException ex) {
                        System.err.println("Erreur enregistrement: " + ex.getMessage());
                    }
                }).start();

            } else {
                // Arrêter l'enregistrement
                if (recordingTimer[0] != null) {
                    recordingTimer[0].cancel();
                }
                recordBtn.setText("⏺ Démarrer");
                recordBtn.setStyle(
                        "-fx-font-size: 14; -fx-padding: 15 30; -fx-cursor: hand; -fx-background-color: #ef4444; -fx-text-fill: white;");
                statusLabel.setText("Enregistrement terminé ✓");
                statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #10b981;");
            }
        });

        content.getChildren().addAll(titleLabel, infoLabel, timerLabel, recordBtn, statusLabel);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> btn == sendButton ? recordedFilePath[0] : null);

        dialog.showAndWait().ifPresent(filePath -> {
            if (filePath != null && !filePath.isEmpty()) {
                // Envoyer le message vocal
                try {
                    User u = Session.getInstance().getCurrentUser();
                    int loggedUserId = u != null ? u.getId() : 1;
                    MessageChat newMsg = new MessageChat("🎤 Message vocal (" + recordingDuration[0] + "s)",
                            selectedGroupe.getId(), loggedUserId);
                    newMsg.setTypeMessage("AUDIO");
                    newMsg.setFichierPath(filePath);
                    messageService.ajouter(newMsg);
                    loadMessages(selectedGroupe.getId());
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de l'envoi du message vocal: " + ex.getMessage())
                            .showAndWait();
                }
            }
        });
    }

    @FXML
    private void onGif() {
        if (selectedGroupe == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un groupe.").showAndWait();
            return;
        }

        Dialog<com.gestion.services.GiphyService.GifData> dialog = new Dialog<>();
        dialog.setTitle("GIFs - Messenger Style");
        dialog.setWidth(1000);
        dialog.setHeight(700);

        ButtonType sendButton = new ButtonType("Envoyer GIF", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButton, ButtonType.CANCEL);

        // Layout principal
        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // ============ PARTIE GAUCHE : Recherche + Grid de GIFs ============
        VBox leftPanel = new VBox(12);
        leftPanel.setPrefWidth(400);
        leftPanel.setStyle("-fx-background-color: #f0f2f5; -fx-border-radius: 12; -fx-padding: 15;");

        // Barre de recherche
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #e4e6eb; -fx-border-radius: 20; -fx-padding: 10 16;");

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher des GIFs...");
        searchField.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 13; -fx-text-fill: #0f172a;");
        searchField.setPrefWidth(350);

        searchBox.getChildren().add(searchField);

        Label loadingStatus = new Label("Chargement des GIFs tendances...");
        loadingStatus.setStyle("-fx-text-fill: #65676b; -fx-font-size: 12;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");
        scrollPane.setFitToWidth(true);

        GridPane giphyGrid = new GridPane();
        giphyGrid.setHgap(10);
        giphyGrid.setVgap(10);
        giphyGrid.setPadding(new Insets(10));
        giphyGrid.setStyle("-fx-background-color: #ffffff;");

        scrollPane.setContent(giphyGrid);

        leftPanel.getChildren().addAll(searchBox, loadingStatus, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // ============ PARTIE DROITE : Aperçu du GIF sélectionné ============
        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(500);
        rightPanel.setStyle("-fx-background-color: #f0f2f5; -fx-border-radius: 12; -fx-padding: 20;");
        rightPanel.setAlignment(Pos.TOP_CENTER);

        Label previewTitle = new Label("Sélectionnez un GIF");
        previewTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        StackPane previewContainer = new StackPane();
        previewContainer.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #e4e6eb; -fx-border-radius: 12; -fx-padding: 10;");
        previewContainer.setPrefHeight(400);

        ImageView largePreview = new ImageView();
        largePreview.setPreserveRatio(true);
        largePreview.setFitWidth(420);
        largePreview.setFitHeight(380);

        Label placeholderLabel = new Label("Aucun GIF sélectionné\nCliquez sur un GIF à gauche");
        placeholderLabel.setStyle("-fx-text-alignment: center; -fx-font-size: 13; -fx-text-fill: #65676b;");

        previewContainer.getChildren().addAll(placeholderLabel, largePreview);

        Label gifInfo = new Label("");
        gifInfo.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b; -fx-wrap-text: true;");
        gifInfo.setMaxWidth(450);

        Label readyLabel = new Label("");
        readyLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #31a24c; -fx-font-weight: bold;");

        rightPanel.getChildren().addAll(previewTitle, previewContainer, gifInfo, readyLabel);

        final com.gestion.services.GiphyService.GifData[] selectedGif = { null };

        Runnable displayGifs = () -> {
            giphyGrid.getChildren().clear();
            String query = searchField.getText().trim();
            loadingStatus.setText("Chargement...");

            new Thread(() -> {
                List<com.gestion.services.GiphyService.GifData> gifs;
                if (query.isEmpty()) {
                    gifs = com.gestion.services.GiphyService.getTrendingGifs(15);
                } else {
                    gifs = com.gestion.services.GiphyService.searchGifs(query, 15);
                }

                final List<com.gestion.services.GiphyService.GifData> finalGifs = gifs;
                Platform.runLater(() -> {
                    giphyGrid.getChildren().clear();

                    if (finalGifs.isEmpty()) {
                        loadingStatus.setText("Aucun GIF trouvé");
                        Label noResults = new Label("Aucun résultat");
                        noResults.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13;");
                        giphyGrid.add(noResults, 0, 0);
                        return;
                    }

                    loadingStatus.setText(finalGifs.size() + " GIFs trouvés");

                    int col = 0;
                    int row = 0;

                    for (com.gestion.services.GiphyService.GifData gif : finalGifs) {
                        try {
                            VBox gifBox = new VBox();
                            gifBox.setStyle(
                                    "-fx-border-color: #e4e6eb; -fx-border-radius: 8; -fx-background-color: #ffffff; -fx-cursor: hand;");
                            gifBox.setCursor(javafx.scene.Cursor.HAND);
                            gifBox.setPrefSize(150, 150);

                            ImageView thumbnail = new ImageView();
                            thumbnail.setPreserveRatio(true);
                            thumbnail.setFitWidth(150);
                            thumbnail.setFitHeight(150);

                            try {
                                Image img = new Image(gif.stillUrl, 150, 150, true, true);
                                thumbnail.setImage(img);
                                gifBox.getChildren().add(thumbnail);
                            } catch (Exception e) {
                                System.err.println("Erreur thumbnail: " + e.getMessage());
                            }

                            gifBox.setAlignment(Pos.CENTER);

                            gifBox.setOnMouseClicked(e -> {
                                selectedGif[0] = gif;

                                try {
                                    Image animatedGif = new Image(gif.url, 420, 380, true, true);
                                    largePreview.setImage(animatedGif);
                                    placeholderLabel.setVisible(false);
                                } catch (Exception ex) {
                                    System.err.println("Erreur GIF animé: " + ex.getMessage());
                                }

                                previewTitle.setText(gif.title);
                                // remove the GIF description underneath the preview (keep title only)
                                gifInfo.setText("");
                                readyLabel.setText("✓ Prêt à envoyer");

                                giphyGrid.getChildren().forEach(node -> {
                                    node.setStyle(
                                            "-fx-border-color: #e4e6eb; -fx-border-radius: 8; -fx-background-color: #ffffff;");
                                });
                                gifBox.setStyle(
                                        "-fx-border-color: #0099ff; -fx-border-width: 3; -fx-border-radius: 8; -fx-background-color: #e8f4fd;");
                            });

                            gifBox.setOnMouseEntered(e -> {
                                if (selectedGif[0] != gif) {
                                    gifBox.setStyle(
                                            "-fx-border-color: #bcc0c4; -fx-border-radius: 8; -fx-background-color: #f9fafb;");
                                }
                            });
                            gifBox.setOnMouseExited(e -> {
                                if (selectedGif[0] != gif) {
                                    gifBox.setStyle(
                                            "-fx-border-color: #e4e6eb; -fx-border-radius: 8; -fx-background-color: #ffffff;");
                                }
                            });

                            giphyGrid.add(gifBox, col, row);
                            col++;
                            if (col >= 2) {
                                col = 0;
                                row++;
                            }
                        } catch (Exception ex) {
                            System.err.println("Erreur création GIF box: " + ex.getMessage());
                        }
                    }
                });
            }).start();
        };

        displayGifs.run();

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                displayGifs.run();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 2) {
                displayGifs.run();
            } else if (newVal.isEmpty()) {
                displayGifs.run();
            }
        });

        mainLayout.getChildren().addAll(leftPanel, rightPanel);

        dialog.getDialogPane().setContent(mainLayout);
        dialog.setResultConverter(btn -> btn == sendButton ? selectedGif[0] : null);

        dialog.showAndWait().ifPresent(gif -> {
            if (gif != null) {
                try {
                    // Prefer an actual GIF URL when available
                    String downloadUrl = (gif.url != null && gif.url.toLowerCase().contains(".gif"))
                            ? gif.url
                            : (gif.mp4Url != null && !gif.mp4Url.isEmpty() ? gif.mp4Url : gif.url);

                    // Determine extension from the chosen URL
                    String ext = ".gif";
                    try {
                        String path = new java.net.URL(downloadUrl).getPath();
                        int idx = path.lastIndexOf('.');
                        if (idx > 0)
                            ext = path.substring(idx);
                        // normalize common mp4 -> use .mp4 if that's what we got
                    } catch (Exception _ex) {
                        // fallback
                    }

                    // If we got a GIF URL, attempt to download and save locally; otherwise keep
                    // remote URL
                    Path uploadDir = Paths.get("uploads");
                    if (!Files.exists(uploadDir))
                        Files.createDirectories(uploadDir);

                    Path targetPath = null;
                    boolean downloaded = false;
                    if (downloadUrl != null && downloadUrl.startsWith("http")) {
                        try (java.io.InputStream in = new java.net.URL(downloadUrl).openStream()) {
                            String newFileName = UUID.randomUUID().toString() + ext;
                            targetPath = uploadDir.resolve(newFileName);
                            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            downloaded = true;
                            System.out.println("[GIF SEND] Downloaded GIF to " + targetPath.toAbsolutePath());
                        } catch (Exception ex) {
                            System.err.println("[GIF SEND] Download failed, will use remote URL: " + ex.getMessage());
                            targetPath = null;
                        }
                    }

                    // Save message: prefer local file path if downloaded, otherwise store the
                    // remote URL in fichierPath
                    User u = Session.getInstance().getCurrentUser();
                    int loggedUserId = u != null ? u.getId() : 1;
                    MessageChat newMsg = new MessageChat("", selectedGroupe.getId(), loggedUserId);
                    newMsg.setTypeMessage("GIF");
                    if (downloaded && targetPath != null) {
                        newMsg.setFichierPath(targetPath.toAbsolutePath().toString());
                    } else {
                        // store remote URL so rendering can use it
                        newMsg.setFichierPath(downloadUrl);
                    }

                    messageService.ajouter(newMsg);
                    loadMessages(selectedGroupe.getId());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de l'envoi du GIF: " + ex.getMessage()).showAndWait();
                }
            }
        });
    }

    private void addReactionButton(HBox messageActions, MessageChat msg) {
        Button btnReact = new Button("😊");
        btnReact.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #f97316; -fx-cursor: hand; -fx-font-size: 14;");
        btnReact.setTooltip(new Tooltip("Ajouter une réaction"));
        btnReact.setOnAction(e -> showEmojiPicker(msg));
        messageActions.getChildren().add(0, btnReact);
    }

    private void showEmojiPicker(MessageChat msg) {
        String[] emojis = { "👍", "❤️", "😂", "😮", "😢", "😡", "🎉", "🔥", "✨", "👏" };

        Popup popup = new Popup();
        popup.setAutoHide(true);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        int cols = 5;
        for (int i = 0; i < emojis.length; i++) {
            String emoji = emojis[i];
            Button b = new Button(emoji);
            b.setStyle(
                    "-fx-font-size:18; -fx-background-radius:8; -fx-background-color: #ffffff; -fx-cursor: hand; -fx-padding:6 10;");
            b.setOnAction(e -> {
                User cur = Session.getInstance().getCurrentUser();
                if (cur != null) {
                    // toggle via service
                    reactionService.addReaction(msg.getId(), cur.getId(), emoji);
                    // animation and refresh
                    loadMessages(selectedGroupe.getId());
                }
                popup.hide();
            });
            grid.add(b, i % cols, i / cols);
        }
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius:10; -fx-padding:8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8,0,0,2);");
        popup.getContent().add(grid);
        if (messageField != null && messageField.getScene() != null) {
            popup.show(messageField.getScene().getWindow());
        } else {
            popup.show(messagesBox.getScene().getWindow());
        }
    }

    private void addReaction(int messageId, String emoji) {
        User u = Session.getInstance().getCurrentUser();
        if (u != null) {
            try {
                // toggle en BD
                reactionService.addReaction(messageId, u.getId(), emoji);
            } catch (Exception ex) {
                System.err.println("Erreur lors de l'ajout de réaction: " + ex.getMessage());
            }
            // recharger messages pour propager aux autres (polling automatique) et retour
            // immédiat
            if (selectedGroupe != null) {
                loadMessages(selectedGroupe.getId());
            }
        }
    }

    private void showReactionDetails(int messageId, String emoji) {
        List<Integer> userIds = reactionService.getUsersWhoReacted(messageId, emoji);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réactions : " + emoji);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        if (userIds == null || userIds.isEmpty()) {
            Label none = new Label("Aucune réaction.");
            none.setFont(Font.font("System", 13));
            content.getChildren().add(none);
        } else {
            for (Integer uid : userIds) {
                User uu = null;
                try {
                    uu = utilisateurService.getUserById(uid);
                } catch (Exception ex) {
                }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                // avatar avec initiales
                StackPane avatar = createInitialsAvatar(uu != null ? uu.getName() : "User", uid);

                Label name = new Label(uu != null ? uu.getName() : "User " + uid);
                name.setFont(Font.font("System", 13));
                name.setTextFill(Color.web("#1e1b4b"));

                Label em = new Label(emoji);
                em.setStyle("-fx-font-size:14px; -fx-padding:4; -fx-background-radius:6;");

                row.getChildren().addAll(avatar, name, em);
                content.getChildren().add(row);
            }
        }
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private StackPane createInitialsAvatar(String name, Integer uid) {
        // default small avatar size
        return createInitialsAvatar(name, uid, 14);
    }

    private StackPane createInitialsAvatar(String name, Integer uid, int size) {
        String initials = "?";
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1)
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            else
                initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        Circle c = new Circle(size);
        // couleur déterminée par uid pour variation
        int colorIdx = (uid != null ? Math.abs(uid.hashCode()) : name.hashCode()) % 6;
        String[] palette = { "#06b6d4", "#f59e0b", "#ef4444", "#10b981", "#8b5cf6", "#f97316" };
        c.setFill(Color.web(palette[colorIdx]));
        c.setEffect(new DropShadow(4, Color.gray(0, 0.25)));
        Label lbl = new Label(initials);
        lbl.setTextFill(Color.WHITE);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        StackPane sp = new StackPane(c, lbl);
        sp.setPrefSize(size + 4, size + 4);
        return sp;
    }
}
