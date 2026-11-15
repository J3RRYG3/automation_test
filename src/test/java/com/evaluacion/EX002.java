package com.evaluacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EX002 - RF-002: Compra de productos sin autenticacion (Guest Checkout).
 */
@DisplayName("EX002 - Compra como invitado (Guest Checkout)")
class EX002 extends BaseTest {

    @ParameterizedTest(name = "{index}: invitado {0} {1}")
    @CsvSource({
            // nombre, apellido,  direccion,              ciudad,     codigo postal
            "Ana,      Prueba,    123 Fake Street,        New York,   10001",
            "Luis,     Ficticio,  456 Imaginary Avenue,   Miami,      33101"
    })
    @DisplayName("Compra como invitado finaliza con 'Your order is confirmed'")
    void guestCanPurchaseProducts(String firstName, String lastName, String address, String city, String postcode) {
        // Email ficticio y unico por ejecucion
        String email = "guest." + firstName.toLowerCase() + "." + System.currentTimeMillis() + "@example.com";
        Customer guest = new Customer(firstName, lastName, email, address, city, postcode);

        // 1. Ingresar al sitio web
        openHomePage();
        assertTrue(isDisplayed(By.cssSelector(".user-info a[href*='my-account'], .user-info a[href*='authentication']")),
                "La prueba debe iniciar sin sesion activa");

        // 2-4. Navegar por el catalogo, seleccionar y agregar productos al carrito
        List<String> addedProducts = addProductsToCart(List.of(
                Product.HUMMINGBIRD_SWEATER,
                Product.MUG_ADVENTURE_BEGINS));

        // 5. Acceder al carrito y validar los productos agregados
        openCart();
        assertProductsInCart(addedProducts);

        // 6. Iniciar el checkout sin autenticarse
        proceedToCheckout();

        // 7. Completar la informacion requerida con datos ficticios
        fillGuestPersonalInformation(guest);
        completeAddressStep(guest);

        // 8. Continuar hasta la pantalla final de confirmacion
        completeShippingStep();

        // 9. Confirmar la orden
        completePaymentStepAndPlaceOrder();

        // Validacion: mensaje final de orden confirmada
        assertOrderConfirmed();
    }

    /** Paso "Personal Information" como invitado (sin contrasena). */
    private void fillGuestPersonalInformation(Customer guest) {
        By form = By.id("customer-form");
        waitVisible(form);

        check(By.cssSelector("#customer-form input[name='id_gender'][value='1']"));
        type(By.cssSelector("#customer-form input[name='firstname']"), guest.firstName());
        type(By.cssSelector("#customer-form input[name='lastname']"), guest.lastName());
        type(By.cssSelector("#customer-form input[name='email']"), guest.email());

        // Marca las casillas obligatorias (politica de privacidad / GDPR, terminos del cliente)
        for (WebElement checkbox : driver.findElements(By.cssSelector("#customer-form input[type='checkbox'][required]"))) {
            check(checkbox);
        }
        click(By.cssSelector("#customer-form button[name='continue']"));
    }
}
