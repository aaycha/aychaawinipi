package com.gestion.tools;

import java.sql.Connection;
import java.sql.Statement;

public class DbFixer {
    public static void main(String[] args) {
        try (Connection conn = MyConnection.getConnectionStatic();
                Statement stmt = conn.createStatement()) {

            System.out.println("Applying fix: Setting EventSponsor.id to AUTO_INCREMENT...");
            stmt.execute("ALTER TABLE EventSponsor MODIFY id INT AUTO_INCREMENT;");
            System.out.println("Fix applied successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to apply fix. Maybe the table structure is different.");
        }
    }
}
