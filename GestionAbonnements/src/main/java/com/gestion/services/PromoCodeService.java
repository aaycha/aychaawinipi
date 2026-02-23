package com.gestion.services;

import com.gestion.entities.PromoCode;
import com.gestion.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PromoCodeService {

    public void create(PromoCode promo) throws SQLException {
        String sql = "INSERT INTO promo_codes (code, discount_percentage, expiration_date, active, usage_limit, current_usage) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, promo.getCode());
        ps.setInt(2, promo.getDiscountPercentage());
        ps.setDate(3, Date.valueOf(promo.getExpirationDate()));
        ps.setBoolean(4, promo.isActive());
        ps.setInt(5, promo.getUsageLimit());
        ps.setInt(6, promo.getCurrentUsage());
        ps.executeUpdate();
    }

    public PromoCode getByCode(String code) throws SQLException {
        String sql = "SELECT * FROM promo_codes WHERE code = ? AND active = true";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, code);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            PromoCode p = new PromoCode();
            p.setId(rs.getLong("id"));
            p.setCode(rs.getString("code"));
            p.setDiscountPercentage(rs.getInt("discount_percentage"));
            p.setExpirationDate(rs.getDate("expiration_date").toLocalDate());
            p.setActive(rs.getBoolean("active"));
            p.setUsageLimit(rs.getInt("usage_limit"));
            p.setCurrentUsage(rs.getInt("current_usage"));
            return p;
        }
        return null;
    }

    public void incrementUsage(Long id) throws SQLException {
        String sql = "UPDATE promo_codes SET current_usage = current_usage + 1 WHERE id = ?";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public List<PromoCode> getAll() throws SQLException {
        List<PromoCode> list = new ArrayList<>();
        String sql = "SELECT * FROM promo_codes ORDER BY id DESC";
        Connection conn = MyConnection.getConnectionStatic();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            PromoCode p = new PromoCode();
            p.setId(rs.getLong("id"));
            p.setCode(rs.getString("code"));
            p.setDiscountPercentage(rs.getInt("discount_percentage"));
            p.setExpirationDate(rs.getDate("expiration_date").toLocalDate());
            p.setActive(rs.getBoolean("active"));
            p.setUsageLimit(rs.getInt("usage_limit"));
            p.setCurrentUsage(rs.getInt("current_usage"));
            list.add(p);
        }
        return list;
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM promo_codes WHERE id = ?";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public void update(PromoCode promo) throws SQLException {
        String sql = "UPDATE promo_codes SET code=?, discount_percentage=?, expiration_date=?, active=?, usage_limit=?, current_usage=? WHERE id=?";
        Connection conn = MyConnection.getConnectionStatic();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, promo.getCode());
        ps.setInt(2, promo.getDiscountPercentage());
        ps.setDate(3, Date.valueOf(promo.getExpirationDate()));
        ps.setBoolean(4, promo.isActive());
        ps.setInt(5, promo.getUsageLimit());
        ps.setInt(6, promo.getCurrentUsage());
        ps.setLong(7, promo.getId());
        ps.executeUpdate();
    }
}
