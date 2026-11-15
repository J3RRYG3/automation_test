# Evaluación Final QA Automation

Pruebas automatizadas del flujo de compra de https://teststore.automationtesting.co.uk/index.php
con **Java 21, Selenium WebDriver 4, JUnit 5 y Maven**.

| Script | Requerimiento | Escenarios |
|--------|---------------|------------|
| `EX001` | RF-001 Compra con usuario autenticado | Parametrizado: un producto / varios productos |
| `EX002` | RF-002 Compra como invitado (Guest Checkout) | Parametrizado: dos clientes ficticios |

Ambos escenarios validan que los productos estén en el carrito y finalizan verificando el mensaje
**"Your order is confirmed"**. EX001 además valida que el inicio de sesión fue exitoso.

## Estructura

```
src/test/java/com/evaluacion/
  BaseTest.java   -> WebDriver, @BeforeEach/@AfterEach, esperas explícitas y pasos comunes del checkout
  EX001.java      -> RF-001
  EX002.java      -> RF-002
  Product.java    -> productos del catálogo usados en las pruebas
  Customer.java   -> datos ficticios del cliente
src/test/resources/config.properties -> URL, tiempo de espera, headless y usuario registrado
```

## Ejecución

Requisitos: JDK 21, Maven 3.9+ y Google Chrome (Selenium Manager descarga el driver automáticamente).

```bash
mvn test                      # ejecuta EX001 y EX002
mvn test -Dtest=EX001         # solo compra autenticada
mvn test -Dtest=EX002         # solo compra como invitado
mvn test -Dheadless=true      # sin abrir la ventana del navegador
mvn test -Duser.email=otro@correo.com -Duser.password=Secreto1   # otro usuario registrado
```
