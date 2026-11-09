package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.IncidentStatus;
import fr.iut.netmonitor.model.Measurement;
import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;
import fr.iut.netmonitor.service.NetMonitorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** MySqlDataStore testé sur une base H2 en mémoire en mode de compatibilité MySQL. */
class MySqlDataStoreTest {

    @TempDir
    Path dir;

    private static String newDatabaseUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    }

    /** DATETIME(6) conserve les microsecondes (arrondies) : on compare à cette précision. */
    private static LocalDateTime micros(LocalDateTime t) {
        return t.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
    }

    @Test
    void roundTripKeepsAllData() throws Exception {
        DataStore store = new MySqlDataStore(newDatabaseUrl(), "sa", "");
        NetMonitorContext ctx = NetMonitorContext.open(store, false);
        User tech = ctx.users().createUser("bob", "bob1", UserRole.TECHNICIAN);
        NetworkDevice d = ctx.devices().addDevice("SRV", "10.0.0.1", DeviceType.SERVER, "Salle 1");
        d.addMeasurement(new Measurement(MeasurementType.CPU, 42.5));
        Incident incident = ctx.incidents().openIncident(d, Severity.HIGH, "CPU élevé").orElseThrow();
        ctx.incidents().takeCharge(tech, incident.getId());
        ctx.save();

        NetMonitorContext reloaded = NetMonitorContext.open(new MySqlDataStore(((MySqlDataStore) store).getUrl(),
                "sa", ""), false);
        NetworkDevice r = reloaded.devices().findDevice(d.getId());
        assertEquals("Salle 1", r.getLocation());
        assertEquals(d.getStatus(), r.getStatus());
        assertEquals(micros(d.getLastContact()), micros(r.getLastContact()));
        assertEquals(42.5, r.getLatestValue(MeasurementType.CPU).orElseThrow());
        assertEquals(d.getEvents().size(), r.getEvents().size());
        assertEquals(micros(d.getEvents().get(0).getTimestamp()), micros(r.getEvents().get(0).getTimestamp()));

        Incident ri = reloaded.incidents().listIncidents().get(0);
        assertEquals(IncidentStatus.IN_PROGRESS, ri.getStatus());
        assertEquals("bob", ri.getAssignedTo());
        assertEquals(micros(incident.getCreatedAt()), micros(ri.getCreatedAt()));
        assertSame(r, ri.getDevice());

        assertNotNull(reloaded.users().authenticate("bob", "bob1"));
        // les identifiants continuent après les existants
        assertEquals(2, reloaded.devices().addDevice("SRV2", "10.0.0.2", DeviceType.SERVER).getId());
    }

    @Test
    void savingTwiceReplacesContent() throws Exception {
        String url = newDatabaseUrl();
        NetMonitorContext ctx = NetMonitorContext.open(new MySqlDataStore(url, "sa", ""), false);
        NetworkDevice d = ctx.devices().addDevice("SRV", "10.0.0.1", DeviceType.SERVER);
        ctx.save();
        ctx.devices().removeDevice(d.getId());
        ctx.save();

        AppData data = new MySqlDataStore(url, "sa", "").load();
        assertTrue(data.getDevices().isEmpty());
    }

    @Test
    void emptyDatabaseImportsSerializedFile() throws Exception {
        DataStore file = new SerializedDataStore(dir.resolve("netmonitor.ser"));
        NetMonitorContext old = NetMonitorContext.open(file, false);
        old.devices().addDevice("OLD", "10.0.0.9", DeviceType.ROUTER);
        old.save();

        String url = newDatabaseUrl();
        NetMonitorContext ctx = NetMonitorContext.open(new MySqlDataStore(url, "sa", "", file), false);
        assertEquals("OLD", ctx.devices().listDevices().get(0).getName());
        ctx.save();

        // une fois copiées, les données viennent de la base, plus du fichier
        AppData data = new MySqlDataStore(url, "sa", "").load();
        assertEquals("OLD", data.getDevices().get(0).getName());
    }

    @Test
    void unreachableDatabaseIsNeverOverwritten() {
        DataStore store = new MySqlDataStore("jdbc:nothing:here", "x", "x");
        NetMonitorContext ctx = NetMonitorContext.open(store, true);
        assertNotNull(ctx.getLoadWarning());
        assertThrows(NetMonitorException.class, ctx::save);
    }
}
