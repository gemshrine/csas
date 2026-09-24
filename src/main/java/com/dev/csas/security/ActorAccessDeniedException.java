package com.dev.csas.security;

public class ActorAccessDeniedException extends RuntimeException {
    public ActorAccessDeniedException(String message) {
        super(message);
    }
}
