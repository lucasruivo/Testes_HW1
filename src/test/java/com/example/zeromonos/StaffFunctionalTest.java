package com.example.zeromonos;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.example.zeromonos.data.BookingRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

    private WebDriver driver;
    private WebDriverWait wait;
    private String bookingToken;
    private LocalDate bookingDate;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() throws Exception {
        // Limpar todas as reservas antes de cada teste
        bookingRepository.deleteAll();

        // Calcular data válida (>=3 dias à frente e não fim de semana)
        bookingDate = LocalDate.now().plusDays(3);
        while (bookingDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || bookingDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            bookingDate = bookingDate.plusDays(1);
        }

        // Criar reserva via repositório para garantir status inicial
        var booking = new com.example.zeromonos.data.Booking();
        booking.setMunicipality("Lisboa");
        booking.setDescription("Teste Staff");
        booking.setRequestedDate(bookingDate);
        booking.setTimeSlot("09:00-11:00");
        bookingRepository.save(booking);

        bookingToken = booking.getToken();
        assertThat(bookingToken).isNotBlank();

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
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

        WebElement bookingRow = findBookingRow("Lisboa", "Teste Staff", bookingDate.toString(), "09:00-11:00");
        assertThat(bookingRow).withFailMessage("Booking criado não encontrado na tabela!").isNotNull();

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

        WebElement refreshedRow = findBookingRow("Lisboa", "Teste Staff", bookingDate.toString(), "09:00-11:00");
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
        WebElement finalRow = findBookingRow("Lisboa", "Teste Staff", bookingDate.toString(), "09:00-11:00");
        WebElement finalStatus = finalRow.findElement(By.cssSelector("td:nth-child(5)"));
        wait.until(d -> finalStatus.getText().equalsIgnoreCase("CONCLUIDO"));
        assertThat(finalStatus.getText()).isEqualTo("CONCLUIDO");

        pause(500);

        List<WebElement> actions = finalRow.findElements(By.cssSelector("td:nth-child(6) button"));
        assertThat(actions).isEmpty();
    }

    @Test
    void ShouldShowAllBookingsWhenMunicipalityIsEmpty() {
        driver.get("http://localhost:" + port + "/staff.html");

        WebElement municipalityInput = driver.findElement(By.id("municipalityInput"));
        WebElement loadBtn = driver.findElement(By.id("loadBtn"));

        municipalityInput.clear();
        loadBtn.click();

        pause(500);

        List<WebElement> allRows = driver.findElements(By.cssSelector("#bookingsTable tbody tr"));
        assertThat(allRows).isNotEmpty();
        assertThat(allRows.stream().anyMatch(r -> r.getText().contains("Teste Staff"))).isTrue();
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