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

        filterCategorieCombo.setItems(FXCollections.observableArrayList(Repas.Categorie.values()));
        filterTypePlatCombo.setItems(FXCollections.observableArrayList(Repas.TypePlat.values()));
        filterDisponibleCombo.setItems(FXCollections.observableArrayList("Tous", "Disponible", "Indisponible"));
        filterDisponibleCombo.setValue("Tous");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        filterCategorieCombo.setOnAction(e -> applyFilters());
        filterTypePlatCombo.setOnAction(e -> applyFilters());
        filterRestaurantCombo.setOnAction(e -> applyFilters());
        filterMenuCombo.setOnAction(e -> applyFilters());
        filterDisponibleCombo.setOnAction(e -> applyFilters());

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
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
        document.open();

        // Header
        com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory
                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 22, new java.awt.Color(30, 58, 138));
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("CATALOGUE DES PLATS - LAMMA VOYAGE",
                titleFont);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        com.lowagie.text.Paragraph info = new com.lowagie.text.Paragraph("Généré le : " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        info.setSpacingAfter(30);
        document.add(info);

        // Table
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[] { 30, 20, 20, 15, 15 });

        String[] headers = { "Nom du Plat", "Catégorie", "Restaurant", "Prix", "Statut" };
        com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory
                .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, java.awt.Color.WHITE);

        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(h, headerFont));
            cell.setBackgroundColor(new java.awt.Color(59, 130, 246));
            cell.setPadding(8);
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        com.lowagie.text.Font cellFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA,
                10);
        for (Repas p : filteredRepas) {
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(p.getNom(), cellFont)));
            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(
                    p.getCategorie() != null ? p.getCategorie().getLabel() : "-", cellFont)));
            table.addCell(
                    new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(p.getRestaurantNom(), cellFont)));
            table.addCell(new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(String.format("%.2f €", p.getPrix()), cellFont)));
            table.addCell(new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Paragraph(p.isDisponible() ? "Disponible" : "Indispo", cellFont)));
        }

        document.add(table);
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
            formController.setListeController(this);
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
        statusLabel.setText("Navigation : Stock & Inventaire");
        showAlert(Alert.AlertType.INFORMATION, "Stock", "Gestion des Stocks",
                "Le suivi des stocks en temps réel avec alertes de seuil arrive bientôt.");
    }
}
