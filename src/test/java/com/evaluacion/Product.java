package com.evaluacion;

/**
 * Producto del catalogo que se agregara al carrito.
 *
 * @param id         valor del atributo data-id-product en el listado
 * @param categoryId categoria del menu donde se encuentra (3=Clothes, 6=Accessories, 9=Art)
 * @param quantity   cantidad a comprar
 */
public record Product(int id, int categoryId, int quantity) {

    public static final Product HUMMINGBIRD_TSHIRT = new Product(1, 3, 1);
    public static final Product HUMMINGBIRD_SWEATER = new Product(2, 3, 1);
    public static final Product POSTER_BEST_IS_YET_TO_COME = new Product(3, 9, 1);
    public static final Product MUG_BEST_IS_YET_TO_COME = new Product(6, 6, 2);
    public static final Product MUG_ADVENTURE_BEGINS = new Product(7, 6, 1);
}
