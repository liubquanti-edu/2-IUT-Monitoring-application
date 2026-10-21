package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Fenêtre principale : en-tête, menu de navigation et écran courant. */
public class MainView {

    private final BorderPane root = new BorderPane();
    private final ToggleGroup menu = new ToggleGroup();
    private final DashboardView dashboard;
    private final DevicesView devices;
    private final IncidentsView incidents;
    private final ToggleButton devicesButton;
    private RefreshableView current;

    public MainView(Session session, Runnable onLogout) {
        User user = session.user();
        dashboard = new DashboardView(session, this);
        devices = new DevicesView(session);
        incidents = new IncidentsView(session);

        Label title = new Label("NetMonitor");
        title.getStyleClass().add("app-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label who = new Label(user.getUsername() + " - " + user.getRole().getLabel());
        who.getStyleClass().add("user-label");
        HBox header = new HBox(title, spacer, who);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        HBox nav = new HBox();
        nav.getStyleClass().add("nav");
        nav.getChildren().add(menuButton("Dashboard", dashboard));
        devicesButton = menuButton("Équipements", devices);
        nav.getChildren().add(devicesButton);
        nav.getChildren().add(menuButton("Incidents", incidents));
        if (session.can(Permission.MANAGE_USERS)) {
            nav.getChildren().add(menuButton("Utilisateurs", new UsersView(session)));
        }
        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        Button password = new Button("Mot de passe");
        password.setOnAction(e -> PasswordDialog.changeOwnPassword(session));
        Button logout = new Button("Logout");
        logout.setOnAction(e -> onLogout.run());
        nav.getChildren().addAll(navSpacer, password, logout);

        // empêche de désélectionner l'écran courant en recliquant dessus
        menu.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                menu.selectToggle(old);
            }
        });

        root.setTop(new VBox(header, nav));
        ((ToggleButton) nav.getChildren().get(0)).fire();
    }

    private ToggleButton menuButton(String label, RefreshableView view) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(menu);
        button.setOnAction(e -> show(view));
        return button;
    }

    private void show(RefreshableView view) {
        current = view;
        view.refresh();
        root.setCenter(view.getView());
    }

    /** Ouvre la liste des équipements filtrée (recherche depuis le tableau de bord). */
    public void showDevices(String query) {
        devices.setQuery(query);
        menu.selectToggle(devicesButton);
        show(devices);
    }

    public void refresh() {
        if (current != null) {
            current.refresh();
        }
    }

    public Parent getView() {
        return root;
    }
}
