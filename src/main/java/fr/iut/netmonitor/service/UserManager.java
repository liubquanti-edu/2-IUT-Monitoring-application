package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.AccessDeniedException;
import fr.iut.netmonitor.exception.AuthenticationException;
import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.exception.NotFoundException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;
import fr.iut.netmonitor.util.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Comptes utilisateurs : authentification et administration (réservée aux administrateurs). */
public class UserManager {

    public static final int MIN_PASSWORD_LENGTH = 4;

    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    public UserManager() {
    }

    public UserManager(List<User> initial) {
        users.addAll(initial);
        nextId = initial.stream().mapToInt(User::getId).max().orElse(0) + 1;
    }

    /** Message volontairement identique que l'identifiant ou le mot de passe soit faux. */
    public synchronized User authenticate(String username, String password) throws AuthenticationException {
        String login = username == null ? "" : username.trim();
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(login))
                .filter(u -> u.authenticate(password))
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("Identifiant ou mot de passe incorrect."));
    }

    /** Création sans contrôle de droits : réservée à l'initialisation (premiers comptes). */
    public synchronized User createUser(String username, String password, UserRole role) throws ValidationException {
        String cleanName = Validator.requireNotBlank(username, "identifiant");
        checkPassword(password);
        if (role == null) {
            throw new ValidationException("Le rôle est obligatoire.");
        }
        if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(cleanName))) {
            throw new ValidationException("L'identifiant « " + cleanName + " » est déjà utilisé.");
        }
        User user = new User(cleanName, password, role);
        user.setId(nextId++);
        users.add(user);
        return user;
    }

    public synchronized User addUser(User actor, String username, String password, UserRole role)
            throws NetMonitorException {
        AccessControl.require(actor, Permission.MANAGE_USERS);
        return createUser(username, password, role);
    }

    public synchronized void changeRole(User actor, int id, UserRole role) throws NetMonitorException {
        AccessControl.require(actor, Permission.MANAGE_USERS);
        if (role == null) {
            throw new ValidationException("Le rôle est obligatoire.");
        }
        User user = findUser(id);
        if (user.isAdmin() && role != UserRole.ADMIN) {
            checkNotLastAdmin(user);
        }
        user.setRole(role);
    }

    public synchronized void resetPassword(User actor, int id, String password) throws NetMonitorException {
        AccessControl.require(actor, Permission.MANAGE_USERS);
        checkPassword(password);
        findUser(id).changePassword(password);
    }

    public synchronized void setActive(User actor, int id, boolean active) throws NetMonitorException {
        AccessControl.require(actor, Permission.MANAGE_USERS);
        User user = findUser(id);
        if (!active) {
            checkNotSelf(actor, user);
            if (user.isAdmin()) {
                checkNotLastAdmin(user);
            }
        }
        user.setActive(active);
    }

    public synchronized void removeUser(User actor, int id) throws NetMonitorException {
        AccessControl.require(actor, Permission.MANAGE_USERS);
        User user = findUser(id);
        checkNotSelf(actor, user);
        if (user.isAdmin()) {
            checkNotLastAdmin(user);
        }
        users.remove(user);
    }

    public synchronized void changeOwnPassword(User user, String oldPassword, String newPassword)
            throws NetMonitorException {
        if (!user.authenticate(oldPassword)) {
            throw new AuthenticationException("Mot de passe actuel incorrect.");
        }
        checkPassword(newPassword);
        user.changePassword(newPassword);
    }

    public synchronized User findUser(int id) throws NotFoundException {
        return users.stream()
                .filter(u -> u.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Aucun utilisateur avec l'id " + id + "."));
    }

    public synchronized List<User> listUsers() {
        List<User> copy = new ArrayList<>(users);
        copy.sort(Comparator.comparingInt(User::getId));
        return copy;
    }

    public synchronized boolean isEmpty() {
        return users.isEmpty();
    }

    private static void checkPassword(String password) throws ValidationException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH
                    + " caractères.");
        }
    }

    private static void checkNotSelf(User actor, User target) throws ValidationException {
        if (actor.getId() == target.getId()) {
            throw new ValidationException("Vous ne pouvez pas désactiver ou supprimer votre propre compte.");
        }
    }

    private void checkNotLastAdmin(User target) throws AccessDeniedException {
        long otherActiveAdmins = users.stream()
                .filter(u -> u != target && u.isAdmin() && u.isActive())
                .count();
        if (otherActiveAdmins == 0) {
            throw new AccessDeniedException("Il doit rester au moins un administrateur actif.");
        }
    }
}
