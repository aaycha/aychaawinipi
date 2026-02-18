package com.gestion.ui.utilisateur;

import com.gestion.controllers.MainController;
import com.gestion.entities.CompositionMenu;
import com.gestion.entities.ParticipantRestauration;
import com.gestion.entities.RepasDetaille;
import com.gestion.entities.Restauration;
import com.gestion.interfaces.CompositionMenuService;
import com.gestion.interfaces.RepasDetailleService;
import com.gestion.interfaces.RestaurationService;
import com.gestion.services.CompositionMenuServiceImpl;
import com.gestion.services.RepasDetailleServiceImpl;
import com.gestion.services.RestaurationServiceImpl;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur pour l'espace utilisateur dédié à la composition de repas/menus
 */
public class MonEspaceRestaurationController {
    
    @FXML private CheckBox checkVegetarien;
    @FXML private CheckBox checkVegan;
    @FXML private CheckBox checkSansGluten;
    @FXML private CheckBox checkHalal;
    @FXML private TextArea inputAllergies;
    
    @FXML private TextField inputEvenementId;
    @FXML private DatePicker inputDateMenu;
    @FXML private Label menuGenereLabel;
    
    @FXML private ListView<RepasDetaille> listeComposantsDispo;
    @FXML private ListView<RepasDetaille> listeMaComposition;
    @FXML private TextField inputNomPlatCompose;
    @FXML private Label errorNomPlatCompose;
    @FXML private ComboBox<String> comboTypePlatCompose;
    @FXML private Label messagePlatCompose;

    @FXML private ComboBox<Restauration> comboMenu;
    @FXML private ComboBox<RepasDetaille> comboRepas;
    @FXML private ListView<RepasDetaille> listeRepasMenu;
    
    @FXML private TextArea apercuMenu;
    
    @FXML private Label statTotalRepas;
    @FXML private Label statTotalDepense;
    @FXML private Label statTotalCalories;
    @FXML private Label statTotalMenus;
    
    private final RepasDetailleService repasService = new RepasDetailleServiceImpl();
    private final RestaurationService restaurationService = new RestaurationServiceImpl();
    private final CompositionMenuService compositionService = new CompositionMenuServiceImpl();
    
    private Long currentUserId = 1L; // À récupérer depuis la session
    private final ObservableList<RepasDetaille> repasMenu = FXCollections.observableArrayList();
    private final ObservableList<RepasDetaille> compositionPlat = FXCollections.observableArrayList();
    private final ObservableList<RepasDetaille> composantsDispoList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        // Déterminer l'ID utilisateur (Simulation - à lier au système de session)
        initUserSession();
        
        setupListViews();
        
        if (comboTypePlatCompose != null) {
            comboTypePlatCompose.setItems(FXCollections.observableArrayList("PETIT_DEJEUNER", "DEJEUNER", "DINER", "SNACK"));
            comboTypePlatCompose.setValue("DEJEUNER");
        }
        
