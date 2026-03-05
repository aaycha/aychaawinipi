package com.gestion.tools;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service pour envoyer les contrats PDF aux sponsors via DocuSign pour signature électronique.
 * Utilise l'authentification JWT réelle avec la clé privée RSA.
 */
public class SignatureService {

    // ============ Configuration DocuSign Sandbox ============
    private static final String DOCUSIGN_DEMO_BASE_URL = "https://demo.docusign.net/restapi/v2.1";
    private static final String DOCUSIGN_AUTH_URL = "https://account-d.docusign.com/oauth/token";

    // ============ Configuration JWT - CREDENTIALS RÉELS ============
    private static final String INTEGRATION_KEY = "dae549e4-1e2a-4e31-b0f9-7c5df52ae32d";
    private static final String DOCUSIGN_USER_ID = "a3bccbba-ee77-4897-a0bf-b180a8c582e4";
    private static final String DOCUSIGN_PRIVATE_KEY_PATH = "src/main/resources/docusign_private_key.pem";
    private static final String API_ACCOUNT_ID = "246e019e-5f49-4d20-b027-59a6c67d7d4";

    // ============ Mode Mock/Test ============
    private static final boolean USE_MOCK_MODE = false;

    private final String accessToken;

    // ============ Constructeur ============
    public SignatureService() throws IOException {
        if (USE_MOCK_MODE) {
            this.accessToken = "MOCK_TOKEN";
            System.out.println("✓ SignatureService initialisé en mode MOCK");
        } else {
            this.accessToken = getAccessTokenJWT();
            System.out.println("✓ SignatureService initialisé avec JWT authentification réelle");
        }
    }

    /**
     * Envoie un contrat PDF à DocuSign pour signature électronique par le sponsor.
     */
    public SignatureResponse sendForSignature(byte[] pdfBytes, String sponsorEmail, String sponsorName)
            throws IOException, IllegalArgumentException {

        Objects.requireNonNull(pdfBytes, "pdfBytes ne peut pas être null");
        if (pdfBytes.length == 0) {
            throw new IllegalArgumentException("Le PDF est vide");
        }

        if (sponsorEmail == null || sponsorEmail.isBlank()) {
            throw new IllegalArgumentException("sponsorEmail manquant ou vide");
        }

        if (sponsorName == null || sponsorName.isBlank()) {
            throw new IllegalArgumentException("sponsorName manquant ou vide");
        }

        System.out.println("📨 Envoi du contrat à signature pour : " + sponsorEmail);

        if (USE_MOCK_MODE) {
            return sendForSignatureMock(pdfBytes, sponsorEmail, sponsorName);
        } else {
            return sendForSignatureReal(pdfBytes, sponsorEmail, sponsorName);
        }
    }

    /**
     * Version mock/test de sendForSignature.
     */
    private SignatureResponse sendForSignatureMock(byte[] pdfBytes, String sponsorEmail, String sponsorName)
            throws IOException {

        String mockEnvelopeId = UUID.randomUUID().toString();
        String mockSigningUrl = "https://demo.docusign.net/signing/?" +
                "envelopeId=" + mockEnvelopeId +
                "&email=" + sponsorEmail +
                "&name=" + sponsorName;

        System.out.println("✓ [MOCK] Enveloppe créée : " + mockEnvelopeId);
        System.out.println("✓ [MOCK] URL de signature (mock) : " + mockSigningUrl);

        return new SignatureResponse(mockEnvelopeId, mockSigningUrl, sponsorEmail, "MOCK_TEST");
    }

    /**
     * Version réelle : crée une enveloppe DocuSign avec authentification JWT.
     */
    private SignatureResponse sendForSignatureReal(byte[] pdfBytes, String sponsorEmail, String sponsorName)
            throws IOException {

        try {
            String envelopeId = createEnvelope(pdfBytes, sponsorEmail, sponsorName);
            System.out.println("✓ Enveloppe créée : " + envelopeId);

            String signingUrl = getSigningUrl(envelopeId, sponsorEmail, sponsorName);
            System.out.println("✓ URL de signature générée pour : " + sponsorEmail);

            return new SignatureResponse(envelopeId, signingUrl, sponsorEmail, "SENT");

        } catch (Exception e) {
            throw new IOException("Erreur lors de la création de l'enveloppe DocuSign: " + e.getMessage(), e);
        }
    }

