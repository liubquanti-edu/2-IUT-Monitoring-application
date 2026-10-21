package fr.iut.netmonitor.gui;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.DeviceType;
import fr.iut.netmonitor.model.NetworkDevice;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

/** Formulaire d'ajout ou de modification d'un équipement. */
public final class DeviceDialog {

    /** Enregistrement des valeurs saisies ; une exception garde le formulaire ouvert. */
    @FunctionalInterface
    public interface SaveAction {
        void save(String name, String ip, DeviceType type, String location) throws NetMonitorException;
    }

    private DeviceDialog() {
    }

    /** @return vrai si l'équipement a été enregistré */
    public static boolean show(String title, NetworkDevice device, SaveAction action) {
        TextField name = new TextField(device == null ? "" : device.getName());
        TextField ip = new TextField(device == null ? "" : device.getIpAddress());
        ip.setPromptText("ex. 192.168.1.20");
        ComboBox<DeviceType> type = new ComboBox<>();
        type.getItems().setAll(DeviceType.values());
        type.setConverter(new StringConverter<>() {
            @Override
            public String toString(DeviceType t) {
                return t == null ? "" : t.getLabel();
            }

            @Override
            public DeviceType fromString(String s) {
                return null;
            }
        });
        type.setValue(device == null ? null : device.getType());
        TextField location = new TextField(device == null ? "" : device.getLocation());
        location.setPromptText("ex. Bâtiment A - Salle serveurs");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setMaxWidth(340);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nom"), name);
        form.addRow(1, new Label("Adresse IP"), ip);
        form.addRow(2, new Label("Type"), type);
        form.addRow(3, new Label("Localisation"), location);
        form.add(error, 0, 4, 2, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // validation : si la saisie est refusée, on affiche l'erreur sans fermer la fenêtre
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                action.save(name.getText(), ip.getText(), type.getValue(), location.getText());
            } catch (NetMonitorException ex) {
                error.setText(ex.getMessage());
                e.consume();
            }
        });
        name.requestFocus();
        return dialog.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
