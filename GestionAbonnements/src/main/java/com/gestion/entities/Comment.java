

package com.gestion.entities;

public class Comment {
    private int id;
    private String content;
    private int postId;

    public Comment() {
    }

    public Comment(int id, String content, int postId) {
        this.id = id;
        this.content = content;
        this.postId = postId;
    }

    public int getId() {
        return this.id;
    }

    public String getContent() {
        return this.content;
    }

    public int getPostId() {
        return this.postId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }
}
