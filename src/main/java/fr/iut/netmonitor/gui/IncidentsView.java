package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.IncidentStatus;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.service.IncidentManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Locale;

/** Liste des incidents avec filtres ; prise en charge et clôture pour techniciens et administrateurs. */
public class IncidentsView implements RefreshableView {

    private static final String ALL = "Tous";

    private final Session session;
    private final IncidentManager manager;
    private final BorderPane root = new BorderPane();
    private final TableView<Incident> table = new TableView<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> severityFilter = new ComboBox<>();
    private final TextField deviceFilter = new TextField();
    private Button take;
    private Button close;

    public IncidentsView(Session session) {
        this.session = session;
        this.manager = session.context().incidents();

        statusFilter.getItems().add(ALL);
        for (IncidentStatus s : IncidentStatus.values()) {
            statusFilter.getItems().add(s.name());
        }
        statusFilter.setValue(ALL);
        severityFilter.getItems().add(ALL);
        for (Severity s : Severity.values()) {
            severityFilter.getItems().add(s.name());
        }
        severityFilter.setValue(ALL);
        deviceFilter.setPromptText("Équipement");
        statusFilter.setOnAction(e -> refresh());
        severityFilter.setOnAction(e -> refresh());
        deviceFilter.textProperty().addListener((obs, old, now) -> refresh());

        HBox toolbar = new HBox(8, new Label("État"), statusFilter, new Label("Gravité"), severityFilter,
                new Label("Équipement"), deviceFilter);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        if (session.can(Permission.HANDLE_INCIDENTS)) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            take = new Button("Prendre en charge");
            take.setOnAction(e -> takeCharge());
            close = new Button("Clôturer");
            close.setOnAction(e -> closeIncident());
            Button create = new Button("Nouvel incident");
            create.setOnAction(e -> create());
            toolbar.getChildren().addAll(spacer, take, close, create);
            table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateButtons());
        }

        table.getColumns().add(Tables.column("ID", Incident::getId, 50));
        table.getColumns().add(Tables.column("Équipement", (Incident i) -> i.getDevice().getName(), 120));
        table.getColumns().add(Tables.enumColumn("Gravité", Incident::getSeverity, "severity", 90));
        table.getColumns().add(Tables.column("Problème", Incident::getDescription, 260));
        table.getColumns().add(Tables.enumColumn("État", Incident::getStatus, "incident", 100));
        table.getColumns().add(Tables.column("Créé le", (Incident i) -> Tables.dateTime(i.getCreatedAt()), 145));
        table.getColumns().add(Tables.column("Technicien", (Incident i) -> Tables.orDash(i.getAssignedTo()), 100));
        table.getColumns().add(Tables.column("Clôturé le", (Incident i) -> Tables.dateTime(i.getClosedAt()), 145));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aucun incident."));

        Label title = new Label("Incidents");
        title.getStyleClass().add("section-title");
        VBox top = new VBox(10, title, toolbar);
        top.setPadding(new Insets(0, 0, 10, 0));
        root.setTop(top);
        root.setCenter(table);
        root.setPadding(new Insets(20));
    }

    @Override
    public void refresh() {
        String status = statusFilter.getValue();
        String severity = severityFilter.getValue();
        String device = deviceFilter.getText().trim().toLowerCase(Locale.ROOT);
        List<Incident> list = manager.listIncidents().stream()
                .filter(i -> ALL.equals(status) || i.getStatus().name().equals(status))
                .filter(i -> ALL.equals(severity) || i.getSeverity().name().equals(severity))
                .filter(i -> i.getDevice().getName().toLowerCase(Locale.ROOT).contains(device))
                .toList();
        Tables.update(table, list, Incident::getId);
        updateButtons();
    }

    private void updateButtons() {
        if (take == null) {
            return;
        }
        Incident i = table.getSelectionModel().getSelectedItem();
        take.setDisable(i == null || i.getStatus() != IncidentStatus.OPEN);
        close.setDisable(i == null || i.getStatus() != IncidentStatus.IN_PROGRESS);
    }

    private void takeCharge() {
        Incident i = table.getSelectionModel().getSelectedItem();
        afterChange(Dialogs.attempt(() -> manager.takeCharge(session.user(), i.getId())));
    }

    private void closeIncident() {
        Incident i = table.getSelectionModel().getSelectedItem();
        if (Dialogs.confirm("Clôturer l'incident #" + i.getId() + " ? Il ne pourra pas être rouvert.")) {
            afterChange(Dialogs.attempt(() -> manager.close(session.user(), i.getId())));
        }
    }

    private void create() {
        ComboBox<NetworkDevice> device = new ComboBox<>();
        device.getItems().setAll(session.context().devices().listDevices());
        device.setConverter(new StringConverter<>() {
            @Override
            public String toString(NetworkDevice d) {
                return d == null ? "" : d.getName() + " (" + d.getIpAddress() + ")";
            }

            @Override
            public NetworkDevice fromString(String s) {
                return null;
            }
        });
        ComboBox<Severity> severity = new ComboBox<>();
        severity.getItems().setAll(Severity.values());
        severity.setValue(Severity.MEDIUM);
        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        description.setWrapText(true);
        Label error = new Label();
        error.getStyleClass().add("error-label");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Équipement"), device);
        form.addRow(1, new Label("Gravité"), severity);
        form.addRow(2, new Label("Problème"), description);
        form.add(error, 0, 3, 2, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvel incident");
        dialog.setHeaderText("Signaler un incident");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            if (device.getValue() == null) {
                error.setText("Choisissez un équipement.");
                e.consume();
                return;
            }
            try {
                manager.createIncident(session.user(), device.getValue().getId(), severity.getValue(),
                        description.getText());
            } catch (NetMonitorException ex) {
                error.setText(ex.getMessage());
                e.consume();
            }
        });
        afterChange(dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent());
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
