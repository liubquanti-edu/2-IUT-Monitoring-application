package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Instantané de toutes les données à conserver. Les incidents, mesures et événements sont
 * rattachés à leur équipement : ils sont donc enregistrés une seule fois, sans duplication
 * (la sérialisation Java conserve les références partagées).
 */
public class AppData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<NetworkDevice> devices;
    private final List<User> users;

    public AppData(List<NetworkDevice> devices, List<User> users) {
        this.devices = new ArrayList<>(devices);
        this.users = new ArrayList<>(users);
    }

    public static AppData empty() {
        return new AppData(List.of(), List.of());
    }

    public List<NetworkDevice> getDevices() {
        return devices;
    }

    public List<User> getUsers() {
        return users;
    }
}
