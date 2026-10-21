package fr.iut.netmonitor.exception;

/** L'utilisateur connecté n'a pas le droit d'effectuer l'action demandée. */
public class AccessDeniedException extends NetMonitorException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
