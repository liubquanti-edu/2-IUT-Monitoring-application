package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;
import fr.iut.netmonitor.service.UserManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Gestion des comptes (administrateur uniquement). */
public class UsersView implements RefreshableView {

    private final Session session;
    private final UserManager manager;
    private final BorderPane root = new BorderPane();
    private final TableView<User> table = new TableView<>();

    public UsersView(Session session) {
        this.session = session;
        this.manager = session.context().users();

        Button add = new Button("Ajouter");
        add.setOnAction(e -> add());
        Button role = new Button("Changer le rôle");
        role.setOnAction(e -> changeRole());
        Button reset = new Button("Réinitialiser le mot de passe");
        reset.setOnAction(e -> resetPassword());
        Button toggle = new Button("Activer / désactiver");
        toggle.setOnAction(e -> toggleActive());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> delete());
        for (Button b : new Button[] {role, reset, toggle, delete}) {
            b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        }
        HBox toolbar = new HBox(8, add, role, reset, toggle, delete);

        table.getColumns().add(Tables.column("ID", User::getId, 50));
        table.getColumns().add(Tables.column("Identifiant", User::getUsername, 200));
        table.getColumns().add(Tables.column("Rôle", (User u) -> u.getRole().getLabel(), 160));
        table.getColumns().add(Tables.column("Compte", (User u) -> u.isActive() ? "Actif" : "Désactivé", 120));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Label title = new Label("Utilisateurs");
        title.getStyleClass().add("section-title");
        VBox top = new VBox(10, title, toolbar);
        top.setPadding(new Insets(0, 0, 10, 0));
        root.setTop(top);
        root.setCenter(table);
        root.setPadding(new Insets(20));
    }

    @Override
    public void refresh() {
        Tables.update(table, manager.listUsers(), User::getId);
    }

    private User selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void add() {
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<UserRole> role = new ComboBox<>();
        role.getItems().setAll(UserRole.values());
        role.setValue(UserRole.TECHNICIAN);
        Label error = new Label();
        error.getStyleClass().add("error-label");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Identifiant"), username);
        form.addRow(1, new Label("Mot de passe"), password);
        form.addRow(2, new Label("Rôle"), role);
        form.add(error, 0, 3, 2, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvel utilisateur");
        dialog.setHeaderText("Créer un compte");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                manager.addUser(session.user(), username.getText(), password.getText(), role.getValue());
            } catch (NetMonitorException ex) {
                error.setText(ex.getMessage());
                e.consume();
            }
        });
        afterChange(dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent());
    }

    private void changeRole() {
        User u = selected();
        ChoiceDialog<UserRole> dialog = new ChoiceDialog<>(u.getRole(), UserRole.values());
        dialog.setTitle("Changer le rôle");
        dialog.setHeaderText("Rôle de " + u.getUsername());
        dialog.showAndWait().ifPresent(r ->
                afterChange(Dialogs.attempt(() -> manager.changeRole(session.user(), u.getId(), r))));
    }

    private void resetPassword() {
        User u = selected();
        PasswordDialog.ask("Nouveau mot de passe pour " + u.getUsername()).ifPresent(p ->
                afterChange(Dialogs.attempt(() -> manager.resetPassword(session.user(), u.getId(), p))));
    }

    private void toggleActive() {
        User u = selected();
        afterChange(Dialogs.attempt(() -> manager.setActive(session.user(), u.getId(), !u.isActive())));
    }

    private void delete() {
        User u = selected();
        if (Dialogs.confirm("Supprimer le compte " + u.getUsername() + " ?")) {
            afterChange(Dialogs.attempt(() -> manager.removeUser(session.user(), u.getId())));
        }
    }

    private void afterChange(boolean changed) {
        if (changed) {
            session.dataChanged();
            refresh();
        }
    }

    @Override
    public Parent getView() {
        return root;
    }
}
