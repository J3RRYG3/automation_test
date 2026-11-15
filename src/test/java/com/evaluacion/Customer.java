package com.evaluacion;

/** Datos ficticios del cliente utilizados durante el checkout. */
public record Customer(String firstName, String lastName, String email,
                       String address, String city, String postcode) {
}
