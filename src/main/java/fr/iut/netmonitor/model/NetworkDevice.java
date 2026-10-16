package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NetworkDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String ipAddress;
    private DeviceType type;
    // Règle de gestion : un nouvel équipement est DOWN tant qu'aucun heartbeat n'a été reçu.
    private DeviceStatus status = DeviceStatus.DOWN;
    private LocalDateTime lastContact;

    private final List<Measurement> measurements = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<Incident> incidents = new ArrayList<>();

    public NetworkDevice(String name, String ipAddress, DeviceType type) {
        this.name = Objects.requireNonNull(name);
        this.ipAddress = Objects.requireNonNull(ipAddress);
        this.type = Objects.requireNonNull(type);
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
        lastContact = measurement.getTimestamp();
    }

    public void addEvent(Event event) {
        events.add(event);
    }

    public void addIncident(Incident incident) {
        incidents.add(incident);
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
