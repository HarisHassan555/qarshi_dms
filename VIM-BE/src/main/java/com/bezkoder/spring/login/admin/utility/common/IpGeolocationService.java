package com.bezkoder.spring.login.admin.utility.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

@Service
public class IpGeolocationService {

    private static final Logger log = LogManager.getLogger(IpGeolocationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ipapi.api.base-url:https://api.ipapi.is/}")
    private String apiBaseUrl;

    @Value("${ipapi.api.key:}")
    private String apiKey;

    public Optional<GeoPoint> resolve(String ip) {
        if (ip == null || ip.trim().isEmpty() || isNonPublicIp(ip.trim())) {
            return Optional.empty();
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("ipapi.api.key is not configured; skipping IP geolocation");
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl())
                    .queryParam("q", ip.trim())
                    .queryParam("key", apiKey.trim())
                    .build(true)
                    .toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }
            return parseResponse(response.getBody());
        } catch (Exception ex) {
            log.warn("IP geolocation lookup failed for " + ip + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isNonPublicIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return true;
        }
        String normalized = ip.trim().toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || normalized.startsWith("::ffff:")) {
            return true;
        }
        if ("::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        } catch (Exception ex) {
            return true;
        }
    }

    private String normalizeBaseUrl() {
        String base = apiBaseUrl != null ? apiBaseUrl.trim() : "https://api.ipapi.is/";
        return base.endsWith("/") ? base : base + "/";
    }

    private Optional<GeoPoint> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("is_bogon").asBoolean(false)) {
                return Optional.empty();
            }
            Double latitude = readCoordinate(root, "lat", "latitude");
            Double longitude = readCoordinate(root, "lon", "longitude");
            JsonNode location = root.get("location");
            if (location != null && location.isObject()) {
                if (latitude == null) {
                    latitude = readCoordinate(location, "latitude", "lat");
                }
                if (longitude == null) {
                    longitude = readCoordinate(location, "longitude", "lon");
                }
            }
            if (latitude == null || longitude == null || !isUsableCoordinate(latitude, longitude)) {
                return Optional.empty();
            }
            String label = buildLabel(root, location);
            return Optional.of(new GeoPoint(latitude, longitude, label));
        } catch (Exception ex) {
            log.warn("Failed to parse ipapi.is response: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static boolean isUsableCoordinate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }
        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
            return false;
        }
        // Reject "Null Island" and other junk coordinates sometimes returned for private/local IPs.
        return !(Math.abs(latitude) < 1.0 && Math.abs(longitude) < 1.0);
    }

    private Double readCoordinate(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isNumber()) {
                return value.doubleValue();
            }
            if (value != null && value.isTextual()) {
                try {
                    return Double.parseDouble(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // try next field
                }
            }
        }
        return null;
    }

    private String buildLabel(JsonNode root, JsonNode location) {
        String city = textAt(root, location, "city");
        String region = textAt(root, location, "region", "state");
        String country = textAt(root, location, "country");
        StringBuilder label = new StringBuilder();
        if (city != null && !city.isEmpty()) {
            label.append(city);
        }
        if (region != null && !region.isEmpty()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(region);
        }
        if (country != null && !country.isEmpty()) {
            if (label.length() > 0) {
                label.append(", ");
            }
            label.append(country);
        }
        return label.length() > 0 ? label.toString() : null;
    }

    private String textAt(JsonNode root, JsonNode location, String... keys) {
        for (String key : keys) {
            if (location != null && location.has(key) && !location.get(key).isNull()) {
                String value = location.get(key).asText("").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
            if (root.has(key) && !root.get(key).isNull()) {
                String value = root.get(key).asText("").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static final class GeoPoint {
        public final double latitude;
        public final double longitude;
        public final String label;

        public GeoPoint(double latitude, double longitude, String label) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.label = label;
        }
    }
}
