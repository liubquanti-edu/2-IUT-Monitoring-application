package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.User;
import fr.iut.netmonitor.service.AgentSimulator;
import fr.iut.netmonitor.service.NetMonitorContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * Application JavaFX : écran de connexion puis fenêtre principale.
 * Toutes les mises à jour (simulation des agents, rafraîchissement) se font sur le thread JavaFX
 * via une Timeline : chaque cycle est court et il n'y a donc pas d'accès concurrent aux données.
 */
public class NetMonitorApp extends Application {

    private static final Duration TICK = Duration.seconds(2);
    /** Enregistrement automatique toutes les 15 × 2 s. */
    private static final int AUTOSAVE_TICKS = 15;

    private NetMonitorContext context;
    private AgentSimulator simulator;
    private Scene scene;
    private Timeline timeline;
    private MainView mainView;
    private int ticks;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        context = NetMonitorContext.open();
        simulator = new AgentSimulator(context.devices(), context.monitoring());

        scene = new Scene(new LoginView(context.users(), this::showMain).getView(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("netmonitor.css").toExternalForm());
        stage.setTitle("NetMonitor");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        if (context.getLoadWarning() != null) {
            Dialogs.error(context.getLoadWarning());
        }

        timeline = new Timeline(new KeyFrame(TICK, e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tick() {
        simulator.tick(LocalDateTime.now());
        if (mainView != null) {
            mainView.refresh();
        }
        if (++ticks % AUTOSAVE_TICKS == 0) {
            saveQuietly();
        }
    }

    private void showMain(User user) {
        mainView = new MainView(new Session(context, user, this::save), this::showLogin);
        scene.setRoot(mainView.getView());
    }

    private void showLogin() {
        mainView = null;
        save();
        scene.setRoot(new LoginView(context.users(), this::showMain).getView());
    }

    /** Enregistrement après une action de l'utilisateur : une erreur lui est signalée. */
    private void save() {
        try {
            context.save();
        } catch (NetMonitorException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void saveQuietly() {
        try {
            context.save();
        } catch (NetMonitorException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        if (context != null) {
            saveQuietly();
        }
    }
}
