package com.evaluacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EX001 - RF-001: Compra de productos con usuario autenticado.
 */
@DisplayName("EX001 - Compra con usuario autenticado")
class EX001 extends BaseTest {

    /** Direccion ficticia que se usa solo si la cuenta aun no tiene una direccion guardada. */
    private static final Customer SHIPPING_DATA =
            new Customer("Carlos", "Tester", null, "742 Evergreen Terrace", "Springfield", "10001");

    static Stream<Arguments> productSets() {
        return Stream.of(
                Arguments.of("Un producto", List.of(Product.HUMMINGBIRD_TSHIRT)),
                Arguments.of("Varios productos", List.of(Product.MUG_BEST_IS_YET_TO_COME, Product.POSTER_BEST_IS_YET_TO_COME))
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("productSets")
    @DisplayName("Compra autenticada finaliza con 'Your order is confirmed'")
    void authenticatedUserCanPurchaseProducts(String scenario, List<Product> products) {
        // 1. Ingresar al sitio web
        openHomePage();

        // 2. Autenticarse con usuario y contrasena validos
        login(config("user.email"), config("user.password"));

        // Validacion: el usuario inicio sesion correctamente
        assertTrue(isDisplayed(By.className("logout")),
                "No se muestra el enlace 'Sign out': el usuario no inicio sesion");
        String accountName = waitVisible(By.cssSelector(".user-info a.account span")).getText();
        assertFalse(accountName.isBlank(), "No se muestra el nombre del usuario autenticado en el encabezado");

        // Precondicion: carrito vacio para que el escenario sea independiente de ejecuciones anteriores
        emptyCart();

        // 3-5. Navegar por el catalogo, seleccionar y agregar productos al carrito
        List<String> addedProducts = addProductsToCart(products);

        // 6. Acceder al carrito y validar que los productos esten en el carrito
        openCart();
        assertProductsInCart(addedProducts);

        // 7. Iniciar el checkout (el paso de informacion personal ya aparece completo al estar autenticado)
        proceedToCheckout();

        // 8-9. Completar la informacion requerida y avanzar hasta la confirmacion
        completeAddressStep(SHIPPING_DATA);
        completeShippingStep();

        // 10. Confirmar la orden
        completePaymentStepAndPlaceOrder();

        // Validacion: mensaje final de orden confirmada
        assertOrderConfirmed();
    }

    private void login(String email, String password) {
        click(By.cssSelector(".user-info a[href*='controller=my-account'], .user-info a[href*='authentication']"));
        type(By.id("field-email"), email);
        type(By.name("password"), password);
        click(By.id("submit-login"));
        waitVisible(By.cssSelector(".user-info a.logout"));
    }
}
