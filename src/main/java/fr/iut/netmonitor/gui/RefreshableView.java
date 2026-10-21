package fr.iut.netmonitor.gui;

import javafx.scene.Parent;

/** Écran de la fenêtre principale, rafraîchi périodiquement. */
public interface RefreshableView {

    Parent getView();

    void refresh();
}
