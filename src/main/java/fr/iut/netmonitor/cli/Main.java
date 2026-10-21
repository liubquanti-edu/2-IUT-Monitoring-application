package fr.iut.netmonitor.cli;

import fr.iut.netmonitor.exception.AuthenticationException;
import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.NetworkDevice;
import fr.iut.netmonitor.model.Permission;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.service.AccessControl;
import fr.iut.netmonitor.service.NetMonitorContext;
import fr.iut.netmonitor.service.NetworkDeviceManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/** Version texte de NetMonitor (l'interface principale est désormais l'application JavaFX). */
public class Main {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final Scanner scanner = new Scanner(System.in);
    private final NetMonitorContext context;
    private final NetworkDeviceManager manager;
    private User user;

    public Main(NetMonitorContext context) {
        this.context = context;
        this.manager = context.devices();
    }

    public static void main(String[] args) {
        NetMonitorContext context = NetMonitorContext.open();
        if (context.getLoadWarning() != null) {
            System.out.println("Attention : " + context.getLoadWarning());
        }
        new Main(context).run();
    }

    public void run() {
        if (!login()) {
            System.out.println("Trop de tentatives. Au revoir.");
            return;
        }
        while (true) {
            System.out.println();
            System.out.println("=== NetMonitor === (" + user.getUsername() + " - " + user.getRole().getLabel() + ")");
            System.out.println("1. Ajouter un équipement");
            System.out.println("2. Afficher les équipements");
            System.out.println("3. Rechercher");
            System.out.println("4. Modifier un équipement");
            System.out.println("5. Supprimer un équipement");
            System.out.println("6. Détail d'un équipement");
            System.out.println("0. Enregistrer et quitter");
            String choice = prompt("Choix");
            try {
                switch (choice) {
                    case "1" -> add();
                    case "2" -> print(manager.listDevices());
                    case "3" -> print(manager.search(prompt("Nom, IP, localisation, type ou état")));
                    case "4" -> update();
                    case "5" -> remove();
                    case "6" -> detail();
                    case "0" -> {
                        save();
                        return;
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (NetMonitorException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    private boolean login() {
        for (int attempt = 0; attempt < MAX_LOGIN_ATTEMPTS; attempt++) {
            String login = prompt("Login");
            String password = prompt("Mot de passe");
            try {
                user = context.users().authenticate(login, password);
                return true;
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
            }
        }
        return false;
    }

    private void add() throws NetMonitorException {
        AccessControl.require(user, Permission.EDIT_DEVICES);
        String name = prompt("Nom");
        String ip = prompt("Adresse IP");
        DeviceType type = askType();
        String location = prompt("Localisation");
        NetworkDevice device = manager.addDevice(name, ip, type, location);
        System.out.println("Équipement ajouté : " + device);
    }

    private void update() throws NetMonitorException {
        AccessControl.require(user, Permission.EDIT_DEVICES);
        NetworkDevice device = manager.findDevice(askId());
        System.out.println("Laisser vide pour conserver la valeur actuelle.");
        String name = promptOrDefault("Nom", device.getName());
        String ip = promptOrDefault("Adresse IP", device.getIpAddress());
        DeviceType type = askTypeOrDefault(device.getType());
        String location = promptOrDefault("Localisation", device.getLocation());
        manager.updateDevice(device.getId(), name, ip, type, location);
        System.out.println("Équipement modifié : " + device);
    }

    private void remove() throws NetMonitorException {
        AccessControl.require(user, Permission.EDIT_DEVICES);
        int id = askId();
        NetworkDevice device = manager.findDevice(id);
        if (prompt("Supprimer " + device.getName() + " ? (o/n)").equalsIgnoreCase("o")) {
            manager.removeDevice(id);
            System.out.println("Équipement supprimé.");
        }
    }

    private void detail() throws NetMonitorException {
        NetworkDevice d = manager.findDevice(askId());
        System.out.println("Équipement : " + d.getName());
        System.out.println("Type       : " + d.getType().getLabel());
        System.out.println("Adresse IP : " + d.getIpAddress());
        System.out.println("Localisation : " + (d.getLocation().isEmpty() ? "-" : d.getLocation()));
        System.out.println("État       : " + d.getStatus());
        System.out.println("Dernier contact : " + (d.getLastContact() == null ? "jamais" : TIME.format(d.getLastContact())));
        System.out.println("Mesures    : " + d.getMeasurements().size());
        d.getEvents().forEach(e -> System.out.println("  " + e));
        d.getIncidents().forEach(i -> System.out.println("  " + i));
    }

    private void save() {
        try {
            context.save();
            System.out.println("Données enregistrées. Au revoir !");
        } catch (NetMonitorException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private void print(List<NetworkDevice> list) {
        if (list.isEmpty()) {
            System.out.println("Aucun équipement.");
            return;
        }
        System.out.printf("%-4s %-12s %-14s %-16s %-7s %-19s %s%n",
                "ID", "Nom", "Type", "Adresse IP", "État", "Dernier contact", "Localisation");
        for (NetworkDevice d : list) {
            String last = d.getLastContact() == null ? "-" : TIME.format(d.getLastContact());
            System.out.printf("%s %-19s %s%n", d, last, d.getLocation());
        }
    }

    private DeviceType askType() {
        DeviceType[] types = DeviceType.values();
        while (true) {
            for (int i = 0; i < types.length; i++) {
                System.out.println("  " + (i + 1) + ". " + types[i].getLabel());
            }
            try {
                int n = Integer.parseInt(prompt("Type"));
                if (n >= 1 && n <= types.length) {
                    return types[n - 1];
                }
            } catch (NumberFormatException ignored) {
                // redemande
            }
            System.out.println("Type invalide.");
        }
    }

    private DeviceType askTypeOrDefault(DeviceType current) {
        String answer = prompt("Changer le type (actuel : " + current.getLabel() + ") ? (o/n)");
        return answer.equalsIgnoreCase("o") ? askType() : current;
    }

    private int askId() throws NetMonitorException {
        String raw = prompt("Id de l'équipement");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new NetMonitorException("Id invalide : " + raw);
        }
    }

    private String prompt(String label) {
        System.out.print(label + " : ");
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "0";
    }

    private String promptOrDefault(String label, String current) {
        String value = prompt(label + " [" + current + "]");
        return value.isEmpty() ? current : value;
    }
}
