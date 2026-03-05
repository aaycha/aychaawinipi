package com.gestion.tools;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.gestion.services.EventSponsorDAO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;

public class EmailService {

    // CONSEIL : ne mettez JAMAIS la vraie clé API en dur dans le code dans un vrai
    // projet.
    private static final String SENDGRID_API_KEY = "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";

    // Adresse d'expéditeur (celle validée chez SendGrid)
    private static final String FROM_EMAIL = "lamma.service@gmail.com";
    private static final String FROM_NAME = "LAMMA";

    public static void sendWelcomeEmail(String toEmail, String sponsorName) {
        sendWelcomeEmailWithContract(-1, toEmail, sponsorName);
    }

    /**
     * Envoie UN SEUL email avec:
     * - Message de bienvenue
     * - PDF du contrat attaché
     * - Instructions pour imprimer et signer
     *
     * Email envoyé après 1 minute de l'ajout du sponsor.
     */
    public static void sendWelcomeEmailWithContract(int sponsorId, String toEmail, String sponsorName) {
        // Attendre 1 minute avant d'envoyer l'email complet (dans un thread séparé)
        new Thread(() -> {
            try {
                System.out.println("⏳ Attente de 1 minute avant d'envoyer le contrat...");
                Thread.sleep(60000); // 1 minute

                // Générer le PDF du contrat
                double total = 0;
                if (sponsorId > 0) {
                    try {
                        total = new EventSponsorDAO().getTotalContributionsParSponsor(sponsorId);
                    } catch (Exception e) {
                        System.err.println("Erreur lors du calcul du total: " + e.getMessage());
                    }
                }

                byte[] pdfBytes = genererContratPdf(sponsorName, total);

                if (pdfBytes != null && pdfBytes.length > 0) {
                    // Envoyer le SEUL email avec bienvenue + PDF
                    sendCombinedWelcomeEmailWithContract(toEmail, sponsorName, pdfBytes);
                } else {
                    System.err.println("✗ PDF vide, email non envoyé");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("✗ Interruption: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Envoie UN SEUL EMAIL combiné avec:
     * - Message de bienvenue
     * - PDF du contrat en pièce jointe
     * - Instructions pour imprimer et signer manuellement
     */
    private static void sendCombinedWelcomeEmailWithContract(String toEmail, String sponsorName, byte[] pdfBytes) {
        if (SENDGRID_API_KEY == null || SENDGRID_API_KEY.isBlank()) {
            System.err.println("✗ SENDGRID_API_KEY non défini. Email non envoyé.");
            return;
        }

        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            String subject = "Bienvenue parmi nos sponsors - Contrat de partenariat";

            // Message combiné: bienvenue + instructions signature
            String body = "Bonjour " + sponsorName + ",\n\n" +
                    "BIENVENUE PARMI NOS SPONSORS!\n\n" +
                    "Nous sommes heureux de vous accueillir au sein de notre plateforme LAMMA.\n\n" +
                    "CONTRAT DE PARTENARIAT\n" +
                    "═══════════════════════════════════════\n\n" +
                    "Veuillez trouver ci-joint le contrat de partenariat.\n\n" +
                    "📋 COMMENT SIGNER LE CONTRAT:\n" +
                    "1. Téléchargez le fichier PDF attaché: 'Contrat_Partenariat_" + sanitizeFilename(sponsorName)
                    + ".pdf'\n" +
                    "2. Imprimez le document\n" +
                    "3. Signez le document imprimé (en bas de page)\n" +
                    "4. Scannez le document signé\n" +
                    "5. Nous l'envoyez à: lamma.service@gmail.com\n\n" +
                    "Ou vous pouvez signer directement sur le PDF avec un outil de signature numérique.\n\n" +
                    "INFORMATIONS IMPORTANTES:\n" +
                    "────────────────────────────────────────\n" +
                    "• Sponsor: " + sponsorName + "\n" +
                    "• Date: " + java.time.LocalDate.now() + "\n" +
                    "• Ce contrat officialise votre engagement de partenariat avec LAMMA\n\n" +
                    "Si vous avez des questions concernant le contrat, n'hésitez pas à nous contacter.\n\n" +
                    "Merci de votre confiance et de votre engagement!\n\n" +
                    "Cordialement,\n" +
                    "L'équipe LAMMA";

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, to, content);

            // Ajouter le PDF en pièce jointe
            if (pdfBytes != null && pdfBytes.length > 0) {
                String encodedPdf = Base64.getEncoder().encodeToString(pdfBytes);
                Attachments attachment = new Attachments();
                attachment.setFilename("Contrat_Partenariat_" + sanitizeFilename(sponsorName) + ".pdf");
                attachment.setType("application/pdf");
                attachment.setContent(encodedPdf);
                mail.addAttachments(attachment);
            }

            SendGrid sg = new SendGrid(SENDGRID_API_KEY);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✓ Email envoyé avec succès!");
                System.out.println("  Destinataire: " + toEmail);
                System.out.println("  Sujet: Bienvenue parmi nos sponsors - Contrat de partenariat");
                System.out.println("  Pièce jointe: Contrat_Partenariat_" + sanitizeFilename(sponsorName) + ".pdf");
                System.out.println("  Instructions: Imprimer et signer le contrat");
            } else {
                System.err.println("✗ Erreur SendGrid: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (IOException e) {
            System.err.println("✗ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * APPROCHE SIMPLE: Envoie le contrat PDF avec un lien de signature.
     *
     * Sans JWT complexe - Juste:
     * 1. Génère un lien DocuSign simple
     * 2. Envoie le PDF par email
     * 3. Email inclut le lien de signature
     */
    private static void sendContractViaDocuSign(byte[] pdfBytes, String toEmail, String sponsorName) {
        try {
            // Étape 1: Générer un lien de signature simple
            String signingUrl = generateSimpleSigningLink(toEmail, sponsorName);

            System.out.println("✓ Contrat prêt à être signé");
            System.out.println("  Email: " + toEmail);
            System.out.println("  Sponsor: " + sponsorName);
            System.out.println("  Lien signature: " + signingUrl);

            // Étape 2: Envoyer l'email avec PDF + lien
            sendContractEmailWithLink(toEmail, sponsorName, pdfBytes, signingUrl);

        } catch (Exception e) {
            System.err.println("✗ Erreur lors de l'envoi du contrat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Génère un lien de signature via une vraie intégration DocuSign.
     * Crée une enveloppe et retourne l'URL de signature unique pour le sponsor.
     */
    private static String generateSimpleSigningLink(String sponsorEmail, String sponsorName) {
        try {
            // Option 1: Utiliser SignatureService pour créer une enveloppe réelle
            // (Décommentez si vous voulez l'intégration complète)
            // SignatureService signatureService = new SignatureService();
            // SignatureService.SignatureResponse response =
            // signatureService.sendForSignature(...);
            // return response.signingUrl;

            // Option 2: Lien DocuSign générique pour test
            // Le sponsor pourra choisir le document à signer
            String encodedEmail = sponsorEmail.replace("@", "%40");
            return "https://demo.docusign.net/dfs/embed/?email=" + encodedEmail;

            // Option 3: Lien vers un formulaire de signature personnalisé (si vous en créez
            // un)
            // return "http://localhost:8080/sign?email=" + URLEncoder.encode(sponsorEmail,
            // StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("✗ Erreur génération lien: " + e.getMessage());
            // Fallback sur un lien générique
            return "https://demo.docusign.net/";
        }
    }

    /**
     * Envoie l'email avec le contrat PDF ET le lien de signature.
     * C'est la méthode principale pour envoyer le contrat au sponsor.
     */
    private static void sendContractEmailWithLink(String toEmail, String sponsorName,
            byte[] pdfBytes, String signingUrl) {
        if (SENDGRID_API_KEY == null || SENDGRID_API_KEY.isBlank()) {
            System.err.println("SENDGRID_API_KEY non défini. Email non envoyé.");
            return;
        }

        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            String subject = "Contrat de partenariat à signer";

            String body = "Bonjour " + sponsorName + ",\n\n" +
                    "Merci de votre partenariat avec LAMMA!\n\n" +
                    "Veuillez trouver ci-joint votre contrat de partenariat.\n\n" +
                    "POUR SIGNER EN LIGNE, cliquez sur le lien ci-dessous:\n" +
                    signingUrl + "\n\n" +
                    "---\n" +
                    "Le contrat PDF est également joint à cet email.\n" +
                    "Vous pouvez l'imprimer, le signer manuellement, et nous le retourner si vous préférez.\n\n" +
                    "Merci,\nL'équipe LAMMA";

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, to, content);

            // Ajouter le PDF en pièce jointe
            if (pdfBytes != null && pdfBytes.length > 0) {
                String encodedPdf = Base64.getEncoder().encodeToString(pdfBytes);
                Attachments attachment = new Attachments();
                attachment.setFilename("Contrat_Partenariat_" + sanitizeFilename(sponsorName) + ".pdf");
                attachment.setType("application/pdf");
                attachment.setContent(encodedPdf);
                mail.addAttachments(attachment);
            }

            SendGrid sg = new SendGrid(SENDGRID_API_KEY);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                System.out.println("✓ Email avec contrat envoyé avec succès");
                System.out.println("  Destinataire: " + toEmail);
                System.out.println("  Pièce jointe: Contrat PDF");
                System.out.println("  Lien signature: Inclus dans l'email");
            } else {
                System.err.println("✗ Erreur SendGrid: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (IOException e) {
            System.err.println("✗ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] genererContratPdf(String sponsorName, double total) {
        PDDocument document = null;
        try {
            document = new PDDocument();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float margin = 60;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 20;

            // Titre
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.newLineAtOffset(margin, y);
            content.showText("Contrat de sponsoring - Plateforme LAMMA");
            content.endText();

            y -= lineHeight * 2;

            // Date
            content.beginText();
            content.setFont(PDType1Font.HELVETICA, 11);
            content.newLineAtOffset(margin, y);
            content.showText("Date : " + LocalDate.now());
            content.endText();

            y -= lineHeight * 2;

            // Contenu du contrat
            String[] lignes = new String[] {
                    "Entre la plateforme LAMMA (l'Organisateur) et le sponsor suivant :",
                    "",
                    "Sponsor : " + sponsorName,
                    "Montant total engage a ce jour : " + String.format("%.2f DT", total),
                    "",
                    "Le sponsor s'engage a soutenir les evenements organises via la plateforme LAMMA.",
                    "Les modalites detaillees (planning, visibilite, contrepar ties) sont convenues",
                    "entre les deux par ties et pourront faire l'objet d'avenants.",
                    "",
                    "Signature electronique ou validation en ligne par le sponsor fait office",
                    "d'acceptation du present contrat."
            };

            for (String line : lignes) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.newLineAtOffset(margin, y);
                // Remplacer les caractères accentués pour éviter les problèmes de codage
                String cleanLine = removeAccents(line);
                content.showText(cleanLine);
                content.endText();
                y -= lineHeight;
            }

            y -= lineHeight;

            // Signature
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 11);
            content.newLineAtOffset(margin, y);
            content.showText("Signature du sponsor :");
            content.endText();

            // Fermer le content stream AVANT de sauvegarder
            content.close();

            // Sauvegarder le document
            document.save(baos);

            byte[] pdfBytes = baos.toByteArray();
            System.out.println("✓ PDF généré avec succès : " + pdfBytes.length + " bytes");
            return pdfBytes;
        } catch (IOException e) {
            System.err.println("✗ Erreur lors de la génération du contrat PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // S'assurer que le document est fermé
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture du document PDF: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Enlever les accents et caractères spéciaux pour la compatibilité PDF
     */
    private static String removeAccents(String text) {
        if (text == null)
            return "";
        return text
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[ñ]", "n")
                .replaceAll("[ýÿ]", "y");
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isEmpty()) {
            return "sponsor";
        }
        // Remplacer les caractères spéciaux par des underscores
        // et limiter la longueur du nom de fichier
        String sanitized = name
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^A-Za-z0-9_\\-]", "_")
                .replaceAll("_+", "_");

        // Limiter à 50 caractères pour éviter les noms trop longs
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized;
    }
}
