package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.DeviceStatus;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.service.DashboardStats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.Map;

/** Tableau de bord : indicateurs, graphiques, derniers incidents et recherche d'équipement. */
public class DashboardView implements RefreshableView {

    private static final int LATEST_INCIDENTS = 10;

    private final Session session;
    private final ScrollPane root;
    private final Label total = new Label();
    private final Label up = new Label();
    private final Label warning = new Label();
    private final Label down = new Label();
    private final Label openIncidents = new Label();
    private final Map<DeviceStatus, PieChart.Data> statusSlices = new EnumMap<>(DeviceStatus.class);
    private final Map<Severity, XYChart.Data<String, Number>> severityBars = new EnumMap<>(Severity.class);
    private final TableView<Incident> latest = new TableView<>();

    public DashboardView(Session session, MainView main) {
        this.session = session;

        HBox tiles = new HBox(16,
                tile(total, "Équipements", "tile-total"),
                tile(up, "En ligne (UP)", "tile-up"),
                tile(warning, "Alertes (WARNING)", "tile-warning"),
                tile(down, "Hors ligne (DOWN)", "tile-down"),
                tile(openIncidents, "Incidents ouverts", "tile-incidents"));

        TextField search = new TextField();
        search.setPromptText("Rechercher un équipement par nom, adresse IP ou localisation");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button go = new Button("Rechercher");
        go.setOnAction(e -> main.showDevices(search.getText()));
        search.setOnAction(e -> main.showDevices(search.getText()));
        HBox searchBar = new HBox(8, search, go);

        HBox charts = new HBox(16, statusChart(), severityChart());
        charts.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));

        latest.getColumns().add(Tables.column("ID", Incident::getId, 50));
        latest.getColumns().add(Tables.column("Équipement", (Incident i) -> i.getDevice().getName(), 130));
        latest.getColumns().add(Tables.enumColumn("Gravité", Incident::getSeverity, "severity", 100));
        latest.getColumns().add(Tables.column("Problème", Incident::getDescription, 300));
        latest.getColumns().add(Tables.enumColumn("État", Incident::getStatus, "incident", 110));
        latest.getColumns().add(Tables.column("Technicien", (Incident i) -> Tables.orDash(i.getAssignedTo()), 110));
        latest.getColumns().add(Tables.column("Créé le", (Incident i) -> Tables.dateTime(i.getCreatedAt()), 150));
        latest.setPlaceholder(new Label("Aucun incident."));
        latest.setPrefHeight(300);
        latest.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Label latestTitle = new Label("Derniers incidents");
        latestTitle.getStyleClass().add("section-title");

        VBox content = new VBox(18, tiles, searchBar, charts, latestTitle, latest);
        content.setPadding(new Insets(20));
        root = new ScrollPane(content);
        root.setFitToWidth(true);
    }

    private static VBox tile(Label value, String caption, String styleClass) {
        value.getStyleClass().add("tile-value");
        Label text = new Label(caption);
        text.getStyleClass().add("tile-caption");
        VBox box = new VBox(4, value, text);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().addAll("tile", styleClass);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private PieChart statusChart() {
        PieChart chart = new PieChart();
        chart.setTitle("État des équipements");
        chart.setAnimated(false);
        chart.setLabelsVisible(false);
        chart.setPrefHeight(280);
        // l'ordre fixe les couleurs (default-color0 = UP, 1 = WARNING, 2 = DOWN, voir la feuille CSS)
        for (DeviceStatus s : DeviceStatus.values()) {
            PieChart.Data slice = new PieChart.Data(s.name(), 0);
            statusSlices.put(s, slice);
            chart.getData().add(slice);
        }
        return chart;
    }

    private BarChart<String, Number> severityChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        // un nombre d'incidents est entier : pas de graduations décimales
        y.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                return n.doubleValue() == Math.rint(n.doubleValue()) ? String.valueOf(n.intValue()) : "";
            }

            @Override
            public Number fromString(String s) {
                return Integer.valueOf(s);
            }
        });
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setTitle("Incidents par gravité");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(280);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Severity s : Severity.values()) {
            XYChart.Data<String, Number> bar = new XYChart.Data<>(s.name(), 0);
            // la barre (Node) n'est créée qu'à l'ajout de la série au graphique ;
            // un style en ligne est prioritaire sur les couleurs par défaut du thème
            bar.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) {
                    node.setStyle("-fx-bar-fill: " + barColor(s) + ";");
                }
            });
            severityBars.put(s, bar);
            series.getData().add(bar);
        }
        chart.getData().add(series);
        return chart;
    }

    private static String barColor(Severity s) {
        return switch (s) {
            case LOW -> "#78909c";
            case MEDIUM -> "#f9a825";
            case HIGH -> "#ef6c00";
            case CRITICAL -> "#c62828";
        };
    }

    @Override
    public void refresh() {
        DashboardStats stats = DashboardStats.compute(session.context().devices().listDevices(), LATEST_INCIDENTS);
        total.setText(String.valueOf(stats.totalDevices()));
        up.setText(String.valueOf(stats.up()));
        warning.setText(String.valueOf(stats.warning()));
        down.setText(String.valueOf(stats.down()));
        openIncidents.setText(String.valueOf(stats.openIncidents()));

        statusSlices.get(DeviceStatus.UP).setPieValue(stats.up());
        statusSlices.get(DeviceStatus.WARNING).setPieValue(stats.warning());
        statusSlices.get(DeviceStatus.DOWN).setPieValue(stats.down());
        statusSlices.forEach((s, slice) -> slice.setName(s.name() + " (" + (int) slice.getPieValue() + ")"));

        stats.incidentsBySeverity().forEach((s, n) -> severityBars.get(s).setYValue(n));

        Tables.update(latest, stats.latestIncidents(), Incident::getId);
    }

    @Override
    public Parent getView() {
        return root;
    }
}
