package com.evaluacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clase base con la configuracion del WebDriver y los pasos comunes del flujo de compra.
 * Cada prueba abre y cierra su propio navegador para garantizar ejecucion independiente.
 */
public abstract class BaseTest {

    protected static final String ORDER_CONFIRMED_MESSAGE = "Your order is confirmed";

    protected static final Properties CONFIG = loadConfig();

    protected WebDriver driver;
    protected WebDriverWait wait;

    // ---------------------------------------------------------------------
    // Ciclo de vida JUnit
    // ---------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized", "--disable-notifications", "--lang=en");
        if (Boolean.parseBoolean(config("headless"))) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        driver = new ChromeDriver(options);
        int waitSeconds = Integer.parseInt(config("explicit.wait.seconds"));
        wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ---------------------------------------------------------------------
    // Configuracion
    // ---------------------------------------------------------------------

    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = BaseTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("No se encontro config.properties en src/test/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer config.properties", e);
        }
        return properties;
    }

    /** Devuelve el valor de configuracion; una propiedad -Dclave=valor tiene prioridad. */
    protected static String config(String key) {
        return System.getProperty(key, CONFIG.getProperty(key));
    }

    // ---------------------------------------------------------------------
    // Utilidades de interaccion con esperas explicitas
    // ---------------------------------------------------------------------

    protected WebElement waitVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // Algun overlay (modal, animacion) cubre el elemento: se usa click por JavaScript.
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /**
     * Selecciona un radio/checkbox del tema (input real oculto detras de un span decorativo).
     * Si el click nativo es bloqueado, se marca mediante JavaScript.
     */
    protected void check(WebElement input) {
        if (input.isSelected()) {
            return;
        }
        try {
            input.click();
        } catch (ElementNotInteractableException e) {
            // ElementNotInteractableException tambien cubre ElementClickInterceptedException
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", input);
        }
    }

    protected void check(By locator) {
        check(wait.until(ExpectedConditions.presenceOfElementLocated(locator)));
    }

    protected void type(By locator, String text) {
        WebElement element = waitVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Pasos del flujo de compra
    // ---------------------------------------------------------------------

    protected void openHomePage() {
        driver.get(config("base.url"));
        waitVisible(By.id("_desktop_logo"));
    }

    /** Navega por el menu principal hacia una categoria (3=Clothes, 6=Accessories, 9=Art). */
    protected void openCategory(int categoryId) {
        click(By.cssSelector("#category-" + categoryId + " > a"));
        waitVisible(By.id("js-product-list"));
    }

    /**
     * Abre un producto desde el listado de la categoria, lo agrega al carrito
     * y devuelve el nombre del producto mostrado en su pagina de detalle.
     */
    protected String addProductToCart(Product product) {
        openCategory(product.categoryId());
        click(By.cssSelector("article[data-id-product='" + product.id() + "'] .product-title a"));

        String productName = waitVisible(By.cssSelector("h1.h1")).getText().trim();

        if (product.quantity() > 1) {
            type(By.id("quantity_wanted"), String.valueOf(product.quantity()));
        }
        click(By.cssSelector("button.add-to-cart[data-button-action='add-to-cart']"));

        // Modal de confirmacion de producto agregado
        WebElement modalTitle = waitVisible(By.cssSelector("#blockcart-modal .modal-title"));
        assertTrue(modalTitle.getText().contains("successfully added"),
                "El modal no confirma que el producto se agrego al carrito: " + modalTitle.getText());

        click(By.cssSelector("#blockcart-modal .cart-content-btn button.btn-secondary")); // Continue shopping
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("blockcart-modal")));
        return productName;
    }

    protected List<String> addProductsToCart(List<Product> products) {
        List<String> addedNames = new ArrayList<>();
        for (Product product : products) {
            addedNames.add(addProductToCart(product));
        }
        return addedNames;
    }

    /**
     * Accede al carrito. Si el icono del encabezado tiene enlace (carrito con productos) se usa;
     * de lo contrario se navega directo a la pagina del carrito para que la Assertion reporte el problema.
     */
    protected void openCart() {
        By headerCartLink = By.cssSelector("#_desktop_cart .blockcart a");
        if (isDisplayed(headerCartLink)) {
            click(headerCartLink);
        } else {
            driver.get(config("base.url") + "?controller=cart&action=show");
        }
        waitVisible(By.cssSelector(".cart-grid"));
    }

    /**
     * Elimina los productos que hayan quedado en el carrito de ejecuciones anteriores
     * (el carrito de un usuario registrado se conserva en el servidor entre sesiones).
     */
    protected void emptyCart() {
        driver.get(config("base.url") + "?controller=cart&action=show");
        waitVisible(By.cssSelector(".cart-grid"));
        By removeButtons = By.cssSelector(".cart-item a.remove-from-cart");
        List<WebElement> items = driver.findElements(removeButtons);
        while (!items.isEmpty()) {
            WebElement firstItem = items.get(0);
            click(removeButtons);
            wait.until(ExpectedConditions.stalenessOf(firstItem));
            items = driver.findElements(removeButtons);
        }
        openHomePage();
    }

    protected List<String> getCartProductNames() {
        List<String> names = new ArrayList<>();
        for (WebElement item : driver.findElements(By.cssSelector(".cart-item .product-line-info a.label"))) {
            names.add(item.getText().trim());
        }
        return names;
    }

    /** Valida (Assertion) que cada producto agregado aparezca en el carrito. */
    protected void assertProductsInCart(List<String> expectedNames) {
        List<String> cartNames = getCartProductNames();
        assertTrue(cartNames.size() >= expectedNames.size(),
                "El carrito tiene menos productos de los esperados. Carrito: " + cartNames);
        for (String expected : expectedNames) {
            boolean found = cartNames.stream().anyMatch(name -> name.equalsIgnoreCase(expected));
            assertTrue(found, "El producto '" + expected + "' no se encuentra en el carrito: " + cartNames);
        }
    }

    protected void proceedToCheckout() {
        click(By.xpath("//div[contains(@class,'cart-detailed-actions')]//a[contains(@class,'btn-primary')]"));
        waitVisible(By.id("checkout-personal-information-step"));
    }

    /** Paso "Addresses": llena el formulario solo si el cliente no tiene direccion guardada. */
    protected void completeAddressStep(Customer customer) {
        By address1 = By.cssSelector("#checkout-addresses-step input[name='address1']");
        waitVisible(By.id("checkout-addresses-step"));

        if (isDisplayed(address1)) {
            type(address1, customer.address());
            type(By.cssSelector("#checkout-addresses-step input[name='city']"), customer.city());

            By stateSelect = By.cssSelector("#checkout-addresses-step select[name='id_state']");
            if (isDisplayed(stateSelect)) {
                new Select(driver.findElement(stateSelect)).selectByIndex(1);
            }
            type(By.cssSelector("#checkout-addresses-step input[name='postcode']"), customer.postcode());
        }
        click(By.cssSelector("#checkout-addresses-step button[name='confirm-addresses']"));
    }

    protected void completeShippingStep() {
        click(By.cssSelector("#checkout-delivery-step button[name='confirmDeliveryOption']"));
    }

    /** Paso "Payment": selecciona el primer metodo de pago, acepta terminos y confirma la orden. */
    protected void completePaymentStepAndPlaceOrder() {
        By paymentOptions = By.cssSelector("#checkout-payment-step input[name='payment-option']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("checkout-payment-step")));
        check(paymentOptions);

        check(By.cssSelector("#checkout-payment-step input[name='conditions_to_approve[terms-and-conditions]']"));
        click(By.cssSelector("#payment-confirmation button[type='submit']"));
    }

    protected String getOrderConfirmationMessage() {
        return waitVisible(By.cssSelector("#content-hook_order_confirmation .card-title")).getText();
    }

    /**
     * Valida que se muestre "Your order is confirmed".
     * El tema aplica text-transform: uppercase y antepone un icono, por eso se compara sin distinguir mayusculas.
     */
    protected void assertOrderConfirmed() {
        String confirmation = getOrderConfirmationMessage();
        assertTrue(confirmation.toLowerCase().contains(ORDER_CONFIRMED_MESSAGE.toLowerCase()),
                "No se mostro el mensaje '" + ORDER_CONFIRMED_MESSAGE + "'. Mensaje obtenido: " + confirmation);
    }
}
