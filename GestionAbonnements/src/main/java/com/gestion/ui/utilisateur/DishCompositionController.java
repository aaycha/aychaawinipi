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

    private final IngredientService ingredientService = new IngredientServiceImpl();
    private RepasDetaille baseDish;
    private RepasDetaille personalizedDish;
    private final DoubleProperty totalPrice = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalCalories = new SimpleIntegerProperty(0);
    private final Map<Ingredient, Integer> selectedIngredients = new HashMap<>();
    private Consumer<RepasDetaille> updateCallback;

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
        if (dish.getImageUrl() != null && !dish.getImageUrl().isEmpty()) {
            try {
                smallHeroImage.setImage(new Image(dish.getImageUrl()));
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
        row.setStyle("-fx-padding: 10px;");

        Label name = new Label(ing.getNom());
        name.getStyleClass().add("card-title-2026");
        name.setStyle("-fx-font-size: 14px;");

        Label price = new Label(
                ing.getPrixSupplement().doubleValue() > 0 ? String.format("+%.2f €", ing.getPrixSupplement())
                        : "Gratuit");
        price.getStyleClass().add("delicious-baseline");
        price.setStyle("-fx-font-size: 11px;");

        VBox info = new VBox(2, name, price);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);

        Button minus = new Button("-");
        minus.getStyleClass().add("btn-ingredient-control");

        Label qty = new Label("0");
        qty.getStyleClass().add("card-title-2026");
        qty.setStyle("-fx-font-size: 14px;");

        Button plus = new Button("+");
        plus.getStyleClass().add("btn-ingredient-control");

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
        row.getChildren().addAll(info, controls);
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
        totalPriceLabel.setStyle("-fx-text-fill: #00D4B4; -fx-font-size: 28px; -fx-font-weight: 950;");
    }

    @FXML
    void onAddToCart() {
        // Add the personalized version to the cart
        com.gestion.services.CartService.getInstance().addItem(personalizedDish);
        System.out.println("Dish customized and added to cart. Total: " + totalPrice.get());
        ((Stage) totalPriceLabel.getScene().getWindow()).close();
    }
}
