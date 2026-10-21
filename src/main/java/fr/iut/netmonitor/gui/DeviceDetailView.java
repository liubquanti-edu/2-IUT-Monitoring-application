package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.Event;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.Measurement;
import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fiche d'un équipement : informations, historique des mesures, événements et incidents. */
public class DeviceDetailView {

    private static final int CHART_POINTS = 40;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final NetworkDevice device;
    private final Label status = new Label();
    private final Label lastContact = new Label();
    private final Label cpu = new Label();
    private final Label ram = new Label();
    private final Label latency = new Label();
    private final XYChart.Series<String, Number> cpuSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> ramSeries = new XYChart.Series<>();
    private final ListView<Event> events = new ListView<>();
    private final ListView<Incident> incidents = new ListView<>();

    public DeviceDetailView(NetworkDevice device) {
        this.device = device;
    }

    public void show(Window owner) {
        GridPane info = new GridPane();
        info.setHgap(16);
        info.setVgap(6);
        info.addRow(0, bold("Équipement"), new Label(device.getName()), bold("État"), status);
        info.addRow(1, bold("Type"), new Label(device.getType().getLabel()), bold("CPU"), cpu);
        info.addRow(2, bold("Adresse IP"), new Label(device.getIpAddress()), bold("RAM"), ram);
        info.addRow(3, bold("Localisation"), new Label(Tables.orDash(device.getLocation())), bold("Latence"), latency);
        info.addRow(4, bold("Dernier contact"), lastContact);

        NumberAxis y = new NumberAxis(0, 100, 10);
        y.setLabel("%");
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        cpuSeries.setName("CPU");
        ramSeries.setName("RAM");
        chart.getData().add(cpuSeries);
        chart.getData().add(ramSeries);

        events.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                getStyleClass().remove("alert-event");
                if (empty || e == null) {
                    setText(null);
                } else {
                    setText(Tables.dateTime(e.getTimestamp()) + "  " + (e.isAlert() ? "[ALERTE] " : "")
                            + e.getDescription());
                    if (e.isAlert()) {
                        getStyleClass().add("alert-event");
                    }
                }
            }
        });
        incidents.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Incident i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : String.format("#%d  %s  [%s]  %s  %s  %s", i.getId(),
                        Tables.dateTime(i.getCreatedAt()), i.getSeverity(), i.getDescription(), i.getStatus(),
                        i.getAssignedTo() == null ? "" : "(" + i.getAssignedTo() + ")"));
            }
        });
        events.setPlaceholder(new Label("Aucun événement."));
        incidents.setPlaceholder(new Label("Aucun incident."));

        TabPane tabs = new TabPane(
                new Tab("Mesures", chart),
                new Tab("Événements", events),
                new Tab("Incidents", incidents));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane(tabs);
        root.setTop(info);
        BorderPane.setMargin(info, new Insets(0, 0, 12, 0));
        root.setPadding(new Insets(16));

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Fiche équipement - " + device.getName());
        Scene scene = new Scene(root, 760, 560);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        stage.setScene(scene);

        refresh();
        // la fiche se met à jour tant qu'elle est ouverte
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        stage.setOnHidden(e -> timeline.stop());
        stage.show();
    }

    private void refresh() {
        status.setText(device.getStatus().name());
        status.getStyleClass().removeIf(s -> s.startsWith("status-"));
        status.getStyleClass().add("status-" + device.getStatus().name());
        lastContact.setText(Tables.dateTime(device.getLastContact()));
        cpu.setText(Tables.latest(device, MeasurementType.CPU));
        ram.setText(Tables.latest(device, MeasurementType.RAM));
        latency.setText(Tables.latest(device, MeasurementType.LATENCY));

        cpuSeries.getData().setAll(points(MeasurementType.CPU));
        ramSeries.getData().setAll(points(MeasurementType.RAM));

        List<Event> evts = new ArrayList<>(device.getEvents());
        Collections.reverse(evts);
        events.getItems().setAll(evts);
        List<Incident> incs = new ArrayList<>(device.getIncidents());
        Collections.reverse(incs);
        incidents.getItems().setAll(incs);
    }

    private List<XYChart.Data<String, Number>> points(MeasurementType type) {
        List<Measurement> values = device.getMeasurements().stream().filter(m -> m.getType() == type).toList();
        return values.subList(Math.max(0, values.size() - CHART_POINTS), values.size()).stream()
                .map(m -> new XYChart.Data<String, Number>(TIME.format(m.getTimestamp()), m.getValue()))
                .toList();
    }

    private static Label bold(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }
}
