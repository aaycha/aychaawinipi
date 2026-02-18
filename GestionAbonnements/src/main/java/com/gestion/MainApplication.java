/*package com.gestion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Chemin relatif au classpath (src/main/resources)
            String fxmlPath = "/views/main-view.fxml";  // ← CHANGE ICI selon ton vrai chemin !

            System.out.println("Tentative de chargement du FXML : " + fxmlPath);

            // Vérification du chemin
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("FXML introuvable ! Chemin : " + fxmlPath +
                        "\nVérifiez dans src/main/resources" +
                        "\nAssurez-vous que le fichier est bien là et non dans target/");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 750);

            // Chargement CSS (vérifie aussi ces chemins)
            scene.getStylesheets().addAll(
                    Objects.requireNonNull(getClass().getResource("/styles/theme-voyage.css")).toExternalForm(),
                    Objects.requireNonNull(getClass().getResource("/styles/restauration.css")).toExternalForm()
            );

            primaryStage.setTitle("LAMMA Voyage - Gestion Abonnements et Participations");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.setResizable(true);
            primaryStage.show();

            System.out.println("Application démarrée avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur fatale chargement FXML : " + e.getMessage());
            e.printStackTrace();

            // Popup d'erreur pour voir directement le problème
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de démarrage");
            alert.setHeaderText("Impossible de charger l'interface principale");
            alert.setContentText("Vérifiez le chemin du FXML :\n" + e.getMessage() +
                    "\n\nLe chemin doit être relatif au classpath (ex. /views/abonnement/abonnement.fxml)");
            alert.showAndWait();
        }
    }
}*/
package com.gestion;

import com.gestion.controllers.MainController;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApplication extends Application {


    private static Stage primaryStage;
    private static MainController mainController;

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        try {
            // Chemin du FXML principal (dashboard avec sidebar et conteneur)
            String mainFxmlPath = "/views/main-view.fxml";

            System.out.println("Tentative de chargement du FXML principal : " + mainFxmlPath);

            URL fxmlUrl = getClass().getResource(mainFxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("FXML principal introuvable : " + mainFxmlPath +
                        "\nVérifiez que le fichier existe dans src/main/resources/views/");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            // Récupération du contrôleur principal
            mainController = loader.getController();
            if (mainController == null) {
                throw new IllegalStateException("Impossible de récupérer MainController depuis " + mainFxmlPath);
            }

            Scene scene = new Scene(root, 1200, 800);

            // Chargement des CSS (ajoute les tiens ici)
            String[] cssFiles = {
                    "/styles/theme-voyage.css",
                    "/styles/restauration.css",
                    "/styles/modern-ui.css"   // ← ton CSS global si tu en as un
            };

            for (String css : cssFiles) {
                URL cssUrl = getClass().getResource(css);
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.err.println("CSS non trouvé : " + css);
                }
            }

            primaryStage.setTitle("LAMMA Voyage - Gestion Abonnements, Participations & Restauration");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("LAMMA Voyage démarrée avec succès !");

            // Charge une vue par défaut au démarrage (optionnel)
            // loadView("/views/abonnements/abonnement-list.fxml");  // décommente si besoin

        } catch (Exception e) {
            System.err.println("Erreur fatale au démarrage : " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de démarrage - LAMMA Voyage");
            alert.setHeaderText("Impossible de charger l'interface principale");
            alert.setContentText("Détails : " + e.getMessage() +
                    "\n\nVérifiez :\n" +
                    "• Chemins FXML/CSS dans src/main/resources\n" +
                    "• Fichiers bien inclus dans le build\n" +
                    "• Dépendances JavaFX (module-path + add-modules)");
            alert.showAndWait();

            Platform.exit();
        }
    }

    /**
     * Charge une nouvelle vue dans le conteneur du MainController (moduleStackPane)
     * avec une transition fade simple.
     *
     * @param fxmlPath chemin relatif au classpath, ex: "/views/abonnements/abonnement-list.fxml"
     */
    public static void loadView(String fxmlPath) {
        if (mainController == null) {
            System.err.println("MainController non initialisé ! Impossible de charger : " + fxmlPath);
            return;
        }

        try {
            URL url = MainApplication.class.getResource(fxmlPath);
            if (url == null) {
                throw new IOException("FXML introuvable : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent newView = loader.load();

            // Fade out (optionnel – on peut fade le stack pane entier)
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), mainController.moduleStackPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeOut.setOnFinished(e -> {
                // Remplacer le contenu
                mainController.moduleStackPane.getChildren().setAll(newView);
                fadeIn.play();
            });

            fadeOut.play();

            System.out.println("Vue chargée avec succès : " + fxmlPath);

            // Appel optionnel à onActualiser() si le contrôleur le supporte
            Object ctrl = loader.getController();
            if (ctrl != null) {
                try {
                    Method refresh = ctrl.getClass().getMethod("onActualiser");
                    refresh.invoke(ctrl);
                } catch (NoSuchMethodException ignored) {
                    // normal si pas de méthode
                } catch (Exception ex) {
                    System.err.println("Erreur appel onActualiser : " + ex.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Erreur chargement vue " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible de charger la page");
            alert.setContentText("Chemin : " + fxmlPath + "\n\n" + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Accesseur pour la fenêtre principale (utile pour ouvrir des popups, dialogs, etc.)
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}