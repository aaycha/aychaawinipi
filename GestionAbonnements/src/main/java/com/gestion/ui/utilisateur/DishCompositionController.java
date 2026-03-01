package com.gestion.ui.utilisateur;

import com.gestion.entities.Ingredient;
import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.IngredientService;
import com.gestion.services.IngredientServiceImpl;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DishCompositionController {

    @FXML
    private ImageView smallHeroImage;
    @FXML
    private Label dishNameLabel;
    @FXML
    private Label basePriceLabel;
    @FXML
    private Label liveCalories;
    @FXML
    private Label liveTime;
    @FXML
    private Label liveProteins;
    @FXML
    private Label totalPriceLabel;
    @FXML
    private VBox ingredientsContainer;
    @FXML
    private Button btnAddToCart;

    private final IngredientService ingredientService = new IngredientServiceImpl();
    private RepasDetaille baseDish;
    private RepasDetaille personalizedDish;
    private final DoubleProperty totalPrice = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalCalories = new SimpleIntegerProperty(0);
    private final Map<Ingredient, Integer> selectedIngredients = new HashMap<>();
    private Consumer<RepasDetaille> updateCallback;

    @FXML
    public void initialize() {
        // Default dish if none provided (for direct dashboard access)
        if (baseDish == null) {
            RepasDetaille defaultDish = new RepasDetaille("Composition Libre",
                    "Composez votre propre menu sur mesure",
                    new BigDecimal("15.00"), 350);
            setInitialDish(defaultDish);
        }
    }

    public void setUpdateCallback(Consumer<RepasDetaille> callback) {
        this.updateCallback = callback;
    }

    public void setInitialDish(RepasDetaille dish) {
        this.baseDish = dish;
        this.personalizedDish = new RepasDetaille(dish.getNom(), dish.getDescription(), dish.getPrix(),
                dish.getCalories());
        this.personalizedDish.setImageUrl(dish.getImageUrl());
        this.personalizedDish.setId(dish.getId());

        this.totalPrice.set(dish.getPrix().doubleValue());
        this.totalCalories.set(dish.getCalories() != null ? dish.getCalories() : 0);

        dishNameLabel.setText("Personnalise ton " + dish.getNom());
        basePriceLabel.setText(String.format("À partir de %.2f €", dish.getPrix()));

        String imageUrl = dish.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = getSmartFallbackImage(dish);
        }

        if (imageUrl != null) {
            try {
                smallHeroImage.setImage(new Image(imageUrl, 250, 250, true, true));
            } catch (Exception e) {
            }
        }

        totalPrice.addListener((obs, oldVal, newVal) -> {
            updateLiveLabels();
            animatePriceUpdate();
        });

        updateLiveLabels();
        loadIngredients();
    }

    private String getSmartFallbackImage(RepasDetaille dish) {
        String name = dish.getNom().toLowerCase();
        if (name.contains("petit-déjeuner") || name.contains("breakfast") || name.contains("croissant")) {
            return "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/continental_breakfast_shelf_1772310863097.png";
        }
        if (name.contains("poulet") || name.contains("chicken") || name.contains("meat")) {
            return "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/grilled_chicken_bowl_1772310876256.png";
        }
        if (name.contains("végé") || name.contains("veggie") || name.contains("buddha") || name.contains("salade")
                || dish.isVegetarien()) {
            return "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/vegetarian_buddha_bowl_1772310888979.png";
        }
        return "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/healthy_power_bowl_1772310539584.png";
    }

    private void loadIngredients() {
        ingredientsContainer.getChildren().clear();
        for (Ingredient.Categorie cat : Ingredient.Categorie.values()) {
            List<Ingredient> list = ingredientService.findByCategorie(cat);
            if (!list.isEmpty()) {
                ingredientsContainer.getChildren().add(createCategorySection(cat.name(), list));
            }
        }
    }

    private VBox createCategorySection(String title, List<Ingredient> ingredients) {
        VBox section = new VBox(15);
        Label header = new Label(title);
        header.getStyleClass().add("card-title-2026");
        header.setStyle("-fx-font-size: 14px; -fx-opacity: 0.6;");
        section.getChildren().add(header);

        for (Ingredient ing : ingredients) {
            section.getChildren().add(createIngredientRow(ing));
        }
        return section;
    }

    private HBox createIngredientRow(Ingredient ing) {
        HBox row = new HBox(15);
        row.getStyleClass().add("neo-card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8px 15px; -fx-background-radius: 18;");

        // Ingredient Icon
        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 12; -fx-padding: 2;");
        ImageView icon = new ImageView();
        String iconUrl = ing.getIconUrl();

        if (iconUrl == null || iconUrl.isEmpty()) {
            // Category-based premium icons
            switch (ing.getCategorie()) {
                case BASE:
                case PROTEINE:
                case LEGUME:
                    iconUrl = "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/vibrant_ingredients_grid_1772310575038.png";
                    break;
                case SAUCE:
                case TOPPING:
                case EXTRA:
                    iconUrl = "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/sauces_and_toppings_1772310589051.png";
                    break;
            }
        }

        try {
            if (iconUrl != null) {
                icon.setImage(new Image(iconUrl, 60, 60, true, true));
            }
        } catch (Exception e) {
        }

        icon.setFitWidth(45);
        icon.setFitHeight(45);
        icon.setPreserveRatio(true);
        iconBox.getChildren().add(icon);

        Label name = new Label(ing.getNom());
        name.getStyleClass().add("card-title-2026");
        name.setStyle("-fx-font-size: 14px;");

        Label priceText = new Label(
                ing.getPrixSupplement().doubleValue() > 0 ? String.format("+%.2f €", ing.getPrixSupplement())
                        : "Inclus");
        priceText.getStyleClass().add("delicious-baseline");
        priceText.setStyle("-fx-font-size: 10px; -fx-text-fill: #00D4B4;");

        VBox info = new VBox(2, name, priceText);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        minus.getStyleClass().add("btn-soft");
        minus.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-padding: 0; -fx-font-size: 14px;");

        Label qty = new Label("0");
        qty.getStyleClass().add("card-title-2026");
        qty.setStyle("-fx-font-size: 14px; -fx-min-width: 20; -fx-alignment: center;");

        Button plus = new Button("+");
        plus.getStyleClass().add("btn-primary");
        plus.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-padding: 0; -fx-font-size: 14px;");

        minus.setOnAction(e -> {
            int current = selectedIngredients.getOrDefault(ing, 0);
            if (current > 0) {
                updateSelection(ing, current - 1);
                qty.setText(String.valueOf(current - 1));
            }
        });

        plus.setOnAction(e -> {
            int current = selectedIngredients.getOrDefault(ing, 0);
            updateSelection(ing, current + 1);
            qty.setText(String.valueOf(current + 1));
        });

        controls.getChildren().addAll(minus, qty, plus);
        row.getChildren().addAll(iconBox, info, controls);

        // Add hover effect
        row.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150),
                    row);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });
        row.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150),
                    row);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return row;
    }

    private void updateSelection(Ingredient ing, int count) {
        if (ing == null)
            return;

        int oldCount = selectedIngredients.getOrDefault(ing, 0);
        selectedIngredients.put(ing, count);

        BigDecimal supplement = ing.getPrixSupplement() != null ? ing.getPrixSupplement() : BigDecimal.ZERO;
        double priceDiff = (count - oldCount) * supplement.doubleValue();
        int calDiff = (count - oldCount) * (ing.getCalories() != null ? ing.getCalories() : 0);

        totalPrice.set(totalPrice.get() + priceDiff);
        totalCalories.set(totalCalories.get() + calDiff);

        // Update the specialized object
        personalizedDish.setPrix(BigDecimal.valueOf(totalPrice.get()));
        personalizedDish.setCalories(totalCalories.get());

        String ingredientsList = selectedIngredients.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getKey().getNom() + " (x" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

        if (!ingredientsList.isEmpty()) {
            personalizedDish.setDescription("Plat composé : " + ingredientsList);
        } else {
            personalizedDish.setDescription(baseDish.getDescription());
        }

        if (updateCallback != null) {
            updateCallback.accept(personalizedDish);
        }
    }

    @FXML
    void onClose() {
        if (totalPriceLabel != null && totalPriceLabel.getScene() != null) {
            ((Stage) totalPriceLabel.getScene().getWindow()).close();
        }
    }

    private void updateLiveLabels() {
        if (totalPriceLabel != null) {
            totalPriceLabel.setText(String.format("%.2f €", totalPrice.get()));
        }
        if (liveCalories != null) {
            liveCalories.setText(String.valueOf(totalCalories.get()));
        }
        // Placeholder for proteins/time logic
        if (liveProteins != null) {
            liveProteins.setText((20 + (totalCalories.get() / 50)) + "g");
        }
        if (liveTime != null) {
            liveTime.setText((25 + (selectedIngredients.size() * 2)) + " Mins");
        }
    }

    private void animatePriceUpdate() {
        if (totalPriceLabel == null)
            return;

        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150),
                totalPriceLabel);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.15);
        st.setToY(1.15);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();

        // Subtle blue glow effect
        totalPriceLabel.setStyle("-fx-text-fill: #00D4B4; -fx-font-size: 28px; -fx-font-weight: 900;");
    }

    @FXML
    void onAddToCart() {
        // Add the personalized version to the cart
        com.gestion.services.CartService.getInstance().addItem(personalizedDish);
        System.out.println("Dish customized and added to cart. Total: " + totalPrice.get());
        ((Stage) totalPriceLabel.getScene().getWindow()).close();
    }
}
