package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.exception.NotFoundException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.Event;
import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.Severity;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.util.Validator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Création et suivi des incidents. Les incidents sont stockés dans leur équipement. */
public class IncidentManager {

    private final NetworkDeviceManager deviceManager;
    private int nextId;

    public IncidentManager(NetworkDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        this.nextId = deviceManager.listDevices().stream()
                .flatMap(d -> d.getIncidents().stream())
                .mapToInt(Incident::getId)
                .max().orElse(0) + 1;
    }

    /** Ouverture automatique (supervision) : aucun doublon si le même problème est déjà ouvert. */
    public synchronized Optional<Incident> openIncident(NetworkDevice device, Severity severity, String description) {
        if (device.hasOpenIncident(description)) {
            return Optional.empty();
        }
        Incident incident = new Incident(device, severity, description);
        incident.setId(nextId++);
        device.addIncident(incident);
        device.addEvent(new Event(device, "Incident #" + incident.getId() + " ouvert : " + description));
        return Optional.of(incident);
    }

    /** Ouverture manuelle par un technicien ou un administrateur. */
    public synchronized Incident createIncident(User actor, int deviceId, Severity severity, String description)
            throws NetMonitorException {
        AccessControl.require(actor, Permission.HANDLE_INCIDENTS);
        String clean = Validator.requireNotBlank(description, "problème");
        if (severity == null) {
            throw new ValidationException("La gravité est obligatoire.");
        }
        NetworkDevice device = deviceManager.findDevice(deviceId);
        return openIncident(device, severity, clean)
                .orElseThrow(() -> new ValidationException("Un incident identique est déjà ouvert pour "
                        + device.getName() + "."));
    }

    public synchronized void takeCharge(User actor, int incidentId) throws NetMonitorException {
        AccessControl.require(actor, Permission.HANDLE_INCIDENTS);
        Incident incident = findIncident(incidentId);
        try {
            incident.takeCharge(actor.getUsername());
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
        incident.getDevice().addEvent(new Event(incident.getDevice(),
                "Incident #" + incidentId + " pris en charge par " + actor.getUsername()));
    }

    public synchronized void close(User actor, int incidentId) throws NetMonitorException {
        AccessControl.require(actor, Permission.HANDLE_INCIDENTS);
        Incident incident = findIncident(incidentId);
        try {
            incident.close();
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
        incident.getDevice().addEvent(new Event(incident.getDevice(),
                "Incident #" + incidentId + " clôturé par " + actor.getUsername()));
    }

    public synchronized Incident findIncident(int id) throws NotFoundException {
        return listIncidents().stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Aucun incident avec l'id " + id + "."));
    }

    /** Tous les incidents, du plus récent au plus ancien. */
    public synchronized List<Incident> listIncidents() {
        return deviceManager.listDevices().stream()
                .flatMap(d -> d.getIncidents().stream())
                .sorted(Comparator.comparingInt(Incident::getId).reversed())
                .toList();
    }
}
