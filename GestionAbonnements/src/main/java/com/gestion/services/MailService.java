package com.gestion.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Service d'envoi d'emails via l'API SendGrid.
 * Clé API : variable d'environnement SENDGRID_API_KEY ou fichier
 * config/mail.properties (sendgrid.api.key).
 */
public class MailService {

    private static final String[] CONFIG_PATHS = {
            "config/mail.properties",
            "src/main/resources/config/mail.properties"
    };
    private final String apiKey;
    private String fromEmail;
    private String lastError;

    public MailService() {
        this.apiKey = loadApiKey();
    }

    private String loadApiKey() {
        String key = System.getenv("SENDGRID_API_KEY");
        if (key != null && !key.isBlank()) {
            loadFromEmailFromFile();
            return decryptKey(key.trim());
        }
        for (String path : CONFIG_PATHS) {
            File f = new File(path);
            if (f.exists()) {
                try (FileInputStream in = new FileInputStream(f)) {
                    Properties p = new Properties();
                    p.load(in);
                    key = p.getProperty("sendgrid.api.key");
                    if (key != null && !key.isBlank()) {
                        fromEmail = p.getProperty("sendgrid.from");
                        if (fromEmail != null)
                            fromEmail = fromEmail.trim();
                        return decryptKey(key.trim());
                    }
                    fromEmail = p.getProperty("sendgrid.from");
                    if (fromEmail != null)
                        fromEmail = fromEmail.trim();
                } catch (IOException e) {
                    System.err.println("Config mail " + path + ": " + e.getMessage());
                }
            }
        }
        try {
            java.io.InputStream in = getClass().getResourceAsStream("/config/mail.properties");
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                key = p.getProperty("sendgrid.api.key");
                if (key != null && !key.isBlank()) {
                    fromEmail = p.getProperty("sendgrid.from");
                    if (fromEmail != null)
                        fromEmail = fromEmail.trim();
                    return decryptKey(key.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Config mail (classpath): " + e.getMessage());
        }
        System.err.println("SendGrid: aucun config trouvé. Répertoire courant: " + System.getProperty("user.dir"));
        return null;
    }

    private void loadFromEmailFromFile() {
        for (String path : CONFIG_PATHS) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    try (FileInputStream in = new FileInputStream(f)) {
                        Properties p = new Properties();
                        p.load(in);
                        fromEmail = p.getProperty("sendgrid.from");
                        if (fromEmail != null)
                            fromEmail = fromEmail.trim();
                        return;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Déchiffre la clé : retire un éventuel préfixe "-" (clé collée avec tiret).
     */
    private String decryptKey(String raw) {
        if (raw == null)
            return null;
        String k = raw.trim();
        if (k.startsWith("-"))
            k = k.substring(1).trim();
        return k.isEmpty() ? null : k;
    }

    /**
     * Expéditeur par défaut (config sendgrid.from). Utilisé pour les notifications
     * automatiques.
     */
    public String getFromEmail() {
        return fromEmail != null && !fromEmail.isEmpty() ? fromEmail : null;
    }

    /** Permet de définir l'expéditeur à l'exécution (override). */
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail != null ? fromEmail.trim() : null;
    }

    /** Dernière erreur SendGrid (utile pour l'UI). */
    public String getLastError() {
        return lastError;
    }

    /**
     * Envoie un email via SendGrid.
     * 
     * @param fromEmail expéditeur (ex: noreply@votredomaine.com ou l'email vérifié
     *                  dans SendGrid)
     * @param toEmail   destinataire
     * @param subject   sujet
     * @param bodyHtml  corps du message (HTML) ou texte
     * @param isHtml    true si bodyHtml est du HTML
     * @return true si envoi réussi
     */
    public boolean send(String fromEmail, String toEmail, String subject, String bodyHtml, boolean isHtml) {
        lastError = null;
        if (apiKey == null || apiKey.isEmpty()) {
            lastError = "Clé API absente. Définir SENDGRID_API_KEY ou config/mail.properties (sendgrid.api.key).";
            System.err.println("❌ SendGrid: " + lastError);
            return false;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            lastError = "Expéditeur (sendgrid.from) manquant. Définir sendgrid.from avec un email vérifié SendGrid.";
            System.err.println("❌ SendGrid: " + lastError);
            return false;
        }
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(toEmail);
            Content content = new Content(isHtml ? "text/html" : "text/plain", bodyHtml != null ? bodyHtml : "");
            Mail mail = new Mail(from, subject, to, content);
            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✅ Email envoyé à " + toEmail);
                return true;
            }
            lastError = "SendGrid " + response.getStatusCode() + ": "
                    + (response.getBody() != null ? response.getBody() : "");
            System.err.println("❌ " + lastError);
            return false;
        } catch (IOException e) {
            lastError = "IOException: " + e.getMessage();
            System.err.println("❌ SendGrid: " + lastError);
            return false;
        }
    }

    /** Envoi simple en texte brut. */
    public boolean sendText(String fromEmail, String toEmail, String subject, String bodyText) {
        return send(fromEmail, toEmail, subject, bodyText, false);
    }

    /** Envoi en HTML. */
    public boolean sendHtml(String fromEmail, String toEmail, String subject, String bodyHtml) {
        return send(fromEmail, toEmail, subject, bodyHtml, true);
    }

    /** Vérifie si la clé API est configurée. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
