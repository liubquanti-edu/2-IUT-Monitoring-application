package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Problème à traiter. Cycle de vie : OPEN -> IN_PROGRESS -> CLOSED (sans réouverture). */
public class Incident implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final NetworkDevice device;
    private final String description;
    private final Severity severity;
    private IncidentStatus status = IncidentStatus.OPEN;
    private final LocalDateTime createdAt;
    private LocalDateTime takenAt;
    private LocalDateTime closedAt;
    private String assignedTo;

    public Incident(NetworkDevice device, Severity severity, String description) {
        this.device = Objects.requireNonNull(device);
        this.severity = Objects.requireNonNull(severity);
        this.description = Objects.requireNonNull(description);
        this.createdAt = LocalDateTime.now();
    }

    public void takeCharge(String technician) {
        if (status != IncidentStatus.OPEN) {
            throw new IllegalStateException("Seul un incident OPEN peut être pris en charge.");
        }
        assignedTo = Objects.requireNonNull(technician);
        takenAt = LocalDateTime.now();
        status = IncidentStatus.IN_PROGRESS;
    }

    public void close() {
        if (status != IncidentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Seul un incident IN_PROGRESS peut être clôturé.");
        }
        closedAt = LocalDateTime.now();
        status = IncidentStatus.CLOSED;
    }

    public boolean isOpen() {
        return status != IncidentStatus.CLOSED;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public NetworkDevice getDevice() {
        return device;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    @Override
    public String toString() {
        return String.format("Incident #%d [%s] %s - %s (%s)", id, severity, device.getName(), description, status);
    }
}
