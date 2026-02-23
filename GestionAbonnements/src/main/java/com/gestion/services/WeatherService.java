package com.gestion.services;

import java.util.Random;

/**
 * Service to fetch weather forecast for events.
 * Currently uses a mocked implementation.
 */
public class WeatherService {

    public static class WeatherInfo {
        public final double temp;
        public final String description;
        public final String icon;

        public WeatherInfo(double temp, String description, String icon) {
            this.temp = temp;
            this.description = description;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return String.format("%.1f°C - %s", temp, description);
        }
    }

    /**
     * Fetches weather for a location and date.
     * Mocked for now.
     */
    public WeatherInfo getForecast(String location, String date) {
        Random r = new Random();
        double temp = 10 + r.nextDouble() * 20;
        String[] descs = { "Ensoleillé", "Nuageux", "Pluie légère", "Ciel dégagé" };
        String desc = descs[r.nextInt(descs.length)];
        return new WeatherInfo(temp, desc, "01d");
    }

    /**
     * Example of real integration structure (uncomment and add dependencies for
     * real usage)
     */
    /*
     * public WeatherInfo fetchRealWeather(String city) {
     * String apiKey = "YOUR_API_KEY";
     * String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
     * "&appid=" + apiKey + "&units=metric";
     * // Use HttpClient or similar to fetch JSON and parse
     * return null;
     * }
     */
}
