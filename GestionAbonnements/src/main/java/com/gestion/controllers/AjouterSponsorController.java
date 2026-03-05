package com.gestion.controllers;

import com.gestion.entities.Sponsor;
import com.gestion.interfaces.DataReceiver;
import com.gestion.tools.MyConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.regex.Pattern;

public class AjouterSponsorController implements DataReceiver<Sponsor> {

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblMessage;
    @FXML
    private TextField txtNom;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelephone;
    @FXML
    private TextField txtLogoPath;
    @FXML
    private ImageView imgSponsorLogo;
    @FXML
    private CheckBox chkStatut;
    @FXML
    private Button btnSubmit;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+216\\d{8}$");

    private Sponsor editingSponsor = null;
    private File selectedFile = null;

    @FXML
    public void initialize() {
        // No background image adjustment needed for the new layout
    }

    @FXML
    private void onRetour() {
        DashboardController.getInstance().loadModule("/views/SponsorView.fxml");
    }

    @Override
    public void setData(Sponsor data) {
        this.editingSponsor = data;
        if (data != null) {
            lblTitle.setText("Modifier Sponsor");
            btnSubmit.setText("Modifier");
            txtNom.setText(data.getNom());
            txtEmail.setText(data.getEmail());
            txtTelephone.setText(data.getTelephone());
            txtLogoPath.setText(data.getLogo());
            chkStatut.setSelected(data.isStatut());

            if (data.getLogo() != null && !data.getLogo().isBlank()) {
                try {
                    Path logoPath = Path.of(System.getProperty("user.dir"), "uploads", "sponsors", data.getLogo());
                    if (Files.exists(logoPath)) {
                        imgSponsorLogo.setImage(new Image(logoPath.toUri().toString()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @FXML
    private void onParcourirLogo(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"));
        Window win = txtNom.getScene().getWindow();
        File file = fc.showOpenDialog(win);
        if (file != null) {
            this.selectedFile = file;
            txtLogoPath.setText(file.getName());
            imgSponsorLogo.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        String nom = safe(txtNom.getText());
        String email = safe(txtEmail.getText());
        String tel = safe(txtTelephone.getText()).replaceAll("\\s", "");
        String logo = safe(txtLogoPath.getText());
        boolean statut = chkStatut.isSelected();

        if (nom.isEmpty() || email.isEmpty() || tel.isEmpty()) {
            showError("❌ Veuillez remplir les champs obligatoires.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("❌ Format email invalide.");
            return;
        }

        if (!PHONE_PATTERN.matcher(tel).matches()) {
            showError("❌ Téléphone invalide (+216 suivi de 8 chiffres).");
            return;
        }

        // Handle File Copy if new file selected
        if (selectedFile != null) {
            try {
                Path uploadsDir = Path.of(System.getProperty("user.dir"), "uploads", "sponsors");
                Files.createDirectories(uploadsDir);
                Files.copy(selectedFile.toPath(), uploadsDir.resolve(selectedFile.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
                logo = selectedFile.getName();
            } catch (IOException ex) {
                showError("❌ Erreur lors de la copie de l'image.");
                ex.printStackTrace();
                return;
            }
        }

        try (Connection conn = MyConnection.getConnectionStatic()) {
            if (editingSponsor == null) {
                // INSERT
                String query = "INSERT INTO Sponsor (nom, telephone, email, logo, statut) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, nom);
                    pstmt.setString(2, tel);
                    pstmt.setString(3, email);
                    pstmt.setString(4, logo.isEmpty() ? null : logo);
                    pstmt.setBoolean(5, statut);
                    pstmt.executeUpdate();
                    showSuccess("✅ Sponsor ajouté avec succès !");
                }
            } else {
                // UPDATE
                String query = "UPDATE Sponsor SET nom=?, telephone=?, email=?, logo=?, statut=? WHERE id=?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, nom);
                    pstmt.setString(2, tel);
                    pstmt.setString(3, email);
                    pstmt.setString(4, logo.isEmpty() ? null : logo);
                    pstmt.setBoolean(5, statut);
                    pstmt.setInt(6, editingSponsor.getId());
                    pstmt.executeUpdate();
                    showSuccess("✅ Sponsor modifié avec succès !");
                }
            }
            // Delay return to give user time to see success message
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(this::onRetour);
            }).start();

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                showError("❌ Cet email ou nom existe déjà.");
            } else {
                showError("❌ Erreur base de données: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private void showSuccess(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("error", "success");
        lblMessage.getStyleClass().add("success");
    }

    private void showError(String msg) {
        lblMessage.setText(msg);
        lblMessage.getStyleClass().removeAll("success", "error");
        lblMessage.getStyleClass().add("error");
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
