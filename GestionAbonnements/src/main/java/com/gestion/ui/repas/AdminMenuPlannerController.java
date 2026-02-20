package com.gestion.ui.repas;

import com.gestion.entities.CompositionMenu;
import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.CompositionMenuService;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.services.CompositionMenuServiceImpl;
import com.gestion.services.RepasDetailleServiceImpl;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur complet pour le planificateur de menus admin (2026 Edition)
 */
public class AdminMenuPlannerController implements Initializable {

    @FXML
    private GridPane plannerGrid;
    @FXML
    private Label currentWeekLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<RepasDetaille> availableDishesList;

    private final RepasDetailleService dishService = new RepasDetailleServiceImpl();
    private final CompositionMenuService compositionService = new CompositionMenuServiceImpl();
    private final com.gestion.interfaces.RestaurationService restaurationService = new com.gestion.services.RestaurationServiceImpl();

    private LocalDate weekStart;
    private Long validMenuId;
    private final ObservableList<RepasDetaille> allDishes = FXCollections.observableArrayList();
    private FilteredList<RepasDetaille> filteredDishes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        findOrCreateValidMenuId();
        setupCatalog();
        setupSearch();
        refreshGrid();
    }

    private void findOrCreateValidMenuId() {
        try {
            List<com.gestion.entities.Restauration> menus = restaurationService
                    .findAll(com.gestion.entities.Restauration.TypeRestauration.MENU);
            if (menus.isEmpty()) {
                com.gestion.entities.Restauration defaultMenu = new com.gestion.entities.Restauration(
                        com.gestion.entities.Restauration.TypeRestauration.MENU);
                defaultMenu.setNom("Menu Admin Par Défaut");
                defaultMenu.setActif(true);
                restaurationService.create(defaultMenu);
                this.validMenuId = defaultMenu.getId();
            } else {
                this.validMenuId = menus.get(0).getId();
            }
        } catch (Exception e) {
            // Fallback if restauration service fails
            this.validMenuId = 1L;
        }
    }

    private void setupCatalog() {
        allDishes.setAll(dishService.findAll());
        filteredDishes = new FilteredList<>(allDishes, p -> true);
        availableDishesList.setItems(filteredDishes);

        // Custom Cell Factory for Catalog
        availableDishesList.setCellFactory(lv -> new ListCell<>() {
            private final ImageView iv = new ImageView();

            @Override
            protected void updateItem(RepasDetaille item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getNom() + " (" + item.getPrix() + " €)");
                    if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                        iv.setImage(new Image(item.getImageUrl(), 30, 30, true, true));
                        setGraphic(iv);
                    }
                    getStyleClass().add("catalog-cell");
                }
            }
        });

        // DRAG SOURCE: ListView
        availableDishesList.setOnDragDetected(event -> {
            RepasDetaille selected = availableDishesList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = availableDishesList.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getId().toString()); // Transfer ID
                db.setContent(content);

                // Visual drag view
                if (selected.getImageUrl() != null) {
                    db.setDragView(new Image(selected.getImageUrl(), 60, 60, true, true));
                }
                event.consume();
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredDishes.setPredicate(dish -> {
                if (newVal == null || newVal.isEmpty())
                    return true;
                String lower = newVal.toLowerCase();
                return dish.getNom().toLowerCase().contains(lower);
            });
        });
    }

    private void refreshGrid() {
        plannerGrid.getChildren().clear();
        updateWeekLabel();

        // 1. Headers (Day Names + Dates)
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMM");

        for (int col = 0; col < 7; col++) {
            LocalDate dayDate = weekStart.plusDays(col);
            VBox header = new VBox(2);
            header.setAlignment(Pos.CENTER);
            header.getStyleClass().add("glass-panel");
            header.setStyle("-fx-padding: 10; -fx-background-color: " +
                    (col >= 5 ? "#FEE2E2" : "#E0E7FF") + "; -fx-background-radius: 12;");

            Label l1 = new Label(dayFmt.format(dayDate).toUpperCase());
            l1.setStyle("-fx-font-weight: 900; -fx-font-size: 11px;");
            Label l2 = new Label(dateFmt.format(dayDate));
            l2.setStyle("-fx-font-size: 10px; -fx-opacity: 0.7;");

            header.getChildren().addAll(l1, l2);
            plannerGrid.add(header, col, 0);
        }

        // 2. Fetch compositions for the week
        // Note: Realistically we'd query by date range. Using findByDate for demo.

        // 3. Slots
        for (int row = 1; row <= 3; row++) {
            final String type = (row == 1) ? "PETIT_DEJEUNER" : (row == 2) ? "DEJEUNER" : "DINER";
            for (int col = 0; col < 7; col++) {
                LocalDate slotDate = weekStart.plusDays(col);
                VBox slot = createSlot(slotDate, type);
                plannerGrid.add(slot, col, row);

                // Load existing data for this slot
                loadCompositionsForSlot(slot, slotDate, type);
            }
        }
    }

    private VBox createSlot(LocalDate date, String type) {
        VBox slot = new VBox(8);
        slot.getStyleClass().add("glass-panel-light");
        slot.setStyle("-fx-min-height: 120; -fx-min-width: 140; -fx-padding: 10; " +
                "-fx-background-color: rgba(255,255,255,0.4); -fx-background-radius: 15; " +
                "-fx-border-color: rgba(0,0,0,0.05); -fx-border-radius: 15;");

        Label typeLabel = new Label(type.replace("_", " "));
        typeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #94A3B8; -fx-font-weight: 800;");
        slot.getChildren().add(typeLabel);

        // DROP TARGET: Slot
        slot.setOnDragOver(event -> {
            if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
                slot.setStyle(
                        slot.getStyle() + "-fx-background-color: rgba(0,212,180,0.1); -fx-border-color: #00D4B4;");
            }
            event.consume();
        });

        slot.setOnDragExited(event -> {
            slot.setStyle(slot.getStyle()
                    .replace("-fx-background-color: rgba(0,212,180,0.1); -fx-border-color: #00D4B4;", ""));
            event.consume();
        });

        slot.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Long dishId = Long.parseLong(db.getString());
                assignDishToSlot(dishId, date, type);
                loadCompositionsForSlot(slot, date, type);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return slot;
    }

    private void loadCompositionsForSlot(VBox slot, LocalDate date, String type) {
        // Clear previous dishes (keep the label)
        if (slot.getChildren().size() > 1) {
            slot.getChildren().remove(1, slot.getChildren().size());
        }

        List<CompositionMenu> comps = compositionService.findByDate(date).stream()
                .filter(c -> type.equals(c.getTypeRepas()))
                .toList();

        for (CompositionMenu comp : comps) {
            RepasDetaille dish = dishService.findById(comp.getRepasId()).orElse(null);
            if (dish != null) {
                slot.getChildren().add(createDishChip(comp, dish, slot, date, type));
            }
        }
    }

    private HBox createDishChip(CompositionMenu comp, RepasDetaille dish, VBox parentSlot, LocalDate date,
            String type) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("dish-chip");
        chip.setStyle("-fx-background-color: white; -fx-padding: 5 8; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label name = new Label(dish.getNom());
        name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        Button del = new Button("×");
        del.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-padding: 0 2; -fx-font-weight: bold;");
        del.setOnAction(e -> {
            compositionService.delete(comp.getId());
            loadCompositionsForSlot(parentSlot, date, type);
        });

        chip.getChildren().addAll(name, new Region() {
            {
                HBox.setHgrow(this, Priority.ALWAYS);
            }
        }, del);
        return chip;
    }

    private void assignDishToSlot(Long dishId, LocalDate date, String type) {
        CompositionMenu comp = new CompositionMenu();
        comp.setRepasId(dishId);
        comp.setDate(date);
        comp.setTypeRepas(type);
        comp.setMenuId(validMenuId != null ? validMenuId : 1L);
        comp.setOrdre(0);
        compositionService.create(comp);
    }

    private void updateWeekLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM yyyy");
        currentWeekLabel.setText("Semaine du " + fmt.format(weekStart) + " au " + fmt.format(weekStart.plusDays(6)));
    }

    @FXML
    private void onPreviousWeek() {
        weekStart = weekStart.minusWeeks(1);
        refreshGrid();
    }

    @FXML
    private void onNextWeek() {
        weekStart = weekStart.plusWeeks(1);
        refreshGrid();
    }

    @FXML
    private void onBack() {
        if (com.gestion.controllers.MainController.getInstance() != null) {
            com.gestion.controllers.MainController.getInstance().retourDashboard();
        }
    }
}
