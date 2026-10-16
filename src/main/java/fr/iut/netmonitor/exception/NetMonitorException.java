package fr.iut.netmonitor.exception;

/** Exception métier de base : son message est destiné à l'utilisateur. */
public class NetMonitorException extends Exception {

    public NetMonitorException(String message) {
        super(message);
    }

    public NetMonitorException(String message, Throwable cause) {
        super(message, cause);
    }
}
