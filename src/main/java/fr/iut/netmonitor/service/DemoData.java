package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.UserRole;

/** Données initiales créées au premier lancement (aucune donnée enregistrée). */
public final class DemoData {

    private DemoData() {
    }

    public static void seedUsers(UserManager users) throws ValidationException {
        if (users.isEmpty()) {
            users.createUser("admin", "admin", UserRole.ADMIN);
            users.createUser("tech", "tech", UserRole.TECHNICIAN);
            users.createUser("viewer", "viewer", UserRole.VIEWER);
        }
    }

    public static void seedDevices(NetworkDeviceManager devices) throws ValidationException {
        if (devices.listDevices().isEmpty()) {
            devices.addDevice("RTR-01", "192.168.1.1", DeviceType.ROUTER, "Bâtiment A - Salle serveurs");
            devices.addDevice("SW-01", "192.168.1.10", DeviceType.SWITCH, "Bâtiment A - Salle serveurs");
            devices.addDevice("SW-02", "192.168.1.11", DeviceType.SWITCH, "Bâtiment B - Local technique");
            devices.addDevice("AP-01", "192.168.1.20", DeviceType.ACCESS_POINT, "Bâtiment B - 1er étage");
            devices.addDevice("SRV-WEB", "192.168.1.30", DeviceType.SERVER, "Bâtiment A - Salle serveurs");
            devices.addDevice("SRV-FILES", "192.168.1.31", DeviceType.SERVER, "Bâtiment A - Salle serveurs");
            devices.addDevice("PRN-01", "192.168.1.50", DeviceType.PRINTER, "Bâtiment B - Accueil");
        }
    }
}
