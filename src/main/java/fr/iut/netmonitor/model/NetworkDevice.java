package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class NetworkDevice implements Serializable {

    private static final long serialVersionUID = 1L;
    /** Historique borné pour ne pas faire grossir indéfiniment le fichier de données. */
    private static final int MAX_HISTORY = 500;

    private int id;
    private String name;
    private String ipAddress;
    private DeviceType type;
    private String location;
    // Règle de gestion : un nouvel équipement est DOWN tant qu'aucun heartbeat n'a été reçu.
    private DeviceStatus status = DeviceStatus.DOWN;
    private LocalDateTime lastContact;

    private final List<Measurement> measurements = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    public NetworkDevice(String name, String ipAddress, DeviceType type) {
        this(name, ipAddress, type, "");
    }

    public NetworkDevice(String name, String ipAddress, DeviceType type, String location) {
        this.name = Objects.requireNonNull(name);
        this.ipAddress = Objects.requireNonNull(ipAddress);
        this.type = Objects.requireNonNull(type);
        this.location = location == null ? "" : location;
    }

    /** Change l'état et journalise l'événement si l'état a réellement changé. */
    public void updateStatus(DeviceStatus newStatus) {
        Objects.requireNonNull(newStatus);
        if (newStatus != status) {
            addEvent(new Event(this, "Changement d'état : " + status + " -> " + newStatus));
            status = newStatus;
        }
    }

    public void addMeasurement(Measurement measurement) {
        measurements.add(measurement);
        if (measurements.size() > MAX_HISTORY) {
            measurements.remove(0);
        }
        lastContact = measurement.getTimestamp();
    }

    public void addEvent(Event event) {
        events.add(event);
        if (events.size() > MAX_HISTORY) {
            events.remove(0);
        }
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
    }

    /** Dernière valeur reçue pour un indicateur, si elle existe. */
    public Optional<Double> getLatestValue(MeasurementType type) {
        for (int i = measurements.size() - 1; i >= 0; i--) {
            if (measurements.get(i).getType() == type) {
                return Optional.of(measurements.get(i).getValue());
            }
        }
        return Optional.empty();
    }

    /** Vrai si un incident non clôturé décrit déjà ce problème (évite les doublons). */
    public boolean hasOpenIncident(String description) {
        return incidents.stream().anyMatch(i -> i.isOpen() && i.getDescription().equals(description));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = Objects.requireNonNull(ipAddress);
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = Objects.requireNonNull(type);
    }

    public String getLocation() {
        // null possible pour les fichiers enregistrés avant l'ajout de ce champ
        return location == null ? "" : location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastContact() {
        return lastContact;
    }

    public void setLastContact(LocalDateTime lastContact) {
        this.lastContact = lastContact;
    }

    public List<Measurement> getMeasurements() {
        return Collections.unmodifiableList(measurements);
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    @Override
    public String toString() {
        return String.format("#%-3d %-12s %-14s %-16s %-7s", id, name, type.getLabel(), ipAddress, status);
    }
}
