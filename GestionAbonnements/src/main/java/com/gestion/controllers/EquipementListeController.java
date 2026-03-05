package com.gestion.controllers;

import com.gestion.entities.equipementwael;
import com.gestion.entities.GroupeChat;
import com.gestion.entities.MessageChat;
import com.gestion.services.equipemementservicewael;
import com.gestion.services.GroupeChatService;
import com.gestion.services.MessageChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.gestion.services.MailService;
import com.gestion.services.StatistiqueService;

import java.math.BigDecimal;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EquipementListeController implements Initializable {

    @FXML
    private ListView<equipementwael> listView;
    @FXML
    private TextField searchField;
    @FXML
    private Label countLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnDetails;

    @FXML
    private ListView<GroupeChat> groupsAdminList;
    @FXML
    private ListView<MessageChat> messagesAdminList;
    @FXML
    private StackPane chartGroupActif;
    @FXML
    private StackPane chartEquipementVu;

    private equipemementservicewael service;
    private GroupeChatService groupeService;
    private MessageChatService messageService;
    private StatistiqueService statistiqueService;
    private MailService mailService;
    private ObservableList<equipementwael> equipementList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        service = new equipemementservicewael();
        groupeService = new GroupeChatService();
        messageService = new MessageChatService();
        statistiqueService = new StatistiqueService();
        mailService = new MailService();
        equipementList = FXCollections.observableArrayList();

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(equipementwael e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox card = new VBox(6);
                    card.setStyle(
                            "-fx-padding: 12; -fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 10;");
                    Label nom = new Label(e.getNom() != null ? e.getNom() : "");
                    nom.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
                    Label info = new Label(String.format("%s • %s • %s TND",
                            e.getCategorie() != null ? e.getCategorie() : "-",
                            e.getType() != null ? e.getType() : "-",
                            e.getPrix() != null ? new DecimalFormat("#,##0.00").format(e.getPrix()) : "0"));
                    info.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");

                    card.getChildren().addAll(nom, info);
                    setGraphic(card);
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = n != null;
            btnModifier.setDisable(!sel);
            btnSupprimer.setDisable(!sel);
            btnDetails.setDisable(!sel);
        });

        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());

        // Config listes admin (groupes / messages) avec CRUD
        if (groupsAdminList != null) {
            groupsAdminList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(GroupeChat g, boolean empty) {
                    super.updateItem(g, empty);
                    if (empty || g == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(g.getNom());
                    }
                }
            });
            groupsAdminList.setContextMenu(createGroupesContextMenu());
        }
        if (messagesAdminList != null) {
            messagesAdminList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(MessageChat m, boolean empty) {
                    super.updateItem(m, empty);
                    if (empty || m == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String txt = m.getContenu() != null && m.getContenu().length() > 40
                                ? m.getContenu().substring(0, 40) + "..."
                                : m.getContenu();
                        setText(txt);
                    }
                }
            });
            messagesAdminList.setContextMenu(createMessagesContextMenu());
        }

        actualiser();
    }

    @FXML
    private void onNouveau() {
        ouvrirFormulaire(null);
    }

    @FXML
    private void onModifier() {
        equipementwael sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        ouvrirFormulaire(sel);
    }

    @FXML
    private void onSupprimer() {
        equipementwael sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Supprimer");
        c.setHeaderText("Supprimer " + sel.getNom() + " ?");
        c.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                service.supprimer(sel.getId());
                actualiser();
            }
        });
    }

    @FXML
    private void onVoirDetails() {
        equipementwael sel = listView.getSelectionModel().getSelectedItem();
        if (sel == null)
            return;
        String msg = String.format("ID: %d\nNom: %s\nDescription: %s\nCatégorie: %s\nType: %s\nPrix: %s TND\nVille: %s",
                sel.getId(),
                sel.getNom(),
                sel.getDescription() != null ? sel.getDescription() : "-",
                sel.getCategorie() != null ? sel.getCategorie() : "-",
                sel.getType(),
                sel.getPrix() != null ? new DecimalFormat("#,##0.00").format(sel.getPrix()) : "0",
                sel.getVille() != null ? sel.getVille() : "-");
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    @FXML
    private void onActualiser() {
        actualiser();
    }

    @FXML
    private void onReinitialiser() {
        searchField.clear();
        appliquerFiltres();
    }

    @FXML
    private void onListClick() {
    }

    @FXML
    private void onEnvoyerEmail() {
        if (mailService == null || !mailService.isConfigured()) {
            new Alert(Alert.AlertType.WARNING,
                    "Clé SendGrid non configurée. Créez config/mail.properties avec sendgrid.api.key=VOTRE_CLE ou définissez SENDGRID_API_KEY.")
                    .showAndWait();
            return;
        }
        Dialog<Boolean> d = new Dialog<>();
        d.setTitle("Envoyer un email (SendGrid)");
        d.setHeaderText("Envoi via API SendGrid");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        TextField fromF = new TextField();
        fromF.setPromptText("Expéditeur (email vérifié SendGrid)");
        fromF.setPrefWidth(320);
        TextField toF = new TextField();
        toF.setPromptText("Destinataire");
        toF.setPrefWidth(320);
        TextField subjectF = new TextField();
        subjectF.setPromptText("Sujet");
        subjectF.setPrefWidth(320);
        TextArea bodyF = new TextArea();
        bodyF.setPromptText("Corps du message");
        bodyF.setPrefRowCount(5);
        bodyF.setPrefWidth(320);
        grid.add(new Label("De:"), 0, 0);
        grid.add(fromF, 1, 0);
        grid.add(new Label("À:"), 0, 1);
        grid.add(toF, 1, 1);
        grid.add(new Label("Sujet:"), 0, 2);
        grid.add(subjectF, 1, 2);
        grid.add(new Label("Message:"), 0, 3);
        grid.add(bodyF, 1, 3);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(btn -> btn == ButtonType.OK ? Boolean.TRUE : null);
        java.util.Optional<Boolean> res = d.showAndWait();
        if (res.isEmpty() || !Boolean.TRUE.equals(res.get()))
            return;
        String from = fromF.getText() != null ? fromF.getText().trim() : "";
        String to = toF.getText() != null ? toF.getText().trim() : "";
        String subject = subjectF.getText() != null ? subjectF.getText().trim() : "";
        String body = bodyF.getText() != null ? bodyF.getText() : "";
        if (from.isEmpty() || to.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "De et À sont obligatoires.").showAndWait();
            return;
        }
        boolean ok = mailService.sendText(from, to, subject, body);
        new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                ok ? "Email envoyé avec succès."
                        : "Échec d'envoi. Vérifiez la clé API et l'expéditeur (doit être vérifié dans SendGrid).")
                .showAndWait();
    }

    private void actualiser() {
        List<equipementwael> all = service.afficher();
        equipementList.clear();
        equipementList.addAll(all);
        appliquerFiltres();
        chargerDashAdmin();
    }

    /**
     * Appelé depuis MainController à chaque fois que l’onglet Équipements est
     * affiché.
     * Permet de garder cette vue synchronisée avec la Boutique (ajout /
     * modification / suppression).
     */
    public void rafraichirDepuisMain() {
        actualiser();
    }

    /**
     * Charge les infos groupes / messages + mini-stats circulaires.
     */
    private void chargerDashAdmin() {
        // Groupes récents
        if (groupsAdminList != null && groupeService != null) {
            List<GroupeChat> groupes = groupeService.afficher();
            groupsAdminList.setItems(FXCollections.observableArrayList(
                    groupes.stream().limit(5).collect(Collectors.toList())));
        }

        // Derniers messages tous groupes confondus
        if (messagesAdminList != null && messageService != null) {
            List<MessageChat> derniers = messageService.afficher()
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());
            messagesAdminList.setItems(FXCollections.observableArrayList(derniers));
        }

        // Stat 1 : PieChart groupe le plus actif
        if (chartGroupActif != null && statistiqueService != null) {
            chartGroupActif.getChildren().clear();
            List<Map.Entry<String, Double>> data = statistiqueService.getGroupesActifsPieData();
            if (!data.isEmpty()) {
                PieChart chart = new PieChart();
                chart.setLegendVisible(false);
                chart.setTitle("Groupes actifs");
                chart.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                String[] colors = { "#F97316", "#22c55e", "#3b82f6", "#94a3b8", "#e5e7eb" };
                for (Map.Entry<String, Double> e : data) {
                    chart.getData().add(
                            new PieChart.Data(e.getKey() + " " + String.format("%.0f%%", e.getValue()), e.getValue()));
                }
                chartGroupActif.getChildren().add(chart);
            }
        }

        // Stat 2 : PieChart équipement le plus vu (nombre_vues)
        if (chartEquipementVu != null && statistiqueService != null) {
            chartEquipementVu.getChildren().clear();
            List<Map.Entry<String, Number>> data = statistiqueService.getEquipementsPlusVusPieData();
            if (!data.isEmpty()) {
                PieChart chart = new PieChart();
                chart.setLegendVisible(false);
                chart.setTitle("Équip. plus vus");
                chart.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                for (Map.Entry<String, Number> e : data) {
                    double v = e.getValue().doubleValue();
                    if (v < 0)
                        v = 0;
                    chart.getData().add(new PieChart.Data(e.getKey(), v));
                }
                chartEquipementVu.getChildren().add(chart);
            }
        }
    }

    private ContextMenu createGroupesContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem edit = new MenuItem("Modifier");
        MenuItem delete = new MenuItem("Supprimer");
        edit.setOnAction(e -> {
            GroupeChat g = groupsAdminList.getSelectionModel().getSelectedItem();
            if (g != null)
                ouvrirDialogGroupe(g);
        });
        delete.setOnAction(e -> {
            GroupeChat g = groupsAdminList.getSelectionModel().getSelectedItem();
            if (g != null) {
                new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le groupe \"" + g.getNom() + "\" ?", ButtonType.OK,
                        ButtonType.CANCEL)
                        .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                            groupeService.supprimer(g.getId());
                            actualiser();
                        });
            }
        });
        menu.getItems().addAll(edit, delete);
        return menu;
    }

    private ContextMenu createMessagesContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem edit = new MenuItem("Modifier");
        MenuItem delete = new MenuItem("Supprimer");
        edit.setOnAction(e -> {
            MessageChat m = messagesAdminList.getSelectionModel().getSelectedItem();
            if (m != null)
                ouvrirDialogMessage(m);
        });
        delete.setOnAction(e -> {
            MessageChat m = messagesAdminList.getSelectionModel().getSelectedItem();
            if (m != null) {
                new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce message ?", ButtonType.OK, ButtonType.CANCEL)
                        .showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
                            messageService.supprimer(m.getId());
                            actualiser();
                        });
            }
        });
        menu.getItems().addAll(edit, delete);
        return menu;
    }

    private void ouvrirDialogGroupe(GroupeChat g) {
        Dialog<GroupeChat> d = new Dialog<>();
        d.setTitle("Modifier le groupe");
        d.setHeaderText(g.getNom());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(15));
        TextField nomF = new TextField(g.getNom());
        TextArea descF = new TextArea(g.getDescription() != null ? g.getDescription() : "");
        descF.setPrefRowCount(2);
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("PUBLIC", "PRIVATE");
        typeF.setValue(g.getType() != null ? g.getType() : "PUBLIC");
        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomF, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descF, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeF, 1, 2);
        d.getDialogPane().setContent(grid);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK)
                return null;
            GroupeChat updated = new GroupeChat();
            updated.setId(g.getId());
            updated.setNom(nomF.getText().trim());
            updated.setDescription(descF.getText().trim());
            updated.setType(typeF.getValue());
            return updated;
        });
        d.showAndWait().ifPresent(updated -> {
            groupeService.modifier(updated);
            actualiser();
        });
    }

    private void ouvrirDialogMessage(MessageChat m) {
        Dialog<MessageChat> d = new Dialog<>();
        d.setTitle("Modifier le message");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextArea contenuF = new TextArea(m.getContenu() != null ? m.getContenu() : "");
        contenuF.setPrefRowCount(4);
        contenuF.setPrefWidth(350);
        d.getDialogPane().setContent(contenuF);
        d.setResultConverter(btn -> {
            if (btn != ButtonType.OK)
                return null;
            MessageChat copy = new MessageChat();
            copy.setId(m.getId());
            copy.setContenu(contenuF.getText().trim());
            copy.setDateEnvoi(m.getDateEnvoi());
            copy.setIdGroupe(m.getIdGroupe());
            copy.setTypeMessage(m.getTypeMessage());
            copy.setFichierPath(m.getFichierPath());
            copy.setLatitude(m.getLatitude());
            copy.setLongitude(m.getLongitude());
            return copy;
        });
        d.showAndWait().ifPresent(updated -> {
            messageService.modifier(updated);
            actualiser();
        });
    }

    private void appliquerFiltres() {
        String search = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        // `statut` filter removed because the Equipement entity no longer exposes it

        List<equipementwael> filtered = equipementList.stream()
                .filter(e -> {
                    boolean matchSearch = search.isEmpty() ||
                            (e.getNom() != null && e.getNom().toLowerCase().contains(search)) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(search)) ||
                            (e.getCategorie() != null && e.getCategorie().toLowerCase().contains(search)) ||
                            (e.getVille() != null && e.getVille().toLowerCase().contains(search));
                    return matchSearch;
                })
                .collect(Collectors.toList());

        listView.setItems(FXCollections.observableArrayList(filtered));
        countLabel.setText(filtered.size() + " équipement(s)");
    }

    private void ouvrirFormulaire(equipementwael e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/wael/EquipementFormView.fxml"));
            Parent root = loader.load();
            EquipementFormController ctrl = loader.getController();
            ctrl.setEquipement(e);
            ctrl.setOnSaved(() -> actualiser());

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 650));
            stage.setTitle(e == null ? "Nouvel équipement" : "Modifier équipement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }
}
