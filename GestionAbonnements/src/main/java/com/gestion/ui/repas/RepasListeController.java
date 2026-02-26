package com.gestion.ui.repas;

import com.gestion.controllers.RepasController;
import com.gestion.entities.Menu;
import com.gestion.entities.Repas;
import com.gestion.entities.Restaurant;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des repas (plats)
 */
public class RepasListeController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Restaurant> filterRestaurantCombo;
    @FXML
    private ComboBox<Menu> filterMenuCombo;
    @FXML
    private ComboBox<Repas.Categorie> filterCategorieCombo;
    @FXML
    private ComboBox<Repas.TypePlat> filterTypePlatCombo;
    @FXML
    private ComboBox<String> filterDisponibleCombo;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<Repas> listView;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private ComboBox<String> spaceSelector;
    @FXML
    private ToggleButton iaToggle;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Circle userAvatar;

    // Preview Pane Components
    @FXML
    private VBox previewContainer;
    @FXML
    private ImageView previewImage;
    @FXML
    private Label previewName;
    @FXML
    private Label previewCategory;
    @FXML
    private Label previewPrice;
    @FXML
    private Label previewStatus;
    @FXML
    private Label previewDescription;

    private final RepasController controller = new RepasController();
    private ObservableList<Repas> repas = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private ObservableList<Menu> menus = FXCollections.observableArrayList();
    private FilteredList<Repas> filteredRepas;
    private Repas selectedRepas;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();
        setupFilters();
        loadRestaurants();
        loadMenus();
        loadRepas();
    }

    private void setupListView() {
        listView.setCellFactory(param -> new ListCell<Repas>() {
            @Override
            protected void updateItem(Repas item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createRepasCard(item));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 10 5 10;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedRepas = newSelection;
            boolean hasSelection = newSelection != null;
            btnModifier.setDisable(!hasSelection);
            btnSupprimer.setDisable(!hasSelection);
            updatePreview(newSelection);
        });
    }

    private void setupFilters() {
        if (spaceSelector != null) {
            spaceSelector
                    .setItems(FXCollections.observableArrayList("Administrateur", "Chef de Cuisine", "Logistique"));
            spaceSelector.setValue("Administrateur");
        }

        if (filterCategorieCombo != null) {
            filterCategorieCombo.setItems(FXCollections.observableArrayList(Repas.Categorie.values()));
            filterCategorieCombo.setOnAction(e -> applyFilters());
        }
        if (filterTypePlatCombo != null) {
            filterTypePlatCombo.setItems(FXCollections.observableArrayList(Repas.TypePlat.values()));
            filterTypePlatCombo.setOnAction(e -> applyFilters());
        }
        if (filterDisponibleCombo != null) {
            filterDisponibleCombo.setItems(FXCollections.observableArrayList("Tous", "Disponible", "Indisponible"));
            filterDisponibleCombo.setValue("Tous");
            filterDisponibleCombo.setOnAction(e -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }

        if (filterRestaurantCombo != null)
            filterRestaurantCombo.setOnAction(e -> applyFilters());
        if (filterMenuCombo != null)
            filterMenuCombo.setOnAction(e -> applyFilters());

        if (sortCombo != null) {
            sortCombo.setItems(
                    FXCollections.observableArrayList("Popularité", "Prix croissant", "Prix décroissant", "Nom (A-Z)"));
            sortCombo.setValue("Popularité");
            sortCombo.setOnAction(e -> applySort());
        }

        if (iaToggle != null) {
            iaToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                iaToggle.setText(newVal ? "ON" : "OFF");
                statusLabel.setText("IA Assist " + (newVal ? "Activée" : "Désactivée"));
            });
        }
    }

    private void applySort() {
        if (filteredRepas == null || sortCombo.getValue() == null)
            return;

        String sortType = sortCombo.getValue();
        // Sorting logic can be added here if we want to wrap the filteredList or sort
        // the source
        // For now, let's just update the status
        statusLabel.setText("Trié par " + sortType);
    }

    private void loadRestaurants() {
        restaurants.setAll(controller.getAvailableRestaurants());
        filterRestaurantCombo.setItems(restaurants);
        filterRestaurantCombo.setPromptText("Tous les restaurants");
    }

    private void loadMenus() {
        menus.setAll(controller.getAvailableMenus());
        filterMenuCombo.setItems(menus);
        filterMenuCombo.setPromptText("Tous les menus");
    }

    private javafx.scene.Node createRepasCard(Repas item) {
        HBox card = new HBox(20);
        card.getStyleClass().add("admin-card");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));

        // Image Preview (Miniature with premium clip)
        javafx.scene.layout.StackPane imagePane = new javafx.scene.layout.StackPane();
        imagePane.setPrefSize(80, 80);
        imagePane.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 20;");

        Image img = loadImage(item.getImageUrl());
        if (img != null) {
            javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
            imgView.setFitWidth(80);
            imgView.setFitHeight(80);
            imgView.setPreserveRatio(false);
            imgView.setSmooth(true);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(80, 80);
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            imgView.setClip(clip);
            imagePane.getChildren().add(imgView);
        } else {
            Label iconPlaceholder = new Label("🍽️");
            iconPlaceholder.setStyle("-fx-font-size: 28px;");
            imagePane.getChildren().add(iconPlaceholder);
        }

        // Content
        VBox content = new VBox(8);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(item.getNom().toUpperCase());
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #1E3A8A; -fx-letter-spacing: 0.5px;");

        HBox badges = new HBox(8);
        badges.getChildren()
                .add(createBadge(item.isDisponible() ? "ACTIF" : "OFF", item.isDisponible() ? "#00D4B4" : "#94A3B8"));

        if (item.getNom().toLowerCase().contains("végan") || item.getNom().toLowerCase().contains("salade")) {
            Label trendBadge = createBadge("TENDANCE", "#6366F1");
            trendBadge.setStyle(trendBadge.getStyle() + " -fx-background-color: #E0E7FF; -fx-text-fill: #6366F1;");
            badges.getChildren().add(trendBadge);
        }

        header.getChildren().addAll(title, badges);

        GridPane details = new GridPane();
        details.setHgap(20);
        details.setVgap(5);

        details.add(createDetailLabel("🏨", item.getRestaurantNom()), 0, 0);
        details.add(createDetailLabel("⏱️", item.getTempsPreparation() + " min"), 1, 0);
        details.add(createDetailLabel("📂", item.getCategorie() != null ? item.getCategorie().getLabel() : "S/S"), 0,
                1);

        // Nutrition Bar (Mocked 2026 feature)
        HBox nutritionBar = new HBox(4);
        nutritionBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        for (int i = 0; i < 5; i++) {
            javafx.scene.shape.Rectangle segment = new javafx.scene.shape.Rectangle(12, 4);
            segment.setArcWidth(4);
            segment.setArcHeight(4);
            segment.setFill(i < 3 ? javafx.scene.paint.Color.web("#00D4B4") : javafx.scene.paint.Color.web("#E2E8F0"));
            nutritionBar.getChildren().add(segment);
        }
        details.add(createDetailLabel("🥗", "Nutri-Score"), 1, 1);
        details.add(nutritionBar, 1, 2);

        content.getChildren().addAll(header, details);

        // Price Section
        VBox priceContainer = new VBox(2);
        priceContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%.2f €", item.getPrix() != null ? item.getPrix() : 0.0));
        price.setStyle("-fx-font-weight: 900; -fx-font-size: 20px; -fx-text-fill: #00D4B4;");
        Label reviews = new Label("⭐ 4.8 avis");
        reviews.setStyle("-fx-font-size: 10px; -fx-text-fill: #FBBF24; -fx-font-weight: bold;");
        priceContainer.getChildren().addAll(price, reviews);

        card.getChildren().addAll(imagePane, content, priceContainer);
        return card;
    }

    private Label createBadge(String text, String color) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + color
                + "; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 3 8; -fx-background-radius: 20;");
        return badge;
    }

    private HBox createDetailLabel(String icon, String valueText) {
        HBox box = new HBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");
        Label value = new Label(valueText != null ? valueText : "-");
        value.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: 500;");
        box.getChildren().addAll(iconLbl, value);
        return box;
    }

    private void loadRepas() {
        repas.setAll(controller.getAllRepas());
        filteredRepas = new FilteredList<>(repas, p -> true);
        listView.setItems(filteredRepas);
        updateCountLabel();
    }

    private void applyFilters() {
        if (filteredRepas == null)
            return;

        String searchText = searchField.getText().toLowerCase();
        String dispoFilter = filterDisponibleCombo.getValue();
        Repas.Categorie categorieFilter = filterCategorieCombo.getValue();
        Repas.TypePlat typePlatFilter = filterTypePlatCombo.getValue();
        Restaurant restaurantFilter = filterRestaurantCombo.getValue();
        Menu menuFilter = filterMenuCombo.getValue();

        filteredRepas.setPredicate(repas -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    (repas.getNom() != null && repas.getNom().toLowerCase().contains(searchText)) ||
                    (repas.getDescription() != null && repas.getDescription().toLowerCase().contains(searchText));

            boolean matchesCategorie = categorieFilter == null || categorieFilter.equals(repas.getCategorie());
            boolean matchesTypePlat = typePlatFilter == null || typePlatFilter.equals(repas.getTypePlat());
            boolean matchesRestaurant = restaurantFilter == null ||
                    (repas.getRestaurantId() != null && repas.getRestaurantId().equals(restaurantFilter.getId()));
            boolean matchesMenu = menuFilter == null ||
                    (repas.getMenuId() != null && repas.getMenuId().equals(menuFilter.getId()));

            boolean matchesDispo = true;
            if ("Disponible".equals(dispoFilter))
                matchesDispo = repas.isDisponible();
            else if ("Indisponible".equals(dispoFilter))
                matchesDispo = !repas.isDisponible();

            return matchesSearch && matchesCategorie && matchesTypePlat &&
                    matchesRestaurant && matchesMenu && matchesDispo;
        });

        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = filteredRepas != null ? filteredRepas.size() : repas.size();
        countLabel.setText(count + " plat" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void onNouveauRepas() {
        openForm(null);
    }

    @FXML
    private void onModifier() {
        if (selectedRepas != null) {
            openForm(selectedRepas);
        }
    }

    @FXML
    private void onSupprimer() {
        if (selectedRepas != null) {
            handleDelete(selectedRepas);
        }
    }

    @FXML
    private void onListClick(javafx.scene.input.MouseEvent event) {
        selectedRepas = listView.getSelectionModel().getSelectedItem();
        boolean hasSelection = selectedRepas != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
    }

    @FXML
    public void onActualiser() {
        loadRestaurants();
        loadMenus();
        loadRepas();
        statusLabel.setText("Liste actualisée");
    }

    @FXML
    private void onReinitialiserFiltres() {
        searchField.clear();
        filterCategorieCombo.setValue(null);
        filterTypePlatCombo.setValue(null);
        filterRestaurantCombo.setValue(null);
        filterMenuCombo.setValue(null);
        filterDisponibleCombo.setValue("Tous");
        applyFilters();
    }

    private void updatePreview(Repas item) {
        if (item == null) {
            previewName.setText("Sélectionnez un plat");
            previewCategory.setText("Catalogue");
            previewCategory.getStyleClass().clear();
            previewCategory.getStyleClass().add("badge-lavender");
            previewPrice.setText("0.00 €");
            previewStatus.setText("-");
            previewDescription.setText("L'IA analysera les détails du plat après sélection.");
            previewImage.setImage(null);
            return;
        }

        previewName.setText(item.getNom().toUpperCase());
        previewCategory.setGraphic(null);
        previewCategory.setText(item.getCategorie() != null ? item.getCategorie().getLabel() : "PLAT");
        previewCategory.getStyleClass().clear();
        previewCategory.getStyleClass().add("badge-mint");

        previewPrice.setText(String.format("%.2f €", item.getPrix() != null ? item.getPrix() : 0.0));
        previewStatus.setText(item.isDisponible() ? "ACTIF EN LIGNE" : "HORS LIGNE");
        previewStatus.setStyle(
                "-fx-text-fill: " + (item.isDisponible() ? "#00D4B4" : "#94A3B8") + "; -fx-font-weight: 900;");

        String desc = item.getDescription();
        if (desc == null || desc.isEmpty())
            desc = "Ce plat est une spécialité du restaurant " + item.getRestaurantNom() + ".";
        previewDescription.setText(desc);

        Image img = loadImage(item.getImageUrl());
        if (img != null) {
            previewImage.setImage(img);
            // Apply a nice reflection or shadow if possible via effect
            previewImage.setEffect(new javafx.scene.effect.DropShadow(20, javafx.scene.paint.Color.web("#B2F2EA")));
        } else {
            previewImage.setImage(null);
        }

        statusLabel.setText("Insights IA générés pour " + item.getNom());
    }

    private Image loadImage(String url) {
        if (url == null || url.isEmpty())
            return null;
        try {
            // Check if it's a local relative path
            if (!url.startsWith("http") && !url.startsWith("file:") && !url.startsWith("jar:")) {
                File file = new File(url);
                if (file.exists()) {
                    return new Image(file.toURI().toString(), true);
                }
            }
            return new Image(url, true);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void onExportPDF() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le catalogue");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Catalogue_Plats_LAMMA.pdf");

        java.io.File file = fileChooser.showSaveDialog(listView.getScene().getWindow());
        if (file != null) {
            try {
                generatePDF(file);
                statusLabel.setText("PDF exporté avec succès");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'export PDF", e.getMessage());
            }
        }
    }

    private void generatePDF(java.io.File file) throws Exception {
        // ── Couleurs premium ──────────────────────────────────────────
        java.awt.Color cBgDark = new java.awt.Color(10, 15, 30);
        java.awt.Color cAccent = new java.awt.Color(56, 189, 147);
        java.awt.Color cAccent2 = new java.awt.Color(99, 102, 241);
        java.awt.Color cWhite = new java.awt.Color(255, 255, 255);
        java.awt.Color cLightGray = new java.awt.Color(200, 210, 225);
        java.awt.Color cMidGray = new java.awt.Color(148, 163, 184);
        java.awt.Color cCardBg = new java.awt.Color(22, 30, 55);
        java.awt.Color cCardAlt = new java.awt.Color(28, 38, 65);
        java.awt.Color cSep = new java.awt.Color(40, 55, 85);

        // ── Fonts ─────────────────────────────────────────────────────
        com.lowagie.text.Font fBrand = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fSub = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.NORMAL, cMidGray);
        com.lowagie.text.Font fSection = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Font fLabel = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,
                com.lowagie.text.Font.BOLD, cLightGray);
        com.lowagie.text.Font fValue = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fHeader = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                com.lowagie.text.Font.BOLD, cWhite);
        com.lowagie.text.Font fCell = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                com.lowagie.text.Font.NORMAL, cLightGray);
        com.lowagie.text.Font fFooter = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7,
                com.lowagie.text.Font.ITALIC, cMidGray);

        com.lowagie.text.Document document = new com.lowagie.text.Document(
                com.lowagie.text.PageSize.A4, 40, 40, 40, 40);
        com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document,
                new java.io.FileOutputStream(file));
        document.open();

        com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContent();
        com.lowagie.text.pdf.PdfContentByte under = writer.getDirectContentUnder();
        float pw = com.lowagie.text.PageSize.A4.getWidth();
        float ph = com.lowagie.text.PageSize.A4.getHeight();

        // ── 1. Fond sombre ────────────────────────────────────────────
        under.setColorFill(cBgDark);
        under.rectangle(0, 0, pw, ph);
        under.fill();

        // ── 2. Filigrane ──────────────────────────────────────────────
        under.saveState();
        com.lowagie.text.pdf.PdfGState gs = new com.lowagie.text.pdf.PdfGState();
        gs.setFillOpacity(0.03f);
        under.setGState(gs);
        com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD,
                com.lowagie.text.pdf.BaseFont.CP1252, false);
        under.setColorFill(cWhite);
        under.beginText();
        under.setFontAndSize(bf, 60);
        under.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER,
                "LAMA EXPEDITION", pw / 2, ph / 2, 35);
        under.endText();
        under.restoreState();

        // ── 3. Cadre décoratif ────────────────────────────────────────
        canvas.setColorStroke(cAccent2);
        canvas.setLineWidth(1.5f);
        canvas.roundRectangle(20, 20, pw - 40, ph - 40, 8);
        canvas.stroke();
        canvas.setColorStroke(cAccent);
        canvas.setLineWidth(0.5f);
        canvas.roundRectangle(24, 24, pw - 48, ph - 48, 6);
        canvas.stroke();

        // ── 4. Bandes accent haut ─────────────────────────────────────
        canvas.setColorFill(cAccent);
        canvas.rectangle(20, ph - 26, pw - 40, 6);
        canvas.fill();
        canvas.setColorFill(cAccent2);
        canvas.rectangle(20, ph - 30, (pw - 40) * 0.6f, 4);
        canvas.fill();

        // ── 5. Bandes accent bas ──────────────────────────────────────
        canvas.setColorFill(cAccent2);
        canvas.rectangle(20, 20, pw - 40, 6);
        canvas.fill();
        canvas.setColorFill(cAccent);
        canvas.rectangle(20 + (pw - 40) * 0.4f, 20, (pw - 40) * 0.6f, 4);
        canvas.fill();

        // ── 6. Logo ───────────────────────────────────────────────────
        try {
            java.io.InputStream logoStream = getClass().getResourceAsStream("/images/lamma-logo.png");
            if (logoStream != null) {
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = logoStream.read(tmp)) != -1)
                    buf.write(tmp, 0, n);
                buf.flush();
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(buf.toByteArray());
                logo.scaleToFit(70, 70);
                logo.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
                document.add(logo);
            }
        } catch (Exception ignored) {
        }

        // ── 7. En-tête ────────────────────────────────────────────────
        com.lowagie.text.Paragraph brand = new com.lowagie.text.Paragraph("LAMA EXPEDITION", fBrand);
        brand.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(brand);

        document.add(
                new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("CATALOGUE DES PLATS", fTitle);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(title);

        String dateStr = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph(
                "Document officiel — Généré le " + dateStr, fSub);
        sub.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(sub);

        document.add(com.lowagie.text.Chunk.NEWLINE);

        // ── 8. Statistiques résumé ────────────────────────────────────
        int totalPlats = filteredRepas.size();
        long disponibles = 0;
        double totalPrix = 0;
        for (Repas r : filteredRepas) {
            if (r.isDisponible())
                disponibles++;
            if (r.getPrix() != null)
                totalPrix += r.getPrix().doubleValue();
        }
        double prixMoyen = totalPlats > 0 ? totalPrix / totalPlats : 0;

        com.lowagie.text.Paragraph statTitle = new com.lowagie.text.Paragraph(
                "▪  RÉSUMÉ", fSection);
        document.add(statTitle);
        document.add(
                new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.pdf.PdfPTable statsTable = new com.lowagie.text.pdf.PdfPTable(3);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(4f);

        // Helper: create stat cell
        String[][] stats = {
                { "TOTAL PLATS", String.valueOf(totalPlats) },
                { "DISPONIBLES", String.valueOf(disponibles) },
                { "PRIX MOYEN", String.format("%.2f €", prixMoyen) }
        };
        for (String[] st : stats) {
            com.lowagie.text.pdf.PdfPCell sc = new com.lowagie.text.pdf.PdfPCell();
            sc.setBackgroundColor(cCardBg);
            sc.setBorderColor(cSep);
            sc.setBorderWidth(1f);
            sc.setPadding(10f);
            sc.addElement(new com.lowagie.text.Paragraph(st[0], fLabel));
            sc.addElement(new com.lowagie.text.Paragraph(st[1], fValue));
            statsTable.addCell(sc);
        }
        document.add(statsTable);

        document.add(com.lowagie.text.Chunk.NEWLINE);

        // ── 9. Séparateur ─────────────────────────────────────────────
        com.lowagie.text.pdf.PdfPTable sep = new com.lowagie.text.pdf.PdfPTable(1);
        sep.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell sepCell = new com.lowagie.text.pdf.PdfPCell();
        sepCell.setBackgroundColor(cSep);
        sepCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        sepCell.setFixedHeight(2f);
        sep.addCell(sepCell);
        document.add(sep);
        document.add(
                new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 4)));

        // ── 10. Table des plats ───────────────────────────────────────
        com.lowagie.text.Paragraph tableTitle = new com.lowagie.text.Paragraph(
                "▪  LISTE DES PLATS", fSection);
        document.add(tableTitle);
        document.add(
                new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4f);
        table.setWidths(new float[] { 30, 18, 22, 15, 15 });

        String[] headers = { "Nom du Plat", "Catégorie", "Restaurant", "Prix", "Statut" };
        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell hc = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(h, fHeader));
            hc.setBackgroundColor(cAccent2);
            hc.setBorderColor(cSep);
            hc.setBorderWidth(0.5f);
            hc.setPadding(8);
            hc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(hc);
        }

        int row = 0;
        for (Repas p : filteredRepas) {
            java.awt.Color rowBg = (row % 2 == 0) ? cCardBg : cCardAlt;

            // Name
            com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(p.getNom(), fCell));
            c1.setBackgroundColor(rowBg);
            c1.setBorderColor(cSep);
            c1.setBorderWidth(0.5f);
            c1.setPadding(7);
            table.addCell(c1);

            // Category
            com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(
                            p.getCategorie() != null ? p.getCategorie().getLabel() : "-", fCell));
            c2.setBackgroundColor(rowBg);
            c2.setBorderColor(cSep);
            c2.setBorderWidth(0.5f);
            c2.setPadding(7);
            table.addCell(c2);

            // Restaurant
            com.lowagie.text.pdf.PdfPCell c3 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(p.getRestaurantNom(), fCell));
            c3.setBackgroundColor(rowBg);
            c3.setBorderColor(cSep);
            c3.setBorderWidth(0.5f);
            c3.setPadding(7);
            table.addCell(c3);

            // Price — accent color
            com.lowagie.text.Font priceFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, cAccent);
            com.lowagie.text.pdf.PdfPCell c4 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(
                            String.format("%.2f €", p.getPrix()), priceFont));
            c4.setBackgroundColor(rowBg);
            c4.setBorderColor(cSep);
            c4.setBorderWidth(0.5f);
            c4.setPadding(7);
            c4.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            table.addCell(c4);

            // Status — colored
            java.awt.Color stColor = p.isDisponible()
                    ? new java.awt.Color(34, 197, 94)
                    : new java.awt.Color(239, 68, 68);
            com.lowagie.text.Font stFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, stColor);
            com.lowagie.text.pdf.PdfPCell c5 = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(
                            p.isDisponible() ? "Disponible" : "Indisponible", stFont));
            c5.setBackgroundColor(rowBg);
            c5.setBorderColor(cSep);
            c5.setBorderWidth(0.5f);
            c5.setPadding(7);
            c5.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(c5);

            row++;
        }

        document.add(table);
        document.add(com.lowagie.text.Chunk.NEWLINE);

        // ── 11. Footer ────────────────────────────────────────────────
        com.lowagie.text.pdf.PdfPTable sepFoot = new com.lowagie.text.pdf.PdfPTable(1);
        sepFoot.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell fc = new com.lowagie.text.pdf.PdfPCell();
        fc.setBackgroundColor(cAccent2);
        fc.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        fc.setFixedHeight(1.5f);
        sepFoot.addCell(fc);
        document.add(sepFoot);
        document.add(
                new com.lowagie.text.Paragraph(" ", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 3)));

        com.lowagie.text.Font fBrandFoot = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, cAccent);
        com.lowagie.text.Paragraph fb1 = new com.lowagie.text.Paragraph("LAMA EXPEDITION™", fBrandFoot);
        fb1.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(fb1);

        com.lowagie.text.Paragraph fb2 = new com.lowagie.text.Paragraph(
                "Document confidentiel — Usage interne uniquement — " + dateStr, fFooter);
        fb2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(fb2);

        com.lowagie.text.Paragraph fb3 = new com.lowagie.text.Paragraph(
                totalPlats + " plat(s) répertorié(s) — Généré par LAMA EXPEDITION™", fFooter);
        fb3.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(fb3);

        document.close();
    }

    private void handleDelete(Repas repas) {
        if (repas == null)
            return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer le plat ?");
        confirmAlert.setContentText(
                "Êtes-vous sûr de vouloir supprimer \"" + repas.getNom() + "\" ?\n\nCette action est irréversible.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (controller.deleteRepas(repas.getId())) {
                        this.repas.remove(repas);
                        updateCountLabel();
                        statusLabel.setText("Plat supprimé avec succès");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée",
                                "Impossible de supprimer le plat.");
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression",
                            e.getMessage());
                }
            }
        });
    }

    private void openForm(Repas repas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/repas/repas-form.fxml"));
            Parent root = loader.load();

            RepasFormController formController = loader.getController();
            formController.setRepas(repas);
            formController.setController(controller);
            formController.setRestaurants(restaurants);
            formController.setMenus(menus);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(repas == null ? "Nouveau Plat" : "Modifier Plat");
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.showAndWait();

            loadRepas();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire",
                    e.getMessage());
        }
    }

    public void refreshList() {
        loadRepas();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- SIDEBAR NAVIGATION ---

    @FXML
    private void onGoDashboard() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        }
    }

    @FXML
    private void onGoPlats() {
        onActualiser();
        statusLabel.setText("Navigation : Plats & Recettes");
    }

    @FXML
    private void onGoPlanner() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance()
                    .loadInternalView("/views/repas/admin-menu-planner.fxml", "Planificateur de Menus");
        }
    }

    @FXML
    private void onGoAnalytics() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance()
                    .loadInternalView("/views/repas/admin-kpi-dashboard.fxml", "Analytics & KPIs");
        }
    }

    @FXML
    private void onGoStock() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance()
                    .loadInternalView("/views/admin/inventaire.fxml", "Gestion des Stocks");
            statusLabel.setText("Inventaire des ingrédients chargé");
        }
    }
}
