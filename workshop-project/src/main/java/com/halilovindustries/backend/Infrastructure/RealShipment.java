package com.halilovindustries.backend.Infrastructure;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IShipment;
import com.halilovindustries.backend.Domain.DTOs.ShipmentDetailsDTO;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Primary
public class RealShipment implements IShipment {

    private static final String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    @Override
    public boolean validateShipmentDetails(ShipmentDetailsDTO details) {
        return details != null && details.fullShipmentDetails();
    }

    @Override
    public Integer processShipment(ShipmentDetailsDTO details) {
        try {
            Map<String, String> formData = Map.of(
                "action_type", "supply",
                "name", details.getName(),
                "address", details.getAddress(),
                "city", details.getCity(),
                "country", details.getCountry(),
                "zip", details.getZipcode()
            );

            String encodedForm = encodeFormData(formData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedForm))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int transactionId = Integer.parseInt(response.body().trim());
            if (transactionId == -1) {
                return null; // Negative transaction ID indicates an error
                
            }
            return (transactionId >= 10000 && transactionId <= 100000) ? transactionId : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean cancelShipment(int transactionId) {
        try {
            Map<String, String> formData = Map.of(
                "action_type", "cancel_supply",
                "transaction_id", String.valueOf(transactionId)
            );

            String encodedForm = encodeFormData(formData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedForm))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return "1".equals(response.body().trim());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String encodeFormData(Map<String, String> data) {
        return data.entrySet()
                .stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                              URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}