        loadAllData();
    }
    
    private void initUserSession() {
        // Dans une vraie app, on récupérerait l'utilisateur connecté
        // Pour la démo, on utilise l'ID 1 ou 2
        this.currentUserId = 1L; 
        System.out.println("Mode MonEspaceRestauration pour utilisateur ID: " + currentUserId);
    }
    
    private void setupListViews() {
        listeRepasMenu.setItems(repasMenu);
        
        if (listeComposantsDispo != null) {
            listeComposantsDispo.setItems(composantsDispoList);
            listeComposantsDispo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(RepasDetaille item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String text = String.format("%s — %.2f €", item.getNom(), (item.getPrix() != null ? item.getPrix() : 0.0));
                        if (item.getCalories() != null) text += " | " + item.getCalories() + " cal";
                        setText(text);
                        // On pourrait ajouter une icône selon le type
                    }
                }
            });
        }
        
        if (listeMaComposition != null) {
            listeMaComposition.setItems(compositionPlat);
            listeMaComposition.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(RepasDetaille item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getNom() + " — " + (item.getPrix() != null ? item.getPrix() + " €" : "0 €"));
                    }
                }
            });
        }
    }
    
    private void loadAllData() {
        Platform.runLater(() -> {
            try {
                loadMenus();
                loadRepas();
                loadRestrictions();
                loadStatistics();
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des données restauration: " + e.getMessage());
            }
        });
    }

    private void loadMenus() {
        List<Restauration> menus = restaurationService.findMenusActifs();
        comboMenu.getItems().setAll(menus);
        if (!menus.isEmpty()) comboMenu.getSelectionModel().selectFirst();
    }
    
    private void loadRepas() {
        // Charger les composants de base disponibles pour tout le monde
        List<RepasDetaille> allRepas = repasService.findAll();
        composantsDispoList.setAll(allRepas);
        
        // Charger les repas filtrés pour le menu (ceux déjà créés ou compatibles)
        List<RepasDetaille> compatibleRepas = filterRepasByRestrictions(allRepas);
        comboRepas.getItems().setAll(compatibleRepas);
        if (!compatibleRepas.isEmpty()) comboRepas.getSelectionModel().selectFirst();
    }
    
    private List<RepasDetaille> filterRepasByRestrictions(List<RepasDetaille> repas) {
        return repas.stream()
            .filter(r -> {
                if (checkVegetarien.isSelected() && !r.isVegetarien()) return false;
                if (checkVegan.isSelected() && !r.isVegan()) return false;
                if (checkSansGluten.isSelected() && !r.isSansGluten()) return false;
                if (checkHalal.isSelected() && !r.isHalal()) return false;
                
                // Vérifier allergies
                String allergies = inputAllergies.getText().toLowerCase();
                if (!allergies.isEmpty()) {
                    for (String allergene : allergies.split(",")) {
                        if (r.getAllergenes().contains(allergene.trim())) {
                            return false;
                        }
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }
    
    private void loadRestrictions() {
        // Charger les restrictions depuis ParticipantRestauration
        // Pour l'instant, valeurs par défaut
    }
    
    @FXML
    void onEnregistrerRestrictions() {
        // Enregistrer les restrictions dans ParticipantRestauration
        ParticipantRestauration besoin = new ParticipantRestauration();
        besoin.setParticipantId(currentUserId);
        besoin.setRestrictionLibelle(buildRestrictionLibelle());
        besoin.setRestrictionDescription(inputAllergies.getText());
        besoin.setNiveauGravite("MODEREE");
        besoin.setRestrictionActive(true);
        
        // Utiliser RestaurationService pour créer le besoin
        restaurationService.createBesoin(besoin);
        
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Restrictions enregistrées avec succès");
        loadRepas(); // Recharger les repas compatibles
    }
    
    private String buildRestrictionLibelle() {
        List<String> restrictions = new ArrayList<>();
        if (checkVegetarien.isSelected()) restrictions.add("Végétarien");
        if (checkVegan.isSelected()) restrictions.add("Végan");
        if (checkSansGluten.isSelected()) restrictions.add("Sans gluten");
        if (checkHalal.isSelected()) restrictions.add("Halal");
        return String.join(", ", restrictions);
    }
    
    @FXML
    void onGenererMenu() {
        Long evenementId = parseLong(inputEvenementId.getText());
        LocalDate date = inputDateMenu.getValue();
        
        if (evenementId == null || date == null) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez remplir l'événement ID et la date");
            return;
        }
        
        // Générer menu automatiquement basé sur les restrictions
        List<RepasDetaille> repasCompatibles = filterRepasByRestrictions(repasService.findAll());
        
        // Algorithme simple: sélectionner un repas par type pour la journée
        List<RepasDetaille> menuGenere = new ArrayList<>();
        for (String type : List.of("PETIT_DEJEUNER", "DEJEUNER", "DINER")) {
            repasCompatibles.stream()
                .filter(r -> type.equals(r.getTypeRepas()))
                .findFirst()
                .ifPresent(menuGenere::add);
        }
        
        if (menuGenere.isEmpty()) {
            menuGenereLabel.setText("Aucun repas compatible trouvé avec vos restrictions.");
        } else {
            repasMenu.clear();
            repasMenu.addAll(menuGenere);
            menuGenereLabel.setText("Menu généré avec " + menuGenere.size() + " repas.");
            updateApercu();
        }
    }
    
    @FXML
    void onAjouterRepasAuMenu() {
        RepasDetaille repas = comboRepas.getValue();
        if (repas != null && !repasMenu.contains(repas)) {
            repasMenu.add(repas);
            updateApercu();
        }
    }
    
    @FXML
    void onSupprimerRepas() {
        RepasDetaille selected = listeRepasMenu.getSelectionModel().getSelectedItem();
        if (selected != null) {
            repasMenu.remove(selected);
            updateApercu();
        }
    }
    
    @FXML
    void onReorganiser() {
        // Permettre de réorganiser l'ordre des repas dans le menu
        showAlert(Alert.AlertType.INFORMATION, "Info", "Fonctionnalité de réorganisation à implémenter");
    }
    
    @FXML
    void onEnregistrerMenu() {
        Restauration menu = comboMenu.getValue();
        if (menu == null || repasMenu.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez sélectionner un menu et ajouter des repas");
            return;
        }
        
        Long evenementId = parseLong(inputEvenementId.getText());
        LocalDate date = inputDateMenu.getValue() != null ? inputDateMenu.getValue() : LocalDate.now();
        
        // Créer les compositions
        for (int i = 0; i < repasMenu.size(); i++) {
            RepasDetaille repas = repasMenu.get(i);
            CompositionMenu composition = new CompositionMenu();
            composition.setMenuId(menu.getId());
            composition.setRepasId(repas.getId());
            composition.setOrdre(i + 1);
            composition.setTypeRepas(repas.getTypeRepas());
            composition.setDate(date);
            composition.setParticipantId(currentUserId);
            composition.setEvenementId(evenementId);
            composition.setActif(true);
            
            compositionService.create(composition);
        }
        
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Menu enregistré avec succès");
        loadStatistics();
    }
    
    @FXML
    void onPrevisualiser() {
        updateApercu();
    }

    // ——— Composer un plat (choix de composants) ———

    @FXML
    void onAjouterComposant() {
        if (listeComposantsDispo == null || listeMaComposition == null) return;
        RepasDetaille selected = listeComposantsDispo.getSelectionModel().getSelectedItem();
        if (selected != null && !compositionPlat.contains(selected)) {
            compositionPlat.add(selected);
        }
    }

    @FXML
    void onRetirerComposant() {
        if (listeMaComposition == null) return;
        RepasDetaille selected = listeMaComposition.getSelectionModel().getSelectedItem();
        if (selected != null) {
            compositionPlat.remove(selected);
        }
    }

    @FXML
    void onEnregistrerPlatCompose() {
        if (errorNomPlatCompose != null) {
            errorNomPlatCompose.setVisible(false);
            errorNomPlatCompose.setManaged(false);
        }
        if (messagePlatCompose != null) messagePlatCompose.setVisible(false);
        String nom = inputNomPlatCompose != null ? inputNomPlatCompose.getText().trim() : "";
        if (nom.isEmpty()) {
            if (errorNomPlatCompose != null) {
                errorNomPlatCompose.setText("Le nom du plat est obligatoire.");
                errorNomPlatCompose.setVisible(true);
                errorNomPlatCompose.setManaged(true);
            }
            return;
        }
        if (compositionPlat.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Composition vide", "Ajoutez au moins un composant à votre plat.");
            return;
        }
        BigDecimal prixTotal = BigDecimal.ZERO;
        int caloriesTotal = 0;
        List<String> ingredients = new ArrayList<>();
        for (RepasDetaille r : compositionPlat) {
            if (r.getPrix() != null) prixTotal = prixTotal.add(r.getPrix());
            if (r.getCalories() != null) caloriesTotal += r.getCalories();
            ingredients.add(r.getNom());
            if (r.getIngredients() != null) ingredients.addAll(r.getIngredients());
        }
        RepasDetaille platCompose = new RepasDetaille();
        platCompose.setNom(nom);
        platCompose.setDescription("Plat composé : " + String.join(", ", compositionPlat.stream().map(RepasDetaille::getNom).collect(Collectors.toList())));
        platCompose.setPrix(prixTotal);
        platCompose.setCalories(caloriesTotal);
        platCompose.setTypeRepas(comboTypePlatCompose != null ? comboTypePlatCompose.getValue() : "DEJEUNER");
        platCompose.setParticipantId(currentUserId);
        platCompose.setIngredients(ingredients);
        platCompose.setActif(true);
        repasService.create(platCompose);
        if (messagePlatCompose != null) {
            messagePlatCompose.setText("Plat \"" + nom + "\" enregistré avec " + compositionPlat.size() + " composant(s). Total : " + prixTotal + " €, " + caloriesTotal + " cal.");
            messagePlatCompose.setVisible(true);
            messagePlatCompose.setManaged(true);
        }
        onReinitialiserComposition();
        loadStatistics();
    }

    @FXML
    void onReinitialiserComposition() {
        compositionPlat.clear();
        if (inputNomPlatCompose != null) inputNomPlatCompose.clear();
        if (comboTypePlatCompose != null) comboTypePlatCompose.setValue("DEJEUNER");
        if (errorNomPlatCompose != null) {
            errorNomPlatCompose.setVisible(false);
            errorNomPlatCompose.setManaged(false);
        }
        if (listeComposantsDispo != null) listeComposantsDispo.getSelectionModel().clearSelection();
        if (listeMaComposition != null) listeMaComposition.getSelectionModel().clearSelection();
    }
    
    private void updateApercu() {
        StringBuilder apercu = new StringBuilder();
        apercu.append("=== MON MENU ===\n\n");
        
        BigDecimal totalPrix = BigDecimal.ZERO;
        int totalCalories = 0;
        
        for (RepasDetaille repas : repasMenu) {
            apercu.append(String.format("• %s (%s)\n", repas.getNom(), repas.getTypeRepas()));
            apercu.append(String.format("  Prix: %.2f € | Calories: %d\n", 
                repas.getPrix(), repas.getCalories() != null ? repas.getCalories() : 0));
            if (repas.getDescription() != null) {
                apercu.append(String.format("  %s\n", repas.getDescription()));
            }
            apercu.append("\n");
            
            totalPrix = totalPrix.add(repas.getPrix());
            totalCalories += repas.getCalories() != null ? repas.getCalories() : 0;
        }
        
        apercu.append(String.format("=== TOTAL ===\n"));
        apercu.append(String.format("Prix total: %.2f €\n", totalPrix));
        apercu.append(String.format("Calories totales: %d\n", totalCalories));
        
        apercuMenu.setText(apercu.toString());
    }
    
    private void loadStatistics() {
        List<RepasDetaille> mesRepas = repasService.findByParticipantId(currentUserId);
        BigDecimal totalDepense = repasService.getTotalPrixByParticipant(currentUserId);
        Integer totalCalories = repasService.getTotalCaloriesByParticipant(currentUserId);
        List<CompositionMenu> mesMenus = compositionService.findByParticipantId(currentUserId);
        
        Platform.runLater(() -> {
            statTotalRepas.setText(String.valueOf(mesRepas.size()));
            statTotalDepense.setText(String.format("%.2f €", totalDepense));
            statTotalCalories.setText(String.valueOf(totalCalories));
            statTotalMenus.setText(String.valueOf(mesMenus.size()));
        });
    }
    
    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
