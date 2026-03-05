package controllers;

import com.gestion.services.CommentDAO;
import com.gestion.services.PostDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.gestion.entities.Comment;
import com.gestion.entities.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class leController {

    @FXML
    private TextField titleField;

    @FXML
    private void onRetour() {
        com.gestion.controllers.MainController main = com.gestion.controllers.MainController.getInstance();
        if (main != null) {
            main.retourDashboard();
        }
    }

    @FXML
    private TextArea contentArea;
    @FXML
    private VBox postsContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnDefault;
    @FXML
    private Button btnComments;
    @FXML
    private Button btnAlpha;
    @FXML
    private Button btnRecent;
    @FXML
    private Label bellLabel;

    private String currentSort = "Default";

    private final PostDAO postDAO = new PostDAO();
    private final CommentDAO commentDAO = new CommentDAO();

    private final List<String> notifications = new ArrayList<>();

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadPosts());
        updateBell();
        loadPosts();
    }

    // ─────────────────────────────
    // ADD POST
    // ─────────────────────────────

    @FXML
    private void handleAddPost() {

        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Erreur", "Le titre et le contenu ne peuvent pas être vides.");
            return;
        }

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);

        postDAO.add(post); // ✅ corrigé

        titleField.clear();
        contentArea.clear();

        loadPosts();
    }

    // ─────────────────────────────
    // SORT BUTTONS
    // ─────────────────────────────

    @FXML
    private void handleSortDefault() {
        currentSort = "Default";
        setActiveButton(btnDefault);
        loadPosts();
    }

    @FXML
    private void handleSortComments() {
        currentSort = "Most Comments";
        setActiveButton(btnComments);
        loadPosts();
    }

    @FXML
    private void handleSortAlpha() {
        currentSort = "Alphabetical";
        setActiveButton(btnAlpha);
        loadPosts();
    }

    @FXML
    private void handleSortRecent() {
        currentSort = "Recent";
        setActiveButton(btnRecent);
        loadPosts();
    }

    // ─────────────────────────────
    // NOTIFICATION BELL
    // ─────────────────────────────

    @FXML
    private void handleBellClick() {
        if (notifications.isEmpty()) {
            showAlert("Notifications", "Aucune notification.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String n : notifications) {
            sb.append("• ").append(n).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Liste des notifications");
        alert.setContentText(sb.toString());
        alert.showAndWait();

        notifications.clear();
        updateBell();
    }

    // ─────────────────────────────
    // ACTIVE BUTTON STYLE
    // ─────────────────────────────

    private void setActiveButton(Button active) {
        btnDefault.getStyleClass().removeAll("filter-btn-active", "filter-btn-glass");
        btnComments.getStyleClass().removeAll("filter-btn-active", "filter-btn-glass");
        btnAlpha.getStyleClass().removeAll("filter-btn-active", "filter-btn-glass");
        btnRecent.getStyleClass().removeAll("filter-btn-active", "filter-btn-glass");

        btnDefault.getStyleClass().add("filter-btn-glass");
        btnComments.getStyleClass().add("filter-btn-glass");
        btnAlpha.getStyleClass().add("filter-btn-glass");
        btnRecent.getStyleClass().add("filter-btn-glass");

        active.getStyleClass().remove("filter-btn-glass");
        active.getStyleClass().add("filter-btn-active");
    }

    // ─────────────────────────────
    // LOAD POSTS
    // ─────────────────────────────

    private void loadPosts() {

        postsContainer.getChildren().clear();

        List<Post> posts = postDAO.getAll(); // ✅ corrigé

        String search = searchField.getText().toLowerCase().trim();

        if (!search.isEmpty()) {
            posts = posts.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }

        if ("Alphabetical".equals(currentSort)) {
            posts.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        } else if ("Most Comments".equals(currentSort)) {
            posts.sort((a, b) -> {
                int countA = commentDAO.getByPostId(a.getId()).size();
                int countB = commentDAO.getByPostId(b.getId()).size();
                return Integer.compare(countB, countA);
            });
        } else if ("Recent".equals(currentSort)) {
            posts.sort((a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null)
                    return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }

        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    // ─────────────────────────────
    // CREATE POST CARD
    // ─────────────────────────────

    private VBox createPostCard(Post post) {

        VBox box = new VBox(10);
        box.getStyleClass().add("post-card");

        Label title = new Label(post.getTitle());
        title.getStyleClass().add("post-title");

        Label content = new Label(post.getContent());
        content.setWrapText(true);
        content.getStyleClass().add("post-content");

        Label meta = new Label("Posté • " + timeAgo(post.getCreatedAt()));
        meta.getStyleClass().add("post-meta");

        // EDIT POST
        Button editBtn = new Button("✏");
        editBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(post.getTitle());
            dialog.setHeaderText("Modifier le titre");
            dialog.showAndWait().ifPresent(newTitle -> {

                TextInputDialog contentDialog = new TextInputDialog(post.getContent());
                contentDialog.setHeaderText("Modifier le contenu");
                contentDialog.showAndWait().ifPresent(newContent -> {

                    postDAO.update(post.getId(), newTitle, newContent); // ✅ corrigé
                    loadPosts();
                });
            });
        });

        // DELETE POST
        Button deleteBtn = new Button("🗑");
        deleteBtn.setOnAction(e -> {
            postDAO.delete(post.getId()); // ✅ corrigé
            loadPosts();
        });

        HBox actions = new HBox(5, editBtn, deleteBtn);

        // COMMENTS
        VBox commentsBox = new VBox(5);
        List<Comment> comments = commentDAO.getByPostId(post.getId()); // ✅ corrigé

        for (Comment c : comments) {

            HBox row = new HBox(5);

            Label commentLabel = new Label(c.getContent());
            commentLabel.setWrapText(true);
            HBox.setHgrow(commentLabel, Priority.ALWAYS);

            Button editComment = new Button("✏");
            editComment.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog(c.getContent());
                dialog.setHeaderText("Modifier commentaire");
                dialog.showAndWait().ifPresent(newText -> {
                    commentDAO.update(c.getId(), newText); // ✅ corrigé
                    loadPosts();
                });
            });

            Button deleteComment = new Button("✕");
            deleteComment.setOnAction(ev -> {
                commentDAO.delete(c.getId()); // ✅ corrigé
                loadPosts();
            });

            row.getChildren().addAll(commentLabel, editComment, deleteComment);
            commentsBox.getChildren().add(row);
        }

        // ADD COMMENT
        HBox addBox = new HBox(5);

        TextField commentField = new TextField();
        commentField.setPromptText("Écrire un commentaire...");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button addCommentBtn = new Button("Commenter");
        addCommentBtn.setOnAction(e -> {

            if (commentField.getText().trim().isEmpty()) {
                showAlert("Erreur", "Commentaire vide.");
                return;
            }

            Comment c = new Comment();
            c.setContent(commentField.getText());
            c.setPostId(post.getId());

            commentDAO.add(c); // ✅ corrigé

            notifications.add("Nouveau commentaire sur : " + post.getTitle());
            updateBell();

            loadPosts();
        });

        addBox.getChildren().addAll(commentField, addCommentBtn);

        box.getChildren().addAll(meta, title, content, actions, commentsBox, addBox);

        return box;
    }

    // ─────────────────────────────

    private String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null)
            return "";
        long minutes = java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes < 60)
            return minutes + " min";
        long hours = minutes / 60;
        if (hours < 24)
            return hours + " h";
        long days = hours / 24;
        return days + " j";
    }

    private void updateBell() {
        int count = notifications.size();
        bellLabel.setText(count > 0 ? "🔔 " + count : "🔔");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}