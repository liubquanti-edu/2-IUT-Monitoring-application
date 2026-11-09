package fr.iut.netmonitor.model;

import fr.iut.netmonitor.util.PasswordHasher;

import java.io.Serializable;
import java.util.Objects;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private final String username;
    private String passwordHash;
    private UserRole role;
    private boolean active = true;

    public User(String username, String password, UserRole role) {
        this.username = Objects.requireNonNull(username);
        this.passwordHash = PasswordHasher.hash(Objects.requireNonNull(password));
        this.role = Objects.requireNonNull(role);
    }

    private User(int id, String username, String passwordHash, UserRole role, boolean active) {
        this.id = id;
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.active = active;
    }

    /** Recrée un compte lu depuis la base de données (le mot de passe est déjà haché). */
    public static User restore(int id, String username, String passwordHash, UserRole role, boolean active) {
        return new User(id, username, passwordHash, role, active);
    }

    public boolean authenticate(String password) {
        return active && PasswordHasher.verify(password, passwordHash);
    }

    public void changePassword(String newPassword) {
        passwordHash = PasswordHasher.hash(Objects.requireNonNull(newPassword));
    }

    public boolean can(Permission permission) {
        return role.can(permission);
    }

    public boolean canHandleIncidents() {
        return can(Permission.HANDLE_INCIDENTS);
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    /** Hachage salé du mot de passe, nécessaire pour l'enregistrer dans la base. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = Objects.requireNonNull(role);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return username + " (" + role + (active ? "" : ", désactivé") + ")";
    }
}
