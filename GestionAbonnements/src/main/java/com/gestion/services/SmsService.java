package com.gestion.services;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SmsService {
    private static final Logger LOGGER = Logger.getLogger(SmsService.class.getName());

    // Endpoints
    private static final String TWILIO_URL_TEMPLATE = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private static final String VONAGE_URL = "https://rest.nexmo.com/sms/json";
    private static final String TOPMESSAGE_URL = "https://api.topmessage.com/v1/messages";

    private final Properties props;

    public SmsService() {
        this.props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("afilnet.properties")) {
            if (input == null) {
                throw new RuntimeException("afilnet.properties not found in resources!");
            }
            props.load(input);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load properties", e);
        }
    }

    /**
     * Primary method to send SMS. Tries Twilio first, fallbacks to Vonage, then
     * TopMessage.
     */
    public boolean sendSMS(String to, String message) {
        // 1. Try Twilio (New Primary)
        if (sendViaTwilio(to, message)) {
            return true;
        }

        // 2. Try Vonage (Fallback 1)
        LOGGER.warning("Twilio failed or not configured. Trying Vonage fallback...");
        if (sendViaVonage(to, message)) {
            return true;
        }

        // 3. Fallback to TopMessage (Fallback 2)
        LOGGER.warning("Vonage failed or not configured. Trying TopMessage fallback...");
        return sendViaTopMessage(to, message);
    }

    private boolean sendViaTwilio(String to, String message) {
        String accountSid = props.getProperty("twilio.account_sid");
        String authToken = props.getProperty("twilio.auth_token");
        String from = props.getProperty("twilio.from_number");

        if (accountSid == null || accountSid.isEmpty() || accountSid.contains("YOUR_")) {
            LOGGER.warning("Twilio credentials not configured.");
            return false;
        }

        if (from != null && from.equals(to)) {
            LOGGER.warning("Twilio error: 'To' and 'From' numbers cannot be the same (" + to
                    + "). Please check your twilio.from_number in afilnet.properties.");
            return false;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = String.format(TWILIO_URL_TEMPLATE, accountSid);

            String form = String.format("From=%s&To=%s&Body=%s",
                    URLEncoder.encode(from, StandardCharsets.UTF_8),
                    URLEncoder.encode(to, StandardCharsets.UTF_8),
                    URLEncoder.encode(message, StandardCharsets.UTF_8));

            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            LOGGER.info("Twilio response [" + response.statusCode() + "]: " + body);

            return response.statusCode() == 201;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Twilio connection error", e);
            return false;
        }
    }

    private boolean sendViaVonage(String to, String message) {
        String apiKey = props.getProperty("vonage.api_key");
        String apiSecret = props.getProperty("vonage.api_secret");
        String sender = props.getProperty("afilnet.sender", "lama");

        if (apiKey == null || apiKey.contains("YOUR_VONAGE")) {
            LOGGER.warning("Vonage API Key not configured.");
            return false;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();

            // Format phone number (Vonage prefers international format without '+')
            String cleanTo = to.startsWith("+") ? to.substring(1) : to;

            String query = String.format(
                    "api_key=%s&api_secret=%s&from=%s&to=%s&text=%s",
                    URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(apiSecret, StandardCharsets.UTF_8),
                    URLEncoder.encode(sender, StandardCharsets.UTF_8),
                    URLEncoder.encode(cleanTo, StandardCharsets.UTF_8),
                    URLEncoder.encode(message, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VONAGE_URL + "?" + query))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            LOGGER.info("Vonage response [" + response.statusCode() + "]: " + body);

            return response.statusCode() == 200 && body.contains("\"status\": \"0\"");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Vonage connection error", e);
            return false;
        }
    }

    private boolean sendViaTopMessage(String to, String message) {
        String apiKey = props.getProperty("afilnet.api_key");
        String sender = props.getProperty("afilnet.sender", "lama");

        if (apiKey == null) {
            LOGGER.warning("TopMessage API Key not configured.");
            return false;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            ObjectNode data = root.putObject("data");
            data.put("from", sender);
            data.put("text", message);
            ArrayNode toArray = data.putArray("to");
            toArray.add(to);

            String jsonPayload = mapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOPMESSAGE_URL))
                    .header("X-TopMessage-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            LOGGER.info("TopMessage response [" + response.statusCode() + "]: " + body);

            return (response.statusCode() == 200 || response.statusCode() == 201)
                    && body != null && !body.contains("ERROR");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "TopMessage connection error", e);
            return false;
        }
    }
}