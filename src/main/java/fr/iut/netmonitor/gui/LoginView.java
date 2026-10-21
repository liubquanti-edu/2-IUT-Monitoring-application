package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.AuthenticationException;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.service.UserManager;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Écran de connexion. */
public class LoginView {

    private final StackPane root = new StackPane();

    public LoginView(UserManager users, Consumer<User> onSuccess) {
        Label title = new Label("NetMonitor");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Supervision réseau");
        subtitle.getStyleClass().add("muted");

        TextField login = new TextField();
        login.setPromptText("Identifiant");
        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Login"), login);
        form.addRow(1, new Label("Password"), password);

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);

        Button connect = new Button("Connexion");
        connect.setDefaultButton(true);
        connect.setMaxWidth(Double.MAX_VALUE);
        connect.setOnAction(e -> {
            try {
                onSuccess.accept(users.authenticate(login.getText(), password.getText()));
            } catch (AuthenticationException ex) {
                error.setText(ex.getMessage());
                password.clear();
                password.requestFocus();
            }
        });

        VBox box = new VBox(14, title, subtitle, form, connect, error);
        box.getStyleClass().add("login-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxSize(360, VBox.USE_PREF_SIZE);
        root.getChildren().add(box);
        root.getStyleClass().add("login-root");
        login.requestFocus();
    }

    public Parent getView() {
        return root;
    }
}
