package com.gestion.services;

import com.gestion.entities.MealTicket;
import com.gestion.entities.Ingredient;
import com.gestion.interfaces.IngredientService;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MealTicketService {
    private final IngredientService ingredientService = new IngredientServiceImpl();

    /**
     * Generates a new meal ticket for a participation.
     * Enforces 1 meal per slot.
     */
    public MealTicket generateTicket(Long participationId, Long userId, LocalDateTime slot) throws SQLException {
        if (hasAlreadyTakenMeal(userId, slot)) {
            throw new SQLException("L'utilisateur a déjà pris un repas pour ce créneau horaire.");
        }

        String qr = "MEAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MealTicket ticket = new MealTicket(participationId, userId, qr, slot);

        String sql = "INSERT INTO meal_tickets (participation_id, user_id, qr_code, time_slot, used) VALUES (?, ?, ?, ?, ?)";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, ticket.getParticipationId());
        ps.setLong(2, ticket.getUserId());
        ps.setString(3, ticket.getQrCode());
        ps.setTimestamp(4, Timestamp.valueOf(ticket.getTimeSlot()));
        ps.setBoolean(5, false);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next())
            ticket.setId(rs.getLong(1));
        return ticket;
    }

    public boolean hasAlreadyTakenMeal(Long userId, LocalDateTime slot) throws SQLException {
        String sql = "SELECT 1 FROM meal_tickets WHERE user_id = ? AND time_slot = ? AND used = true";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, userId);
        ps.setTimestamp(2, Timestamp.valueOf(slot));
        return ps.executeQuery().next();
    }

    /**
     * Scans and consumes a meal ticket.
     * Deducts ingredients from stock.
     */
    public void consumeTicket(String qrCode) throws SQLException {
        String sql = "SELECT * FROM meal_tickets WHERE qr_code = ? AND used = false";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, qrCode);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Long id = rs.getLong("id");
            // Mark as used
            String updateSql = "UPDATE meal_tickets SET used = true, used_at = ? WHERE id = ?";
            PreparedStatement ups = conn.prepareStatement(updateSql);
            ups.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ups.setLong(2, id);
            ups.executeUpdate();

            // Deduct stock (Static deduction for demo, normally linked to Repas)
            deductDefaultIngredients();
        } else {
            throw new SQLException("Ticket invalide ou déjà utilisé.");
        }
    }

    private void deductDefaultIngredients() {
        try {
            // Deduct 1 unit from popular ingredients
            List<Ingredient> ingredients = ingredientService.findAll();
            for (Ingredient ing : ingredients) {
                if (ing.isActif() && ing.getStockQuantite() > 0) {
                    ing.setStockQuantite(ing.getStockQuantite() - 1);
                    ingredientService.update(ing);

                    // Alert if below threshold
                    if (ing.estSousSeuil()) {
                        // Send Admin Notification (Logic simplified here)
                        System.out.println("ALERTE STOCK BAS: " + ing.getNom());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
