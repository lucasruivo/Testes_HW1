package com.example.zeromonos;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.Select;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

public class CitizenSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private String token;

    @Given("que estou na página inicial do cidadão")
    public void open_homepage() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080/index.html"); // adapta o porto conforme necessário
    }

    @When("preencho o formulário com {string}, {string}, {string}, {string}")
    public void fill_form(String municipality, String description, String date, String timeslot) {
        WebElement municipalitySelect = driver.findElement(By.id("municipality"));
        WebElement descInput = driver.findElement(By.id("description"));
        WebElement dateInput = driver.findElement(By.id("requestedDate"));
        WebElement timeslotSelect = driver.findElement(By.id("timeslot"));

        // Aguarda pelo carregamento das opções de municípios via fetch("/api/municipios")
        wait.until(d -> d.findElements(By.cssSelector("#municipality option")).size() > 0);
        Select muniSel = new Select(municipalitySelect);
        try {
            muniSel.selectByVisibleText(municipality);
        } catch (NoSuchElementException ignore) {
            // Município inválido (ex.: "Atlantis"): força valor vazio para disparar validação
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", municipalitySelect);
        }

        descInput.sendKeys(description);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", dateInput, date);

        // Aguarda que os timeslots estejam preenchidos
        wait.until(d -> d.findElements(By.cssSelector("#timeslot option")).size() > 0);
        Select timeSel = new Select(timeslotSelect);
        try {
            timeSel.selectByVisibleText(timeslot);
        } catch (NoSuchElementException ignore) {
            // Timeslot inexistente (ex.: "10:00-11:00"): escolhe o primeiro válido
            timeSel.selectByIndex(0);
        }
    }

    @When("submeto o pedido")
    public void submit_booking() {
        ((JavascriptExecutor) driver)
                .executeScript("document.getElementById('bookingForm').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));");
    }

    @Then("devo ver uma mensagem de sucesso com um código")
    public void check_success_message() {
        WebElement bookingSuccess = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingSuccess")));
        String text = bookingSuccess.getText();
        assertThat(text).contains("Pedido criado com sucesso!");
        token = text.replaceAll(".*código: ", "").trim();
    }

    @When("consulto o pedido com o token retornado")
    public void consult_booking_with_token() {
        WebElement tokenInput = driver.findElement(By.id("tokenInput"));
        WebElement checkBtn = driver.findElement(By.id("checkBtn"));
        tokenInput.sendKeys(token);
        checkBtn.click();
    }

    @When("consulto o pedido com token {string}")
    public void consult_booking_with_specific_token(String inputToken) {
        WebElement tokenInput = driver.findElement(By.id("tokenInput"));
        WebElement checkBtn = driver.findElement(By.id("checkBtn"));
        tokenInput.sendKeys(inputToken);
        checkBtn.click();
    }

    @Then("devo ver o pedido com descrição {string} e horário {string}")
    public void verify_booking_details(String description, String timeslot) {
        WebElement resultDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        String text = resultDiv.getText();
        assertThat(text).contains(description);
        assertThat(text).contains(timeslot);
    }

    @When("cancelo o pedido")
    public void cancel_booking() {
        WebElement resultDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        WebElement cancelBtn = resultDiv.findElement(By.tagName("button"));
        cancelBtn.click();
        driver.switchTo().alert().accept();
    }

    @Then("o estado do pedido deve ser {string}")
    public void verify_booking_state(String state) {
        WebElement resultDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        wait.until(d -> resultDiv.getText().contains("Estado: " + state));
        assertThat(resultDiv.getText()).contains(state);
        driver.quit();
    }

    @Then("devo ver a mensagem de erro {string}")
    public void check_error_message(String msg) {
        WebElement bookingError = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingError")));
        assertThat(bookingError.getText()).contains(msg);
        driver.quit();
    }

    @Then("devo ver a mensagem {string}")
    public void check_generic_message(String msg) {
        // Tenta primeiro a área de erro; se não existir, valida na área de resultados
        WebElement el;
        try {
            el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bookingError")));
        } catch (TimeoutException ex) {
            el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusResult")));
        }
        assertThat(el.getText()).contains(msg);
        driver.quit();
    }
}