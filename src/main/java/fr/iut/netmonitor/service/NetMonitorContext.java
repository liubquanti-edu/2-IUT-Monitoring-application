package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.persistence.AppData;
import fr.iut.netmonitor.persistence.DataStore;
import fr.iut.netmonitor.persistence.SerializedDataStore;

import java.nio.file.Path;

/** Regroupe les services de l'application et gère le chargement / l'enregistrement des données. */
public class NetMonitorContext {

    public static final Path DATA_FILE = Path.of("data", "netmonitor.ser");
    /** Fichier de la première version (équipements seulement), importé s'il existe. */
    public static final Path LEGACY_DEVICES_FILE = Path.of("data", "devices.ser");

    private final DataStore store;
    private final NetworkDeviceManager devices;
    private final UserManager users;
    private final IncidentManager incidents;
    private final MonitoringService monitoring;
    private final String loadWarning;

    private NetMonitorContext(DataStore store, AppData data, String loadWarning) {
        this.store = store;
        this.devices = new NetworkDeviceManager(data.getDevices());
        this.users = new UserManager(data.getUsers());
        this.incidents = new IncidentManager(devices);
        this.monitoring = new MonitoringService(devices, incidents);
        this.loadWarning = loadWarning;
    }

    public static NetMonitorContext open() {
        return open(new SerializedDataStore(DATA_FILE, LEGACY_DEVICES_FILE), true);
    }

    /**
     * Charge les données. Si le fichier est illisible, l'application démarre quand même avec
     * des données vides et un avertissement (le fichier n'est écrasé qu'au prochain enregistrement).
     */
    public static NetMonitorContext open(DataStore store, boolean seedDemoDevices) {
        AppData data;
        String warning = null;
        try {
            data = store.load();
        } catch (NetMonitorException e) {
            warning = e.getMessage() + " Démarrage avec des données vides.";
            data = AppData.empty();
        }
        NetMonitorContext ctx = new NetMonitorContext(store, data, warning);
        try {
            DemoData.seedUsers(ctx.users);
            if (seedDemoDevices) {
                DemoData.seedDevices(ctx.devices);
            }
        } catch (ValidationException e) {
            throw new IllegalStateException("Données de démonstration invalides", e);
        }
        return ctx;
    }

    public void save() throws NetMonitorException {
        store.save(new AppData(devices.listDevices(), users.listUsers()));
    }

    public NetworkDeviceManager devices() {
        return devices;
    }

    public UserManager users() {
        return users;
    }

    public IncidentManager incidents() {
        return incidents;
    }

    public MonitoringService monitoring() {
        return monitoring;
    }

    /** Message à afficher si les données n'ont pas pu être relues, sinon null. */
    public String getLoadWarning() {
        return loadWarning;
    }
}
