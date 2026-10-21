package fr.iut.netmonitor.gui;

/**
 * Point d'entrée « classique » : permet de lancer l'application depuis un IDE ou un jar
 * sans module-info (une classe qui étend Application ne peut pas être lancée directement
 * quand JavaFX est sur le classpath).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        NetMonitorApp.main(args);
    }
}
