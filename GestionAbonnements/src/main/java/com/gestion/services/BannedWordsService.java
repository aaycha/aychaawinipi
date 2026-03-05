package com.gestion.services;

import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service anti-gros mots : liste des mots interdits (table banned_words ou
 * liste par défaut).
 */
public class BannedWordsService {

    private final Connection cnx;
    private static final List<String> DEFAULT_BANNED = List.of(
            "merde", "putain", "connard", "idiot", "con", "salope", "enculé", "nique", "fuck", "shit");

    public BannedWordsService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public List<String> getAllBannedWords() {
        List<String> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery("SELECT mot FROM banned_words")) {
            while (rs.next()) {
                String m = rs.getString("mot");
                if (m != null && !m.trim().isEmpty())
                    list.add(m.trim().toLowerCase());
            }
        } catch (SQLException e) {
            return new ArrayList<>(DEFAULT_BANNED);
        }
        return list.isEmpty() ? new ArrayList<>(DEFAULT_BANNED) : list;
    }

    /**
     * Retourne true si le texte contient au moins un mot interdit (insensible à la
     * casse).
     */
    public boolean containsBannedWord(String text) {
        if (text == null || text.trim().isEmpty())
            return false;
        String lower = text.trim().toLowerCase();
        for (String word : getAllBannedWords()) {
            if (word != null && !word.isEmpty() && lower.contains(word))
                return true;
        }
        return false;
    }
}
