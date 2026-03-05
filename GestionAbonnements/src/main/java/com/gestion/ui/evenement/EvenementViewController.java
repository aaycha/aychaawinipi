package com.gestion.ui.evenement;

import com.gestion.controllers.EvenementDAO;
import com.gestion.entities.Evenement;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EvenementViewController {

    @FXML
    private ListView<Evenement> listView;
    @FXML
    private TextField searchField;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnNouvelEvent;

    private final EvenementDAO dao = new EvenementDAO();
    private final ObservableList<Evenement> data = FXCollections.observableArrayList();
    private FilteredList<Evenement> filteredData;
    private Evenement selectedEvent = null;

    @FXML
    public void initialize() {
        setupListView();
        filteredData = new FilteredList<>(data, p -> true);
        listView.setItems(filteredData);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(event -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lowerCaseFilter = newVal.toLowerCase();
                if (event.getTitre().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (event.getLieu() != null && event.getLieu().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
            updateCount();
        });

        applyRoleRestrictions();
        onActualiser();
    }

    private void applyRoleRestrictions() {
        boolean isAdmin = com.gestion.controllers.MainController
                .getCurrentRole() == com.gestion.controllers.MainController.Role.ADMIN;
        btnModifier.setVisible(isAdmin);
        btnModifier.setManaged(isAdmin);
        btnSupprimer.setVisible(isAdmin);
        btnSupprimer.setManaged(isAdmin);
        if (btnNouvelEvent != null) {
            btnNouvelEvent.setVisible(isAdmin);
            btnNouvelEvent.setManaged(isAdmin);
        }
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createEventCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 8 15;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedEvent = newVal;
            boolean isAdmin = com.gestion.controllers.MainController
                    .getCurrentRole() == com.gestion.controllers.MainController.Role.ADMIN;
            btnModifier.setDisable(!isAdmin || newVal == null);
            btnSupprimer.setDisable(!isAdmin || newVal == null);
        });
    }

    private javafx.scene.Node createEventCard(Evenement item) {
        HBox card = new HBox(15);
        card.getStyleClass().add("modern-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 15; -fx-padding: 15; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 15;");

        StackPane iconPane = new StackPane();
        Circle bg = new Circle(22, Color.web("#3b82f6", 0.15));
        Text icon = new Text("📅");
        icon.setFont(Font.font(20));
        iconPane.getChildren().addAll(bg, icon);

        VBox info = new VBox(5);
        Label title = new Label(item.getTitre());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label details = new Label(item.getLieu() + " • "
                + (item.getDateDebut() != null ? item.getDateDebut().toLocalDate().toString() : "TBD"));
        details.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        info.getChildren().addAll(title, details);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox side = new VBox(8);
        side.setAlignment(Pos.CENTER_RIGHT);
        Label typeBadge = new Label(item.getType());
        typeBadge.setStyle(
                "-fx-background-color: rgba(59, 130, 246, 0.2); -fx-text-fill: #60a5fa; -fx-padding: 2 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        side.getChildren().add(typeBadge);

        // Bouton Participer pour les utilisateurs
        if (com.gestion.controllers.MainController
                .getCurrentRole() == com.gestion.controllers.MainController.Role.UTILISATEUR) {
            Button btnParticiper = new Button("PARTICIPER");
            btnParticiper.setStyle(
                    "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20;");
            btnParticiper.setOnAction(e -> onParticiper(item));
            side.getChildren().add(btnParticiper);
        }

        card.getChildren().addAll(iconPane, info, spacer, side);
        return card;
    }

    private void onParticiper(Evenement item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/participation/participation-form.fxml"));
            Parent root = loader.load();
            com.gestion.ui.participation.ParticipationFormController ctrl = loader.getController();

            ctrl.setAdminMode(false);
            ctrl.setCurrentUserId(Long.valueOf(com.gestion.tools.Session.getInstance().getCurrentUserId()));

            // Créer une participation blanche liée à l'événement
            com.gestion.entities.Participation p = new com.gestion.entities.Participation();
            p.setEvenementId(Long.valueOf(item.getIdEvent()));
            ctrl.setParticipation(p);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Participer à " + item.getTitre());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void onActualiser() {
        try {
            List<Evenement> list = dao.findAll();
            data.setAll(list);
            updateCount();
            if (statusInfoLabel != null)
                statusInfoLabel.setText("Données actualisées");
        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
        }
    }

    private void updateCount() {
        if (countLabel != null)
            countLabel.setText(filteredData.size() + " événement(s)");
    }

    @FXML
    public void onNouvelEvenement() {
        openForm(null);
    }

    @FXML
    public void onModifier() {
        if (selectedEvent != null)
            openForm(selectedEvent);
    }

    @FXML
    public void onSupprimer() {
        if (selectedEvent != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet événement ?", ButtonType.YES,
                    ButtonType.NO);
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    try {
                        dao.delete(selectedEvent.getIdEvent());
                        onActualiser();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @FXML
    public void onListClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2 && selectedEvent != null) {
            onModifier();
        }
    }

    private void openForm(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/evenement/evenement-form.fxml"));
            Parent root = loader.load();
            EvenementFormController ctrl = loader.getController();
            ctrl.setEvenement(e);
            ctrl.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(e == null ? "Créer un événement" : "Modifier l'événement");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
