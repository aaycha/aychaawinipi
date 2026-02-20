package com.gestion.ui.utilisateur;

import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.services.CartService;
import com.gestion.services.RepasDetailleServiceImpl;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Restauration2026Controller {

    @FXML
    private VBox dishListContainer;
    @FXML
    private ImageView featuredImage;
    @FXML
    private Label featuredTitle;
    @FXML
    private Label featuredPrice;
    @FXML
    private Label featuredTime;
    @FXML
    private Label featuredMarketingText;
    @FXML
    private Label cartCountBadge;

    private final RepasDetailleService repasService = new RepasDetailleServiceImpl();
    private final CartService cartService = CartService.getInstance();
    private List<RepasDetaille> allDishes;
    private RepasDetaille currentFeatured;
    private final Set<Long> favoriteIds = new HashSet<>();
    private boolean favoritesOnly = false;

    @FXML
    public void initialize() {
        loadData();
        setupCartListener();
        updateCartBadge();
    }

    private void setupCartListener() {
        cartService.getItems().addListener((MapChangeListener<RepasDetaille, Integer>) change -> {
            updateCartBadge();
        });
    }

    private void updateCartBadge() {
        int count = cartService.getItemCount();
        cartCountBadge.setText(String.valueOf(count));
        cartCountBadge.setVisible(count > 0);
    }

    private void loadData() {
        allDishes = repasService.findAll();
        if (!allDishes.isEmpty()) {
            setFeatured(allDishes.get(0));
            applyFilters();
        }
    }

    private void applyFilters() {
        List<RepasDetaille> filtered = allDishes;
        if (favoritesOnly) {
            filtered = allDishes.stream()
                    .filter(d -> favoriteIds.contains(d.getId()))
                    .collect(Collectors.toList());
        }
        renderDishList(filtered);
    }

    @FXML
    public void toggleFavorites() {
        this.favoritesOnly = !this.favoritesOnly;
        applyFilters();
    }

    private void renderDishList(List<RepasDetaille> dishes) {
        dishListContainer.getChildren().clear();
        for (RepasDetaille dish : dishes) {
            dishListContainer.getChildren().add(createDishCard(dish));
        }
    }

    private VBox createDishCard(RepasDetaille dish) {
        VBox card = new VBox(10);
        card.getStyleClass().add("neo-card");
        card.setPrefWidth(450);

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ImageView img = new ImageView();
        if (dish.getImageUrl() != null && !dish.getImageUrl().isEmpty()) {
            try {
                img.setImage(new Image(dish.getImageUrl()));
            } catch (Exception e) {
                // Fallback icon or default image
            }
        }
        img.setFitWidth(80);
        img.setFitHeight(80);
        img.setPreserveRatio(true);

        VBox info = new VBox(5);
        Label title = new Label(dish.getNom());
        title.getStyleClass().add("card-title-2026");

        HBox badges = new HBox(8);
        if (dish.getCalories() != null && dish.getCalories() < 500) {
            Label calBadge = new Label("Low Cal 🔥");
            calBadge.getStyleClass().add("health-badge");
            badges.getChildren().add(calBadge);
        }
        if (dish.getNom().toLowerCase().contains("poulet") || dish.getNom().toLowerCase().contains("crab")) {
            Label proBadge = new Label("High Protein 💪");
            proBadge.getStyleClass().add("health-badge");
            badges.getChildren().add(proBadge);
        }

        Label desc = new Label(dish.getDescription());
        desc.getStyleClass().add("delicious-baseline");
        desc.setWrapText(true);
        info.getChildren().addAll(title, badges, desc);
        HBox.setHgrow(info, Priority.ALWAYS);

        topRow.getChildren().addAll(img, info);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("%.2f €", dish.getPrix()));
        price.getStyleClass().add("card-price-2026");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Label fav = new Label(favoriteIds.contains(dish.getId()) ? "❤️" : "🤍");
        fav.getStyleClass().add("fav-heart");
        fav.setOnMouseClicked(e -> {
            boolean isFav = favoriteIds.contains(dish.getId());
            if (isFav) {
                favoriteIds.remove(dish.getId());
                fav.setText("🤍");
            } else {
                favoriteIds.add(dish.getId());
                fav.setText("❤️");
                // Scale animation
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                        javafx.util.Duration.millis(200), fav);
                st.setFromX(1.0);
                st.setFromY(1.0);
                st.setToX(1.3);
                st.setToY(1.3);
                st.setCycleCount(2);
                st.setAutoReverse(true);
                st.play();
            }
            if (favoritesOnly)
                applyFilters();
            e.consume();
        });

        Button quickAdd = new Button("+");
        quickAdd.getStyleClass().add("quick-add-btn");
        quickAdd.setOnAction(e -> {
            cartService.addItem(dish);
            System.out.println("Quick added: " + dish.getNom());
            e.consume();
        });

        actions.getChildren().addAll(fav, quickAdd);
        bottomRow.getChildren().addAll(price, spacer, actions);

        card.getChildren().addAll(topRow, bottomRow);
        card.setOnMouseClicked(e -> {
            setFeatured(dish);
            // Click scale effect
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(100),
                    card);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.02);
            st.setToY(1.02);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });

        return card;
    }

    private void setFeatured(RepasDetaille dish) {
        this.currentFeatured = dish;
        featuredTitle.setText(dish.getNom());
        featuredPrice.setText(String.format("%.2f €", dish.getPrix()));
        featuredMarketingText.setText(dish.getDescription());
        // In a real app, we'd have prep time in the DB. For now, randomized placeholder
        // or metadata.
        featuredTime.setText("⏱ " + (15 + (dish.getNom().length() % 20)) + " Mins");

        if (dish.getImageUrl() != null && !dish.getImageUrl().isEmpty()) {
            try {
                featuredImage.setImage(new Image(dish.getImageUrl()));
            } catch (Exception e) {
            }
        }
    }

    @FXML
    void onCategoryClick() {
        // Mock filtering
        renderDishList(allDishes);
    }

    @FXML
    void onPersonalize() {
        if (currentFeatured == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/utilisateur/dish-composition-modal.fxml"));
            Parent root = loader.load();

            DishCompositionController controller = loader.getController();
            controller.setInitialDish(currentFeatured);

            // Real-time synchronization callback
            controller.setUpdateCallback(personalized -> {
                featuredPrice.setText(String.format("%.2f €", personalized.getPrix()));
                featuredMarketingText.setText(personalized.getDescription());
                // Also update the time if it's dynamic
                featuredTime.setText("⏱ " + (25 + (personalized.getDescription().length() % 20)) + " Mins");
            });

            showModal(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onAddToCart() {
        if (currentFeatured != null) {
            cartService.addItem(currentFeatured);
            System.out.println("Added to cart: " + currentFeatured.getNom());
        }
    }

    @FXML
    void onOpenCheckout() {
        if (cartService.getItemCount() == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Ton panier est vide !");
            alert.show();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/utilisateur/checkout-modal.fxml"));
            Parent root = loader.load();
            showModal(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showModal(Parent root) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);
        stage.show();
    }
}
