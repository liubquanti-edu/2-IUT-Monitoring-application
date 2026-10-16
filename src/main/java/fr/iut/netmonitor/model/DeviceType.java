package fr.iut.netmonitor.model;

public enum DeviceType {
    ROUTER("Routeur"),
    SWITCH("Switch"),
    ACCESS_POINT("Point d'accès"),
    SERVER("Serveur"),
    PRINTER("Imprimante"),
    OTHER("Autre");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
