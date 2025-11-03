package com.example.zeromonos;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffFunctionalTest {

    @LocalServerPort
    private int port; 

    private WebDriver driver;
    private WebDriverWait wait;
    private String bookingToken;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() throws Exception {

        HttpClient client = HttpClient.newHttpClient();
        String bookingJson = """
        {
          "municipality": "Lisboa",
          "description": "Teste Staff",
          "requestedDate": "2025-11-19",
          "timeSlot": "09:00-11:00"
        }
        """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bookingJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        bookingToken = response.body().replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(bookingToken).isNotBlank();

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    @Test
    void FindAndChangeCreatedBookingStatus() {
        driver.get("http://localhost:" + port + "/staff.html");

        WebElement municipalityInput = driver.findElement(By.id("municipalityInput"));
        WebElement loadBtn = driver.findElement(By.id("loadBtn"));

        loadBtn.click();
        wait.until(d -> d.findElements(By.cssSelector("#bookingsTable tbody tr")).size() > 0);
        List<WebElement> allRows = driver.findElements(By.cssSelector("#bookingsTable tbody tr"));
        assertThat(allRows).isNotEmpty();

        municipalityInput.clear();
        municipalityInput.sendKeys("Lisboa");
        loadBtn.click();
        
        pause(500);

        wait.until(d -> findBookingRow("Lisboa", "Teste Staff", "2025-11-19", "09:00-11:00") != null);
        List<WebElement> filteredRows = driver.findElements(By.cssSelector("#bookingsTable tbody tr"));
        assertThat(filteredRows).isNotEmpty();
        assertThat(filteredRows.stream().allMatch(r ->
                r.findElement(By.cssSelector("td:first-child")).getText().equalsIgnoreCase("Lisboa")
        )).isTrue();

        WebElement bookingRow = findBookingRow("Lisboa", "Teste Staff", "2025-11-19", "09:00-11:00");
        assertThat(bookingRow)
                .withFailMessage("Booking criado não encontrado na tabela!")
                .isNotNull();

        WebElement actionBtn = bookingRow.findElement(By.cssSelector("td:nth-child(6) button"));
        assertThat(actionBtn.getText()).isEqualTo("Em Progresso");

        wait.until(ExpectedConditions.elementToBeClickable(actionBtn));
        actionBtn.click();

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        assertThat(alert.getText()).contains("EM_PROG");
        alert.accept();

        pause(500);

        loadBtn.click();
        wait.until(d -> d.findElements(By.cssSelector("#bookingsTable tbody tr")).size() > 0);

        WebElement refreshedRow = findBookingRow("Lisboa", "Teste Staff", "2025-11-19", "09:00-11:00");
        WebElement refreshedStatus = refreshedRow.findElement(By.cssSelector("td:nth-child(5)"));
        wait.until(d -> refreshedStatus.getText().equalsIgnoreCase("EM_PROG"));
        assertThat(refreshedStatus.getText()).isEqualTo("EM_PROG");

        pause(500);

        WebElement nextBtn = refreshedRow.findElement(By.cssSelector("td:nth-child(6) button"));
        assertThat(nextBtn.getText()).isEqualTo("Concluído");
        wait.until(ExpectedConditions.elementToBeClickable(nextBtn));
        nextBtn.click();

        Alert alert2 = wait.until(ExpectedConditions.alertIsPresent());
        assertThat(alert2.getText()).contains("CONCLUIDO");
        alert2.accept();

        pause(500);

        loadBtn.click();
        wait.until(d -> d.findElements(By.cssSelector("#bookingsTable tbody tr")).size() > 0);
        WebElement finalRow = findBookingRow("Lisboa", "Teste Staff", "2025-11-19", "09:00-11:00");
        WebElement finalStatus = finalRow.findElement(By.cssSelector("td:nth-child(5)"));
        wait.until(d -> finalStatus.getText().equalsIgnoreCase("CONCLUIDO"));
        assertThat(finalStatus.getText()).isEqualTo("CONCLUIDO");
        pause(500);

        List<WebElement> actions = finalRow.findElements(By.cssSelector("td:nth-child(6) button"));
        assertThat(actions).isEmpty();
    }

    private WebElement findBookingRow(String municipality, String description, String date, String timeSlot) {
        List<WebElement> rows = driver.findElements(By.cssSelector("#bookingsTable tbody tr"));
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() >= 4 &&
                cells.get(0).getText().equalsIgnoreCase(municipality) &&
                cells.get(1).getText().equalsIgnoreCase(description) &&
                cells.get(2).getText().contains(date) &&
                cells.get(3).getText().contains(timeSlot)) {
                return row;
            }
        }
        return null;
    }

    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}