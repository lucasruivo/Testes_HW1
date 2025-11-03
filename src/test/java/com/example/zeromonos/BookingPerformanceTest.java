package com.example.zeromonos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingPerformanceTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void measureBookingCreationTime() throws Exception {
        String bookingJson = """
        {
          "municipality": "Coimbra",
          "description": "Teste Performance",
          "requestedDate": "2025-11-20",
          "timeSlot": "11:00-13:00"
        }
        """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(bookingJson))
                .build();

        long start = System.nanoTime();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Tempo de criação do booking: " + durationMs + "ms");

        assertThat(response.statusCode()).isEqualTo(200);
    }
}