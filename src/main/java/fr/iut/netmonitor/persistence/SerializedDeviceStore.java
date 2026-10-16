package fr.iut.netmonitor.persistence;

import fr.iut.netmonitor.exception.NetMonitorException;
import fr.iut.netmonitor.model.NetworkDevice;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SerializedDeviceStore implements DeviceStore {

    private final Path file;

    public SerializedDeviceStore(Path file) {
        this.file = file;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NetworkDevice> load() throws NetMonitorException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (List<NetworkDevice>) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new NetMonitorException("Impossible de lire le fichier de données " + file + ".", e);
        }
    }

    @Override
    public void save(List<NetworkDevice> devices) throws NetMonitorException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(new ArrayList<>(devices));
            }
        } catch (IOException e) {
            throw new NetMonitorException("Impossible d'enregistrer les données dans " + file + ".", e);
        }
    }
}
