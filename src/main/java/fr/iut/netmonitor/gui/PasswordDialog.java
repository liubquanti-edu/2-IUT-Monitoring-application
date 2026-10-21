package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/** Saisie de mots de passe (changement du sien, réinitialisation par l'administrateur). */
public final class PasswordDialog {

    private PasswordDialog() {
    }

    /** Demande un nouveau mot de passe avec confirmation. */
    public static Optional<String> ask(String header) {
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        Label error = new Label();
        error.getStyleClass().add("error-label");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nouveau mot de passe"), password);
        form.addRow(1, new Label("Confirmation"), confirm);
        form.add(error, 0, 2, 2, 1);

        Dialog<ButtonType> dialog = dialog(header, form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            if (!password.getText().equals(confirm.getText())) {
                error.setText("Les deux mots de passe sont différents.");
                e.consume();
            }
        });
        return dialog.showAndWait().filter(b -> b == ButtonType.OK).map(b -> password.getText());
    }

    public static void changeOwnPassword(Session session) {
        PasswordField current = new PasswordField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        Label error = new Label();
        error.getStyleClass().add("error-label");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Mot de passe actuel"), current);
        form.addRow(1, new Label("Nouveau mot de passe"), password);
        form.addRow(2, new Label("Confirmation"), confirm);
        form.add(error, 0, 3, 2, 1);

        Dialog<ButtonType> dialog = dialog("Changer mon mot de passe", form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            if (!password.getText().equals(confirm.getText())) {
                error.setText("Les deux mots de passe sont différents.");
                e.consume();
                return;
            }
            try {
                session.context().users().changeOwnPassword(session.user(), current.getText(), password.getText());
            } catch (NetMonitorException ex) {
                error.setText(ex.getMessage());
                e.consume();
            }
        });
        if (dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
            session.dataChanged();
            Dialogs.info("Mot de passe modifié.");
        }
    }

    private static Dialog<ButtonType> dialog(String header, GridPane form) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Mot de passe");
        dialog.setHeaderText(header);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog;
    }
}
