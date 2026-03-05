package com.gestion.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.gestion.entities.EventSponsor;
import com.gestion.tools.MyConnection;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class EventSponsorViewController implements Initializable {

    @FXML
    private ListView<EventSponsor> listView;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusInfoLabel;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    @FXML
    private ComboBox<String> filterStatut;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private TextField filterUserId;
    @FXML
    private TextField filterEvenementId;
    @FXML
    private TextField searchField;

    private ObservableList<EventSponsor> associationList = FXCollections.observableArrayList();
    private FilteredList<EventSponsor> filteredData;
    private EventSponsor selectedAssociation = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listView.setCellFactory(lv -> new EventSponsorListCell());

        filterStatut.setItems(FXCollections.observableArrayList("Tous", "GOLD", "SILVER", "BRONZE", "PARTENAIRE"));
        filterType.setItems(FXCollections.observableArrayList("Défaut", "Montant Croissant", "Montant Décroissant",
                "Nom Événement", "Nom Sponsor"));

        loadAssociations();

        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selectedAssociation = n;
            btnModifier.setDisable(n == null);
            btnSupprimer.setDisable(n == null);
        });

        if (statusInfoLabel != null) {
            statusInfoLabel.setText("Base de données connectée");
        }
    }

    private void loadAssociations() {
        associationList.clear();
        String query = "SELECT es.*, e.titre as event_nom, s.nom as sponsor_nom " +
                "FROM EventSponsor es " +
                "JOIN Evenement e ON es.event_id = e.id_event " +
                "JOIN Sponsor s ON es.sponsor_id = s.id " +
                "ORDER BY e.date_debut DESC, es.id DESC";
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                EventSponsor es = new EventSponsor(rs.getInt("id"), rs.getString("event_nom"),
                        rs.getString("sponsor_nom"), rs.getString("niveau"), rs.getDouble("montant"));
                es.setEventId(rs.getInt("event_id"));
                es.setSponsorId(rs.getInt("sponsor_id"));
                associationList.add(es);
            }

            filteredData = new FilteredList<>(associationList, p -> true);
            listView.setItems(filteredData);
            updateCountLabel();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement associations: " + e.getMessage());
        }
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText(filteredData.size() + " associations localisées");
        }
    }

    @FXML
    private void onNouvelle() {
        DashboardController.getInstance().loadModule("/Feryel/AjouterEventSponsor.fxml");
    }

    @FXML
    private void onActualiser() {
        loadAssociations();
        onFiltrer();
    }

    @FXML
    private void onModifier() {
        if (selectedAssociation == null) {
            showAlert("Erreur", "Sélectionnez une association à modifier.");
            return;
        }
        DashboardController.getInstance().loadModule("/Feryel/AjouterEventSponsor.fxml", selectedAssociation);
    }

    @FXML
    private void onSupprimer() {
        if (selectedAssociation == null) {
            showAlert("Erreur", "Sélectionnez une association à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'association");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette association ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;

        try (Connection conn = MyConnection.getConnectionStatic();
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM EventSponsor WHERE id = ?")) {
            pstmt.setInt(1, selectedAssociation.getId());
            pstmt.executeUpdate();
            associationList.remove(selectedAssociation);
            selectedAssociation = null;
            listView.getSelectionModel().clearSelection();
            updateCountLabel();
            showAlert("Succès", "Association supprimée avec succès.");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @FXML
    private void onFiltrer() {
        if (filteredData == null)
            return;

        String nivo = filterStatut.getValue();
        String sponIdStr = filterUserId.getText().toLowerCase().trim();
        String eventIdStr = filterEvenementId.getText().toLowerCase().trim();
        String search = searchField.getText().toLowerCase().trim();

        filteredData.setPredicate(es -> {
            // Niveau Filter
            if (nivo != null && !nivo.equals("Tous")) {
                if (!es.getNiveau().equalsIgnoreCase(nivo))
                    return false;
            }

            // Sponsor ID/Name Search
            if (!sponIdStr.isEmpty()) {
                if (!String.valueOf(es.getSponsorId()).contains(sponIdStr) &&
                        !es.getNomSponsor().toLowerCase().contains(sponIdStr))
                    return false;
            }

            // Event ID/Name Search
            if (!eventIdStr.isEmpty()) {
                if (!String.valueOf(es.getEventId()).contains(eventIdStr) &&
                        !es.getNomEvenement().toLowerCase().contains(eventIdStr))
                    return false;
            }

            // Global Search
            if (!search.isEmpty()) {
                if (!es.getNomEvenement().toLowerCase().contains(search) &&
                        !es.getNomSponsor().toLowerCase().contains(search) &&
                        !es.getNiveau().toLowerCase().contains(search))
                    return false;
            }

            return true;
        });

        // Sorting
        String sort = filterType.getValue();
        if (sort != null) {
            switch (sort) {
                case "Montant Croissant":
                    associationList.sort((a, b) -> Double.compare(a.getMontant(), b.getMontant()));
                    break;
                case "Montant Décroissant":
                    associationList.sort((a, b) -> Double.compare(b.getMontant(), a.getMontant()));
                    break;
                case "Nom Événement":
                    associationList.sort((a, b) -> a.getNomEvenement().compareToIgnoreCase(b.getNomEvenement()));
                    break;
                case "Nom Sponsor":
                    associationList.sort((a, b) -> a.getNomSponsor().compareToIgnoreCase(b.getNomSponsor()));
                    break;
            }
        }

        updateCountLabel();
    }

    @FXML
    private void onRechercher() {
        onFiltrer();
    }

    @FXML
    private void onStatistiques() {
        showAlert("Information", "Fonctionnalité de statistiques bientôt disponible.");
    }

    @FXML
    private void onRetour() {
        DashboardController.getInstance().showEvenementSelector();
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2 && selectedAssociation != null) {
            onModifier();
        }
    }

    @FXML
    private void onExporterPdf() {
        if (associationList.isEmpty()) {
            showAlert("Information", "Aucune association à exporter.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la liste en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        chooser.setInitialFileName("associations-event-sponsor.pdf");
        File file = chooser.showSaveDialog(listView.getScene().getWindow());
        if (file == null)
            return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 40;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float tableWidth = pageWidth - 2 * margin;

            float[] colWidths = new float[] {
                    tableWidth * 0.40f, // Événement
                    tableWidth * 0.30f, // Sponsor
                    tableWidth * 0.15f, // Niveau
                    tableWidth * 0.15f // Montant
            };

            float rowHeight = 20f;
            PDPageContentStream content = new PDPageContentStream(document, page);

            // Titre
            float y = pageHeight - 60;
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.newLineAtOffset(margin, y);
            content.showText("Liste des associations événement - sponsor");
            content.endText();

            y -= 30;
            drawPdfHeaderRow(content, y, margin, rowHeight, tableWidth, colWidths);
            y -= rowHeight;

            int index = 0;
            for (EventSponsor es : filteredData) {
                if (y < margin + rowHeight * 2) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - 60;
                    drawPdfHeaderRow(content, y, margin, rowHeight, tableWidth, colWidths);
                    y -= rowHeight;
                }

                float x = margin;
                if (index % 2 == 0) {
                    content.setNonStrokingColor(248, 249, 250);
                    content.addRect(margin, y - rowHeight, tableWidth, rowHeight);
                    content.fill();
                }

                content.setStrokingColor(200, 200, 200);
                content.addRect(margin, y - rowHeight, tableWidth, rowHeight);
                content.stroke();

                content.setNonStrokingColor(33, 37, 41);
                float textY = y - 14;

                String eventName = es.getNomEvenement() != null ? es.getNomEvenement() : "-";
                String sponsorName = es.getNomSponsor() != null ? es.getNomSponsor() : "-";
                String niveau = es.getNiveau() != null ? es.getNiveau() : "-";
                String montant = String.format("%.2f", es.getMontant());

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(eventName);
                content.endText();

                x += colWidths[0];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(sponsorName);
                content.endText();

                x += colWidths[1];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(niveau);
                content.endText();

                x += colWidths[2];
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(x + 4, textY);
                content.showText(montant);
                content.endText();

                y -= rowHeight;
                index++;
            }

            content.close();
            document.save(file);
            showAlert("Succès", "PDF généré avec succès : " + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    private void drawPdfHeaderRow(PDPageContentStream content, float headerY, float margin, float rowHeight,
            float tableWidth, float[] colWidths) throws java.io.IOException {
        float x = margin;
        content.setNonStrokingColor(44, 62, 80);
        content.addRect(margin, headerY - rowHeight, tableWidth, rowHeight);
        content.fill();
        content.setStrokingColor(30, 39, 46);
        content.addRect(margin, headerY - rowHeight, tableWidth, rowHeight);
        content.stroke();
        content.setNonStrokingColor(255, 255, 255);
        float textY = headerY - 14;
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Événement");
        content.endText();
        x += colWidths[0];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Sponsor");
        content.endText();
        x += colWidths[1];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Niveau");
        content.endText();
        x += colWidths[2];
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        content.newLineAtOffset(x + 4, textY);
        content.showText("Montant (DT)");
        content.endText();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class EventSponsorListCell extends ListCell<EventSponsor> {
        @Override
        protected void updateItem(EventSponsor item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            VBox card = new VBox(8);
            card.getStyleClass().add("modern-card");

            HBox header = new HBox(15);
            header.getStyleClass().add("card-header");

            VBox titles = new VBox(2);
            Label lblEvent = new Label(item.getNomEvenement());
            lblEvent.getStyleClass().add("card-title");
            Label lblSponsor = new Label("Sponsor: " + item.getNomSponsor());
            lblSponsor.getStyleClass().add("card-subtitle");
            titles.getChildren().addAll(lblEvent, lblSponsor);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblNiveau = new Label(item.getNiveau());
            lblNiveau.getStyleClass().addAll("status-badge", item.getNiveau());

            header.getChildren().addAll(titles, spacer, lblNiveau);

            GridPane grid = new GridPane();
            grid.getStyleClass().add("card-grid");
            grid.setHgap(20);
            grid.setVgap(5);

            Label l1 = new Label("MONTANT");
            l1.getStyleClass().add("card-label");
            Label v1 = new Label(String.format("%.2f DT", item.getMontant()));
            v1.getStyleClass().add("card-price");

            grid.add(l1, 0, 0);
            grid.add(v1, 0, 1);

            card.getChildren().addAll(header, grid);
            setGraphic(card);
            setText(null);
        }
    }
}
