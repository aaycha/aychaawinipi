package com.gestion.ui.menu;

import com.gestion.controllers.MenuController;
import com.gestion.entities.Menu;
import com.gestion.entities.Restaurant;
import com.gestion.entities.ValidationResult;
import com.gestion.services.GeminiMenuAnalyzer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.ObservableList;

/**
 * Contrôleur pour le formulaire de menu
 * — avec génération automatique des détails via Google Gemini Vision API.
 */
public class MenuFormController implements Initializable {

    // ── Champs du formulaire ──────────────────────────────────────────────────
    @FXML
    private Label formTitle;
    @FXML
    private TextField inputNom;
    @FXML
    private ComboBox<Restaurant> comboRestaurant;
    @FXML
    private TextField inputPrix;
    @FXML
    private TextArea inputDescription;
    @FXML
    private DatePicker inputDateDebut;
    @FXML
    private DatePicker inputDateFin;
    @FXML
    private CheckBox checkActif;
    @FXML
    private Label errorNom;
    @FXML
    private Label errorRestaurant;
    @FXML
    private Label errorPrix;
    @FXML
    private Label errorDates;
    @FXML
    private Label errorDescription;
    @FXML
    private Label hintNom;
    @FXML
    private Label hintPrix;
    @FXML
    private VBox errorContainer;
    @FXML
    private Label globalErrorMessage;

    // ── Section IA ────────────────────────────────────────────────────────────
    @FXML
    private ImageView imagePreview;
    @FXML
    private Label imagePlaceholder;
    @FXML
    private Button btnAnalyser;
    @FXML
    private ProgressIndicator aiProgress;
    @FXML
    private Label aiStatusLabel;

    private static final int MAX_NOM_MENU = 100;
    private final MenuController controller = new MenuController();
    private final GeminiMenuAnalyzer geminiAnalyzer = new GeminiMenuAnalyzer();