    /**
     * Génère un JWT Bearer token pour l'authentification OAuth DocuSign.
     * Le JWT doit contenir:
     * - iss: INTEGRATION_KEY
     * - sub: DOCUSIGN_USER_ID
     * - aud: https://account-d.docusign.com (audience)
     * - scope: signature impersonation
     */
    private String getAccessTokenJWT() throws IOException {
        try {
            PrivateKey privateKey = loadPrivateKey(DOCUSIGN_PRIVATE_KEY_PATH);

            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            Date expiryDate = new Date(nowMillis + (60 * 1000)); // Expire dans 60 secondes

            String jwt = Jwts.builder()
                    .setIssuer(INTEGRATION_KEY)
                    .setSubject(DOCUSIGN_USER_ID)
                    .setAudience("https://account-d.docusign.com")  // ✓ REQUIS par DocuSign
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .claim("scope", "signature impersonation")
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact();

            System.out.println("✓ JWT généré avec succès");
            System.out.println("  - Issuer: " + INTEGRATION_KEY);
            System.out.println("  - Subject: " + DOCUSIGN_USER_ID);
            System.out.println("  - Audience: https://account-d.docusign.com");

            return exchangeJWTForAccessToken(jwt);

        } catch (Exception e) {
            throw new IOException("Erreur lors de la génération du JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Charge la clé privée RSA depuis un fichier .pem ou depuis les configurations
     * Utilise BouncyCastle pour gérer les formats PKCS1 et PKCS8
     */
    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        String keyContent;

        // Essayer de charger depuis le fichier d'abord
        try {
            keyContent = new String(Files.readAllBytes(Paths.get(keyPath)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Si le fichier n'existe pas, utiliser la clé embarquée
            System.out.println("⚠ Fichier clé non trouvé, utilisation de la clé configurée");
            keyContent = getEmbeddedPrivateKey();
        }

        // Nettoyer la clé : supprimer les espaces supplémentaires, normaliser les sauts de ligne
        keyContent = keyContent.replaceAll("\\s+", " ").trim();
        keyContent = keyContent.replaceAll(" (?=-----)", "\n");
        keyContent = keyContent.replaceAll("-----BEGIN RSA PRIVATE KEY-----", "-----BEGIN RSA PRIVATE KEY-----\n");
        keyContent = keyContent.replaceAll("-----END RSA PRIVATE KEY-----", "\n-----END RSA PRIVATE KEY-----");
        keyContent = keyContent.replaceAll("([A-Za-z0-9+/]{64})", "$1\n");

        try (PEMParser pemParser = new PEMParser(new StringReader(keyContent))) {
            Object object = pemParser.readObject();

            if (object == null) {
                throw new IllegalArgumentException("Impossible de parser la clé PEM : contenu vide ou format invalide");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PEMKeyPair) {
                // Format RSA PRIVATE KEY (PKCS1)
                PEMKeyPair pair = (PEMKeyPair) object;
                return converter.getPrivateKey(pair.getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                // Format PRIVATE KEY (PKCS8)
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo =
                    (org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object;
                return converter.getPrivateKey(privateKeyInfo);
            } else {
                throw new IllegalArgumentException("Format de clé non reconnu : " +
                    (object != null ? object.getClass().getName() : "null") +
                    ". La clé doit être au format PKCS1 (RSA PRIVATE KEY) ou PKCS8 (PRIVATE KEY)");
            }
        }
    }

    /**
     * Retourne la clé privée RSA configurée
     * KeyPair ID: 26395f46-bef6-4f74-a5f0-059b22bdba34
     */
    private String getEmbeddedPrivateKey() {
        return "-----BEGIN RSA PRIVATE KEY-----\n" +
               "MIIEowIBAAKCAQEAqoKDAX5RwzPh5Sup7ECx931SIaEaNIxEDuqmYF2LAxuqVGOS\n" +
               "FhUJJa9lIFx1ktfC4xHqswY59XTkq6Ylz1nOjaJAoV0h8zWGVNrjFv5pSx6pyX7S\n" +
               "uOiHcR7LInEFnYnN9E2XJbLnYABAs8+piaWnQhVqtsStt3Ae7r0/hLX7tKZFm43v\n" +
               "+fHqhTXlFQ69zAkaUxRjUJoIUTHrcLarBBwu+/g8mbHUmtUxSgi2Acw3bk4diJne\n" +
               "HIGFozkeL3i+gc3/DpzgcxdNyKk0sZ8XroYwiyn0e1c0/fpXbrlZjBCzM4IhxU4e\n" +
               "GQA/wi5hL2qIC2G3ooZrMd3gKiuzKuByX1EmPQIDAQABAoIBAAHCFD868fVhDtJ5\n" +
               "24ft19ftMSBbJKXHj3kW5s3GESWgTqaNm0dCgsRidVi601SHCIGmYOTlKspS4Pu7\n" +
               "HBcx/hz8QA3Z47x6AA9WbXKCf4vwfkAvYFVbQnBCaMZLY4svDVJYTXdsS5H0Fraz\n" +
               "7LPYYyvdhCrrxr3GOG9uQJ4p8TU44F4SBl5XKXrW9I7ILtjM7TP/hPDliH+FmBDj\n" +
               "14Z1fEAP+fKQ1PRkEDgHnqgYNXB+1xdp5uLt0pHOgS9b0Rwb/5wAJ99UNIetNN3/\n" +
               "DkoO9yYP/lb4qGoW6JI32sNZJ/JeSMckdgyGSmtz5YjuBsm1UyHyAtCNLjofKEZa\n" +
               "x/Wkn0ECgYEA1eXYaV+QHsWxOJqnZpcpE05svnVQSIDpdz0NnWIbaNQEeJeYJHB2\n" +
               "u3evNQG2KRj54Rzk5IoE49bYTt0woOoSv+GhgxsQtheS96DRtE9Q7g52T6+k1gGd\n" +
               "EPB3sUQ2ZTqGAVSq425UR7ARStNl9oCOLkIxpdrFhTHsMbFh2gMu80ECgYEAzBJg\n" +
               "NzFWZ3M095Zw88SKaJj7Au9gVHyemzprbe7nN8oal8NL+KDwRXwJHpxQMd3jl+ih\n" +
               "opkhzluNCtnPd7CBG2A2y6+yizwgKp15psMHuHwUYRd3IEn/pDUrN+DW4MNi7Qpa\n" +
               "/OvkiCBjvtGftw2T23tdGfymODEF3egYGFdd//0CgYAZvcp4i+6dNW9pf3sr5n8j\n" +
               "+qktlpKt1w3BstBDo33m9LUBCkVeBUbUssecGnOgqIfR5pdJRa7m+IEZpJbolqLh\n" +
               "ArdcPtKZB2dBc9Io3+6+Z3Enj9zHgHQ1iKru42WkcxGTcWwLaMitw+ug1KYNr1Tt\n" +
               "DjxJWHWy3Nou9Mjw4rcwwQKBgANvP71OEEU3dCnCl24S3b167aaT/swNOOIl7o7w\n" +
               "GWYf+aiaOdoaY1DYEJM6UEM2l9EDsky5RQ9jDRGkrtA14UZjrShqcZfUUDpHYjfD\n" +
               "Sor5U6J6sZ+Sf0H1Px/2occwoqaYqQVetv7vhJ4+ivCawuVLwzpfvTBu6slcUaN/\n" +
               "PO7FAoGBAL9T+Hk5FY2ejtMgQOQnS6z6yk55DCrrwC7k4qpIg69ng5I2h00k7wTI\n" +
               "LIyzgM1XCc+54qeL9N069XjFWuadB8TyuvN7aV18jH5/ZfHpFbhVTATdfEW2XX9Z\n" +
               "suApWNg+SNJv47Ux5S+c9FWhWNI8b7bm3uxDXMFkaZ0uIW1MzjRp\n" +
               "-----END RSA PRIVATE KEY-----\n";
    }


    /**
     * Échange un JWT contre un access_token OAuth auprès de DocuSign.
     */
    private String exchangeJWTForAccessToken(String jwt) throws IOException {
        try {
            System.out.println("\n🔐 Échange JWT contre Access Token...");
            System.out.println("  URL OAuth: " + DOCUSIGN_AUTH_URL);

            String requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DOCUSIGN_AUTH_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            System.out.println("  Envoi de la requête OAuth...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("  Réponse OAuth - Status Code: " + response.statusCode());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
                String accessToken = (String) responseMap.get("access_token");

                System.out.println("✓ Access token obtenu avec succès");
                System.out.println("  Token length: " + (accessToken != null ? accessToken.length() : "null") + " chars");
                return accessToken;
            } else {
                String errorBody = response.body();
                System.out.println("  ✗ Réponse d'erreur: " + errorBody);

                // Parser l'erreur pour plus de détails
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> errorMap = mapper.readValue(errorBody, Map.class);
                    String errorMsg = (String) errorMap.get("error");
                    String errorDesc = (String) errorMap.get("error_description");

                    System.out.println("  Erreur: " + errorMsg);
                    System.out.println("  Description: " + errorDesc);

                    if ("no_valid_keys_or_signatures".equals(errorDesc)) {
                        System.out.println("\n⚠️  DIAGNOSTIC: no_valid_keys_or_signatures");
                        System.out.println("  Possible causes:");
                        System.out.println("  1. La clé privée ne correspond pas au KeyPair ID enregistré");
                        System.out.println("  2. Le KeyPair ID n'existe pas ou est inactif dans DocuSign");
                        System.out.println("  3. Le JWT n'est pas signée correctement");
                        System.out.println("  4. L'audience (aud) claim est incorrect");
                    }
                } catch (Exception e) {
                    // Impossible de parser, afficher le raw body
                }

                throw new IOException("Erreur DocuSign OAuth: " + response.statusCode() + " - " + errorBody);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruption lors de l'obtention du token OAuth", e);
        }
    }

    /**
     * Crée une enveloppe DocuSign avec le PDF et un signataire.
     */
    private String createEnvelope(byte[] pdfBytes, String sponsorEmail, String sponsorName) throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();

            requestBody.put("emailSubject", "Veuillez signer le contrat de partenariat");
            requestBody.put("status", "sent");

            var documentsArray = mapper.createArrayNode();
            var docNode = mapper.createObjectNode();
            docNode.put("documentBase64", Base64.getEncoder().encodeToString(pdfBytes));
            docNode.put("name", "Contract.pdf");
            docNode.put("fileFormat", "pdf");
            docNode.put("documentId", "1");
            documentsArray.add(docNode);
            requestBody.set("documents", documentsArray);

            var recipientsNode = mapper.createObjectNode();
            var signersArray = mapper.createArrayNode();
            var signerNode = mapper.createObjectNode();
            signerNode.put("email", sponsorEmail);
            signerNode.put("name", sponsorName);
            signerNode.put("recipientId", "1");

            var signHereArray = mapper.createArrayNode();
            var signHere = mapper.createObjectNode();
            signHere.put("documentId", "1");
            signHere.put("pageNumber", "1");
            signHere.put("xPosition", "100");
            signHere.put("yPosition", "150");
            signHereArray.add(signHere);
            signerNode.set("signHereTabs", mapper.createObjectNode().set("signHereTab", signHereArray));

            signersArray.add(signerNode);
            recipientsNode.set("signers", signersArray);
            requestBody.set("recipients", recipientsNode);

            HttpClient client = HttpClient.newHttpClient();
            String url = DOCUSIGN_DEMO_BASE_URL + "/accounts/" + API_ACCOUNT_ID + "/envelopes";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
                return (String) responseMap.get("envelopeId");
            } else {
                throw new IOException("Erreur DocuSign (code " + response.statusCode() + "): " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruption lors de la création de l'enveloppe", e);
        }
    }

    /**
     * Récupère une URL de signature pour le signataire.
     */
    private String getSigningUrl(String envelopeId, String sponsorEmail, String sponsorName) throws IOException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.put("returnUrl", "http://localhost:8080/signing-complete");
            requestBody.put("authenticationMethod", "none");
            requestBody.put("clientUserId", "1");

            var recipientsArray = mapper.createArrayNode();
            var recipient = mapper.createObjectNode();
            recipient.put("email", sponsorEmail);
            recipient.put("name", sponsorName);
            recipient.put("clientUserId", "1");
            recipient.put("recipientId", "1");
            recipientsArray.add(recipient);
            requestBody.set("signers", recipientsArray);

            HttpClient client = HttpClient.newHttpClient();
            String url = DOCUSIGN_DEMO_BASE_URL + "/accounts/" + API_ACCOUNT_ID +
                         "/envelopes/" + envelopeId + "/views/recipient";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
                return (String) responseMap.get("url");
            } else {
                throw new IOException("Erreur DocuSign: " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruption lors de la récupération de l'URL de signature", e);
        }
    }

    /**
     * Classe interne pour encapsuler la réponse de signature.
     */
    public static class SignatureResponse {
        public final String envelopeId;
        public final String signingUrl;
        public final String sponsorEmail;
        public final String status;

        public SignatureResponse(String envelopeId, String signingUrl, String sponsorEmail, String status) {
            this.envelopeId = envelopeId;
            this.signingUrl = signingUrl;
            this.sponsorEmail = sponsorEmail;
            this.status = status;
        }

        @Override
        public String toString() {
            return "SignatureResponse{" +
                    "envelopeId='" + envelopeId + '\'' +
                    ", signingUrl='" + signingUrl + '\'' +
                    ", sponsorEmail='" + sponsorEmail + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

    /**
     * Utilitaire pour encoder les bytes en Base64.
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Utilitaire pour décoder une réponse Base64.
     */
    public static byte[] decodeBase64(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    /**
     * Test simple : vérifie que le service initialise correctement.
     */
    public static void main(String[] args) {
        try {
            SignatureService service = new SignatureService();
            System.out.println("✓ SignatureService initialisé avec succès");
            System.out.println("  Mode Mock : " + USE_MOCK_MODE);
        } catch (Exception e) {
            System.err.println("✗ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

