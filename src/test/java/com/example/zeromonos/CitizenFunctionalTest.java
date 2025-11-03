package com.example.zeromonos;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CitizenFunctionalTest {

    @LocalServerPort
    private int port;  // Porta aleatória do Spring Boot

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    @Test
    void shouldCreateConsultAndCancelBooking() {
        // Usar a porta aleatória do Spring Boot
        driver.get("http://localhost:" + port + "/index.html");

        // --- Preencher formulário ---
        WebElement municipality = driver.findElement(By.id("municipality"));
        WebElement description = driver.findElement(By.id("description"));
        WebElement requestedDate = driver.findElement(By.id("requestedDate"));

        // Definir a data usando JavascriptExecutor
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];", 
            requestedDate, 
            "2025-11-19"
        );

        // Esperar pelo select de horários estar populado
        wait.until(d -> d.findElements(By.cssSelector("#timeslot option")).size() > 0);
        WebElement timeslot = driver.findElement(By.id("timeslot"));

        // Preencher restante formulário
        municipality.sendKeys("Lisboa");
        description.sendKeys("Teste funcional");
        timeslot.sendKeys("09:00-11:00");

        // Esperar que todos os campos estejam com valor definido
        wait.until(d -> !municipality.getDomProperty("value").isEmpty());
        wait.until(d -> !description.getDomProperty("value").isEmpty());
        wait.until(d -> !timeslot.getDomProperty("value").isEmpty());

        // Submeter o formulário via JS para disparar o listener corretamente
        ((JavascriptExecutor) driver)
                .executeScript("document.getElementById('bookingForm').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));");

        // --- Verificar mensagem de sucesso ---
        WebElement bookingSuccess = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingSuccess")));
        String successText = bookingSuccess.getText();
        assertThat(successText).contains("Pedido criado com sucesso!");

        // Extrair token do texto
        String token = successText.replaceAll(".*código: ", "").trim();

        // --- Consultar pedido pelo token ---
        WebElement tokenInput = driver.findElement(By.id("tokenInput"));
        WebElement checkBtn = driver.findElement(By.id("checkBtn"));

        tokenInput.sendKeys(token);
        checkBtn.click();

        WebElement resultDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        String resultText = resultDiv.getText();
        assertThat(resultText).contains("Teste funcional");
        assertThat(resultText).contains("09:00-11:00");

        // --- Cancelar pedido ---
        WebElement cancelBtn = resultDiv.findElement(By.tagName("button"));
        cancelBtn.click();

        // Confirm dialog
        driver.switchTo().alert().accept();

        // Esperar que o status seja atualizado para CANCELADO
        wait.until(d -> resultDiv.getText().contains("Estado: CANCELADO"));
        assertThat(resultDiv.getText()).contains("CANCELADO");
    }
}