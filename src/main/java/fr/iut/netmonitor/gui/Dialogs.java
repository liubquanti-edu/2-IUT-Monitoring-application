package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Boîtes de dialogue communes. */
public final class Dialogs {

    /** Action métier pouvant échouer avec un message destiné à l'utilisateur. */
    @FunctionalInterface
    public interface Action {
        void run() throws NetMonitorException;
    }

    private Dialogs() {
    }

    /** Exécute l'action et affiche l'erreur éventuelle. Retourne vrai si l'action a réussi. */
    public static boolean attempt(Action action) {
        try {
            action.run();
            return true;
        } catch (NetMonitorException e) {
            error(e.getMessage());
            return false;
        }
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("NetMonitor");
        alert.setHeaderText("Erreur");
        alert.showAndWait();
    }

    public static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("NetMonitor");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("NetMonitor");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
