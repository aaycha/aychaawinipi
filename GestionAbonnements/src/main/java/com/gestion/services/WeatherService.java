package com.gestion.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Service to fetch real-time weather data using Open-Meteo (free, no API key).
 * Includes SSL bypass for environments with certificate issues.
 */
public class WeatherService {
    private static final Logger LOGGER = Logger.getLogger(WeatherService.class.getName());

    // Open-Meteo: free, no API key, reliable worldwide
    // Tunis coordinates: 36.8065, 10.1815
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=36.8065&longitude=10.1815"
            + "&current=temperature_2m,weather_code&timezone=Africa/Tunis";

    private static WeatherService instance;
    private final HttpClient client;
    private final ObjectMapper mapper;

    private WeatherService() {
        this.client = createInsecureClient();
        this.mapper = new ObjectMapper();
    }

    public static synchronized WeatherService getInstance() {
        if (instance == null) {
            instance = new WeatherService();
        }
        return instance;
    }

    public static class WeatherInfo {
        public final String temp;
        public final String condition;
        public final String city;
        public final String iconUrl;

        public WeatherInfo(String temp, String condition, String city, String iconUrl) {
            this.temp = temp;
            this.condition = condition;
            this.city = city;
            this.iconUrl = iconUrl;
        }

        public static WeatherInfo offline() {
            return new WeatherInfo("--°C", "Offline", "Tunis", null);
        }
    }

    /**
     * Fetches current weather for Tunis using Open-Meteo.
     */
    public WeatherInfo getCurrentWeather(String city) {
        try {
            LOGGER.info("Fetching weather from Open-Meteo...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.info("Open-Meteo response status: " + response.statusCode());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode current = root.get("current");

                double tempC = current.get("temperature_2m").asDouble();
                int weatherCode = current.get("weather_code").asInt();
                String condition = decodeWeatherCode(weatherCode);

                String result = String.format("%.1f°C", tempC);
                LOGGER.info("Weather fetched successfully: " + result + " - " + condition);

                return new WeatherInfo(result, condition, "Tunis", null);
            } else {
                LOGGER.warning("Open-Meteo returned status: " + response.statusCode()
                        + " body: " + response.body());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch weather: " + e.getMessage(), e);
        }
        return WeatherInfo.offline();
    }

    /**
     * Translates WMO Weather Code to a human-readable French condition string.
     * See: https://open-meteo.com/en/docs#weathervariables
     */
    private String decodeWeatherCode(int code) {
        if (code == 0)
            return "Ciel dégagé ☀️";
        if (code == 1)
            return "Principalement dégagé 🌤️";
        if (code == 2)
            return "Partiellement nuageux ⛅";
        if (code == 3)
            return "Couvert ☁️";
        if (code >= 45 && code <= 48)
            return "Brouillard 🌫️";
        if (code >= 51 && code <= 55)
            return "Bruine 🌧️";
        if (code >= 56 && code <= 57)
            return "Bruine verglaçante 🌧️";
        if (code >= 61 && code <= 65)
            return "Pluie 🌧️";
        if (code >= 66 && code <= 67)
            return "Pluie verglaçante 🌧️";
        if (code >= 71 && code <= 77)
            return "Neige ❄️";
        if (code >= 80 && code <= 82)
            return "Averses 🌦️";
        if (code >= 85 && code <= 86)
            return "Averses de neige ❄️";
        if (code >= 95 && code <= 99)
            return "Orage ⛈️";
        return "Inconnu";
    }

    /**
     * Creates an HttpClient that ignores SSL certificate validation.
     * Necessary for environments with proxy or certificate injection issues.
     */
    private HttpClient createInsecureClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sc)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not create insecure client, falling back to default", e);
            return HttpClient.newHttpClient();
        }
    }
}
