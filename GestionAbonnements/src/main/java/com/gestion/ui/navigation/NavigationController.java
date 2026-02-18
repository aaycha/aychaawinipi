package com.gestion.ui.navigation;

import com.gestion.controllers.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Contrôleur centralisé pour la navigation entre pages
 * Gère l'historique et les transitions fluides
 */
public class NavigationController {
    
    private static NavigationController instance;
    private StackPane mainContentPane;
    private Stack<String> history = new Stack<>();
    private Map<String, Parent> cache = new HashMap<>();
    
    private NavigationController() {}
    
    public static NavigationController getInstance() {
        if (instance == null) {
            instance = new NavigationController();
        }
        return instance;
    }
    
    public void setMainContentPane(StackPane pane) {
        this.mainContentPane = pane;
    }
    
    /**
     * Navigue vers une nouvelle page
     */
    public void navigateTo(String fxmlPath, String pageName) {
        try {
            Parent root = loadFXML(fxmlPath);
            if (mainContentPane != null) {
                mainContentPane.getChildren().clear();
                mainContentPane.getChildren().add(root);
                history.push(pageName);
            }
        } catch (IOException e) {
            System.err.println("Erreur navigation vers " + fxmlPath + ": " + e.getMessage());
        }
    }
    
    /**
     * Retour en arrière
     */
    public void goBack() {
        if (!history.isEmpty()) {
            history.pop();
            if (!history.isEmpty()) {
                String previousPage = history.peek();
                // Recharger la page précédente si nécessaire
            }
        }
    }
    
    /**
     * Charge un FXML avec cache
     */
    private Parent loadFXML(String fxmlPath) throws IOException {
        if (cache.containsKey(fxmlPath)) {
            return cache.get(fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        cache.put(fxmlPath, root);
        return root;
    }
    
    /**
     * Vide le cache (utile après modifications)
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Ouvre une nouvelle fenêtre pour les formulaires/détails
     */
    public void openWindow(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture fenêtre " + fxmlPath + ": " + e.getMessage());
        }
    }
}
