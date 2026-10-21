package fr.iut.netmonitor.model;

import java.util.EnumSet;
import java.util.Set;

public enum UserRole {
    ADMIN("Administrateur", EnumSet.allOf(Permission.class)),
    TECHNICIAN("Technicien", EnumSet.of(Permission.VIEW_DEVICES, Permission.VIEW_INCIDENTS,
            Permission.HANDLE_INCIDENTS)),
    VIEWER("Observateur", EnumSet.of(Permission.VIEW_DEVICES, Permission.VIEW_INCIDENTS));

    private final String label;
    private final Set<Permission> permissions;

    UserRole(String label, Set<Permission> permissions) {
        this.label = label;
        this.permissions = permissions;
    }

    public String getLabel() {
        return label;
    }

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }
}
