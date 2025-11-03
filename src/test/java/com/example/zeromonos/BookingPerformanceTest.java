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

  @Test
  void measureBookingFetchByToken() throws Exception {
      // Criar uma reserva primeiro
      String bookingJson = """
      {
        "municipality": "Coimbra",
        "description": "Teste Fetch",
        "requestedDate": "2025-11-20",
        "timeSlot": "13:00-15:00"
      }
      """;

      HttpRequest createRequest = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/bookings"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(bookingJson))
              .build();

      HttpResponse<String> createResp = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
      assertThat(createResp.statusCode()).isEqualTo(200);

      String token = createResp.body().replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

      // Medir tempo de fetch
      HttpRequest fetchRequest = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/bookings/" + token))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();

      long start = System.nanoTime();
      HttpResponse<String> fetchResp = client.send(fetchRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = (System.nanoTime() - start) / 1_000_000;

      System.out.println("Tempo de fetch por token: " + durationMs + "ms");
      assertThat(fetchResp.statusCode()).isEqualTo(200);
  }

  @Test
  void measureBookingStatusUpdateTime() throws Exception {
      // Criar uma reserva
      String bookingJson = """
      {
        "municipality": "Coimbra",
        "description": "Teste Update",
        "requestedDate": "2025-11-20",
        "timeSlot": "15:00-17:00"
      }
      """;

      HttpRequest createRequest = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/bookings"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(bookingJson))
              .build();

      HttpResponse<String> createResp = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
      assertThat(createResp.statusCode()).isEqualTo(200);

      String token = createResp.body().replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

      // Medir tempo de update
      String updateJson = """
      {
        "status": "EM_PROG"
      }
      """;

      HttpRequest updateRequest = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/bookings/" + token + "?status=EM_PROG"))
              .header("Content-Type", "application/json")
              .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
              .timeout(Duration.ofSeconds(5))
              .build();

      long start = System.nanoTime();
      HttpResponse<String> updateResp = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = (System.nanoTime() - start) / 1_000_000;

      System.out.println("Tempo de update do estado: " + durationMs + "ms");
      assertThat(updateResp.statusCode()).isEqualTo(200);
  }

  @Test
  void measureAllBookingsFetchTime() throws Exception {
      HttpRequest listRequest = HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:" + port + "/api/bookings"))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();

      long start = System.nanoTime();
      HttpResponse<String> listResp = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
      long durationMs = (System.nanoTime() - start) / 1_000_000;

      System.out.println("Tempo de listagem de todas reservas: " + durationMs + "ms");
      assertThat(listResp.statusCode()).isEqualTo(200);
  }
}