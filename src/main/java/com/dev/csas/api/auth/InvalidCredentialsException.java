package com.dev.csas.api.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Неверный логин или пароль");
    }
}
