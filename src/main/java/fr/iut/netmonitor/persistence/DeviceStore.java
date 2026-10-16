package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.NetworkDevice;

import java.util.List;

/**
 * Abstraction de la persistance des équipements. Implémentation actuelle : sérialisation
 * dans un fichier ; une implémentation JDBC pourra la remplacer sans toucher au reste.
 */
public interface DeviceStore {

    List<NetworkDevice> load() throws NetMonitorException;

    void save(List<NetworkDevice> devices) throws NetMonitorException;
}
