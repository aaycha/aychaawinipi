package com.gestion.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gestion.entities.Restaurant;
import com.gestion.services.RestaurantServiceImpl;
import com.gestion.interfaces.RestaurantService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.gestion.services.UserService;
import com.gestion.entities.User;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Embedded HTTP server to provide REST APIs for the system.
 */
public class RestApiService {

    private static final int PORT = 8081;
    private HttpServer server;
    private final RestaurantService restaurantService = new RestaurantServiceImpl();
    private final UserService userService = new UserService();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Endpoints
            server.createContext("/api/restaurants/map", this::handleRestaurantsMap);
            server.createContext("/api/menus/today", this::handleTodayMenu);
            server.createContext("/api/restaurants/", this::handleRestaurantRating); // Pattern matching in handler
            server.createContext("/api/map/heatmap", this::handleHeatmap);
            server.createContext("/api/chatbot", this::handleChatbot);

            server.setExecutor(null);
            server.start();
            System.out.println("🚀 REST API Server started on port " + PORT);
        } catch (IOException e) {
            System.err.println("❌ Failed to start API server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleRestaurantsMap(HttpExchange exchange) throws IOException {
        List<Restaurant> restaurants = restaurantService.findAll();
        List<Map<String, Object>> result = restaurants.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("name", r.getNom());
            map.put("address", r.getAdresse());
            map.put("latitude", r.getLatitude() != null ? r.getLatitude() : 36.8065); // Default to Tunis
            map.put("longitude", r.getLongitude() != null ? r.getLongitude() : 10.1815);
            map.put("phone", r.getTelephone());
            map.put("rating", r.getRating());
            map.put("isOpen", r.isOpen());
            return map;
        }).collect(Collectors.toList());

        sendResponse(exchange, result, 200);
    }

    private void handleTodayMenu(HttpExchange exchange) throws IOException {
        // Mocking for now as requested
        Map<String, Object> menu = new HashMap<>();
        menu.put("date", "2026-02-21");
        menu.put("special", "Couscous Royal");
        menu.put("price", 15.5);
        sendResponse(exchange, menu, 200);
    }

    private void handleRestaurantRating(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.contains("/rating")) {
            // Extract ID: /api/restaurants/{id}/rating
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                try {
                    Long id = Long.parseLong(parts[3]);
                    Map<String, Object> rating = new HashMap<>();
                    rating.put("restaurantId", id);
                    rating.put("averageRating", 4.5); // Mock
                    sendResponse(exchange, rating, 200);
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        sendResponse(exchange, "Not Found", 404);
    }

    private void handleHeatmap(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> heatmap = new ArrayList<>();
        heatmap.add(createHeatmapPoint("Tunis Centre", 200));
        heatmap.add(createHeatmapPoint("La Marsa", 150));
        heatmap.add(createHeatmapPoint("Sidi Bou Said", 80));
        sendResponse(exchange, heatmap, 200);
    }

    private void handleChatbot(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String q = "";
        if (query != null && query.contains("q=")) {
            q = URLDecoder.decode(query.split("q=")[1].split("&")[0], StandardCharsets.UTF_8);
        }

        Map<String, String> response = new HashMap<>();
        String lower = q.toLowerCase();

        if (lower.contains("admin")) {
            try {
                long count = userService.recuperer().stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                        .count();
                response.put("reply", "Il y a actuellement " + count + " administrateurs connectés au système LAMMA.");
            } catch (Exception e) {
                response.put("reply", "Désolé, je ne peux pas accéder à la liste des utilisateurs pour le moment.");
            }
        } else if (lower.contains("restaurant") || lower.contains("manger")) {
            List<Restaurant> rest = restaurantService.findAll();
            if (!rest.isEmpty()) {
                Restaurant best = rest.get(0);
                response.put("reply", "Je vous recommande '" + best.getNom() + "' (Note: " + best.getRating()
                        + "/5). C'est le favori des scouts en ce moment !");
            } else {
                response.put("reply", "Aucun restaurant n'est répertorié pour le moment.");
            }
        } else if (lower.contains("aide") || lower.contains("help")) {
            response.put("reply",
                    "Je peux vous aider à trouver un restaurant, connaître le nombre d'admins, ou vous guider dans vos abonnements.");
        } else {
            response.put("reply",
                    "Je ne suis pas sûr de comprendre votre question d'explorateur. Essayez de me demander qui sont les admins ou quel est le meilleur restaurant !");
        }

        sendResponse(exchange, response, 200);
    }

    private Map<String, Object> createHeatmapPoint(String zone, int count) {
        Map<String, Object> point = new HashMap<>();
        point.put("zone", zone);
        point.put("ordersCount", count);
        return point;
    }

    private void sendResponse(HttpExchange exchange, Object data, int statusCode) throws IOException {
        byte[] responseBody = mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // For frontend consumers
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
}
