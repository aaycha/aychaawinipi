package com.gestion.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * Intégration simple avec l'API CurrencyFreaks.
 * Base technique : l'API gratuite renvoie les taux avec base USD.
 * On reconstruit donc les taux "1 TND -> devise" via :
 * 1 TND = (rateUSD->DEV / rateUSD->TND).
 */
public class CurrencyService {

    // Clé fournie par l'utilisateur (attention : ne pas pousser sur GitHub en vrai projet)
    private static final String API_KEY = "e406021b57f04337808b3fcc8d3b69f5";
    private static final String LATEST_URL =
            "https://api.currencyfreaks.com/v2.0/rates/latest?apikey=" + API_KEY;

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Map<String, BigDecimal> cachedRatesFromTnd;
    private static Instant lastFetch;

    /**
     * Retourne une map "code devise" -> "taux pour 1 TND".
     */
    public static Map<String, BigDecimal> getRatesFromTnd() throws IOException, InterruptedException {
        if (cachedRatesFromTnd != null && lastFetch != null &&
                lastFetch.isAfter(Instant.now().minusSeconds(1800))) {
            return cachedRatesFromTnd;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LATEST_URL))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erreur HTTP CurrencyFreaks: " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode ratesNode = root.get("rates");
        if (ratesNode == null || ratesNode.get("TND") == null) {
            throw new IOException("Réponse CurrencyFreaks invalide ou sans taux TND");
        }

        BigDecimal usdToTnd = new BigDecimal(ratesNode.get("TND").asText());

        Map<String, BigDecimal> result = new TreeMap<>();
        ratesNode.fields().forEachRemaining(entry -> {
            String code = entry.getKey();
            BigDecimal usdToCode = new BigDecimal(entry.getValue().asText());
            // 1 TND -> code = (USD->code) / (USD->TND)
            BigDecimal tndToCode = usdToCode.divide(usdToTnd, 6, RoundingMode.HALF_UP);
            result.put(code, tndToCode);
        });

        cachedRatesFromTnd = result;
        lastFetch = Instant.now();
        return result;
    }
}

