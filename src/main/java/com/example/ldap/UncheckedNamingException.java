package com.example.ldap;

public final class UncheckedNamingException
extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UncheckedNamingException() {
    }

    public UncheckedNamingException(final String message) {
        super(message);
    }

    public UncheckedNamingException(final Throwable throwable) {
        super(throwable);
    }

    public UncheckedNamingException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

}
