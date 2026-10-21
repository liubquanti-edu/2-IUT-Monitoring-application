package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.AccessDeniedException;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;

/** Vérification des droits avant chaque action sensible. */
public final class AccessControl {

    private AccessControl() {
    }

    public static void require(User user, Permission permission) throws AccessDeniedException {
        if (user == null || !user.isActive() || !user.can(permission)) {
            throw new AccessDeniedException("Action non autorisée pour votre rôle.");
        }
    }
}
