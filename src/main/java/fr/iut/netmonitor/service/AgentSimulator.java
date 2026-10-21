package fr.iut.netmonitor.service;

import fr.iut.netmonitor.model.NetworkDevice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simule localement les agents des équipements (en attendant les vrais agents réseau) :
 * mesures aléatoires, pics de charge occasionnels et pannes temporaires.
 */
public class AgentSimulator {

    private static final double OUTAGE_PROBABILITY = 0.004;
    private static final double SPIKE_PROBABILITY = 0.005;

    private final NetworkDeviceManager deviceManager;
    private final MonitoringService monitoring;
    private final Random random;
    private final Map<Integer, LocalDateTime> offlineUntil = new HashMap<>();

    public AgentSimulator(NetworkDeviceManager deviceManager, MonitoringService monitoring) {
        this(deviceManager, monitoring, new Random());
    }

    public AgentSimulator(NetworkDeviceManager deviceManager, MonitoringService monitoring, Random random) {
        this.deviceManager = deviceManager;
        this.monitoring = monitoring;
        this.random = random;
    }

    /** Un cycle : chaque agent actif envoie un heartbeat, puis contrôle des délais. */
    public void tick(LocalDateTime now) {
        for (NetworkDevice device : deviceManager.listDevices()) {
            LocalDateTime until = offlineUntil.get(device.getId());
            if (until != null && now.isBefore(until)) {
                continue;
            }
            offlineUntil.remove(device.getId());
            if (random.nextDouble() < OUTAGE_PROBABILITY) {
                // panne simulée de 40 à 90 s : dépasse le délai de heartbeat
                offlineUntil.put(device.getId(), now.plusSeconds(40 + random.nextInt(51)));
                continue;
            }
            double base = 15 + (device.getId() * 7) % 40;
            double cpu = spike() ? 91 + random.nextDouble() * 9 : clamp(base + random.nextGaussian() * 8);
            double ram = spike() ? 91 + random.nextDouble() * 9 : clamp(base + 20 + random.nextGaussian() * 6);
            double latency = spike() ? 210 + random.nextDouble() * 150 : Math.max(1, 10 + random.nextGaussian() * 5);
            monitoring.receiveHeartbeat(device, round(cpu), round(ram), round(latency), now);
        }
        monitoring.checkHeartbeats(now);
    }

    private boolean spike() {
        return random.nextDouble() < SPIKE_PROBABILITY;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(89, v));
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
