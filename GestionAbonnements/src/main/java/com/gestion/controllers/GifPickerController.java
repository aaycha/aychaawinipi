package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la sélection de GIFs
 * (Fonctionnalité future)
 */
public class GifPickerController implements Initializable {

    @FXML private TextField searchField;
    @FXML private FlowPane gifsContainer;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation par défaut
        statusLabel.setText("Fonctionnalité GIF en cours de développement...");
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            statusLabel.setText("Veuillez entrer un terme de recherche.");
            return;
        }

        statusLabel.setText("Recherche de GIFs pour: '" + query + "'...");
        // TODO: Implémenter API Tenor/Giphy
    }
}

