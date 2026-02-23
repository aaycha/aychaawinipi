package com.gestion.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the main Admin Dashboard.
 * Handles user management, navigation, and smart feature integration.
 */
public class DashboardController {

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
    private static final String WEATHER_API_KEY = "PMxJCjIOARDqkE9u";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserService userService = new UserService();
    private User currentUser;
    private List<User> allUsersList = new ArrayList<>();

    @FXML
    public void initialize() {
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
            try {
                String url = "http://api.weatherapi.com/v1/current.json?key=" + WEATHER_API_KEY + "&q=Tunis&lang=fr";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode current = root.get("current");
                    JsonNode location = root.get("location");

                    String temp = String.valueOf(current.get("temp_c").asDouble()) + "\u00B0C";
                    String condition = current.get("condition").get("text").asText();
                    String city = location.get("name").asText();

                    Platform.runLater(() -> {
                        if (weatherTemp != null)
                            weatherTemp.setText(temp);
                        if (weatherCondition != null)
                            weatherCondition.setText(condition);
                        if (weatherLocation != null)
                            weatherLocation.setText("\uD83D\uDCCD " + city);
                    });
                } else {
                    System.err.println("Weather API returned status: " + response.statusCode());
                    Platform.runLater(() -> {
                        if (weatherTemp != null)
                            weatherTemp.setText("--\u00B0C");
                        if (weatherCondition != null)
                            weatherCondition.setText("Unavailable");
                    });
                }
            } catch (Exception e) {
                System.err.println("Weather fetch failed: " + e.getMessage());
                Platform.runLater(() -> {
                    if (weatherTemp != null)
                        weatherTemp.setText("--\u00B0C");
                    if (weatherCondition != null)
                        weatherCondition.setText("Offline");
                });
            }
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
        if (dashboardOverview != null && mainContentArea != null) {
            mainContentArea.getChildren().setAll(dashboardOverview);
            updateStatistics();
        }
    }

    @FXML
    private void showUsers() {
        showDashboard();
    }

    @FXML
    private void loadAbonnements() {
        loadModule("/views/abonnement/abonnement.fxml", "Abonnements");
    }

    @FXML
    private void loadParticipations() {
        loadModule("/views/participation/participation.fxml", "Participations");
    }

    @FXML
    private void loadRestaurants() {
        loadModule("/views/restaurant/restaurant-liste.fxml", "Restaurants");
    }

    @FXML
    private void loadRepas() {
        loadModule("/views/repas/repas-liste.fxml", "Repas");
    }

    @FXML
    private void loadMenus() {
        loadModule("/views/menu/menu-liste.fxml", "Menus");
    }

    @FXML
    private void loadRecommandations() {
        loadModule("/views/recommandations/recommandations.fxml", "AI Recommandations");
    }

    @FXML
    private void loadPlanner() {
        loadModule("/views/repas/admin-menu-planner.fxml", "Planner");
    }

    @FXML
    private void loadPromoCodes() {
        loadModule("/views/admin/promo-list.fxml", "Codes Promo");
    }

    @FXML
    private void loadInventory() {
        loadModule("/views/admin/inventaire.fxml", "Inventaire");
    }

    @FXML
    private void loadAnalytics() {
        loadModule("/views/analytics/analytics.fxml", "Analytics");
    }

    @FXML
    private void loadMap() {
        loadModule("/views/map/map-view.fxml", "Restaurant Map");
    }

    @FXML
    private void toggleChatbot() {
        if (chatbotContainer != null) {
            chatbotContainer.setVisible(!chatbotContainer.isVisible());
        }
    }

    private void loadModule(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (mainContentArea != null) {
                mainContentArea.getChildren().setAll(root);
            }
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("onActualiser");
                    method.invoke(controller);
                } catch (NoSuchMethodException ignored) {
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
