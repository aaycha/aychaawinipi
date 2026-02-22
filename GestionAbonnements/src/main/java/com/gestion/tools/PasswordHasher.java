package com.gestion.tools;
import org.mindrot.jbcrypt.BCrypt;
public class PasswordHasher {
    private static final int LOG_ROUNDS = 12;

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean needsRehash(String hashedPassword) {
        try {
            String salt = hashedPassword.substring(0, 29);
            int currentRounds = Integer.parseInt(salt.substring(4, 6));
            return currentRounds < LOG_ROUNDS;
        } catch (Exception e) {
            return true;
        }
}
}
