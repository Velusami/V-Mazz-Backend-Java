package com.example.catalog.vmazz.exception;

/** The request is valid but conflicts with the current state (duplicate, not enough stock, wrong status ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
