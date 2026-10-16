package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.NotFoundException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.DeviceStatus;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.NetworkDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDeviceManagerTest {

    private NetworkDeviceManager manager;

    @BeforeEach
    void setUp() {
        manager = new NetworkDeviceManager();
    }

    @Test
    void newDeviceIsDownWithGeneratedId() throws Exception {
        NetworkDevice d = manager.addDevice("RTR-01", "192.168.1.1", DeviceType.ROUTER);
        assertEquals(1, d.getId());
        assertEquals(DeviceStatus.DOWN, d.getStatus());
        assertNull(d.getLastContact());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> manager.addDevice(" ", "10.0.0.1", DeviceType.SERVER));
        assertThrows(ValidationException.class, () -> manager.addDevice("SRV", "300.0.0.1", DeviceType.SERVER));
        assertThrows(ValidationException.class, () -> manager.addDevice("SRV", "10.0.0", DeviceType.SERVER));
        assertThrows(ValidationException.class, () -> manager.addDevice("SRV", "10.0.0.1", null));
    }

    @Test
    void rejectsDuplicateNameOrIp() throws Exception {
        manager.addDevice("SW-01", "192.168.1.10", DeviceType.SWITCH);
        assertThrows(ValidationException.class, () -> manager.addDevice("sw-01", "192.168.1.11", DeviceType.SWITCH));
        assertThrows(ValidationException.class, () -> manager.addDevice("SW-02", "192.168.1.10", DeviceType.SWITCH));
    }

    @Test
    void updateKeepsOwnNameAndIpAllowed() throws Exception {
        NetworkDevice d = manager.addDevice("AP-01", "192.168.1.20", DeviceType.ACCESS_POINT);
        manager.updateDevice(d.getId(), "AP-01", "192.168.1.21", DeviceType.ACCESS_POINT);
        assertEquals("192.168.1.21", d.getIpAddress());
    }

    @Test
    void removeAndFind() throws Exception {
        NetworkDevice d = manager.addDevice("SRV-WEB", "192.168.1.30", DeviceType.SERVER);
        manager.removeDevice(d.getId());
        assertThrows(NotFoundException.class, () -> manager.findDevice(d.getId()));
        assertTrue(manager.listDevices().isEmpty());
    }

    @Test
    void searchByNameIpTypeOrStatus() throws Exception {
        manager.addDevice("SRV-WEB", "192.168.1.30", DeviceType.SERVER);
        manager.addDevice("RTR-01", "192.168.1.1", DeviceType.ROUTER);
        assertEquals(1, manager.search("web").size());
        assertEquals(1, manager.search("192.168.1.1").size());
        assertEquals(1, manager.search("router").size());
        assertEquals(2, manager.search("down").size());
    }
}
