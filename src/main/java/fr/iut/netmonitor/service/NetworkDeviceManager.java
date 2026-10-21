package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.NotFoundException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.util.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Gestion des équipements en mémoire. Les méthodes sont synchronisées afin de pouvoir
 * être appelées plus tard depuis plusieurs threads (clients, agents) sans corruption.
 */
public class NetworkDeviceManager {

    private final List<NetworkDevice> devices = new ArrayList<>();
    private int nextId = 1;

    public NetworkDeviceManager() {
    }

    /** Recharge une liste existante (ex. depuis un fichier). */
    public NetworkDeviceManager(List<NetworkDevice> initial) {
        devices.addAll(initial);
        nextId = initial.stream().mapToInt(NetworkDevice::getId).max().orElse(0) + 1;
    }

    public NetworkDevice addDevice(String name, String ipAddress, DeviceType type) throws ValidationException {
        return addDevice(name, ipAddress, type, "");
    }

    public synchronized NetworkDevice addDevice(String name, String ipAddress, DeviceType type, String location)
            throws ValidationException {
        String cleanName = Validator.requireNotBlank(name, "nom");
        String cleanIp = Validator.requireIpv4(ipAddress);
        if (type == null) {
            throw new ValidationException("Le type d'équipement est obligatoire.");
        }
        checkUnique(cleanName, cleanIp, -1);

        NetworkDevice device = new NetworkDevice(cleanName, cleanIp, type, clean(location));
        device.setId(nextId++);
        devices.add(device);
        return device;
    }

    public synchronized void removeDevice(int id) throws NotFoundException {
        devices.remove(findDevice(id));
    }

    public synchronized NetworkDevice updateDevice(int id, String name, String ipAddress, DeviceType type)
            throws NotFoundException, ValidationException {
        return updateDevice(id, name, ipAddress, type, findDevice(id).getLocation());
    }

    public synchronized NetworkDevice updateDevice(int id, String name, String ipAddress, DeviceType type,
                                                   String location)
            throws NotFoundException, ValidationException {
        NetworkDevice device = findDevice(id);
        String cleanName = Validator.requireNotBlank(name, "nom");
        String cleanIp = Validator.requireIpv4(ipAddress);
        if (type == null) {
            throw new ValidationException("Le type d'équipement est obligatoire.");
        }
        checkUnique(cleanName, cleanIp, id);

        device.setName(cleanName);
        device.setIpAddress(cleanIp);
        device.setType(type);
        device.setLocation(clean(location));
        return device;
    }

    public synchronized NetworkDevice findDevice(int id) throws NotFoundException {
        return devices.stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Aucun équipement avec l'id " + id + "."));
    }

    public synchronized List<NetworkDevice> listDevices() {
        List<NetworkDevice> copy = new ArrayList<>(devices);
        copy.sort(Comparator.comparingInt(NetworkDevice::getId));
        return copy;
    }

    /**
     * Recherche par nom, adresse IP, localisation, type ou état
     * (insensible à la casse, correspondance partielle sauf pour l'état).
     */
    public synchronized List<NetworkDevice> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return listDevices().stream()
                .filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(q)
                        || d.getIpAddress().contains(q)
                        || d.getLocation().toLowerCase(Locale.ROOT).contains(q)
                        || d.getType().name().toLowerCase(Locale.ROOT).contains(q)
                        || d.getType().getLabel().toLowerCase(Locale.ROOT).contains(q)
                        || d.getStatus().name().toLowerCase(Locale.ROOT).equals(q))
                .toList();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void checkUnique(String name, String ip, int ignoredId) throws ValidationException {
        for (NetworkDevice d : devices) {
            if (d.getId() == ignoredId) {
                continue;
            }
            if (d.getName().equalsIgnoreCase(name)) {
                throw new ValidationException("Un équipement nommé « " + name + " » existe déjà.");
            }
            if (d.getIpAddress().equals(ip)) {
                throw new ValidationException("L'adresse IP " + ip + " est déjà utilisée.");
            }
        }
    }
}
