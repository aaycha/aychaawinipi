package com.gestion.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.gestion.entities.User;
import com.gestion.services.UserService;
import com.gestion.tools.Session; // ✅ NEW: Import Session

import java.sql.SQLException;

public class Logincontroller {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordVisible;

    @FXML
    private Button loginBtn;

    @FXML
    private Button togglePasswordBtn;

    @FXML
    private Label eyeIcon;

    @FXML
    private Hyperlink signupLink;

    @FXML
    private Button closeBtn;

    @FXML
    private Button minimizeBtn;

    @FXML
    private Button maximizeBtn;

    private UserService userService = new UserService();

    @FXML
    public void initialize() {
        // Sync password fields
        if (passwordField != null && passwordVisible != null) {
            passwordField.textProperty().bindBidirectional(passwordVisible.textProperty());
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String plainPassword = passwordField.getText();

        // Validation
        if (email.isEmpty() || plainPassword.isEmpty()) {
            showAlert("Please enter email and password");
            return;
        }

        try {
            // ✅ LOGIN METHOD USES BCrypt TO VERIFY PASSWORD
            User user = userService.login(email, plainPassword);

            if (user != null) {
                // ✅ LOGIN SUCCESSFUL

                // ✅ NEW: Store user in Session
                Session.getInstance().setCurrentUser(user);

                showSuccess("Welcome back, " + user.getName() + "! 🎉");

                // ✅ ROLE-BASED NAVIGATION
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(1.5));
                delay.setOnFinished(event -> {
                    try {
                        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                            // Navigate to Dashboard for admins
                            loadDashboard(user);
                        } else {
                            // Navigate to main app for regular users
                            loadMainApp(user);
                        }
                    } catch (Exception e) {
                        showAlert("Navigation error: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                delay.play();

                clearFields();

            } else {
                // ❌ INVALID CREDENTIALS
                showAlert("Invalid email or password");
            }

        } catch (SQLException e) {
            showAlert("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    @FXML
    private void goToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/usersaif/UserSignUP.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) signupLink.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            showAlert("Error loading signup page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDashboard(User user) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/usersaif/Dashboard.fxml"));
        Parent root = loader.load();

        // Pass user data to dashboard controller
        DashboardController controller = loader.getController();
        controller.setCurrentUser(user);

        Stage stage = (Stage) loginBtn.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();

        System.out.println("✅ Admin dashboard loaded for: " + user.getName());
    }

    private void loadMainApp(User user) throws Exception {
        // ✅ NEW: Set the role globally so MainController starts in user mode
        MainController.setCurrentRole(MainController.Role.UTILISATEUR);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main-view.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) loginBtn.getScene().getWindow();
        stage.setScene(new Scene(root, 1200, 800));
        stage.centerOnScreen();
        stage.show();

        System.out.println("✅ Main application loaded for: " + user.getName());
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordVisible.setVisible(true);
            eyeIcon.setText("🙈");
        } else {
            passwordField.setVisible(true);
            passwordVisible.setVisible(false);
            eyeIcon.setText("👁");
        }
    }

    // ── Window Controls ────────────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) minimizeBtn.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) maximizeBtn.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void onCloseHover() {
        closeBtn.setStyle(
                "-fx-background-color: #c42b1c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    @FXML
    private void onCloseExit() {
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #000000;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    @FXML
    private void onMinimizeHover() {
        minimizeBtn.setStyle(
                "-fx-background-color: #e5e5e5;" +
                        "-fx-text-fill: #000000;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    @FXML
    private void onMinimizeExit() {
        minimizeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #000000;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    @FXML
    private void onMaximizeHover() {
        maximizeBtn.setStyle(
                "-fx-background-color: #e5e5e5;" +
                        "-fx-text-fill: #000000;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    @FXML
    private void onMaximizeExit() {
        maximizeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #000000;" +
                        "-fx-font-size: 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void showAlert(String message) {
        showToast(message, "error");
    }

    private void showSuccess(String message) {
        showToast(message, "success");
    }

    private void showToast(String message, String type) {
        // Get the right side form container (where the login form is)
        javafx.scene.Parent root = loginBtn.getParent();
        while (root != null && !(root instanceof javafx.scene.layout.AnchorPane)) {
            root = root.getParent();
        }

        if (root == null)
            return;

        javafx.scene.layout.AnchorPane formContainer = (javafx.scene.layout.AnchorPane) root;

        // Create toast box
        javafx.scene.layout.VBox toast = new javafx.scene.layout.VBox();
        toast.setAlignment(javafx.geometry.Pos.CENTER);
        toast.setPadding(new javafx.geometry.Insets(16, 24, 16, 24));
        toast.setSpacing(8);
        toast.setMaxWidth(380);

        // Style based on type
        if (type.equals("error")) {
            toast.setStyle(
                    "-fx-background-color: #fee2e2;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: #ef4444;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");
        } else {
            toast.setStyle(
                    "-fx-background-color: #d1fae5;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: #10b981;" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");
        }

        // Icon and message in HBox
        javafx.scene.layout.HBox content = new javafx.scene.layout.HBox();
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setSpacing(12);

        Label icon = new Label(type.equals("error") ? "⚠️" : "✅");
        icon.setStyle("-fx-font-size: 20;");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-font-size: 14;" +
                        "-fx-text-fill: " + (type.equals("error") ? "#dc2626" : "#047857") + ";" +
                        "-fx-font-weight: 600;");

        content.getChildren().addAll(icon, messageLabel);
        toast.getChildren().add(content);

        // Position toast at top center of the form
        javafx.scene.layout.AnchorPane.setTopAnchor(toast, 20.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(toast, 60.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(toast, 60.0);

        formContainer.getChildren().add(toast);

        // Animations
        toast.setOpacity(0);
        toast.setTranslateY(-20);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                toast);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        javafx.animation.TranslateTransition slideDown = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(300), toast);
        slideDown.setFromY(-20);
        slideDown.setToY(0);

        fadeIn.play();
        slideDown.play();

        // Shake for errors
        if (type.equals("error")) {
            javafx.animation.Timeline shake = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(0),
                            new javafx.animation.KeyValue(toast.translateXProperty(), 0)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(50),
                            new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(100),
                            new javafx.animation.KeyValue(toast.translateXProperty(), 10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(150),
                            new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(200),
                            new javafx.animation.KeyValue(toast.translateXProperty(), 10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(250),
                            new javafx.animation.KeyValue(toast.translateXProperty(), -10)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                            new javafx.animation.KeyValue(toast.translateXProperty(), 0)));
            shake.play();
        }

        // Auto dismiss
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3.5));
        pause.setOnFinished(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), toast);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            javafx.animation.TranslateTransition slideUp = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(300), toast);
            slideUp.setFromY(0);
            slideUp.setToY(-20);

            fadeOut.play();
            slideUp.play();

            fadeOut.setOnFinished(ev -> formContainer.getChildren().remove(toast));
        });
        pause.play();
    }

    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        passwordVisible.clear();
    }
}