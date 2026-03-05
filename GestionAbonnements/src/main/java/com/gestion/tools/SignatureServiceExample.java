package com.gestion.tools;

import java.io.IOException;

/**
 * Exemple d'utilisation de SignatureService pour envoyer des contrats PDF à DocuSign.
 *
 * À exécuter après avoir configuré la variable d'environnement DOCUSIGN_API_KEY
 */
public class SignatureServiceExample {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("   Exemple d'utilisation de SignatureService");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");

        // ========== ÉTAPE 1 : Initialiser le service ==========
        System.out.println("1️⃣  Initialisation du SignatureService...");
        SignatureService signatureService;
        try {
            signatureService = new SignatureService();
            System.out.println("   ✓ Service prêt\n");
        } catch (IOException e) {
            System.err.println("   ✗ Erreur I/O : " + e.getMessage());
            e.printStackTrace();
            return;
        } catch (IllegalStateException e) {
            System.err.println("   ✗ Erreur : " + e.getMessage());
            System.err.println("   Assure-toi que DOCUSIGN_API_KEY est défini !");
            return;
        }

        // ========== ÉTAPE 2 : Générer un contrat PDF ==========
        System.out.println("2️⃣  Génération d'un contrat PDF exemple...");
        byte[] pdfBytes;
        try {
            pdfBytes = generateSamplePdf();
        } catch (IOException e) {
            System.err.println("   ✗ Erreur I/O lors de la génération du PDF : " + e.getMessage());
            return;
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            System.err.println("   ✗ Impossible de générer le PDF");
            return;
        }
        System.out.println("   ✓ PDF généré (" + pdfBytes.length + " bytes)\n");

        // ========== ÉTAPE 3 : Envoyer le contrat pour signature ==========
        System.out.println("3️⃣  Envoi du contrat pour signature électronique...");
        String sponsorEmail = "test.sponsor@example.com";
        String sponsorName = "ACME Corporation";

        try {
            SignatureService.SignatureResponse response = signatureService.sendForSignature(
                    pdfBytes,
                    sponsorEmail,
                    sponsorName
            );
            System.out.println("   ✓ Contrat envoyé avec succès !");
            System.out.println("\n   📋 Détails de la réponse :");
            System.out.println("   - Envelope ID : " + response.envelopeId);
            System.out.println("   - Email sponsor : " + response.sponsorEmail);
            System.out.println("   - Status : " + response.status);
            System.out.println("   - URL signature : " + response.signingUrl);
            System.out.println();

        } catch (IOException e) {
            System.err.println("   ✗ Erreur réseau ou traitement PDF : " + e.getMessage());
            System.err.println("   Détail : " + e);
        } catch (IllegalArgumentException e) {
            System.err.println("   ✗ Erreur validation : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("   ✗ Erreur inattendue : " + e.getMessage());
            System.err.println("   Détail : " + e);
        }

        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("   ✓ Exemple d'utilisation terminé");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Génère un PDF exemple (simple texte).
     * À remplacer par un vrai contrat généré via EmailService.genererContratPdf()
     */
    private static byte[] generateSamplePdf() throws IOException {
        try {
            // Utilise la méthode de EmailService si elle est publique
            // Sinon crée un PDF simple pour l'exemple
            return createSimplePdfBytes();
        } catch (Exception e) {
            System.err.println("   Erreur : " + e.getMessage());
            return null;
        }
    }

    /**
     * Crée un PDF très simple pour l'exemple (en cas où EmailService.genererContratPdf n'est pas accessible)
     */
    private static byte[] createSimplePdfBytes() {
        // Utilisé uniquement pour l'exemple - version simplifié
        String content = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /Resources 4 0 R /MediaBox [0 0 612 792] /Contents 5 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >>\n" +
                "endobj\n" +
                "5 0 obj\n" +
                "<< /Length 44 >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(Exemple de contrat) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f\n" +
                "0000000009 00000 n\n" +
                "0000000058 00000 n\n" +
                "0000000115 00000 n\n" +
                "0000000214 00000 n\n" +
                "0000000333 00000 n\n" +
                "trailer\n" +
                "<< /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "428\n" +
                "%%EOF\n";

        return content.getBytes();
    }
}

