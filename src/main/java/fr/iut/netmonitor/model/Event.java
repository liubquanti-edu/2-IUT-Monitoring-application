package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Fait horodaté concernant un équipement (heartbeat, changement d'état…) ou alerte (seuil dépassé). */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final NetworkDevice device;
    private final LocalDateTime timestamp;
    private final String description;
    private final EventLevel level;

    public Event(NetworkDevice device, String description) {
        this(device, EventLevel.INFO, description);
    }

    public Event(NetworkDevice device, EventLevel level, String description) {
        this.device = Objects.requireNonNull(device);
        this.level = Objects.requireNonNull(level);
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

    public EventLevel getLevel() {
        // null possible pour les fichiers enregistrés avant l'ajout de ce champ
        return level == null ? EventLevel.INFO : level;
    }

    public boolean isAlert() {
        return getLevel() == EventLevel.ALERT;
    }

    @Override
    public String toString() {
        return timestamp + " - " + (isAlert() ? "[ALERTE] " : "") + device.getName() + " : " + description;
    }
}
