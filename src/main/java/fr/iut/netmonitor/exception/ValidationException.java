package fr.iut.netmonitor.exception;

/** Saisie invalide : nom vide, adresse IP incorrecte, doublon… */
public class ValidationException extends NetMonitorException {

    public ValidationException(String message) {
        super(message);
    }
}
