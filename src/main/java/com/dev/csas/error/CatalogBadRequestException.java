package com.dev.csas.error;

public class CatalogBadRequestException extends RuntimeException {

    public CatalogBadRequestException(String message) {
        super(message);
    }
}
