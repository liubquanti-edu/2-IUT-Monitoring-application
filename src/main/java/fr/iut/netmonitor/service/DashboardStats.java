package fr.iut.netmonitor.service;

import fr.iut.netmonitor.model.Incident;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Severity;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Indicateurs du tableau de bord, calculés à partir des données (jamais codés en dur). */
public record DashboardStats(int totalDevices, int up, int warning, int down, int openIncidents,
                             Map<Severity, Integer> incidentsBySeverity, List<Incident> latestIncidents) {

    public static DashboardStats compute(List<NetworkDevice> devices, int latestCount) {
        int up = 0;
        int warning = 0;
        int down = 0;
        for (NetworkDevice d : devices) {
            switch (d.getStatus()) {
                case UP -> up++;
                case WARNING -> warning++;
                case DOWN -> down++;
            }
        }
        List<Incident> incidents = devices.stream().flatMap(d -> d.getIncidents().stream()).toList();
        Map<Severity, Integer> bySeverity = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) {
            bySeverity.put(s, 0);
        }
        incidents.forEach(i -> bySeverity.merge(i.getSeverity(), 1, Integer::sum));
        int open = (int) incidents.stream().filter(Incident::isOpen).count();
        List<Incident> latest = incidents.stream()
                .sorted(Comparator.comparingInt(Incident::getId).reversed())
                .limit(latestCount)
                .toList();
        return new DashboardStats(devices.size(), up, warning, down, open,
                Collections.unmodifiableMap(bySeverity), latest);
    }
}
