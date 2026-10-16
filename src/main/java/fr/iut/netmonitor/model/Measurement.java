package fr.iut.netmonitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class Measurement implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final MeasurementType type;
    private final double value;
    private final LocalDateTime timestamp;

    public Measurement(MeasurementType type, double value) {
        this(type, value, LocalDateTime.now());
    }

    public Measurement(MeasurementType type, double value, LocalDateTime timestamp) {
        this.type = Objects.requireNonNull(type);
        this.value = value;
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MeasurementType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return type + " = " + value + " " + type.getUnit() + " (" + timestamp + ")";
    }
}
