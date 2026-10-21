package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;

/**
 * Persistance de l'ensemble des données de l'application. Implémentation actuelle :
 * sérialisation dans un fichier ; une implémentation JDBC pourra la remplacer.
 */
public interface DataStore {

    AppData load() throws NetMonitorException;

    void save(AppData data) throws NetMonitorException;
}
