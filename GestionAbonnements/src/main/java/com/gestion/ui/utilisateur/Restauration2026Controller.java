package com.gestion.ui.utilisateur;

import com.gestion.entities.RepasDetaille;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.services.AutoPromoService;
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
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gestion.services.CartInvoiceGenerator;

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
    private final AutoPromoService autoPromoService = new AutoPromoService();
    private final com.gestion.interfaces.MenuService menuService = new com.gestion.services.MenuServiceImpl();
    private List<RepasDetaille> allDishes;
    private List<com.gestion.entities.Menu> allMenus;
    private RepasDetaille currentFeatured;
    private final Set<Long> favoriteIds = new HashSet<>();
    private boolean favoritesOnly = false;
    private Long restaurantFilterId = null;

    @FXML
    public void initialize() {
        autoPromoService.generateAutoPromos();
        if (restaurantFilterId == null) {
            loadData();
        }
        setupCartListener();
        updateCartBadge();
    }

    public void setRestaurantFilter(Long restaurantId) {
        this.restaurantFilterId = restaurantId;
        loadData();
    }

    private void setupCartListener() {
        cartService.getItems().addListener((MapChangeListener<RepasDetaille, Integer>) change -> {
            updateCartBadge();
        });
    }

    @FXML
    private void onHome(MouseEvent event) {
        System.out.println("🏠 Home clicked");
    }

    @FXML
    private void onProfile(MouseEvent event) {
        System.out.println("👤 Profile clicked");
    }

    private void updateCartBadge() {
        int count = cartService.getItemCount();
        cartCountBadge.setText(String.valueOf(count));
        cartCountBadge.setVisible(count > 0);
    }

    private void loadData() {
        if (restaurantFilterId != null) {
            allDishes = repasService.findAll().stream()
                    .filter(d -> d.getRestaurantId() != null && d.getRestaurantId().equals(restaurantFilterId))
                    .collect(Collectors.toList());
            allMenus = menuService.findActifsByRestaurantId(restaurantFilterId);
        } else {
            allDishes = repasService.findAll();
            allMenus = menuService.findActifs();
        }

        if (!allDishes.isEmpty()) {
            setFeatured(allDishes.get(0));
        }
        applyFilters();
    }

    @FXML
    private void onFavorites(MouseEvent event) {
        System.out.println("Favorites icon clicked");
        // TODO: show favorite dishes only (can reuse your toggleFavorites logic)
    }

    @FXML
    private void handleNavClick(MouseEvent event) {
        var source = (Label) event.getSource();
        String id = source.getId();

        if (id == null) {
            // fallback using text or style class
            String text = source.getText();
            if ("🏠".equals(text)) {
                /* home */ } else if ("❤️".equals(text)) {
                /* favorites */ } else if ("👤".equals(text)) {
                /* profile */ }
            return;
        }

        switch (id) {
            case "navHome":
                System.out.println("Home");
            case "navFav":
                System.out.println("Favorites");
            case "navProfile":
                System.out.println("Profile");
        }
    }

    @FXML
    private void applyFilters() {
        List<RepasDetaille> filtered = allDishes;
        if (favoritesOnly) {
            filtered = allDishes.stream()
                    .filter(d -> favoriteIds.contains(d.getId()))
                    .collect(Collectors.toList());
        }
        renderContent(filtered, allMenus);
    }

    @FXML
    public void toggleFavorites() {
        this.favoritesOnly = !this.favoritesOnly;
        applyFilters();
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

        // Default premium fallback
        return "file:/C:/Users/USER/.gemini/antigravity/brain/c03f7ff3-5f44-4774-b2ee-64b568bd854b/healthy_power_bowl_1772310539584.png";
    }

    private void renderContent(List<RepasDetaille> dishes, List<com.gestion.entities.Menu> menus) {
        dishListContainer.getChildren().clear();

        // Menus are now removed as requested - focus on bowls/dishes
        if (!dishes.isEmpty()) {
            Label dishHeader = new Label("🍱 Explore Our Bowls");
            dishHeader.setStyle(
                    "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 900; -fx-padding: 10 0;");
            dishListContainer.getChildren().add(dishHeader);
        }

        for (RepasDetaille dish : dishes) {
            dishListContainer.getChildren().add(createDishCard(dish));
        }
    }

    private VBox createDishCard(RepasDetaille dish) {
        VBox card = new VBox(15);
        card.getStyleClass().add("neo-card");
        card.setPrefWidth(500); // Slightly wider for 2026 aesthetic

        HBox contentRow = new HBox(20);
        contentRow.setAlignment(Pos.CENTER_LEFT);

        // Image Container with subtle glow
        StackPane imgBox = new StackPane();
        imgBox.getStyleClass().add("featured-image-box");
        imgBox.setStyle("-fx-background-radius: 18; -fx-padding: 3; -fx-min-width: 90; -fx-min-height: 90;");

        ImageView img = new ImageView();
        String imageUrl = dish.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = getSmartFallbackImage(dish);
        }

        try {
            img.setImage(new Image(imageUrl, 180, 180, true, true));
        } catch (Exception e) {
            // Final fallback icon if file not found
        }
        img.setFitWidth(85);
        img.setFitHeight(85);
        img.setPreserveRatio(true);
        img.getStyleClass().add("featured-image-container");
        imgBox.getChildren().add(img);

        VBox info = new VBox(8);
        Label title = new Label(dish.getNom());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: white; -fx-letter-spacing: -0.3;");

        HBox badges = new HBox(8);

        // Auto Promo Badge
        if (autoPromoService.hasAutoPromo(dish.getId())) {
            Label promoBadge = new Label("🏷️ -" + autoPromoService.getDiscountPercent(dish.getId()) + "%");
            promoBadge.getStyleClass().add("health-badge");
            promoBadge.setStyle(
                    "-fx-background-color: rgba(249, 115, 22, 0.15); -fx-text-fill: #F97316; -fx-font-weight: 900;");
            badges.getChildren().add(promoBadge);
        }

        if (dish.getCalories() != null && dish.getCalories() < 500) {
            Label calBadge = new Label("LIGHT PACK 🌬️");
            calBadge.getStyleClass().add("health-badge");
            calBadge.setStyle(
                    "-fx-background-color: rgba(56, 189, 248, 0.1); -fx-text-fill: #38bdf8; -fx-font-size: 9;");
            badges.getChildren().add(calBadge);
        }
        if (dish.isVegetarien()) {
            Label vegBadge = new Label("FORAGER 🌱");
            vegBadge.getStyleClass().add("health-badge");
            vegBadge.setStyle(
                    "-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #22c55e; -fx-font-size: 9;");
            badges.getChildren().add(vegBadge);
        }

        Label desc = new Label(dish.getDescription());
        desc.getStyleClass().add("delicious-baseline");
        desc.setWrapText(true);
        desc.setMaxHeight(40);
        info.getChildren().addAll(title, badges, desc);
        HBox.setHgrow(info, Priority.ALWAYS);

        contentRow.getChildren().addAll(imgBox, info);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("%.2f €", dish.getPrix()));
        price.getStyleClass().add("card-price-2026");
        price.setStyle("-fx-font-size: 20px; -fx-text-fill: #00D4B4;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(15);
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
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                        javafx.util.Duration.millis(200), fav);
                st.setFromX(1.0);
                st.setFromY(1.0);
                st.setToX(1.4);
                st.setToY(1.4);
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
            e.consume();
        });

        actions.getChildren().addAll(fav, quickAdd);
        bottomRow.getChildren().addAll(price, spacer, actions);

        card.getChildren().addAll(contentRow, bottomRow);
        card.setOnMouseClicked(e -> {
            setFeatured(dish);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150),
                    card);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.03);
            st.setToY(1.03);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });

        return card;
    }

    private void setFeatured(RepasDetaille dish) {
        this.currentFeatured = dish;
        featuredTitle.setText(dish.getNom());

        BigDecimal price = dish.getPrix();
        if (autoPromoService.hasAutoPromo(dish.getId())) {
            int discount = autoPromoService.getDiscountPercent(dish.getId());
            BigDecimal multiplier = BigDecimal.valueOf(100 - discount).divide(BigDecimal.valueOf(100));
            price = price.multiply(multiplier);
            featuredTitle.setText(dish.getNom() + " (PROMO!)");
        }

        featuredPrice.setText(String.format("%.2f €", price));
        featuredMarketingText.setText(dish.getDescription());
        // In a real app, we'd have prep time in the DB. For now, randomized placeholder
        // or metadata.
        featuredTime.setText("⏱ " + (15 + (dish.getNom().length() % 20)) + " Mins");

        String imageUrl = dish.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = getSmartFallbackImage(dish);
        }

        try {
            featuredImage.setImage(new Image(imageUrl));
        } catch (Exception e) {
        }
    }

    @FXML
    void onCategoryClick() {
        // Mock filtering
        renderContent(allDishes, allMenus);
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