package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Fait horodaté concernant un équipement (heartbeat, changement d'état…). */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final NetworkDevice device;
    private final LocalDateTime timestamp;
    private final String description;

    public Event(NetworkDevice device, String description) {
        this.device = Objects.requireNonNull(device);
        this.description = Objects.requireNonNull(description);
        this.timestamp = LocalDateTime.now();
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return timestamp + " - " + device.getName() + " : " + description;
    }
}
