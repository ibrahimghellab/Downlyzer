package fr.efrei.downlyzer.exception;

public abstract class ExceptionMetier extends RuntimeException {

    protected ExceptionMetier(String message) {
        super(message);
    }

    protected ExceptionMetier(String message, Throwable cause) {
        super(message, cause);
    }
}

