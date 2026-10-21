package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.service.NetMonitorContext;

/**
 * Utilisateur connecté et accès aux services.
 *
 * @param onDataChanged appelé après chaque modification faite par l'utilisateur (enregistrement)
 */
public record Session(NetMonitorContext context, User user, Runnable onDataChanged) {

    public boolean can(Permission permission) {
        return user.can(permission);
    }

    public void dataChanged() {
        onDataChanged.run();
    }
}
