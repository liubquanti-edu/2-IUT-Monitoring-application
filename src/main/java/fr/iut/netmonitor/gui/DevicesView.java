package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.service.AccessControl;
import fr.iut.netmonitor.service.NetworkDeviceManager;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Liste des équipements avec recherche ; ajout, modification et suppression pour l'administrateur. */
public class DevicesView implements RefreshableView {

    private final Session session;
    private final NetworkDeviceManager manager;
    private final BorderPane root = new BorderPane();
    private final TextField search = new TextField();
    private final TableView<NetworkDevice> table = new TableView<>();

    public DevicesView(Session session) {
        this.session = session;
        this.manager = session.context().devices();

        search.setPromptText("Rechercher par nom, adresse IP, localisation, type ou état");
        search.textProperty().addListener((obs, old, now) -> refresh());
        HBox.setHgrow(search, Priority.ALWAYS);

        Button details = new Button("Fiche équipement");
        details.setOnAction(e -> openDetails());
        details.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        HBox toolbar = new HBox(8, search, details);

        if (session.can(Permission.EDIT_DEVICES)) {
            Button add = new Button("Ajouter");
            add.setOnAction(e -> add());
            Button edit = new Button("Modifier");
            edit.setOnAction(e -> edit());
            edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            Button delete = new Button("Supprimer");
            delete.getStyleClass().add("danger");
            delete.setOnAction(e -> delete());
            delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            toolbar.getChildren().addAll(add, edit, delete);
        }

        table.getColumns().add(Tables.column("ID", NetworkDevice::getId, 50));
        table.getColumns().add(Tables.column("Nom", NetworkDevice::getName, 120));
        table.getColumns().add(Tables.column("Type", (NetworkDevice d) -> d.getType().getLabel(), 110));
        table.getColumns().add(Tables.column("Adresse IP", NetworkDevice::getIpAddress, 120));
        table.getColumns().add(Tables.column("Localisation", (NetworkDevice d) -> Tables.orDash(d.getLocation()), 200));
        table.getColumns().add(Tables.enumColumn("État", NetworkDevice::getStatus, "status", 90));
        table.getColumns().add(Tables.column("CPU", (NetworkDevice d) -> Tables.latest(d, MeasurementType.CPU), 70));
        table.getColumns().add(Tables.column("RAM", (NetworkDevice d) -> Tables.latest(d, MeasurementType.RAM), 70));
        table.getColumns().add(Tables.column("Latence", (NetworkDevice d) -> Tables.latest(d, MeasurementType.LATENCY), 80));
        table.getColumns().add(Tables.column("Dernier contact", (NetworkDevice d) -> Tables.dateTime(d.getLastContact()), 150));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aucun équipement trouvé."));
        table.setRowFactory(t -> {
            TableRow<NetworkDevice> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetails();
                }
            });
            return row;
        });

        Label title = new Label("Équipements");
        title.getStyleClass().add("section-title");
        VBox top = new VBox(10, title, toolbar);
        top.setPadding(new Insets(0, 0, 10, 0));
        root.setTop(top);
        root.setCenter(table);
        root.setPadding(new Insets(20));
    }

    public void setQuery(String query) {
        search.setText(query == null ? "" : query);
    }

    @Override
    public void refresh() {
        Tables.update(table, manager.search(search.getText()), NetworkDevice::getId);
    }

    private void openDetails() {
        NetworkDevice selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            new DeviceDetailView(selected).show(root.getScene().getWindow());
        }
    }

    private void add() {
        boolean saved = DeviceDialog.show("Ajouter un équipement", null, (name, ip, type, location) -> {
            AccessControl.require(session.user(), Permission.EDIT_DEVICES);
            manager.addDevice(name, ip, type, location);
        });
        afterChange(saved);
    }

    private void edit() {
        NetworkDevice d = table.getSelectionModel().getSelectedItem();
        boolean saved = DeviceDialog.show("Modifier " + d.getName(), d, (name, ip, type, location) -> {
            AccessControl.require(session.user(), Permission.EDIT_DEVICES);
            manager.updateDevice(d.getId(), name, ip, type, location);
        });
        afterChange(saved);
    }

    private void delete() {
        NetworkDevice d = table.getSelectionModel().getSelectedItem();
        if (!Dialogs.confirm("Supprimer l'équipement " + d.getName()
                + " ainsi que son historique (mesures, événements, incidents) ?")) {
            return;
        }
        afterChange(Dialogs.attempt(() -> {
            AccessControl.require(session.user(), Permission.EDIT_DEVICES);
            manager.removeDevice(d.getId());
        }));
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