    private MenuListeController listeController;
    private ObservableList<Restaurant> restaurants;
    private Menu menu;
    private boolean isEditMode = false;
    private File selectedImageFile = null;

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        checkActif.setSelected(true);
        setupValidationListeners();
        updateHintNom();
        updateHintPrix();
    }

    private void setupValidationListeners() {
        inputNom.textProperty().addListener((obs, o, n) -> {
            clearError(errorNom);
            updateHintNom();
        });
        inputPrix.textProperty().addListener((obs, o, n) -> {
            clearError(errorPrix);
            updateHintPrix();
        });
        comboRestaurant.valueProperty().addListener((obs, o, n) -> clearError(errorRestaurant));
        inputDateDebut.valueProperty().addListener((obs, o, n) -> clearError(errorDates));
        inputDateFin.valueProperty().addListener((obs, o, n) -> clearError(errorDates));
        if (inputDescription != null)
            inputDescription.textProperty().addListener((obs, o, n) -> clearError(errorDescription));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS — IA IMAGE
    // ─────────────────────────────────────────────────────────────────────────

    /** Ouvre un FileChooser pour sélectionner une image du plat. */
    @FXML
    private void onChoisirImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image du plat");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        Stage stage = (Stage) inputNom.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null)
            return;

        selectedImageFile = file;

        // Afficher l'aperçu
        try {
            Image img = new Image(file.toURI().toString(), 118, 88, true, true);
            imagePreview.setImage(img);
            imagePreview.setVisible(true);
            imagePreview.setManaged(true);
            imagePlaceholder.setVisible(false);
            imagePlaceholder.setManaged(false);
        } catch (Exception e) {
            imagePlaceholder.setText("⚠ Aperçu indisponible");
        }

        btnAnalyser.setDisable(false);
        setAiStatus("✅ Image sélectionnée : " + file.getName() + " — Cliquez sur « Analyser ».", "#22c55e");
    }

    /**
     * Lance l'analyse de l'image via Gemini Vision API sur un thread background.
     * Remplit automatiquement les champs du formulaire avec le résultat.
     */
    @FXML
    private void onAnalyserImage() {
        if (selectedImageFile == null)
            return;

        btnAnalyser.setDisable(true);
        aiProgress.setVisible(true);
        aiProgress.setManaged(true);
        setAiStatus("🤖 Analyse en cours… (Gemini Vision)", "#a5b4fc");

        final File imageFile = selectedImageFile;

        Task<GeminiMenuAnalyzer.MenuAnalysisResult> task = new Task<GeminiMenuAnalyzer.MenuAnalysisResult>() {
            @Override
            protected GeminiMenuAnalyzer.MenuAnalysisResult call() throws Exception {
                return geminiAnalyzer.analyze(imageFile);
            }
        };

        task.setOnSucceeded(e -> {
            GeminiMenuAnalyzer.MenuAnalysisResult result = task.getValue();
            Platform.runLater(() -> fillFormWithAiResult(result));
        });

        task.setOnFailed(e -> {
            Throwable err = task.getException();
            Platform.runLater(() -> {
                aiProgress.setVisible(false);
                aiProgress.setManaged(false);
                btnAnalyser.setDisable(false);
                setAiStatus("❌ Erreur : " + (err != null ? err.getMessage() : "inconnue"), "#f87171");
            });
        });

        Thread t = new Thread(task, "GeminiMenuAnalysis");
        t.setDaemon(true);
        t.start();
    }

    /** Remplit les champs du formulaire avec le résultat de l'analyse IA. */
    private void fillFormWithAiResult(GeminiMenuAnalyzer.MenuAnalysisResult result) {
        aiProgress.setVisible(false);
        aiProgress.setManaged(false);
        btnAnalyser.setDisable(false);

        if (result == null) {
            setAiStatus("❌ L'analyse n'a retourné aucun résultat.", "#f87171");
            return;
        }

        // Vérification des références FXML
        if (inputNom == null || inputPrix == null || inputDescription == null) {
            System.err.println("CRITICAL: FXML fields are NULL! Check fx:id in FXML.");
            setAiStatus("❌ Erreur interne : champs UI manquants.", "#f87171");
            return;
        }

        // Remplissage forcé
        String nom = (result.nom != null && !result.nom.isBlank()) ? result.nom : "Nouveau Plat";
        inputNom.setText(nom);

        String desc = (result.description != null && !result.description.isBlank()) ? result.description
                : "Description générée par l'IA.";
        if (result.tags != null && !result.tags.isBlank()) {
            desc += "\n\n🏷 Tags : " + result.tags;
        }
        inputDescription.setText(desc);

        double prix = result.prixEstime;
        if (prix <= 0)
            prix = 15.00; // Prix par défaut si l'IA échoue
        inputPrix.setText(String.valueOf(prix));

        // Dates par défaut
        if (inputDateDebut.getValue() == null)
            inputDateDebut.setValue(LocalDate.now());
        if (inputDateFin.getValue() == null)
            inputDateFin.setValue(LocalDate.now().plusDays(7));

        setAiStatus("✨ \"" + nom + "\" généré avec succès !", "#34d399");
    }

    private void setAiStatus(String message, String hexColor) {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(message);
            aiStatusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 9.5px; -fx-font-style: italic;");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SETTERS
    // ─────────────────────────────────────────────────────────────────────────

    public void setMenu(Menu menu) {
        this.menu = menu;
        this.isEditMode = (menu != null);
        if (isEditMode) {
            formTitle.setText("Modifier Menu");
            populateFields();
        } else {
            formTitle.setText("Nouveau Menu");
        }
    }

    public void setListeController(MenuListeController controller) {
        this.listeController = controller;
    }

    public void setRestaurants(ObservableList<Restaurant> restaurants) {
        this.restaurants = restaurants;
        comboRestaurant.setItems(restaurants);
        comboRestaurant.setPromptText("Sélectionnez un restaurant");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void populateFields() {
        if (menu == null)
            return;
        inputNom.setText(menu.getNom());
        inputPrix.setText(menu.getPrix() != null ? menu.getPrix().toString() : "");
        inputDescription.setText(menu.getDescription());
        inputDateDebut.setValue(menu.getDateDebut());
        inputDateFin.setValue(menu.getDateFin());
        checkActif.setSelected(menu.isActif());

        if (restaurants != null && menu.getRestaurantId() != null) {
            for (Restaurant r : restaurants) {
                if (r.getId().equals(menu.getRestaurantId())) {
                    comboRestaurant.setValue(r);
                    break;
                }
            }
        }
    }

    @FXML
    private void onEnregistrer() {
        clearAllErrors();

        Menu m = isEditMode ? menu : new Menu();
        m.setNom(inputNom.getText().trim());

        Restaurant selectedRestaurant = comboRestaurant.getValue();
        if (selectedRestaurant != null) {
            m.setRestaurantId(selectedRestaurant.getId());
            m.setRestaurantNom(selectedRestaurant.getNom());
        } else {
            m.setRestaurantId(null);
        }

        String prixText = inputPrix.getText().trim();
        if (!prixText.isEmpty()) {
            try {
                m.setPrix(new BigDecimal(prixText.replace(",", ".")));
            } catch (NumberFormatException e) {
                showError(errorPrix, "Le prix doit être un nombre valide");
                return;
            }
        } else {
            m.setPrix(null);
        }

        m.setDescription(inputDescription.getText().trim());
        m.setDateDebut(inputDateDebut.getValue());
        m.setDateFin(inputDateFin.getValue());
        m.setActif(checkActif.isSelected());

        ValidationResult validation = m.validate();
        if (validation.hasErrors()) {
            displayValidationErrors(validation);
            return;
        }

        try {
            if (isEditMode) {
                controller.updateMenu(m);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu mis à jour",
                        "Le menu a été modifié avec succès.");
            } else {
                controller.createMenu(m);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu créé",
                        "Le menu a été créé avec succès.");
            }
            closeForm();
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        closeForm();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void updateHintNom() {
        if (hintNom == null)
            return;
        String s = inputNom.getText();
        int len = s == null ? 0 : s.trim().length();
        hintNom.setText(len + " / " + MAX_NOM_MENU + " car."
                + (len == 0 ? " — Obligatoire" : len > MAX_NOM_MENU ? " — Trop long !" : ""));
        hintNom.setStyle(len > MAX_NOM_MENU
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                : "-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
    }

    private void updateHintPrix() {
        if (hintPrix == null)
            return;
        String s = inputPrix.getText();
        if (s == null || s.trim().isEmpty()) {
            hintPrix.setText("Obligatoire. Ex: 15.00");
            hintPrix.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
            return;
        }
        try {
            BigDecimal v = new BigDecimal(s.trim().replace(",", "."));
            hintPrix.setText(v.compareTo(BigDecimal.ZERO) < 0 ? "Le prix ne peut pas être négatif." : "Format valide.");
            hintPrix.setStyle(v.compareTo(BigDecimal.ZERO) < 0
                    ? "-fx-text-fill: #e74c3c; -fx-font-size: 11px;"
                    : "-fx-text-fill: #27ae60; -fx-font-size: 11px;");
        } catch (NumberFormatException e) {
            hintPrix.setText("Saisir un nombre (ex. 15.00).");
            hintPrix.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
        }
    }

    private void displayValidationErrors(ValidationResult validation) {
        if (!isEmpty(validation.getFieldErrors("nom")))
            showError(errorNom, validation.getFieldErrors("nom").get(0));
        if (!isEmpty(validation.getFieldErrors("restaurant")))
            showError(errorRestaurant, validation.getFieldErrors("restaurant").get(0));
        if (!isEmpty(validation.getFieldErrors("prix")))
            showError(errorPrix, validation.getFieldErrors("prix").get(0));
        if (!isEmpty(validation.getFieldErrors("dates")))
            showError(errorDates, validation.getFieldErrors("dates").get(0));
        if (errorDescription != null && !isEmpty(validation.getFieldErrors("description")))
            showError(errorDescription, validation.getFieldErrors("description").get(0));
        if (validation.hasErrors())
            showGlobalError("Veuillez corriger les erreurs indiquées avant d'enregistrer.");
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    private void clearAllErrors() {
        clearError(errorNom);
        clearError(errorRestaurant);
        clearError(errorPrix);
        clearError(errorDates);
        if (errorDescription != null)
            clearError(errorDescription);
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
    }

    private void clearError(Label label) {
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void showError(Label label, String message) {
        label.setText("⚠ " + message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showGlobalError(String message) {
        globalErrorMessage.setText(message);
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
    }

    private void closeForm() {
        Stage stage = (Stage) formTitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue", message);
    }
}
