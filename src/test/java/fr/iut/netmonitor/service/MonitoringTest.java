package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.AccessDeniedException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.DeviceStatus;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.IncidentStatus;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringTest {

    private NetworkDeviceManager devices;
    private IncidentManager incidents;
    private MonitoringService monitoring;
    private NetworkDevice srv;
    private final LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 14, 31, 4);

    @BeforeEach
    void setUp() throws Exception {
        devices = new NetworkDeviceManager();
        incidents = new IncidentManager(devices);
        monitoring = new MonitoringService(devices, incidents);
        srv = devices.addDevice("SRV-WEB", "192.168.1.30", DeviceType.SERVER, "Bâtiment A");
    }

    @Test
    void heartbeatBringsDeviceUp() {
        monitoring.receiveHeartbeat(srv, 34, 62, 12, t0);
        assertEquals(DeviceStatus.UP, srv.getStatus());
        assertEquals(t0, srv.getLastContact());
        assertEquals(34.0, srv.getLatestValue(fr.iut.netmonitor.model.MeasurementType.CPU).orElseThrow());
    }

    @Test
    void thresholdGivesWarningAndSingleIncident() {
        monitoring.receiveHeartbeat(srv, 95, 50, 10, t0);
        monitoring.receiveHeartbeat(srv, 97, 50, 10, t0.plusSeconds(2));
        assertEquals(DeviceStatus.WARNING, srv.getStatus());
        assertEquals(1, srv.getIncidents().size());
        assertTrue(srv.getEvents().stream().anyMatch(e -> e.isAlert()));
        monitoring.receiveHeartbeat(srv, 20, 50, 10, t0.plusSeconds(4));
        assertEquals(DeviceStatus.UP, srv.getStatus());
    }

    @Test
    void missingHeartbeatDeclaresDownWithCriticalIncident() {
        monitoring.receiveHeartbeat(srv, 30, 30, 10, t0);
        assertTrue(monitoring.checkHeartbeats(t0.plusSeconds(20)).isEmpty());
        assertEquals(1, monitoring.checkHeartbeats(t0.plusSeconds(36)).size());
        assertEquals(DeviceStatus.DOWN, srv.getStatus());
        Incident incident = srv.getIncidents().get(0);
        assertEquals(Severity.CRITICAL, incident.getSeverity());
        // pas de second incident tant que le premier est ouvert
        monitoring.receiveHeartbeat(srv, 30, 30, 10, t0.plusSeconds(40));
        monitoring.checkHeartbeats(t0.plusSeconds(80));
        assertEquals(1, srv.getIncidents().size());
    }

    @Test
    void incidentWorkflowRespectsRights() throws Exception {
        User tech = new User("tech", "tech", UserRole.TECHNICIAN);
        User viewer = new User("viewer", "viewer", UserRole.VIEWER);
        Incident i = incidents.createIncident(tech, srv.getId(), Severity.HIGH, "Ventilateur bruyant");
        assertThrows(ValidationException.class,
                () -> incidents.createIncident(tech, srv.getId(), Severity.HIGH, "Ventilateur bruyant"));
        assertThrows(AccessDeniedException.class, () -> incidents.takeCharge(viewer, i.getId()));
        assertThrows(ValidationException.class, () -> incidents.close(tech, i.getId()));
        incidents.takeCharge(tech, i.getId());
        assertEquals("tech", i.getAssignedTo());
        incidents.close(tech, i.getId());
        assertEquals(IncidentStatus.CLOSED, i.getStatus());
    }

    @Test
    void dashboardStatsAreComputedFromData() throws Exception {
        NetworkDevice ap = devices.addDevice("AP-01", "192.168.1.20", DeviceType.ACCESS_POINT);
        devices.addDevice("SW-01", "192.168.1.10", DeviceType.SWITCH);
        monitoring.receiveHeartbeat(srv, 20, 20, 10, t0);
        monitoring.receiveHeartbeat(ap, 99, 20, 10, t0);

        DashboardStats stats = DashboardStats.compute(devices.listDevices(), 10);
        assertEquals(3, stats.totalDevices());
        assertEquals(1, stats.up());
        assertEquals(1, stats.warning());
        assertEquals(1, stats.down());
        assertEquals(1, stats.openIncidents());
        assertEquals(1, stats.incidentsBySeverity().get(Severity.MEDIUM));
        assertEquals(0, stats.incidentsBySeverity().get(Severity.CRITICAL));
        assertEquals(1, stats.latestIncidents().size());
    }

    @Test
    void searchByLocation() throws Exception {
        devices.addDevice("AP-01", "192.168.1.20", DeviceType.ACCESS_POINT, "Bâtiment B");
        assertEquals(1, devices.search("bâtiment a").size());
        assertEquals(2, devices.search("bâtiment").size());
    }
}
