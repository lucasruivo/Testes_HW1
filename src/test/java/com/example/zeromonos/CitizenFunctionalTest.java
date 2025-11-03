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
    private int port;

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
    void CreateConsultAndCancelBooking() {

        driver.get("http://localhost:" + port + "/index.html");

        WebElement municipality = driver.findElement(By.id("municipality"));
        WebElement description = driver.findElement(By.id("description"));
        WebElement requestedDate = driver.findElement(By.id("requestedDate"));

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];", 
            requestedDate, 
            "2025-11-19"
        );

        wait.until(d -> d.findElements(By.cssSelector("#timeslot option")).size() > 0);
        WebElement timeslot = driver.findElement(By.id("timeslot"));

        municipality.sendKeys("Lisboa");
        description.sendKeys("Teste funcional");
        timeslot.sendKeys("09:00-11:00");

        wait.until(d -> !municipality.getDomProperty("value").isEmpty());
        wait.until(d -> !description.getDomProperty("value").isEmpty());
        wait.until(d -> !timeslot.getDomProperty("value").isEmpty());

        ((JavascriptExecutor) driver)
                .executeScript("document.getElementById('bookingForm').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));");

        WebElement bookingSuccess = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingSuccess")));
        String successText = bookingSuccess.getText();
        assertThat(successText).contains("Pedido criado com sucesso!");

        String token = successText.replaceAll(".*código: ", "").trim();

        WebElement tokenInput = driver.findElement(By.id("tokenInput"));
        WebElement checkBtn = driver.findElement(By.id("checkBtn"));

        tokenInput.sendKeys(token);
        checkBtn.click();

        WebElement resultDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        String resultText = resultDiv.getText();
        assertThat(resultText).contains("Teste funcional");
        assertThat(resultText).contains("09:00-11:00");

        WebElement cancelBtn = resultDiv.findElement(By.tagName("button"));
        cancelBtn.click();

        driver.switchTo().alert().accept();

        wait.until(d -> resultDiv.getText().contains("Estado: CANCELADO"));
        assertThat(resultDiv.getText()).contains("CANCELADO");
    }
}