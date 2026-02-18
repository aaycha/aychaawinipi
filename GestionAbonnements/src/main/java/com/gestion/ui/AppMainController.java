package com.gestion.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

public class AppMainController {

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("Application LAMMA chargee");
        }
    }
}
