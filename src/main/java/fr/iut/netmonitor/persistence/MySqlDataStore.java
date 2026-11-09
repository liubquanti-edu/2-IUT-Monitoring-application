package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.DeviceStatus;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.Event;
import fr.iut.netmonitor.model.EventLevel;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.IncidentStatus;
import fr.iut.netmonitor.model.Measurement;
import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistance dans une base MySQL (JDBC). Les tables sont créées au premier chargement.
 * L'enregistrement remplace tout le contenu dans une transaction, comme le fichier sérialisé :
 * en cas d'erreur, la base reste dans son état précédent.
 *
 * <p>Si la base est vide au premier chargement, les données sont importées depuis {@code importFrom}
 * (l'ancien fichier {@code netmonitor.ser}) puis écrites dans MySQL au prochain enregistrement.
 */
public class MySqlDataStore implements DataStore {

    /** Variables d'environnement (ou propriétés système {@code netmonitor.db.*}) lues par {@link #fromEnvironment}. */
    public static final String ENV_URL = "NETMONITOR_DB_URL";
    public static final String ENV_USER = "NETMONITOR_DB_USER";
    public static final String ENV_PASSWORD = "NETMONITOR_DB_PASSWORD";

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id            INT          NOT NULL PRIMARY KEY,
                username      VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role          VARCHAR(20)  NOT NULL,
                active        BOOLEAN      NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS devices (
                id           INT           NOT NULL PRIMARY KEY,
                name         VARCHAR(100)  NOT NULL,
                ip_address   VARCHAR(15)   NOT NULL,
                device_type  VARCHAR(20)   NOT NULL,
                location     VARCHAR(1000) NOT NULL,
                status       VARCHAR(20)   NOT NULL,
                last_contact DATETIME(6)   NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS measurements (
                id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                device_id        INT         NOT NULL,
                measurement_type VARCHAR(20) NOT NULL,
                measured_value   DOUBLE      NOT NULL,
                measured_at      DATETIME(6) NOT NULL,
                FOREIGN KEY (device_id) REFERENCES devices (id) ON DELETE CASCADE
            )""",
            """
            CREATE TABLE IF NOT EXISTS events (
                id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
                device_id   INT           NOT NULL,
                event_level VARCHAR(20)   NOT NULL,
                description VARCHAR(1000) NOT NULL,
                occurred_at DATETIME(6)   NOT NULL,
                FOREIGN KEY (device_id) REFERENCES devices (id) ON DELETE CASCADE
            )""",
            """
            CREATE TABLE IF NOT EXISTS incidents (
                id          INT           NOT NULL PRIMARY KEY,
                device_id   INT           NOT NULL,
                severity    VARCHAR(20)   NOT NULL,
                description VARCHAR(1000) NOT NULL,
                status      VARCHAR(20)   NOT NULL,
                created_at  DATETIME(6)   NOT NULL,
                taken_at    DATETIME(6)   NULL,
                closed_at   DATETIME(6)   NULL,
                assigned_to VARCHAR(100)  NULL,
                FOREIGN KEY (device_id) REFERENCES devices (id) ON DELETE CASCADE
            )"""
    };

    private final String url;
    private final String user;
    private final String password;
    private final DataStore importFrom;
    /** Faux tant que la base n'a pas été lue : on refuse alors d'écraser son contenu. */
    private boolean loaded;

    public MySqlDataStore(String url, String user, String password) {
        this(url, user, password, null);
    }

    /** @param importFrom source importée si la base est vide (peut être null) */
    public MySqlDataStore(String url, String user, String password, DataStore importFrom) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.importFrom = importFrom;
    }

    /** Base configurée par variables d'environnement, ou vide si {@value #ENV_URL} n'est pas défini. */
    public static Optional<MySqlDataStore> fromEnvironment(DataStore importFrom) {
        String url = setting("netmonitor.db.url", ENV_URL);
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new MySqlDataStore(url, setting("netmonitor.db.user", ENV_USER),
                setting("netmonitor.db.password", ENV_PASSWORD), importFrom));
    }

    private static String setting(String property, String env) {
        String value = System.getProperty(property);
        return value != null ? value : System.getenv(env);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public AppData load() throws NetMonitorException {
        AppData data;
        try (Connection c = connect()) {
            createSchema(c);
            data = read(c);
        } catch (SQLException | IllegalArgumentException e) {
            throw new NetMonitorException("Impossible de lire la base de données " + url + " : " + e.getMessage(), e);
        }
        loaded = true;
        if (data.getDevices().isEmpty() && data.getUsers().isEmpty() && importFrom != null) {
            return importFrom.load();
        }
        return data;
    }

    @Override
    public void save(AppData data) throws NetMonitorException {
        if (!loaded) {
            throw new NetMonitorException("La base de données " + url + " n'a pas pu être lue au démarrage : "
                    + "enregistrement annulé pour ne pas écraser son contenu.");
        }
        try (Connection c = connect()) {
            createSchema(c);
            c.setAutoCommit(false);
            try {
                write(c, data);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new NetMonitorException("Impossible d'enregistrer les données dans " + url + " : "
                    + e.getMessage(), e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static void createSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (String sql : SCHEMA) {
                st.execute(sql);
            }
        }
    }

    // ---------------------------------------------------------------- lecture

    private static AppData read(Connection c) throws SQLException {
        List<User> users = new ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, username, password_hash, role, active FROM users ORDER BY id")) {
            while (rs.next()) {
                users.add(User.restore(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"),
                        UserRole.valueOf(rs.getString("role")), rs.getBoolean("active")));
            }
        }

        Map<Integer, NetworkDevice> devices = new LinkedHashMap<>();
        Map<Integer, DeviceStatus> statuses = new LinkedHashMap<>();
        Map<Integer, LocalDateTime> lastContacts = new LinkedHashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, ip_address, device_type, location, status, "
                     + "last_contact FROM devices ORDER BY id")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                NetworkDevice d = new NetworkDevice(rs.getString("name"), rs.getString("ip_address"),
                        DeviceType.valueOf(rs.getString("device_type")), rs.getString("location"));
                d.setId(id);
                devices.put(id, d);
                statuses.put(id, DeviceStatus.valueOf(rs.getString("status")));
                lastContacts.put(id, rs.getObject("last_contact", LocalDateTime.class));
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT device_id, measurement_type, measured_value, measured_at "
                     + "FROM measurements ORDER BY id")) {
            while (rs.next()) {
                NetworkDevice d = devices.get(rs.getInt("device_id"));
                if (d != null) {
                    d.addMeasurement(new Measurement(MeasurementType.valueOf(rs.getString("measurement_type")),
                            rs.getDouble("measured_value"), rs.getObject("measured_at", LocalDateTime.class)));
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT device_id, event_level, description, occurred_at "
                     + "FROM events ORDER BY id")) {
            while (rs.next()) {
                NetworkDevice d = devices.get(rs.getInt("device_id"));
                if (d != null) {
                    d.addEvent(new Event(d, EventLevel.valueOf(rs.getString("event_level")),
                            rs.getString("description"), rs.getObject("occurred_at", LocalDateTime.class)));
                }
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, device_id, severity, description, status, created_at, "
                     + "taken_at, closed_at, assigned_to FROM incidents ORDER BY id")) {
            while (rs.next()) {
                NetworkDevice d = devices.get(rs.getInt("device_id"));
                if (d != null) {
                    d.addIncident(Incident.restore(rs.getInt("id"), d, Severity.valueOf(rs.getString("severity")),
                            rs.getString("description"), IncidentStatus.valueOf(rs.getString("status")),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("taken_at", LocalDateTime.class),
                            rs.getObject("closed_at", LocalDateTime.class), rs.getString("assigned_to")));
                }
            }
        }

        // L'état est rétabli en dernier : l'ajout des mesures modifie la date du dernier contact.
        for (Map.Entry<Integer, NetworkDevice> e : devices.entrySet()) {
            e.getValue().restoreState(statuses.get(e.getKey()), lastContacts.get(e.getKey()));
        }
        return new AppData(new ArrayList<>(devices.values()), users);
    }

    // ---------------------------------------------------------------- écriture

    private static void write(Connection c, AppData data) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (String table : new String[] {"measurements", "events", "incidents", "devices", "users"}) {
                st.executeUpdate("DELETE FROM " + table);
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (id, username, password_hash, role, active) VALUES (?, ?, ?, ?, ?)")) {
            for (User u : data.getUsers()) {
                ps.setInt(1, u.getId());
                ps.setString(2, u.getUsername());
                ps.setString(3, u.getPasswordHash());
                ps.setString(4, u.getRole().name());
                ps.setBoolean(5, u.isActive());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = c.prepareStatement("INSERT INTO devices (id, name, ip_address, device_type, "
                + "location, status, last_contact) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (NetworkDevice d : data.getDevices()) {
                ps.setInt(1, d.getId());
                ps.setString(2, d.getName());
                ps.setString(3, d.getIpAddress());
                ps.setString(4, d.getType().name());
                ps.setString(5, d.getLocation());
                ps.setString(6, d.getStatus().name());
                setDateTime(ps, 7, d.getLastContact());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = c.prepareStatement("INSERT INTO measurements (device_id, measurement_type, "
                + "measured_value, measured_at) VALUES (?, ?, ?, ?)")) {
            for (NetworkDevice d : data.getDevices()) {
                for (Measurement m : d.getMeasurements()) {
                    ps.setInt(1, d.getId());
                    ps.setString(2, m.getType().name());
                    ps.setDouble(3, m.getValue());
                    setDateTime(ps, 4, m.getTimestamp());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = c.prepareStatement("INSERT INTO events (device_id, event_level, description, "
                + "occurred_at) VALUES (?, ?, ?, ?)")) {
            for (NetworkDevice d : data.getDevices()) {
                for (Event e : d.getEvents()) {
                    ps.setInt(1, d.getId());
                    ps.setString(2, e.getLevel().name());
                    ps.setString(3, e.getDescription());
                    setDateTime(ps, 4, e.getTimestamp());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = c.prepareStatement("INSERT INTO incidents (id, device_id, severity, "
                + "description, status, created_at, taken_at, closed_at, assigned_to) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (NetworkDevice d : data.getDevices()) {
                for (Incident i : d.getIncidents()) {
                    ps.setInt(1, i.getId());
                    ps.setInt(2, d.getId());
                    ps.setString(3, i.getSeverity().name());
                    ps.setString(4, i.getDescription());
                    ps.setString(5, i.getStatus().name());
                    setDateTime(ps, 6, i.getCreatedAt());
                    setDateTime(ps, 7, i.getTakenAt());
                    setDateTime(ps, 8, i.getClosedAt());
                    ps.setString(9, i.getAssignedTo());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private static void setDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setObject(index, value);
        }
    }
}
