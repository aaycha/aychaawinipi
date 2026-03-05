package com.gestion.controllers;

import com.gestion.entities.User;
import com.gestion.services.UserService;
import com.gestion.tools.PasswordHasher;
import com.gestion.tools.Session;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.gestion.interfaces.DataReceiver;

/**
 * Controller for the main Admin Dashboard.
 * Handles user management, navigation, and smart feature integration.
 */
public class DashboardController implements DataReceiver<String> {

    @FXML
    private TextField searchField;
    @FXML
    private Label profileName;
    @FXML
    private Label profileEmail;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label adminUsersLabel;
    @FXML
    private Label regularUsersLabel;
    @FXML
    private Label profileRole;
    @FXML
    private GridPane usersGrid;
    @FXML
    private Button dashboardBtn;
    @FXML
    private Button filterAllBtn;
    @FXML
    private Button filterAlphabetBtn;
    @FXML
    private Button filterLatestBtn;
    @FXML
    private Label topBarUserName;
    @FXML
    private Label welcomeLabel;
    @FXML
    private VBox chatbotContainer;
    @FXML
    private StackPane mainContentArea;
    @FXML
    private BorderPane dashboardOverview;
    @FXML
    private Label apiStatusLabel;
    @FXML
    private Circle apiStatusCircle;
    @FXML
    private Label weatherTemp;
    @FXML
    private Label weatherCondition;
    @FXML
    private Label weatherLocation;

    private static final String GEMINI_API_KEY = "AIzaSyDsEq2TiVuJPoFhaTMUFI6NcELBiJePwNc";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final com.gestion.services.WeatherService weatherService = com.gestion.services.WeatherService
            .getInstance();

    private final UserService userService = new UserService();
    private User currentUser;
    private List<User> allUsersList = new ArrayList<>();

    private static DashboardController instance;

