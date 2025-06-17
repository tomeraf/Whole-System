package com.halilovindustries.backend.Infrastructure;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.IPayment;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import com.halilovindustries.backend.Domain.init.ExternalConfig;
import com.halilovindustries.backend.Domain.DTOs.PaymentDetailsDTO;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Primary
public class RealPayment implements IPayment {

    private final String apiUrl;

    @Autowired
    public RealPayment(ExternalConfig externalConfig) {
        this.apiUrl = externalConfig.getExternalUrl();
    }

    @Override
    public boolean validatePaymentDetails(PaymentDetailsDTO details) {
        // You can add real validation logic here
        return details != null && details.fullDetails();
    }

    @Override
    public Integer processPayment(double price, PaymentDetailsDTO details) {
        try {
            Map<String, String> formData = Map.of(
                "action_type", "pay",
                "amount", String.valueOf((int) price),
                "currency", "USD",
                "card_number", details.getCardNumber(),
                "month", details.getMonth(),
                "year", details.getYear(),
                "holder", details.getCardHolderName(),
                "cvv", details.getCvv(),
                "id", details.getHolderID()
            );

            String encodedForm = encodeFormData(formData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedForm))
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("External system error: status code = " + response.statusCode());
            }
            int transactionId = Integer.parseInt(response.body().trim());
            if (transactionId == -1) {
                return null; // Negative transaction ID indicates failure
            }
            return (transactionId >= 10000 && transactionId <= 100000) ? transactionId : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean cancelPayment(int transactionId) {
        try {
            Map<String, String> formData = Map.of(
                "action_type", "cancel_pay",
                "transaction_id", String.valueOf(transactionId)
            );

            String encodedForm = encodeFormData(formData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
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
