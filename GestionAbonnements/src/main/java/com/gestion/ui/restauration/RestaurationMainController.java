package com.gestion.ui.restauration;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

public class RestaurationMainController {

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("Application Restauration chargée");
        }
    }

    public void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
        }
    }
}