    public static DashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        loadUsers();
        updateStatistics();
        startApiMonitoring();
        fetchWeather();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isEmpty()) {
                    displayUsers(allUsersList);
                } else {
                    List<User> filtered = allUsersList.stream()
                            .filter(u -> (u.getName() != null
                                    && u.getName().toLowerCase().contains(newVal.toLowerCase()))
                                    || (u.getEmail() != null
                                            && u.getEmail().toLowerCase().contains(newVal.toLowerCase())))
                            .collect(Collectors.toList());
                    displayUsers(filtered);
                }
            });
        }
    }

    private void startApiMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                boolean online = checkApiSignal();
                Platform.runLater(() -> updateApiStatusUI(online));
                try {
                    Thread.sleep(10000); // Check every 10s
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private boolean checkApiSignal() {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + GEMINI_API_KEY;
            String body = "{\"contents\":[{\"parts\":[{\"text\":\"ping\"}]}],\"generationConfig\":{\"maxOutputTokens\":5}}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return (response.statusCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateApiStatusUI(boolean online) {
        if (apiStatusLabel != null && apiStatusCircle != null) {
            if (online) {
                apiStatusLabel.setText("GEMINI AI: ONLINE");
                apiStatusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11; -fx-font-weight: 800;");
                apiStatusCircle.setFill(Color.web("#22c55e"));
            } else {
                apiStatusLabel.setText("GEMINI AI: OFFLINE");
                apiStatusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11; -fx-font-weight: 800;");
                apiStatusCircle.setFill(Color.web("#f87171"));
            }
        }
    }

    /**
     * Fetches current weather from weatherapi.com and updates the dashboard widget.
     */
    private void fetchWeather() {
        Thread weatherThread = new Thread(() -> {
            com.gestion.services.WeatherService.WeatherInfo info = weatherService.getCurrentWeather("Tunis");
            Platform.runLater(() -> {
                if (weatherTemp != null)
                    weatherTemp.setText(info.temp);
                if (weatherCondition != null)
                    weatherCondition.setText(info.condition);
                if (weatherLocation != null)
                    weatherLocation.setText("\uD83D\uDCCD " + info.city);
            });
        });
        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    private void updateStatistics() {
        try {
            List<User> users = userService.recuperer();
            if (users == null)
                return;

            if (totalUsersLabel != null)
                totalUsersLabel.setText(String.valueOf(users.size()));

            long adminCount = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
            if (adminUsersLabel != null)
                adminUsersLabel.setText(String.valueOf(adminCount));

            long regularCount = users.stream().filter(u -> "USER".equalsIgnoreCase(u.getRole())).count();
            if (regularUsersLabel != null)
                regularUsersLabel.setText(String.valueOf(regularCount));
        } catch (Exception e) {
            System.err.println("DB Status: Unstable. Retrying statistics later: " + e.getMessage());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (profileName != null)
            profileName.setText(user.getName());
        if (profileEmail != null)
            profileEmail.setText(user.getEmail());
        if (profileRole != null)
            profileRole.setText(user.getRole());
        if (topBarUserName != null)
            topBarUserName.setText(user.getName());
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome back, " + user.getName());
    }

    @Override
    public void setData(String module) {
        if ("EventSponsor".equals(module)) {
            Platform.runLater(this::showEventSponsor);
        } else if ("Sponsor".equals(module)) {
            Platform.runLater(() -> loadModule("/views/SponsorView.fxml", "Gestion Sponsors"));
        }
    }

    private void loadUsers() {
        try {
            allUsersList = userService.recuperer();
            if (allUsersList != null) {
                displayUsers(allUsersList);
            }
        } catch (Exception e) {
            System.err.println("DB Status: Unstable. Retrying user load later...");
            allUsersList = new ArrayList<>();
        }
    }

    private void displayUsers(List<User> users) {
        if (usersGrid == null)
            return;
        usersGrid.getChildren().clear();

        int column = 0;
        int row = 0;
        int maxColumns = 3;

        for (User user : users) {
            VBox userCard = createUserCard(user);
            usersGrid.add(userCard, column, row);
            column++;
            if (column == maxColumns) {
                column = 0;
                row++;
            }
        }
    }

    private VBox createUserCard(User user) {
        VBox card = new VBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.setSpacing(12);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        card.setPrefWidth(250);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #7B5FF5; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(123,95,245,0.15), 12, 0, 0, 4);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        StackPane avatarContainer = new StackPane();
        Circle avatar = new Circle(20);
        avatar.setFill(Color.web(getRandomColor()));
        Label initial = new Label(user.getName().isEmpty() ? "?" : user.getName().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-font-size: 16; -fx-font-weight: 700; -fx-text-fill: white;");
        avatarContainer.getChildren().addAll(avatar, initial);

        VBox userInfo = new VBox(2);
        Label userName = new Label(user.getName());
        userName.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #212529;");
        Label userRole = new Label(user.getRole());
        userRole.setStyle("-fx-font-size: 12; -fx-text-fill: #6c757d;");
        userInfo.getChildren().addAll(userName, userRole);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button menuBtn = new Button("⋮");
        menuBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #6c757d; -fx-font-size: 18; -fx-cursor: hand; -fx-padding: 0;");
        menuBtn.setOnAction(e -> showUserMenu(user, menuBtn, card, userRole));

        header.getChildren().addAll(avatarContainer, userInfo, spacer, menuBtn);
        Label email = new Label(user.getEmail());
        email.setStyle("-fx-font-size: 12; -fx-text-fill: #6c757d;");
        email.setWrapText(true);
        card.getChildren().addAll(header, email);
        card.setOnMouseClicked(ev -> showUserDetails(user));

        return card;
    }

    private void showUserMenu(User user, Button btn, VBox card, Label roleLabel) {
        VBox popupContent = new VBox(8);
        popupContent.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 6; -fx-min-width: 160; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 6);");

        String[] actions = { "⭐ Add to favourites", "⬆ Promote", "🚫 Ban", "🗑 Delete" };
        for (String actionText : actions) {
            Label action = new Label(actionText);
            styleMenuItem(action, actionText.contains("Delete"));
            action.setOnMouseClicked(ev -> {
                Popup popup = (Popup) action.getScene().getWindow();
                handleUserAction(actionText, user, card, roleLabel);
                popup.hide();
            });
            popupContent.getChildren().add(action);
        }

        Popup popup = new Popup();
        popup.getContent().add(popupContent);
        popup.setAutoHide(true);
        double x = btn.localToScreen(btn.getBoundsInLocal()).getMinX() - 100 + btn.getBoundsInLocal().getWidth();
        double y = btn.localToScreen(btn.getBoundsInLocal()).getMinY() + btn.getBoundsInLocal().getHeight() + 4;
        popup.show(btn, x, y);
    }

    private void handleUserAction(String action, User user, VBox card, Label roleLabel) {
        try {
            if (action.contains("Delete")) {
                userService.supprimer(user.getId());
                usersGrid.getChildren().remove(card);
                updateStatistics();
            } else if (action.contains("Promote")) {
                if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                    user.setRole("ADMIN");
                    userService.modifier(user);
                    if (roleLabel != null)
                        roleLabel.setText("ADMIN");
                    updateStatistics();
                }
            } else {
                System.out.println("Action clicked: " + action + " for user " + user.getName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getRandomColor() {
        String[] colors = { "#7B5FF5", "#10b981", "#ef4444", "#f59e0b", "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6" };
        return colors[(int) (Math.random() * colors.length)];
    }

    private void showUserDetails(User user) {
        if (profileName != null)
            profileName.setText(user.getName());
        if (profileEmail != null)
            profileEmail.setText(user.getEmail());
        if (profileRole != null)
            profileRole.setText(user.getRole());
    }

    @FXML
    private void showDashboard() {
        if (dashboardOverview != null) {
            dashboardOverview.setVisible(true);
            dashboardOverview.setManaged(true);
            if (mainContentArea != null) {
                mainContentArea.getChildren().setAll(dashboardOverview);
            }
        }
    }

    @FXML
    private void showUsers() {
        showDashboard(); // Users list is part of the dashboard overview in this design
    }

    @FXML
    public void showParticipationSelector() {
        String[][] items = {
                { "🤝", "Participation", "Gérer les participations aux événements",
                        "/views/participation/participation.fxml" },
                { "🎫", "Abonnement", "Gestion des abonnements", "/views/abonnement/abonnement.fxml" }
        };
        VBox selector = buildSelectorView("Participation & Abonnement", "Gérez vos membres et leurs accès", items, 2);
        if (mainContentArea != null) {
            mainContentArea.getChildren().setAll(selector);
        }
    }

    @FXML
    public void showEvenementSelector() {
        String[][] items = {
                { "🎪", "Événements", "Ajouter un nouvel événement", "/Feryel/AjouterEvenement.fxml" },
                { "📅", "Programmes", "Ajouter un programme à un événement", "/Feryel/AjouterProgramme.fxml" },
                { "🤝", "Sponsors", "Gérer les sponsors des événements", "/views/EventSponsorView.fxml" }
        };
        VBox selector = buildSelectorView("Événements & Programmes", "Organisez vos activités de camping", items, 3);
        if (mainContentArea != null) {
            mainContentArea.getChildren().setAll(selector);
        }
    }

    @FXML
    public void showRestaurationSelector() {
        String[][] items = {
                { "🍱", "Repas", "Gérer les repas disponibles", "/views/repas/repas-liste.fxml" },
                { "📜", "Menus", "Composer et gérer les menus", "/views/menu/menu-liste.fxml" },
                { "🗓️", "Planification", "Planifier les menus de la semaine", "/views/repas/admin-menu-planner.fxml" },
                { "📈", "Analytics", "Statistiques et rapports détaillés", "/views/analytics/analytics.fxml" },
                { "🤖", "IA Recommandations", "Recommandations intelligentes par IA",
                        "/views/recommandations/recommandations.fxml" },
                { "🎟️", "Codes Promo", "Gérer les codes promotionnels", "/views/admin/promo-list.fxml" },
                { "📦", "Inventaire", "Suivi du stock et inventaire", "/views/admin/inventaire.fxml" },
                { "🏨", "Restaurants", "Gérer les restaurants partenaires", "/views/restaurant/restaurant-liste.fxml" },
                { "🚩", "Restaurant Map", "Carte interactive des restaurants", "/views/map/map-view.fxml" }
        };
        VBox selector = buildSelectorView("Restauration", "Choisissez un module à gérer", items, 3);
        if (mainContentArea != null) {
            mainContentArea.getChildren().setAll(selector);
        }
    }

    @FXML
    private void showEventSponsor() {
        loadModule("/views/EventSponsorView.fxml");
    }

    @FXML
    private void showEquipements() {
        loadModule("/views/wael/EquipementView.fxml");
    }

    @FXML
    private void showBoutique() {
        loadModule("/views/wael/EquipementStoreView.fxml");
    }

    @FXML
    private void showMessagerie() {
        loadModule("/views/wael/ChatView.fxml");
    }

    /**
     * Builds a premium card-based selector view.
     */
    private VBox buildSelectorView(String title, String subtitle, String[][] items, int maxColumns) {
        VBox container = new VBox(20);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(40, 30, 30, 30));
        container.setStyle("-fx-background-color: transparent;");

        // Back button
        Button backBtn = new Button("← Retour au Dashboard");
        backBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: 600; "
                        + "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand; "
                        + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: 600; "
                        + "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand; "
                        + "-fx-border-color: rgba(249,115,22,0.5); -fx-border-radius: 20;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #cbd5e1; -fx-font-size: 11; -fx-font-weight: 600; "
                        + "-fx-background-radius: 20; -fx-padding: 8 18; -fx-cursor: hand; "
                        + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;"));
        backBtn.setOnAction(e -> showDashboard());

        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER_LEFT);

        // Title section
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 26; -fx-font-weight: 900; -fx-text-fill: white;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #94a3b8; -fx-font-weight: 500;");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Cards grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10, 0, 0, 0));

        int col = 0;
        int row = 0;
        for (String[] item : items) {
            VBox card = createModuleCard(item[0], item[1], item[2], item[3]);
            grid.add(card, col, row);
            col++;
            if (col >= maxColumns) {
                col = 0;
                row++;
            }
        }

        // Wrap grid in a glass panel
        VBox glassWrapper = new VBox(grid);
        glassWrapper.setAlignment(Pos.CENTER);
        glassWrapper.setPadding(new Insets(25));
        glassWrapper.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.75); -fx-background-radius: 16; "
                        + "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-border-radius: 16; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 8);");

        container.getChildren().addAll(backRow, headerBox, glassWrapper);
        return container;
    }

    /**
     * Creates a single premium module card for the selector view.
     */
    private VBox createModuleCard(String icon, String name, String description, String fxmlPath) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 16, 20, 16));
        card.setPrefWidth(200);
        card.setPrefHeight(170);

        String normalStyle = "-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 14; "
                + "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1; -fx-border-radius: 14; "
                + "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);";
        String hoverStyle = "-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 14; "
                + "-fx-border-color: rgba(249,115,22,0.6); -fx-border-width: 1.5; -fx-border-radius: 14; "
                + "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(249,115,22,0.2), 15, 0, 0, 5);";

        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        // Icon
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32; -fx-text-fill: #F97316;");

        // Name
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 800; -fx-text-fill: white;");

        // Description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8; -fx-font-weight: 500;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(170);
        descLabel.setAlignment(Pos.CENTER);

        // Open button
        Button openBtn = new Button("Ouvrir →");
        openBtn.setStyle(
                "-fx-background-color: rgba(249,115,22,0.8); -fx-text-fill: white; -fx-font-size: 10; "
                        + "-fx-font-weight: 700; -fx-background-radius: 16; -fx-padding: 5 14; -fx-cursor: hand;");
        openBtn.setOnMouseEntered(e -> openBtn.setStyle(
                "-fx-background-color: #F97316; -fx-text-fill: white; -fx-font-size: 10; "
                        + "-fx-font-weight: 700; -fx-background-radius: 16; -fx-padding: 5 14; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.4), 10, 0, 0, 0);"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle(
                "-fx-background-color: rgba(249,115,22,0.8); -fx-text-fill: white; -fx-font-size: 10; "
                        + "-fx-font-weight: 700; -fx-background-radius: 16; -fx-padding: 5 14; -fx-cursor: hand;"));
        openBtn.setOnAction(e -> loadModule(fxmlPath));

        card.getChildren().addAll(iconLabel, nameLabel, descLabel, openBtn);

        // Also allow click on the entire card
        card.setOnMouseClicked(e -> loadModule(fxmlPath));

        return card;
    }

    @FXML
    private void toggleChatbot() {
        if (chatbotContainer != null) {
            chatbotContainer.setVisible(!chatbotContainer.isVisible());
        }
    }

    public void loadModule(String fxmlPath) {
        loadModule(fxmlPath, null);
    }

    public <T> void loadModule(String fxmlPath, T data) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("FXML introuvable: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            if (mainContentArea != null) {
                mainContentArea.getChildren().setAll(root);
            }

            Object controller = loader.getController();
            if (controller instanceof DataReceiver) {
                @SuppressWarnings("unchecked")
                DataReceiver<T> receiver = (DataReceiver<T>) controller;
                receiver.setData(data);
            }

            // Trigger refresh if method exists
            if (controller != null) {
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("onActualiser");
                    method.invoke(controller);
                } catch (NoSuchMethodException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading module: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddUser() {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("USER", "ADMIN");
        roleBox.setValue("USER");

        Button saveBtn = new Button("Add User");
        saveBtn.setStyle(
                "-fx-background-color: #7B5FF5; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            try {
                User u = new User(nameField.getText(), emailField.getText(),
                        PasswordHasher.hashPassword(passField.getText()), roleBox.getValue(), phoneField.getText(),
                        "NO", null, 0);
                userService.ajouter(u);
                loadUsers();
                updateStatistics();
                stage.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(new Label("Add New User"), nameField, emailField, passField, phoneField, roleBox,
                saveBtn);
        stage.setScene(new Scene(root, 350, 450));
        stage.setTitle("Add User");
        stage.show();
    }

    @FXML
    private void sortAlphabet() {
        filterUsers("alphabet");
    }

    @FXML
    private void sortRecent() {
        filterUsers("recent");
    }

    @FXML
    private void sortLatest() {
        filterUsers("latest");
    }

    private void filterUsers(String type) {
        try {
            List<User> users = userService.recuperer();
            if ("alphabet".equals(type)) {
                users.sort(Comparator.comparing(User::getName));
            } else if ("latest".equals(type)) {
                users.sort((u1, u2) -> Integer.compare(u2.getId(), u1.getId()));
            }
            displayUsers(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void closeWindow() {
        Platform.exit();
    }

    @FXML
    private void minimizeWindow() {
        ((Stage) dashboardBtn.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage s = (Stage) dashboardBtn.getScene().getWindow();
        s.setMaximized(!s.isMaximized());
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            Session.getInstance().logout();
            Parent root = FXMLLoader.load(getClass().getResource("/usersaif/User.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void styleMenuItem(Label action, boolean isDelete) {
        String normalColor = isDelete ? "#e74c3c" : "#212529";
        String hoverColor = isDelete ? "#c0392b" : "#7B5FF5";

        action.setStyle("-fx-font-size: 13; -fx-text-fill: " + normalColor
                + "; -fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: 500;");
        action.setOnMouseEntered(e -> action.setStyle("-fx-font-size: 13; -fx-text-fill: " + hoverColor
                + "; -fx-padding: 8 14; -fx-background-radius: 8; -fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-font-weight: 500;"));
        action.setOnMouseExited(e -> action.setStyle("-fx-font-size: 13; -fx-text-fill: " + normalColor
                + "; -fx-padding: 8 14; -fx-background-radius: 8; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: 500;"));
    }
}
