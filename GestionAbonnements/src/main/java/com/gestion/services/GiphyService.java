package com.gestion.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GiphyService {
    private static final String TENOR_API_KEY = "LIVDSRZULELA";
    private static final String TENOR_BASE_URL = "https://g.tenor.com/v1";

    public static class GifData {
        public String id;
        public String title;
        public String url;
        public String mp4Url;
        public String stillUrl;

        public GifData(String id, String title, String url, String mp4Url, String stillUrl) {
            this.id = id;
            this.title = title;
            this.url = url != null && !url.isEmpty() ? url : "https://placeholder.com/300x250?text=GIF";
            this.mp4Url = mp4Url != null && !mp4Url.isEmpty() ? mp4Url : this.url;
            this.stillUrl = stillUrl != null && !stillUrl.isEmpty() ? stillUrl : this.url;
        }
    }

    public static List<GifData> searchGifs(String query, int limit) {
        return fetchTenorGifs(TENOR_BASE_URL + "/search?key=" + TENOR_API_KEY + "&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + limit);
    }

    public static List<GifData> getTrendingGifs(int limit) {
        return fetchTenorGifs(TENOR_BASE_URL + "/featured?key=" + TENOR_API_KEY + "&limit=" + limit);
    }

    private static List<GifData> fetchTenorGifs(String apiUrl) {
        List<GifData> results = new ArrayList<>();
        try {
            System.out.println("[✓ Tenor API] URL: " + apiUrl);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            // Decouper par results pour eviter de mélanger les URLs
            String[] resultBlocks = json.split("\"id\":");

            // Le bloc 0 est avant le premier "id"
            for (int i = 1; i < resultBlocks.length; i++) {
                String block = resultBlocks[i];
                String id = "tenor_" + i;

                String title = "GIF " + i;
                Matcher mTitle = Pattern.compile("\"content_description\":\\s*\"([^\"]+)\"").matcher(block);
                if (mTitle.find()) {
                    title = mTitle.group(1);
                }

                String gifUrl = null;
                // Chercher tinygif ou gif normal
                Matcher mGif = Pattern.compile("\"tinygif\":\\s*\\{[^}]*?\"url\":\\s*\"([^\"]+)\"").matcher(block);
                if (mGif.find()) {
                    gifUrl = mGif.group(1);
                } else {
                    Matcher mGif2 = Pattern.compile("\"gif\":\\s*\\{[^}]*?\"url\":\\s*\"([^\"]+)\"").matcher(block);
                    if (mGif2.find()) {
                        gifUrl = mGif2.group(1);
                    }
                }

                String mp4Url = null;
                Matcher mMp4 = Pattern.compile("\"tinymp4\":\\s*\\{[^}]*?\"url\":\\s*\"([^\"]+)\"").matcher(block);
                if (mMp4.find()) {
                    mp4Url = mMp4.group(1);
                }

                if (gifUrl != null) {
                    results.add(new GifData(id, title, gifUrl, mp4Url != null ? mp4Url : gifUrl, gifUrl));
                }

                if (results.size() >= 15)
                    break; // Limite de sécurité
            }
            System.out.println("[✓ Tenor API] Trouvé " + results.size() + " GIFs");
        } catch (Exception e) {
            System.err.println("[✗ Erreur Tenor API] " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
}
