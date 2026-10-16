package fr.iut.netmonitor.model;

public enum MeasurementType {
    CPU("%"),
    RAM("%"),
    LATENCY("ms"),
    AVAILABILITY("%");

    private final String unit;

    MeasurementType(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }
}
