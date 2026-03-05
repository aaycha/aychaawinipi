package com.gestion.entities;

public class OptionSondage {
    private int id;
    private int messageId;
    private String optionText;
    private int votes;

    public OptionSondage() {}

    public OptionSondage(int messageId, String optionText) {
        this.messageId = messageId;
        this.optionText = optionText;
        this.votes = 0;
    }

    public OptionSondage(int id, int messageId, String optionText, int votes) {
        this.id = id;
        this.messageId = messageId;
        this.optionText = optionText;
        this.votes = votes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    @Override
    public String toString() {
        return "OptionSondage{" +
                "id=" + id +
                ", messageId=" + messageId +
                ", optionText='" + optionText + '\'' +
                ", votes=" + votes +
                '}';
    }
}
