package fr.iut.netmonitor.service;

import fr.iut.netmonitor.model.DeviceStatus;
import fr.iut.netmonitor.model.Event;
import fr.iut.netmonitor.model.EventLevel;
import fr.iut.netmonitor.model.Measurement;
import fr.iut.netmonitor.model.MeasurementType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Severity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Règles de supervision : seuils dépassés (WARNING) et absence de heartbeat (DOWN).
 * Génère les événements, alertes et incidents correspondants.
 */
public class MonitoringService {

    public static final double CPU_THRESHOLD = 90;
    public static final double RAM_THRESHOLD = 90;
    public static final double LATENCY_THRESHOLD = 200;
    public static final Duration DEFAULT_HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);
    public static final String UNREACHABLE = "Équipement injoignable (aucun heartbeat)";

    private final NetworkDeviceManager deviceManager;
    private final IncidentManager incidentManager;
    private final Duration heartbeatTimeout;

    public MonitoringService(NetworkDeviceManager deviceManager, IncidentManager incidentManager) {
        this(deviceManager, incidentManager, DEFAULT_HEARTBEAT_TIMEOUT);
    }

    public MonitoringService(NetworkDeviceManager deviceManager, IncidentManager incidentManager,
                             Duration heartbeatTimeout) {
        this.deviceManager = deviceManager;
        this.incidentManager = incidentManager;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    /** Traite un heartbeat accompagné des mesures de l'équipement. */
    public synchronized void receiveHeartbeat(NetworkDevice device, double cpu, double ram, double latency,
                                              LocalDateTime when) {
        device.addMeasurement(new Measurement(MeasurementType.CPU, cpu, when));
        device.addMeasurement(new Measurement(MeasurementType.RAM, ram, when));
        device.addMeasurement(new Measurement(MeasurementType.LATENCY, latency, when));
        device.addEvent(new Event(device, device.getName() + " a envoyé son état."));

        List<String> problems = new ArrayList<>();
        if (cpu > CPU_THRESHOLD) {
            problems.add(String.format("CPU supérieur à %.0f %%", CPU_THRESHOLD));
        }
        if (ram > RAM_THRESHOLD) {
            problems.add(String.format("RAM supérieure à %.0f %%", RAM_THRESHOLD));
        }
        if (latency > LATENCY_THRESHOLD) {
            problems.add(String.format("Latence supérieure à %.0f ms", LATENCY_THRESHOLD));
        }
        for (String problem : problems) {
            if (!device.hasOpenIncident(problem)) {
                device.addEvent(new Event(device, EventLevel.ALERT, problem));
                incidentManager.openIncident(device, Severity.MEDIUM, problem);
            }
        }
        device.updateStatus(problems.isEmpty() ? DeviceStatus.UP : DeviceStatus.WARNING);
    }

    /** Déclare DOWN les équipements silencieux depuis plus longtemps que le délai autorisé. */
    public synchronized List<NetworkDevice> checkHeartbeats(LocalDateTime now) {
        List<NetworkDevice> lost = new ArrayList<>();
        for (NetworkDevice device : deviceManager.listDevices()) {
            LocalDateTime last = device.getLastContact();
            if (device.getStatus() == DeviceStatus.DOWN || last == null) {
                continue;
            }
            long silence = Duration.between(last, now).toSeconds();
            if (silence > heartbeatTimeout.toSeconds()) {
                device.addEvent(new Event(device, EventLevel.ALERT,
                        "Aucun heartbeat depuis " + silence + " secondes."));
                device.updateStatus(DeviceStatus.DOWN);
                incidentManager.openIncident(device, Severity.CRITICAL, UNREACHABLE);
                lost.add(device);
            }
        }
        return lost;
    }
}
