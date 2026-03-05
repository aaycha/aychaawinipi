/**
 * 📋 INTEGRATION EXAMPLE: Ajouter la signature électronique DocuSign à EmailService
 *
 * Copie-colle le code ci-dessous dans EmailService.sendWelcomeEmailWithContract()
 * APRÈS l'envoi du mail SendGrid.
 *
 * Cela va :
 * 1. Envoyer le contrat PDF à DocuSign
 * 2. Récupérer l'URL de signature
 * 3. Afficher l'information
 */

// ============ À AJOUTER dans EmailService.sendWelcomeEmailWithContract() ============
// (Après la ligne: response = sg.api(request);)

public static void sendWelcomeEmailWithContract(int sponsorId, String toEmail, String sponsorName) {
    // ... EXISTING CODE ...

    try {
        // ... SENDGRID MAIL SENDING CODE ...
        Response response = sg.api(request);
        System.out.println("Email SendGrid status: " + response.getStatusCode());

        // ============ AJOUTER À PARTIR D'ICI ============

        // 📨 Optionnel : Envoyer le contrat pour signature électronique via DocuSign
        // (ne pas oublier d'importer: import utils.SignatureService;)
        if (pdfBytes != null && pdfBytes.length > 0) {
            try {
                SignatureService signatureService = new SignatureService();

                // Envoyer pour signature électronique
                SignatureService.SignatureResponse sigResponse =
                    signatureService.sendForSignature(pdfBytes, toEmail, sponsorName);

                // Log du succès
                System.out.println("✓ Contrat envoyé pour signature électronique !");
                System.out.println("  Envelope ID : " + sigResponse.envelopeId);
                System.out.println("  Email sponsor : " + sigResponse.sponsorEmail);
                System.out.println("  Status : " + sigResponse.status);
                System.out.println("  URL de signature : " + sigResponse.signingUrl);

                // OPTIONNEL: Sauvegarder en base de données pour suivi
                // Exemple (adapter selon ta structure DAO):
                // SponsorDAO dao = new SponsorDAO();
                // dao.updateDocuSignEnvelopeId(sponsorId, sigResponse.envelopeId);

            } catch (IllegalStateException e) {
                // DOCUSIGN_API_KEY non défini -> affiche conseil
                System.err.println("⚠️ Signature électronique non disponible : " + e.getMessage());
                System.err.println("   Astuce : Configure la variable d'environnement DOCUSIGN_API_KEY");
            } catch (IOException e) {
                // Erreur réseau ou PDF
                System.err.println("⚠️ Erreur lors de l'envoi pour signature : " + e.getMessage());
                System.err.println("   Le mail a été envoyé, mais la signature électronique a échoué.");
            } catch (IllegalArgumentException e) {
                // Paramètres invalides (email vide, PDF vide, etc.)
                System.err.println("⚠️ Paramètres invalides pour la signature : " + e.getMessage());
            } catch (Exception e) {
                // Erreur inattendue
                System.err.println("⚠️ Erreur inattendue lors de la signature : " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ============ FIN DE L'AJOUT ============

    } catch (IOException e) {
        System.err.println("Erreur lors de l'envoi de l'e-mail: " + e.getMessage());
    }
}

// ============ IMPORT À AJOUTER EN HAUT DE EMAILSERVICE.JAVA ============
// import utils.SignatureService;
// import utils.SignatureService.SignatureResponse;

// ============ NOTES D'IMPLÉMENTATION ============
/**
 * 🎯 Avantages de cette intégration :
 *
 * 1. ✅ Double envoi :
 *    - Mail SendGrid avec PDF attaché (pour archivage)
 *    - Lien DocuSign pour signature en ligne (signer immédiatement)
 *
 * 2. ✅ Sécurité :
 *    - Pas de clé API en dur (utilise variables d'environnement)
 *    - Gestion d'erreurs complète (mail envoyé même si signature échoue)
 *
 * 3. ✅ Flexibilité :
 *    - Mode MOCK pour développement (pas besoin de credentials)
 *    - Bascule facile en mode RÉEL (credentials JWT)
 *
 * 4. ✅ Suivi :
 *    - envelopeId peut être stocké en base de données
 *    - Permet de récupérer le statut de signature plus tard
 *
 * ============ OPTIONNEL: SAUVEGARDER ENVELOPPE ID ============
 *
 * Pour ajouter le suivi des signatures, tu peux :
 *
 * 1. Ajouter une colonne en base : ALTER TABLE sponsor ADD COLUMN docusign_envelope_id VARCHAR(255);
 *
 * 2. Créer une DAO pour sauvegarder :
 *    public void updateDocuSignEnvelopeId(int sponsorId, String envelopeId) {
 *        String query = "UPDATE sponsor SET docusign_envelope_id = ? WHERE id = ?";
 *        // Exécute la requête...
 *    }
 *
 * 3. Appeler dans sendWelcomeEmailWithContract() :
 *    if (sponsorId > 0 && sigResponse != null) {
 *        new SponsorDAO().updateDocuSignEnvelopeId(sponsorId, sigResponse.envelopeId);
 *    }
 *
 * ============ TESTER L'INTÉGRATION ============
 *
 * 1. Lance l'application
 * 2. Crée un nouveau sponsor
 * 3. Vérifie les logs :
 *    ✓ Email SendGrid status: 202
 *    ✓ Contrat envoyé pour signature électronique !
 *    ✓ Envelope ID : <uuid>
 *    ✓ URL de signature : https://demo.docusign.net/signing/?...
 *
 * 4. En mode MOCK : Les URLs sont factices (pour dev)
 *    En mode RÉEL : Les URLs sont authentifiées (vers vraie API DocuSign)
 *
 * ============ DÉBOGAGE ============
 *
 * Si tu vois "DOCUSIGN_API_KEY non défini" :
 *   → Redémarre IntelliJ
 *   → Vérifies .idea/runConfigurations/Main.xml
 *   → Ou configure système : setx DOCUSIGN_API_KEY "..."
 *
 * Si tu vois "IOException" :
 *   → Vérifies que pdfBytes n'est pas null/vide
 *   → Vérifies que toEmail est une adresse valide
 *   → Vérifies que sponsorName n'est pas vide
 */

