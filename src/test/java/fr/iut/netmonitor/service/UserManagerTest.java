package fr.iut.netmonitor.service;

import fr.iut.netmonitor.exception.AccessDeniedException;
import fr.iut.netmonitor.exception.AuthenticationException;
import fr.iut.netmonitor.exception.ValidationException;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager users;
    private User admin;
    private User tech;

    @BeforeEach
    void setUp() throws Exception {
        users = new UserManager();
        admin = users.createUser("alice", "alice", UserRole.ADMIN);
        tech = users.createUser("bob", "bob1", UserRole.TECHNICIAN);
    }

    @Test
    void authenticateChecksPasswordAndActiveFlag() throws Exception {
        assertEquals(admin, users.authenticate("ALICE", "alice"));
        assertThrows(AuthenticationException.class, () -> users.authenticate("alice", "wrong"));
        assertThrows(AuthenticationException.class, () -> users.authenticate("nobody", "x"));
        users.setActive(admin, tech.getId(), false);
        assertThrows(AuthenticationException.class, () -> users.authenticate("bob", "bob1"));
    }

    @Test
    void rightsMatchRoleTable() {
        assertTrue(admin.can(Permission.EDIT_DEVICES));
        assertTrue(admin.can(Permission.MANAGE_USERS));
        assertFalse(tech.can(Permission.EDIT_DEVICES));
        assertFalse(tech.can(Permission.MANAGE_USERS));
        assertTrue(tech.can(Permission.HANDLE_INCIDENTS));
        assertFalse(UserRole.VIEWER.can(Permission.HANDLE_INCIDENTS));
        assertTrue(UserRole.VIEWER.can(Permission.VIEW_INCIDENTS));
    }

    @Test
    void onlyAdminManagesUsers() {
        assertThrows(AccessDeniedException.class, () -> users.addUser(tech, "carol", "carol", UserRole.VIEWER));
        assertThrows(AccessDeniedException.class, () -> users.removeUser(tech, admin.getId()));
    }

    @Test
    void rejectsDuplicateUsernameAndShortPassword() {
        assertThrows(ValidationException.class, () -> users.addUser(admin, "Bob", "secret", UserRole.VIEWER));
        assertThrows(ValidationException.class, () -> users.addUser(admin, "carol", "abc", UserRole.VIEWER));
    }

    @Test
    void lastAdminCannotBeRemovedOrDemoted() throws Exception {
        assertThrows(ValidationException.class, () -> users.removeUser(admin, admin.getId()));
        assertThrows(AccessDeniedException.class, () -> users.changeRole(admin, admin.getId(), UserRole.VIEWER));
        users.changeRole(admin, tech.getId(), UserRole.ADMIN);
        users.changeRole(admin, admin.getId(), UserRole.VIEWER);
        assertEquals(UserRole.VIEWER, admin.getRole());
    }

    @Test
    void changeOwnPasswordRequiresCurrentOne() throws Exception {
        assertThrows(AuthenticationException.class, () -> users.changeOwnPassword(tech, "bad", "newpass"));
        users.changeOwnPassword(tech, "bob1", "newpass");
        assertEquals(tech, users.authenticate("bob", "newpass"));
    }
}
