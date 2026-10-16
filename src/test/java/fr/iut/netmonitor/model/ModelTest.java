package fr.iut.netmonitor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void passwordIsHashedAndVerified() {
        User u = new User("alice", "secret", UserRole.TECHNICIAN);
        assertTrue(u.authenticate("secret"));
        assertFalse(u.authenticate("wrong"));
        u.setActive(false);
        assertFalse(u.authenticate("secret"));
    }

    @Test
    void incidentLifecycleCannotBeReopened() {
        NetworkDevice d = new NetworkDevice("SRV", "10.0.0.1", DeviceType.SERVER);
        Incident i = new Incident(d, Severity.CRITICAL, "serveur inaccessible");
        assertThrows(IllegalStateException.class, i::close);
        i.takeCharge("alice");
        assertEquals(IncidentStatus.IN_PROGRESS, i.getStatus());
        i.close();
        assertEquals(IncidentStatus.CLOSED, i.getStatus());
        assertThrows(IllegalStateException.class, () -> i.takeCharge("bob"));
    }

    @Test
    void statusChangeLogsEvent() {
        NetworkDevice d = new NetworkDevice("SRV", "10.0.0.1", DeviceType.SERVER);
        d.updateStatus(DeviceStatus.DOWN);
        assertTrue(d.getEvents().isEmpty());
        d.updateStatus(DeviceStatus.UP);
        assertEquals(1, d.getEvents().size());
    }

    @Test
    void measurementUpdatesLastContact() {
        NetworkDevice d = new NetworkDevice("SRV", "10.0.0.1", DeviceType.SERVER);
        Measurement m = new Measurement(MeasurementType.CPU, 42);
        d.addMeasurement(m);
        assertEquals(m.getTimestamp(), d.getLastContact());
    }
}
