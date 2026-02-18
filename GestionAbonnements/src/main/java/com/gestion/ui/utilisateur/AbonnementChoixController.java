package com.gestion.ui.utilisateur;

import com.gestion.controllers.AbonnementController;
import com.gestion.entities.Abonnement;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vue simplifiée "Mon Abonnement" pour l'espace utilisateur.
 * Affiche les abonnements filtrés sur userId.
 */
public class AbonnementChoixController {

    @FXML private TableView<Abonnement> table;
    @FXML private TableColumn<Abonnement, Long> colId;
    @FXML private TableColumn<Abonnement, String> colType;
    @FXML private TableColumn<Abonnement, String> colDateDebut;
    @FXML private TableColumn<Abonnement, String> colDateFin;
    @FXML private TableColumn<Abonnement, String> colPrix;
    @FXML private TableColumn<Abonnement, String> colStatut;
    @FXML private TableColumn<Abonnement, String> colAutoRenew;
    @FXML private Label statusLabel;

    private final AbonnementController controller = new AbonnementController();
    // TODO: remplacer par l'utilisateur connecté
    private Long currentUserId = 1L;

    @FXML
    public void initialize() {
        setupColumns();
        onActualiser();
    }

    private void setupColumns() {
        if (table == null) return;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (colId != null) {
            colId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()).asObject());
        }
        if (colType != null) {
            colType.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getType() != null ? cd.getValue().getType().getLabel() : ""));
        }
        if (colDateDebut != null) {
            colDateDebut.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getDateDebut() != null ? cd.getValue().getDateDebut().format(df) : ""));
        }
        if (colDateFin != null) {
            colDateFin.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getDateFin() != null ? cd.getValue().getDateFin().format(df) : ""));
        }
        if (colPrix != null) {
            colPrix.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getPrix() != null ? cd.getValue().getPrix().toPlainString() : ""));
        }
        if (colStatut != null) {
            colStatut.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getStatut() != null ? cd.getValue().getStatut().getLabel() : ""));
        }
        if (colAutoRenew != null) {
            colAutoRenew.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().isAutoRenew() ? "Oui" : "Non"));
        }
    }

    @FXML
    public void onActualiser() {
        if (table == null) return;
        Platform.runLater(() -> {
            try {
                List<Abonnement> all = controller.getAll();
                List<Abonnement> mine = all.stream()
                        .filter(a -> a.getUserId() != null && a.getUserId().equals(currentUserId))
                        .collect(Collectors.toList());
                table.getItems().setAll(mine);
                if (statusLabel != null) {
                    statusLabel.setText(mine.isEmpty()
                            ? "Aucun abonnement trouvé pour l'utilisateur " + currentUserId
                            : mine.size() + " abonnement(s) trouvé(s)");
                }
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("Erreur chargement : " + e.getMessage());
                }
            }
        });
    }
}

