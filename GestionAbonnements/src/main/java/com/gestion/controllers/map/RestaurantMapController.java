package com.gestion.controllers.map;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Restaurant Map view.
 * Uses a WebView to load an interactive Leaflet map.
 */
public class RestaurantMapController implements Initializable {

    @FXML
    private WebView mapView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        WebEngine engine = mapView.getEngine();

        String content = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        #map { height: 100vh; width: 100%; margin: 0; padding: 0; border-radius: 12px; }\n" +
                "        body { margin: 0; padding: 0; background: #f8f9fa; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([36.8065, 10.1815], 13);\n" +
                "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            attribution: '&copy; OpenStreetMap contributors'\n" +
                "        }).addTo(map);\n" +
                "\n" +
                "        function loadMarkers() {\n" +
                "            fetch('http://localhost:8081/api/restaurants/map')\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    data.forEach(p => {\n" +
                "                        L.marker([p.latitude, p.longitude]).addTo(map)\n" +
                "                         .bindPopup('<b>'+p.name+'</b><br>'+p.address);\n" +
                "                    });\n" +
                "                })\n" +
                "                .catch(() => {\n" +
                "                    console.log('Map API Offline - Loading Scout Markers');\n" +
                "                    var fallbacks = [\n" +
                "                        {name: 'Base Camp Alpha', lat: 36.8065, lng: 10.1815, desc: 'High Mountain Rations'},\n"
                +
                "                        {name: 'Summit Refuge', lat: 36.8500, lng: 10.2000, desc: 'Gourmet Trail Food'},\n"
                +
                "                        {name: 'Glacier Lodge', lat: 36.7800, lng: 10.1500, desc: 'Artisanal Snacks'}\n"
                +
                "                    ];\n" +
                "                    fallbacks.forEach(f => {\n" +
                "                        L.marker([f.lat, f.lng]).addTo(map).bindPopup('<b>'+f.name+'</b><br>'+f.desc);\n"
                +
                "                    });\n" +
                "                });\n" +
                "        }\n" +
                "        loadMarkers();\n" +
                "    </script>\n" +
                "</body></html>";

        engine.loadContent(content);
    }
}
