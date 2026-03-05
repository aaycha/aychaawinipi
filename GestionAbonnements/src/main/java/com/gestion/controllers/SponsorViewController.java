package com.gestion.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import com.gestion.entities.Sponsor;
import com.gestion.tools.MyConnection;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SponsorViewController implements Initializable {

    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cboTri;
    @FXML
    private ComboBox<String> cboFiltreStatut;
    @FXML
    private ListView<Sponsor> listSponsors;
    @FXML
    private Label lblCount;

    @FXML
    private VBox boxDetail;
    @FXML
    private ImageView imgDetailLogo;
    @FXML
    private Label lblDetailNom;
    @FXML
    private Label lblDetailEmail;
    @FXML
    private Label lblDetailTel;
    @FXML
    private Label lblDetailStatut;
    @FXML
    private Label lblPlaceholder;

    private ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private Sponsor selectedSponsor = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listSponsors.setCellFactory(lv -> new SponsorListCellCompact());

        cboFiltreStatut.setItems(FXCollections.observableArrayList("Tous", "Actifs", "Inactifs"));
        cboFiltreStatut.getSelectionModel().select("Tous");
        cboTri.setItems(FXCollections.observableArrayList("Nom (A-Z)", "Nom (Z-A)", "Email (A-Z)", "ID"));
        cboTri.getSelectionModel().select("Nom (A-Z)");

        txtRecherche.textProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboFiltreStatut.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());
        cboTri.valueProperty().addListener((obs, o, n) -> applyFiltresEtTri());

        loadSponsors();

        listSponsors.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedSponsor = newSelection;
            updateDetailPanel(newSelection);
        });
    }

    private void updateDetailPanel(Sponsor s) {
        if (s == null) {
            if (lblPlaceholder != null)
                lblPlaceholder.setVisible(true);
            if (boxDetail != null)
                boxDetail.setVisible(false);
            return;
        }
        if (lblPlaceholder != null)
            lblPlaceholder.setVisible(false);
        if (boxDetail != null)
            boxDetail.setVisible(true);
        if (lblDetailNom != null)
            lblDetailNom.setText("Nom: " + s.getNom());
        if (lblDetailTel != null)
            lblDetailTel.setText("Tél: " + (s.getTelephone() != null ? s.getTelephone() : "-"));
        if (lblDetailEmail != null)
            lblDetailEmail.setText("Email: " + (s.getEmail() != null ? s.getEmail() : "-"));
        if (lblDetailStatut != null) {
            lblDetailStatut.setText(s.isStatut() ? "✓ Actif" : "Inactif");
            lblDetailStatut.setStyle(s.isStatut() ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
        }
        if (imgDetailLogo != null) {
            if (s.getLogo() != null && !s.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", s.getLogo());
                    if (Files.exists(logoPath)) {
                        imgDetailLogo.setImage(new Image(logoPath.toUri().toString()));
                    } else
                        imgDetailLogo.setImage(null);
                } catch (Exception e) {
                    imgDetailLogo.setImage(null);
                }
            } else
                imgDetailLogo.setImage(null);
        }
    }

    @FXML
    private void onRetour() {
        DashboardController.getInstance().showEvenementSelector();
    }

    @FXML
    private void ouvrirFormulaireAjout() {
        DashboardController.getInstance().loadModule("/Feryel/AjouterSponsor.fxml");
    }

    @FXML
    private void ouvrirFormulaireModification() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Sélectionnez un sponsor à modifier.");
            return;
        }
        DashboardController.getInstance().loadModule("/Feryel/AjouterSponsor.fxml", selectedSponsor);
    }

    /**
     * Après 1 minute, si le sponsor n'est pas associé à au moins 3 événements,
     * son statut passe automatiquement à inactif.
     */

    @FXML
    private void supprimerSponsor() {
        if (selectedSponsor == null) {
            showAlert("Erreur", "Sélectionnez un sponsor à supprimer.");
            return;
        }
        if (hasAssociatedEvents(selectedSponsor.getId())) {
            showAlert("Attention", "Ce sponsor est associé à des événements. Supprimez d'abord ces associations.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le sponsor");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce sponsor ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM Sponsor WHERE id = ?")) {
            pstmt.setInt(1, selectedSponsor.getId());
            pstmt.executeUpdate();
            sponsorList.remove(selectedSponsor);
            selectedSponsor = null;
            updateDetailPanel(null);
            listSponsors.getSelectionModel().clearSelection();
            applyFiltresEtTri();
            showAlert("Succès", "Sponsor supprimé avec succès.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @FXML
    private void actualiser() {
        loadSponsors();
    }

    @FXML
    private void reinitialiserFiltres() {
        if (txtRecherche != null)
            txtRecherche.clear();
        if (cboFiltreStatut != null)
            cboFiltreStatut.getSelectionModel().select("Tous");
        if (cboTri != null)
            cboTri.getSelectionModel().select("Nom (A-Z)");
        applyFiltresEtTri();
    }

    private void loadSponsors() {
        sponsorList.clear();
        String query = "SELECT * FROM Sponsor ORDER BY nom";
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                sponsorList.add(new Sponsor(rs.getInt("id"), rs.getString("nom"), rs.getString("telephone"),
                        rs.getString("email"), rs.getString("logo"), rs.getBoolean("statut")));
            }
            applyFiltresEtTri();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement: " + e.getMessage());
        }
    }

    private void applyFiltresEtTri() {
        String search = txtRecherche != null ? txtRecherche.getText().trim().toLowerCase() : "";
        String filtreStatut = (cboFiltreStatut != null && cboFiltreStatut.getValue() != null)
                ? cboFiltreStatut.getValue()
                : "Tous";
        String tri = (cboTri != null && cboTri.getValue() != null) ? cboTri.getValue() : "Nom (A-Z)";

        Stream<Sponsor> stream = sponsorList.stream()
                .filter(s -> search.isEmpty() || Stream
                        .of(s.getNom(), s.getEmail() != null ? s.getEmail() : "",
                                s.getTelephone() != null ? s.getTelephone() : "")
                        .anyMatch(v -> v.toLowerCase().contains(search)))
                .filter(s -> "Tous".equals(filtreStatut) || ("Actifs".equals(filtreStatut) && s.isStatut())
                        || ("Inactifs".equals(filtreStatut) && !s.isStatut()));

        Comparator<Sponsor> comparator;
        switch (tri) {
            case "Nom (Z-A)":
                comparator = Comparator.comparing(Sponsor::getNom, Comparator.reverseOrder());
                break;
            case "Email (A-Z)":
                comparator = Comparator.comparing(s2 -> s2.getEmail() != null ? s2.getEmail() : "");
                break;
            case "ID":
                comparator = Comparator.comparingInt(Sponsor::getId);
                break;
            default:
                comparator = Comparator.comparing(Sponsor::getNom, String.CASE_INSENSITIVE_ORDER);
                break;
        }
        List<Sponsor> filtered = stream.sorted(comparator).collect(Collectors.toList());
        listSponsors.setItems(FXCollections.observableArrayList(filtered));
        if (lblCount != null)
            lblCount.setText(filtered.size() + " sponsors");
    }

    private boolean hasAssociatedEvents(int sponsorId) {
        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn
                        .prepareStatement("SELECT COUNT(*) FROM EventSponsor WHERE sponsor_id = ?")) {
            pstmt.setInt(1, sponsorId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class SponsorListCellCompact extends ListCell<Sponsor> {
        @Override
        protected void updateItem(Sponsor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            ImageView img = new ImageView();
            img.setFitHeight(32);
            img.setFitWidth(32);
            img.setPreserveRatio(true);
            if (item.getLogo() != null && !item.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", item.getLogo());
                    if (Files.exists(logoPath))
                        img.setImage(new Image(logoPath.toUri().toString()));
                } catch (Exception ignored) {
                }
            }
            Label name = new Label(item.getNom());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498db;");
            Label statut = new Label(item.isStatut() ? "ACTIF" : "Inactif");
            statut.setStyle(item.isStatut() ? "-fx-text-fill: #27ae60; -fx-font-size: 11px;"
                    : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            row.getChildren().addAll(img, name, statut);
            setGraphic(row);
        }
    }
}
