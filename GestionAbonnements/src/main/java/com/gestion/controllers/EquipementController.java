package com.gestion.controllers;

import com.gestion.entities.equipementwael;
import com.gestion.services.equipemementservicewael;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EquipementController implements Initializable {

    @FXML
    private TableView<equipementwael> equipementTable;
    @FXML
    private TableColumn<equipementwael, Long> colId;
    @FXML
    private TableColumn<equipementwael, String> colNom;
    @FXML
    private TableColumn<equipementwael, String> colDescription;
    @FXML
    private TableColumn<equipementwael, String> colCategorie;
    @FXML
    private TableColumn<equipementwael, String> colType;
    @FXML
    private TableColumn<equipementwael, BigDecimal> colPrix;
    @FXML
    private TableColumn<equipementwael, String> colVille;
    @FXML
    private TableColumn<equipementwael, Timestamp> colDateAjout;
    @FXML
    private TableColumn<equipementwael, String> colActions;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterCategorie;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private TextField nomField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField categorieField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField prixField;
    @FXML
    private TextField villeField;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnActualiser;
    @FXML
    private Button btnReset;
    @FXML
    private Button btnFermerForm;
    @FXML
    private VBox formSection;

    @FXML
    private Label countLabel;

    private equipemementservicewael service;
    private ObservableList<equipementwael> equipementList;
    private equipementwael selectedEquipement;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new equipemementservicewael();
        equipementList = FXCollections.observableArrayList();

        // Configuration des colonnes
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colDateAjout.setCellValueFactory(new PropertyValueFactory<>("dateAjout"));

        // Formatage des colonnes
        colPrix.setCellFactory(column -> new TableCell<equipementwael, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal prix, boolean empty) {
                super.updateItem(prix, empty);
                if (empty || prix == null) {
                    setText(null);
                } else {
                    DecimalFormat df = new DecimalFormat("#,##0.00");
                    setText(df.format(prix) + " TND");
                }
            }
        });

        colDateAjout.setCellFactory(column -> new TableCell<equipementwael, Timestamp>() {
            @Override
            protected void updateItem(Timestamp date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    setText(sdf.format(date));
                }
            }
        });

        // Colonne Actions avec boutons
        colActions.setCellFactory(column -> new TableCell<equipementwael, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    equipementwael e = getTableView().getItems().get(getIndex());

                    Button btnEdit = new Button("✏️");
                    btnEdit.getStyleClass().add("action-btn-edit");
                    btnEdit.setOnAction(event -> modifierEquipement(e));

                    Button btnDelete = new Button("🗑️");
                    btnDelete.getStyleClass().add("action-btn-delete");
                    btnDelete.setOnAction(event -> supprimerEquipement(e));

                    Button btnDetails = new Button("ℹ️");
                    btnDetails.getStyleClass().add("action-btn-details");
                    btnDetails.setOnAction(event -> afficherDetails(e));

                    HBox hbox = new HBox(5, btnEdit, btnDelete, btnDetails);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });

        // Configuration des ComboBox
        typeCombo.getItems().addAll("VENTE", "LOCATION");

        // Filtres : Type uniquement (pas "statut")
        filterType.getItems().addAll("VENTE", "LOCATION");
        sortCombo.getItems().addAll(
                "Date (récent)",
                "Date (ancien)",
                "Prix (croissant)",
                "Prix (décroissant)",
                "Nom (A-Z)",
                "Nom (Z-A)");
        sortCombo.setValue("Date (récent)");

        // Tooltips pour une utilisation plus simple
        btnAjouter.setTooltip(new Tooltip(
                "Cliquez pour ajouter un nouvel équipement. Remplissez le formulaire en bas puis enregistrez."));
        btnActualiser.setTooltip(new Tooltip("Recharge la liste depuis la base de données."));
        btnModifier.setTooltip(new Tooltip("Enregistre les modifications sur l'équipement sélectionné."));
        btnSupprimer.setTooltip(new Tooltip("Supprime l'équipement sélectionné (demande confirmation)."));
        searchField.setTooltip(new Tooltip("Recherche par nom, description, catégorie ou ville."));
        prixField.setTooltip(new Tooltip("Prix en dinars tunisiens (TND). Exemple: 150.500"));

        // Écouteurs pour recherche et filtres
        searchField.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        filterCategorie.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        filterType.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());

        // Sélection dans la table
        equipementTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedEquipement = newVal;
            if (newVal != null) {
                chargerEquipementDansFormulaire(newVal);
            }
        });

        // Formulaire masqué au départ pour libérer l'espace
        if (formSection != null) {
            formSection.setVisible(false);
            formSection.setManaged(false);
        }

        // Charger les données (ne pas bloquer l'affichage si la DB échoue)
        try {
            actualiser();
        } catch (Exception e) {
            System.err.println("Chargement équipements: " + e.getMessage());
            countLabel.setText("0 équipement(s)");
        }
    }

    private void montrerFormulaire() {
        if (formSection != null) {
            formSection.setVisible(true);
            formSection.setManaged(true);
        }
    }

    @FXML
    private void masquerFormulaire() {
        if (formSection != null) {
            formSection.setVisible(false);
            formSection.setManaged(false);
        }
    }

    @FXML
    private void actualiser() {
        List<equipementwael> all = service.afficher();
        equipementList.clear();
        equipementList.addAll(all);

        // Mettre à jour les catégories disponibles
        List<String> categories = all.stream()
                .map(equipementwael::getCategorie)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        filterCategorie.getItems().clear();
        filterCategorie.getItems().addAll(categories);
        filterCategorie.setValue(null);

        appliquerFiltres();
    }

    @FXML
    private void appliquerFiltres() {
        // Filtrer d'abord
        List<equipementwael> filteredList = equipementList.stream()
                .filter(creerPredicatFiltre())
                .collect(Collectors.toList());

        // Appliquer le tri - utiliser une variable finale
        final List<equipementwael> baseList = filteredList;
        String tri = sortCombo.getValue();
        List<equipementwael> sortedList;

        if (tri != null) {
            switch (tri) {
                case "Date (récent)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                Timestamp da = a.getDateAjout();
                                Timestamp db = b.getDateAjout();
                                if (da == null && db == null)
                                    return 0;
                                if (da == null)
                                    return 1;
                                if (db == null)
                                    return -1;
                                return db.compareTo(da);
                            })
                            .collect(Collectors.toList());
                    break;
                case "Date (ancien)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                Timestamp da = a.getDateAjout();
                                Timestamp db = b.getDateAjout();
                                if (da == null && db == null)
                                    return 0;
                                if (da == null)
                                    return -1;
                                if (db == null)
                                    return 1;
                                return da.compareTo(db);
                            })
                            .collect(Collectors.toList());
                    break;
                case "Prix (croissant)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                BigDecimal pa = a.getPrix() != null ? a.getPrix() : BigDecimal.ZERO;
                                BigDecimal pb = b.getPrix() != null ? b.getPrix() : BigDecimal.ZERO;
                                return pa.compareTo(pb);
                            })
                            .collect(Collectors.toList());
                    break;
                case "Prix (décroissant)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                BigDecimal pa = a.getPrix() != null ? a.getPrix() : BigDecimal.ZERO;
                                BigDecimal pb = b.getPrix() != null ? b.getPrix() : BigDecimal.ZERO;
                                return pb.compareTo(pa);
                            })
                            .collect(Collectors.toList());
                    break;
                case "Nom (A-Z)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                String na = a.getNom() != null ? a.getNom().toLowerCase() : "";
                                String nb = b.getNom() != null ? b.getNom().toLowerCase() : "";
                                return na.compareTo(nb);
                            })
                            .collect(Collectors.toList());
                    break;
                case "Nom (Z-A)":
                    sortedList = baseList.stream()
                            .sorted((a, b) -> {
                                String na = a.getNom() != null ? a.getNom().toLowerCase() : "";
                                String nb = b.getNom() != null ? b.getNom().toLowerCase() : "";
                                return nb.compareTo(na);
                            })
                            .collect(Collectors.toList());
                    break;
                default:
                    sortedList = baseList;
                    break;
            }
        } else {
            sortedList = baseList;
        }

        equipementTable.setItems(FXCollections.observableArrayList(sortedList));
        countLabel.setText(sortedList.size() + " équipement(s)");
    }

    private Predicate<equipementwael> creerPredicatFiltre() {
        String search = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String cat = filterCategorie.getValue();
        String type = filterType.getValue();

        return e -> {
            // Recherche globale
            boolean matchSearch = search.isEmpty() ||
                    (e.getNom() != null && e.getNom().toLowerCase().contains(search)) ||
                    (e.getDescription() != null && e.getDescription().toLowerCase().contains(search)) ||
                    (e.getCategorie() != null && e.getCategorie().toLowerCase().contains(search)) ||
                    (e.getVille() != null && e.getVille().toLowerCase().contains(search));

            // Filtre catégorie (null = pas de filtre)
            boolean matchCat = cat == null || (e.getCategorie() != null && e.getCategorie().equals(cat));

            // Filtre type (null = pas de filtre)
            boolean matchType = type == null || (e.getType() != null && e.getType().equals(type));

            return matchSearch && matchCat && matchType;
        };
    }

    @FXML
    private void ajouterEquipement() {
        // Si le formulaire est masqué, l'afficher et vider pour saisie (ne pas ajouter
        // tout de suite)
        if (formSection != null && !formSection.isVisible()) {
            viderFormulaire();
            selectedEquipement = null;
            montrerFormulaire();
            return;
        }
        if (!validerFormulaire())
            return;

        try {
            equipementwael e = new equipementwael(
                    nomField.getText().trim(),
                    descriptionField.getText().trim(),
                    categorieField.getText().trim(),
                    typeCombo.getValue(),
                    new BigDecimal(prixField.getText().trim()),
                    villeField.getText().trim()

            );

            service.ajouter(e);
            viderFormulaire();
            actualiser();
            masquerFormulaire();
            afficherMessage("✅ Équipement ajouté avec succès !", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            afficherMessage("❌ Erreur lors de l'ajout : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Méthode wrapper pour appels depuis FXML (sans paramètre)
    @FXML
    private void modifierEquipement() {
        modifierEquipement(null);
    }

    // Méthode avec paramètre pour appels depuis le code
    private void modifierEquipement(equipementwael e) {
        equipementwael equipementAModifier = e;
        if (equipementAModifier == null) {
            equipementAModifier = selectedEquipement;
        }
        if (equipementAModifier == null) {
            afficherMessage("⚠️ Veuillez sélectionner un équipement dans le tableau", Alert.AlertType.WARNING);
            return;
        }

        montrerFormulaire();
        chargerEquipementDansFormulaire(equipementAModifier);
    }

    @FXML
    private void enregistrerModification() {
        if (selectedEquipement == null) {
            afficherMessage("⚠️ Aucun équipement sélectionné", Alert.AlertType.WARNING);
            return;
        }

        if (!validerFormulaire())
            return;

        try {
            selectedEquipement.setNom(nomField.getText().trim());
            selectedEquipement.setDescription(descriptionField.getText().trim());
            selectedEquipement.setCategorie(categorieField.getText().trim());
            selectedEquipement.setType(typeCombo.getValue());
            selectedEquipement.setPrix(new BigDecimal(prixField.getText().trim()));
            selectedEquipement.setVille(villeField.getText().trim());

            service.modifier(selectedEquipement);
            viderFormulaire();
            selectedEquipement = null;
            actualiser();
            masquerFormulaire();
            afficherMessage("✅ Équipement modifié avec succès !", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            afficherMessage("❌ Erreur lors de la modification : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Méthode wrapper pour appels depuis FXML (sans paramètre)
    @FXML
    private void supprimerEquipement() {
        supprimerEquipement(null);
    }

    // Méthode avec paramètre pour appels depuis le code
    private void supprimerEquipement(equipementwael e) {
        equipementwael equipementASupprimer = e;
        if (equipementASupprimer == null) {
            equipementASupprimer = selectedEquipement;
        }
        if (equipementASupprimer == null) {
            afficherMessage("⚠️ Veuillez sélectionner un équipement", Alert.AlertType.WARNING);
            return;
        }

        // Créer une référence finale pour la lambda
        final equipementwael equipementFinal = equipementASupprimer;
        final Long equipementId = equipementFinal.getId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'équipement");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer : " + equipementFinal.getNom() + " ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimer(equipementId);
                if (selectedEquipement != null && selectedEquipement.getId().equals(equipementId)) {
                    viderFormulaire();
                    selectedEquipement = null;
                    masquerFormulaire();
                }
                actualiser();
                afficherMessage("✅ Équipement supprimé avec succès !", Alert.AlertType.INFORMATION);
            }
        });
    }

    // Méthode wrapper pour appels depuis FXML (sans paramètre)
    @FXML
    private void afficherDetails() {
        afficherDetails(null);
    }

    // Méthode avec paramètre pour appels depuis le code
    private void afficherDetails(equipementwael e) {
        equipementwael equipementADetails = e;
        if (equipementADetails == null) {
            equipementADetails = selectedEquipement;
        }
        if (equipementADetails == null) {
            afficherMessage("⚠️ Veuillez sélectionner un équipement", Alert.AlertType.WARNING);
            return;
        }

        // Créer une référence finale pour éviter les problèmes de lambda
        final equipementwael equipementFinal = equipementADetails;

        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails de l'équipement");
        details.setHeaderText(equipementFinal.getNom());

        String content = String.format(
                "ID: %d\n" +
                        "Nom: %s\n" +
                        "Description: %s\n" +
                        "Catégorie: %s\n" +
                        "Type: %s\n" +
                        "Prix: %s TND\n" +
                        "Ville: %s\n" +
                        "Date d'ajout: %s",
                equipementFinal.getId(),
                equipementFinal.getNom(),
                equipementFinal.getDescription() != null ? equipementFinal.getDescription() : "N/A",
                equipementFinal.getCategorie() != null ? equipementFinal.getCategorie() : "N/A",
                equipementFinal.getType(),
                equipementFinal.getPrix() != null ? new DecimalFormat("#,##0.00").format(equipementFinal.getPrix())
                        : "N/A",
                equipementFinal.getVille() != null ? equipementFinal.getVille() : "N/A",

                equipementFinal.getDateAjout() != null
                        ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(equipementFinal.getDateAjout())
                        : "N/A");

        details.setContentText(content);
        details.showAndWait();
    }

    @FXML
    private void onRetourDashboard() {
        // Navigation gérée par MainController (bouton Retour style LAMMA)
    }

    @FXML
    private void resetFiltres() {
        searchField.clear();
        filterCategorie.setValue(null);
        filterType.setValue(null);
        sortCombo.setValue("Date (récent)");
    }

    private void chargerEquipementDansFormulaire(equipementwael e) {
        nomField.setText(e.getNom());
        descriptionField.setText(e.getDescription());
        categorieField.setText(e.getCategorie());
        typeCombo.setValue(e.getType());
        prixField.setText(e.getPrix() != null ? e.getPrix().toString() : "");
        villeField.setText(e.getVille());

    }

    private void viderFormulaire() {
        nomField.clear();
        descriptionField.clear();
        categorieField.clear();
        typeCombo.setValue(null);
        prixField.clear();
        villeField.clear();
    }

    // ✅ Contrôle de saisie (validation)
    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();

        // Validation nom (obligatoire)
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            errors.append("• Le nom est obligatoire\n");
        } else if (nomField.getText().trim().length() > 100) {
            errors.append("• Le nom ne doit pas dépasser 100 caractères\n");
        }

        // Validation type (obligatoire)
        if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
            errors.append("• Le type est obligatoire\n");
        }

        // Validation prix (obligatoire et numérique positif)
        try {
            String prixText = prixField.getText().trim();
            if (prixText.isEmpty()) {
                errors.append("• Le prix est obligatoire\n");
            } else {
                BigDecimal prix = new BigDecimal(prixText);
                if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("• Le prix doit être supérieur à 0\n");
                }
            }
        } catch (NumberFormatException e) {
            errors.append("• Le prix doit être un nombre valide (en dinars tunisiens TND)\n");
        }

        // Validation description (max TEXT)
        if (descriptionField.getText() != null && descriptionField.getText().length() > 65535) {
            errors.append("• La description est trop longue\n");
        }

        // Validation catégorie (max 50)
        if (categorieField.getText() != null && categorieField.getText().trim().length() > 50) {
            errors.append("• La catégorie ne doit pas dépasser 50 caractères\n");
        }

        // Validation ville (max 100)
        if (villeField.getText() != null && villeField.getText().trim().length() > 100) {
            errors.append("• La ville ne doit pas dépasser 100 caractères\n");
        }

        if (errors.length() > 0) {
            afficherMessage("Erreurs de validation :\n" + errors.toString(), Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private void afficherMessage(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erreur"
                : type == Alert.AlertType.WARNING ? "Avertissement" : "Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
