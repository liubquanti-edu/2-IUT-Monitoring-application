package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.UserRole;
import fr.iut.netmonitor.service.NetMonitorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SerializedDataStoreTest {

    @TempDir
    Path dir;

    @Test
    void roundTripKeepsDevicesUsersAndIncidents() throws Exception {
        DataStore store = new SerializedDataStore(dir.resolve("data.ser"));
        NetMonitorContext ctx = NetMonitorContext.open(store, false);
        NetworkDevice d = ctx.devices().addDevice("SRV", "10.0.0.1", DeviceType.SERVER, "Salle 1");
        ctx.incidents().openIncident(d, fr.iut.netmonitor.model.Severity.LOW, "test");
        ctx.users().createUser("carol", "carol", UserRole.VIEWER);
        ctx.save();

        NetMonitorContext reloaded = NetMonitorContext.open(store, false);
        assertEquals("Salle 1", reloaded.devices().findDevice(d.getId()).getLocation());
        assertEquals(1, reloaded.incidents().listIncidents().size());
        assertNotNull(reloaded.users().authenticate("carol", "carol"));
        // les identifiants continuent après les existants
        assertEquals(2, reloaded.devices().addDevice("SRV2", "10.0.0.2", DeviceType.SERVER).getId());
    }

    @Test
    void firstLaunchCreatesDefaultAccounts() throws Exception {
        NetMonitorContext ctx = NetMonitorContext.open(new SerializedDataStore(dir.resolve("none.ser")), true);
        assertTrue(ctx.users().authenticate("admin", "admin").isAdmin());
        assertFalse(ctx.devices().listDevices().isEmpty());
    }

    @Test
    void corruptedFileIsReportedWithoutCrash() throws Exception {
        Path file = dir.resolve("bad.ser");
        Files.writeString(file, "pas un fichier sérialisé");
        DataStore store = new SerializedDataStore(file);
        assertThrows(NetMonitorException.class, store::load);
        NetMonitorContext ctx = NetMonitorContext.open(store, false);
        assertNotNull(ctx.getLoadWarning());
    }

    @Test
    void importsLegacyDevicesFile() throws Exception {
        Path legacy = dir.resolve("devices.ser");
        new SerializedDeviceStore(legacy).save(List.of(new NetworkDevice("OLD", "10.0.0.9", DeviceType.ROUTER)));
        AppData data = new SerializedDataStore(dir.resolve("netmonitor.ser"), legacy).load();
        assertEquals("OLD", data.getDevices().get(0).getName());
        assertTrue(data.getUsers().isEmpty());
    }
}
